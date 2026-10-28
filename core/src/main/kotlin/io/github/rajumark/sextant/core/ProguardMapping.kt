package io.github.rajumark.sextant.core

import jadx.api.data.ICodeRename
import jadx.api.data.IJavaNodeRef.RefType
import jadx.api.data.impl.JadxCodeData
import jadx.api.data.impl.JadxCodeRename
import jadx.api.data.impl.JadxNodeRef
import java.nio.file.Files
import java.nio.file.Path

/**
 * A ProGuard / R8 `mapping.txt`: original names for obfuscated classes, fields and methods. Member signatures are
 * kept in dex form (`b(La/c;I)V`) as they appear in the obfuscated APK, so they match jadx and the index directly.
 */
class ProguardMapping private constructor(
    /** obfuscated class name (dotted) → original class name. */
    val classes: Map<String, String>,
    /** obfuscated class → (obfuscated field `name:Ltype;` → original field name). */
    val fields: Map<String, Map<String, String>>,
    /** obfuscated class → (obfuscated method `name(args)ret` → original method name). */
    val methods: Map<String, Map<String, String>>,
) {
    private val byOriginal = classes.entries.associate { (obf, orig) -> orig to obf }

    /** The original name of an obfuscated class, or null when the mapping does not rename it. */
    fun original(obfClass: String): String? = classes[obfClass]?.takeIf { it != obfClass }

    /** The obfuscated name of an original class, or null when it is not in the mapping. */
    fun obfuscated(originalClass: String): String? = byOriginal[originalClass]

    /** Every entry whose original or obfuscated name contains [text], as "original -> obfuscated" lines. */
    fun lookup(text: String): List<String> =
        buildList {
            for ((obf, orig) in classes) {
                if (orig.contains(text, true) || obf.contains(text, true)) add("$orig -> $obf")
                fields[obf]?.forEach { (o, n) ->
                    if (n.contains(text, true) ||
                        o.substringBefore(':').equals(text, true)
                    ) {
                        add("$orig.$n -> $obf.$o")
                    }
                }
                methods[obf]?.forEach { (o, n) -> if (n.contains(text, true)) add("$orig.$n -> $obf.$o") }
            }
        }

    /** The renames as jadx code data: decompiled code then shows original names (jadx-gui does the same with a project). */
    fun codeData(): JadxCodeData {
        val renames = mutableListOf<ICodeRename>()
        for ((obf, orig) in classes) {
            if (obf != orig) renames += JadxCodeRename(JadxNodeRef.forCls(obf), orig)
            fields[obf]?.forEach { (id, name) ->
                if (id.substringBefore(':') != name) renames += JadxCodeRename(JadxNodeRef(RefType.FIELD, obf, id), name)
            }
            methods[obf]?.forEach { (id, name) ->
                if (id.substringBefore('(') != name) renames += JadxCodeRename(JadxNodeRef(RefType.METHOD, obf, id), name)
            }
        }
        return JadxCodeData().apply { setRenames(renames) }
    }

    val size: Int get() = classes.size

    /** A short hash of the whole mapping, so caches built with different mappings never mix. */
    val fingerprint: String by lazy {
        val text =
            buildString {
                classes.toSortedMap().forEach { (k, v) -> append(k).append('=').append(v).append('\n') }
                fields.toSortedMap().forEach { (k, v) -> append(k).append(v.toSortedMap()).append('\n') }
                methods.toSortedMap().forEach { (k, v) -> append(k).append(v.toSortedMap()).append('\n') }
            }
        java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(text.toByteArray())
            .toHex()
            .take(12)
    }

    companion object {
        private val CLASS_LINE = Regex("""^(\S+) -> (\S+):\s*$""")

        // "    1:5:void run(int,a.b):10:14 -> c" or "    int count -> a". Line ranges are optional.
        private val MEMBER_LINE = Regex("""^\s+(?:\d+:\d+:)?(\S+) ([^\s(]+)(\(([^)]*)\))?(?::\d+(?::\d+)?)? -> (\S+)\s*$""")

        fun read(path: Path): ProguardMapping {
            if (!Files.isRegularFile(path)) throw SextantException.input("mapping file not found: $path")
            return parse(Files.readAllLines(path))
        }

        fun parse(lines: List<String>): ProguardMapping {
            val classes = LinkedHashMap<String, String>()
            // Members are kept with original types until every class is known, then converted to obfuscated dex form.
            val rawFields = HashMap<String, MutableList<Triple<String, String, String>>>()
            val rawMethods = HashMap<String, MutableList<Pair<String, Triple<String, String, String>>>>()
            var current: String? = null
            for (line in lines) {
                if (line.isBlank() || line.trimStart().startsWith("#")) continue
                val c = CLASS_LINE.matchEntire(line)
                if (c != null && !line.startsWith(" ")) {
                    val (orig, obf) = c.destructured
                    classes[obf] = orig
                    current = obf
                    continue
                }
                val cls = current ?: continue
                val m = MEMBER_LINE.matchEntire(line) ?: continue
                val type = m.groupValues[1]
                val name = m.groupValues[2]
                val obf = m.groupValues[5]
                // Inlined frames ("a.b.c.m" names) describe callers, not members of this class.
                if ('.' in name) continue
                if (m.groupValues[3].isEmpty()) {
                    rawFields.getOrPut(cls) { mutableListOf() } += Triple(obf, type, name)
                } else {
                    rawMethods.getOrPut(cls) { mutableListOf() } += obf to Triple(type, m.groupValues[4], name)
                }
            }
            val toObf = classes.entries.associate { (obf, orig) -> orig to obf }

            fun desc(javaType: String): String {
                var t = javaType.trim()
                var dims = 0
                while (t.endsWith("[]")) {
                    dims++
                    t = t.removeSuffix("[]")
                }
                val base =
                    when (t) {
                        "void" -> "V"
                        "boolean" -> "Z"
                        "byte" -> "B"
                        "char" -> "C"
                        "short" -> "S"
                        "int" -> "I"
                        "long" -> "J"
                        "float" -> "F"
                        "double" -> "D"
                        else -> "L" + (toObf[t] ?: t).replace('.', '/') + ";"
                    }
                return "[".repeat(dims) + base
            }
            val fields =
                rawFields.mapValues { (_, list) -> list.associate { (obf, type, name) -> "$obf:${desc(type)}" to name } }
            val methods =
                rawMethods.mapValues { (_, list) ->
                    list.associate { (obf, sig) ->
                        val (ret, args, name) = sig
                        val argDesc = args.split(',').filter { it.isNotBlank() }.joinToString("") { desc(it) }
                        "$obf($argDesc)${desc(ret)}" to name
                    }
                }
            return ProguardMapping(classes, fields, methods)
        }
    }
}
