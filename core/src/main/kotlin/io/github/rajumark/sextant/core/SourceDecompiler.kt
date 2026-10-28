package io.github.rajumark.sextant.core

import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.core.dex.nodes.MethodNode
import java.nio.file.Files
import java.nio.file.Path

/** Decompiled Java for one class or method. */
data class DecompiledSource(
    /** The class or method that was asked for, as a smali descriptor. */
    val target: String,
    /** The top-level class whose code was decompiled (inner classes are printed inside their outer class). */
    val topClass: String,
    val code: String,
)

/**
 * Decompiles on demand with jadx. Only this is slow-ish (jadx parses every dex), so it is never part of `extract`.
 * Names are not deobfuscated, so they match the descriptors in the index.
 */
object SourceDecompiler {
    /**
     * jadx loaded for [apk] (a bundle is read through its merged APK). With a [mapping], decompiled code shows the
     * original names; lookups still use the obfuscated names, as in the index.
     */
    fun open(
        apk: Path,
        mapping: ProguardMapping? = null,
        threads: Int = 1,
    ): JadxDecompiler {
        if (!Files.isRegularFile(apk)) throw SextantException.input("$apk is not a file", "decompiling needs the APK itself")
        val args =
            JadxArgs().apply {
                setInputFile(Bundles.open(apk).apk.toFile())
                setSkipResources(true)
                setDeobfuscationOn(false)
                setShowInconsistentCode(true)
                setThreadsCount(threads)
                mapping?.let { setCodeData(it.codeData()) }
            }
        val jadx = JadxDecompiler(args)
        try {
            jadx.load()
        } catch (e: Exception) {
            jadx.close()
            throw SextantException.input("jadx could not load $apk: ${e.message}", cause = e)
        }
        return jadx
    }

    fun decompile(
        apk: Path,
        target: String,
        mapping: ProguardMapping? = null,
    ): DecompiledSource {
        // With a mapping, an original class name (com.app.Login) is accepted and looked up by its obfuscated name.
        val resolved =
            mapping?.let { m ->
                val (cls, member) = splitTarget(target)
                val dotted = CodeExtractor.className(cls)
                m.obfuscated(dotted)?.let { obf -> "L${obf.replace('.', '/')};" + (member?.let { "->$it" } ?: "") }
            } ?: target
        return open(apk, mapping).use { jadx ->
            // `com.a.B.m` is ambiguous: try it as a class first, then as class `com.a.B` + method `m`.
            val candidates = listOf(splitTarget(resolved)) + methodFallback(resolved)
            val found =
                candidates.firstNotNullOfOrNull { (c, m) ->
                    jadx.searchJavaClassOrItsParentByOrigFullName(CodeExtractor.className(c))?.let { Triple(c, m, it) }
                }
            if (found == null) {
                // jadx inlines anonymous and lambda classes (`Outer$1`) into the enclosing class: show that instead.
                val outer =
                    candidates.firstNotNullOfOrNull { (c, _) ->
                        generateSequence(c.removeSuffix(";")) { d -> d.substringBeforeLast('$', "").ifEmpty { null } }
                            .drop(1)
                            .firstNotNullOfOrNull { d ->
                                jadx.searchJavaClassByOrigFullName(CodeExtractor.className("$d;"))?.let {
                                    "$d;" to
                                        it
                                }
                            }
                    }
                        ?: throw SextantException.input(
                            "no class found for '$target' in $apk",
                            "check the name with `sextant find <dir> <name>`",
                        )
                return@use DecompiledSource(
                    target,
                    outer.first,
                    "// ${target.substringBefore("->")} is inlined by jadx; enclosing class:\n" + outer.second.code,
                )
            }
            val (classDescriptor, methodPart, cls) = found
            val dotted = CodeExtractor.className(classDescriptor)
            val top = generateSequence(cls) { it.declaringClass }.last()
            if (methodPart == null) {
                return@use DecompiledSource(classDescriptor, "L${top.rawName.replace('.', '/')};", top.code)
            }
            top.decompile()
            val methods = cls.methods.filter { matches(it.codeNodeRef as? MethodNode, methodPart) }
            if (methods.isEmpty()) {
                throw SextantException.input("method $methodPart not found in $dotted", "list methods with `sextant class <dir> $dotted`")
            }
            val code = methods.joinToString("\n\n") { it.codeStr.trimIndent() }
            DecompiledSource("$classDescriptor->$methodPart", "L${top.rawName.replace('.', '/')};", code)
        }
    }

    /** `Lcom/a/B;->m(I)V` → (`Lcom/a/B;`, `m(I)V`); `com.a.B.m` → (`Lcom/a/B;`, `m`); `com.a.B` → (`Lcom/a/B;`, null). */
    fun splitTarget(target: String): Pair<String, String?> =
        when {
            "->" in target -> target.substringBefore("->") to target.substringAfter("->")
            target.startsWith("L") && target.endsWith(";") -> target to null
            else -> "L${target.replace('.', '/')};" to null
        }

    private fun methodFallback(target: String): List<Pair<String, String?>> =
        if ("->" in target || target.startsWith("L") || '.' !in target) {
            emptyList()
        } else {
            listOf("L${target.substringBeforeLast('.').replace('.', '/')};" to target.substringAfterLast('.'))
        }

    /** `m(I)V` matches exactly; a bare `m` matches every overload. */
    private fun matches(
        node: MethodNode?,
        query: String,
    ): Boolean {
        val info = node?.methodInfo ?: return false
        return if ('(' in query) info.shortId == query else info.name == query
    }
}
