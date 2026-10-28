package io.github.rajumark.sextant.core

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.dexbacked.DexBackedClassDef
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.iface.value.BooleanEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ByteEncodedValue
import com.android.tools.smali.dexlib2.iface.value.CharEncodedValue
import com.android.tools.smali.dexlib2.iface.value.DoubleEncodedValue
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.iface.value.FloatEncodedValue
import com.android.tools.smali.dexlib2.iface.value.IntEncodedValue
import com.android.tools.smali.dexlib2.iface.value.LongEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ShortEncodedValue
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import io.github.rajumark.sextant.core.model.CallRecord
import io.github.rajumark.sextant.core.model.ClassRecord
import io.github.rajumark.sextant.core.model.CodeStats
import io.github.rajumark.sextant.core.model.FieldRecord
import io.github.rajumark.sextant.core.model.FieldRefRecord
import io.github.rajumark.sextant.core.model.MAX_OFFSETS
import io.github.rajumark.sextant.core.model.MethodRecord
import io.github.rajumark.sextant.core.model.Obfuscation
import io.github.rajumark.sextant.core.model.PackageCount
import io.github.rajumark.sextant.core.model.StringRecord
import io.github.rajumark.sextant.core.model.TypeRefRecord
import java.nio.file.Path
import java.util.zip.ZipFile

/** Receives code records in a deterministic order: classes sorted by id, members sorted within each class. */
interface CodeSink {
    fun classRecord(record: ClassRecord) {}

    fun method(record: MethodRecord) {}

    fun field(record: FieldRecord) {}

    fun call(record: CallRecord) {}

    fun string(record: StringRecord) {}

    fun fieldRef(record: FieldRefRecord) {}

    fun typeRef(record: TypeRefRecord) {}
}

/**
 * Reads every root `classesN.dex` with dexlib2 and streams classes, methods, fields, call edges and string
 * constants to a [CodeSink]. Nothing is decompiled, so this works even where Java decompilation fails.
 */
object CodeExtractor {
    private const val MAX_DEX_BYTES = 256L * 1024 * 1024
    private val DEX_ENTRY = Regex("""classes(\d*)\.dex""")

    fun extract(
        apk: Path,
        sink: CodeSink,
        warnings: MutableList<String>,
    ): CodeStats {
        val dexFiles = loadDexFiles(apk, warnings)

        // Android loads classes.dex first, then classes2.dex, ...; the first definition of a class wins.
        val classes = sortedMapOf<String, Pair<String, DexBackedClassDef>>()
        var duplicates = 0
        for ((name, dex) in dexFiles) {
            for (def in dex.classes) {
                if (classes.putIfAbsent(def.type, name to def) != null) duplicates++
            }
        }
        if (duplicates > 0) warnings += "$duplicates classes are defined in more than one dex file; the first definition is used"

        val counter = Counter()
        val packages = HashMap<String, Int>()
        for ((_, entry) in classes) {
            val (dexName, def) = entry
            val record = classRecord(def, dexName)
            packages.merge(record.`package`, 1, Int::plus)
            if (record.kotlin) counter.kotlin++
            if (looksRenamed(record.name)) counter.shortNamed++
            if (record.`package`.isEmpty()) counter.defaultPackage++
            val src = record.sourceFile
            if (src == null || src == "SourceFile" || src.startsWith("r8-map-id-")) counter.sourceStripped++
            sink.classRecord(record)
            counter.classes++

            for (field in def.fields.map { it to DexFormatter.INSTANCE.getFieldDescriptor(it) }.sortedBy { it.second }) {
                val (f, id) = field
                sink.field(
                    FieldRecord(
                        id = id,
                        `class` = def.type,
                        name = f.name,
                        type = f.type,
                        access = access(AccessFlags.formatAccessFlagsForField(f.accessFlags)),
                        initialValue = f.initialValue?.let(::encodedValue),
                    ),
                )
                counter.fields++
            }

            for ((method, id) in def.methods.map { it to DexFormatter.INSTANCE.getMethodDescriptor(it) }.sortedBy { it.second }) {
                method(method, id, def.type, sink, counter)
            }
        }
        if (counter.unreadable > 0) warnings += "${counter.unreadable} methods have bytecode that could not be fully read"

        return CodeStats(
            dexFiles = dexFiles.map { it.first },
            classes = counter.classes,
            methods = counter.methods,
            fields = counter.fields,
            callEdges = counter.callEdges,
            callSites = counter.callSites,
            stringRefs = counter.stringRefs,
            distinctStrings = counter.distinctStrings.size,
            fieldRefs = counter.fieldRefs,
            typeRefs = counter.typeRefs,
            kotlinClasses = counter.kotlin,
            duplicateClasses = duplicates,
            unreadableMethods = counter.unreadable,
            topPackages =
                packages.entries
                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                    .take(TOP_PACKAGES)
                    .map { PackageCount(it.key, it.value) },
            obfuscation =
                Obfuscation(
                    shortNamedClasses = counter.shortNamed,
                    defaultPackageClasses = counter.defaultPackage,
                    sourceFileStripped = counter.sourceStripped,
                    level =
                        when {
                            counter.classes == 0 -> "none"
                            counter.shortNamed * 100 / counter.classes > 30 -> "heavy"
                            counter.shortNamed * 100 / counter.classes >= 5 -> "light"
                            else -> "none"
                        },
                ),
        )
    }

    internal fun loadDexFiles(
        apk: Path,
        warnings: MutableList<String>,
    ): List<Pair<String, DexBackedDexFile>> =
        ZipFile(apk.toFile()).use { zip ->
            zip
                .entries()
                .asSequence()
                .mapNotNull { e -> DEX_ENTRY.matchEntire(e.name)?.let { m -> e to (m.groupValues[1].toIntOrNull() ?: 1) } }
                .sortedBy { it.second }
                .mapNotNull { (entry, _) ->
                    try {
                        val bytes = zip.getInputStream(entry).use { it.readNBytes((MAX_DEX_BYTES + 1).toInt()) }
                        if (bytes.size > MAX_DEX_BYTES) {
                            warnings += "${entry.name} is larger than ${MAX_DEX_BYTES / 1024 / 1024} MB; skipped"
                            null
                        } else {
                            entry.name to DexBackedDexFile(Opcodes.getDefault(), bytes)
                        }
                    } catch (e: Exception) {
                        warnings += "${entry.name} could not be read (${e.javaClass.simpleName}: ${e.message}); skipped"
                        null
                    }
                }.toList()
        }

    private fun classRecord(
        def: DexBackedClassDef,
        dexName: String,
    ): ClassRecord {
        val name = className(def.type)
        val annotations = def.annotations.map { it.type }.sorted()
        return ClassRecord(
            id = def.type,
            name = name,
            `package` = name.substringBeforeLast('.', ""),
            superclass = def.superclass,
            interfaces = def.interfaces.toList(),
            access = access(AccessFlags.formatAccessFlagsForClass(def.accessFlags)),
            sourceFile = def.sourceFile,
            dex = dexName,
            kotlin = "Lkotlin/Metadata;" in annotations,
            annotations = annotations,
            methods = def.methods.count(),
            fields = def.fields.count(),
        )
    }

    private fun method(
        method: Method,
        id: String,
        owner: String,
        sink: CodeSink,
        counter: Counter,
    ) {
        val calls = LinkedHashMap<Pair<String, String>, MutableList<Int>>()
        val strings = LinkedHashMap<String, MutableList<Int>>()
        val fieldRefs = LinkedHashMap<Pair<String, String>, MutableList<Int>>()
        val typeRefs = LinkedHashMap<Pair<String, String>, MutableList<Int>>()
        val impl = method.implementation
        var instructions = 0
        if (impl != null) {
            try {
                var offset = 0
                for (insn in impl.instructions) {
                    instructions++
                    if (insn is ReferenceInstruction) {
                        when (insn.referenceType) {
                            ReferenceType.METHOD -> {
                                val ref = insn.reference as MethodReference
                                val kind = invokeKind(insn.opcode.name)
                                calls.getOrPut(DexFormatter.INSTANCE.getMethodDescriptor(ref) to kind) { mutableListOf() } += offset
                            }
                            ReferenceType.STRING ->
                                strings.getOrPut((insn.reference as StringReference).string) { mutableListOf() } +=
                                    offset
                            ReferenceType.FIELD -> {
                                val ref = insn.reference as FieldReference
                                val kind = if (insn.opcode.name.let { it.startsWith("iput") || it.startsWith("sput") }) "write" else "read"
                                fieldRefs.getOrPut(DexFormatter.INSTANCE.getFieldDescriptor(ref) to kind) { mutableListOf() } += offset
                            }
                            ReferenceType.TYPE -> {
                                typeKind(insn.opcode.name)?.let { kind ->
                                    typeRefs.getOrPut((insn.reference as TypeReference).type to kind) { mutableListOf() } += offset
                                }
                            }
                            else -> Unit
                        }
                    }
                    offset += insn.codeUnits
                }
            } catch (e: Exception) {
                counter.unreadable++
            }
        }

        sink.method(
            MethodRecord(
                id = id,
                `class` = owner,
                name = method.name,
                params = method.parameterTypes.map { it.toString() },
                returnType = method.returnType,
                access = access(AccessFlags.formatAccessFlagsForMethod(method.accessFlags)),
                hasCode = impl != null,
                instructions = instructions,
                registers = impl?.registerCount ?: 0,
                callsOut = calls.values.sumOf { it.size },
                strings = strings.values.sumOf { it.size },
            ),
        )
        counter.methods++

        for ((key, offsets) in calls.entries.sortedWith(compareBy({ it.key.first }, { it.key.second }))) {
            sink.call(CallRecord(id, key.first, key.second, offsets.size, offsets.take(MAX_OFFSETS)))
            counter.callEdges++
            counter.callSites += offsets.size
        }
        for ((value, offsets) in strings.entries.sortedBy { it.key }) {
            sink.string(StringRecord(value, id, offsets.size, offsets.take(MAX_OFFSETS)))
            counter.stringRefs += offsets.size
            counter.distinctStrings += value
        }
        for ((key, offsets) in fieldRefs.entries.sortedWith(compareBy({ it.key.first }, { it.key.second }))) {
            sink.fieldRef(FieldRefRecord(id, key.first, key.second, offsets.size, offsets.take(MAX_OFFSETS)))
            counter.fieldRefs++
        }
        for ((key, offsets) in typeRefs.entries.sortedWith(compareBy({ it.key.first }, { it.key.second }))) {
            sink.typeRef(TypeRefRecord(id, key.first, key.second, offsets.size, offsets.take(MAX_OFFSETS)))
            counter.typeRefs++
        }
    }

    /** `new-instance` → `new`, `const-class` → `class`, `check-cast` → `cast`, `instance-of` → `instanceof`, arrays → `array`. */
    private fun typeKind(opcode: String): String? =
        when {
            opcode.startsWith("new-instance") -> "new"
            opcode.startsWith("const-class") -> "class"
            opcode.startsWith("check-cast") -> "cast"
            opcode.startsWith("instance-of") -> "instanceof"
            opcode.startsWith("new-array") || opcode.startsWith("filled-new-array") -> "array"
            else -> null
        }

    /** `invoke-virtual/range` → `virtual`, `invoke-static` → `static`, ... */
    private fun invokeKind(opcode: String) =
        opcode
            .removePrefix("invoke-")
            .substringBefore('/')
            .removeSuffix("-quick")

    private fun encodedValue(value: EncodedValue): String? =
        when (value) {
            is StringEncodedValue -> value.value
            is IntEncodedValue -> value.value.toString()
            is LongEncodedValue -> value.value.toString()
            is BooleanEncodedValue -> value.value.toString()
            is ByteEncodedValue -> value.value.toString()
            is ShortEncodedValue -> value.value.toString()
            is CharEncodedValue -> value.value.toString()
            is FloatEncodedValue -> Decimals.format(value.value)
            is DoubleEncodedValue -> Decimals.format(value.value)
            else -> null
        }

    private fun access(formatted: String) = formatted.split(' ').filter { it.isNotEmpty() }

    /** `Lcom/app/Foo$Bar;` → `com.app.Foo$Bar` */
    fun className(descriptor: String) = descriptor.removePrefix("L").removeSuffix(";").replace('/', '.')

    private const val TOP_PACKAGES = 15

    private val RENAMED = Regex("[a-z]{1,3}[0-9]{0,2}")

    /** `a.b.c`, `yi0` or `a.b$c`: the outer simple name is what R8/ProGuard give renamed classes. */
    internal fun looksRenamed(className: String): Boolean = RENAMED.matches(className.substringAfterLast('.').substringBefore('$'))

    private class Counter {
        var classes = 0
        var methods = 0
        var fields = 0
        var callEdges = 0
        var callSites = 0
        var stringRefs = 0
        var kotlin = 0
        var unreadable = 0
        var fieldRefs = 0
        var typeRefs = 0
        var shortNamed = 0
        var defaultPackage = 0
        var sourceStripped = 0
        val distinctStrings = HashSet<String>()
    }
}
