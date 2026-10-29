package io.github.rajumark.sextant.core.model

import kotlinx.serialization.Serializable

// Version 2: code structure read straight from DEX bytecode (no decompilation).
// IDs are smali-style descriptors, the format every Android tool understands:
//   class  Lcom/app/Login;
//   method Lcom/app/Login;->auth(Ljava/lang/String;)Z
//   field  Lcom/app/Login;->token:Ljava/lang/String;

@Serializable
data class ClassRecord(
    val id: String,
    val name: String,
    val `package`: String,
    val superclass: String?,
    val interfaces: List<String>,
    val access: List<String>,
    val sourceFile: String?,
    val dex: String,
    val kotlin: Boolean,
    val annotations: List<String>,
    val methods: Int,
    val fields: Int,
)

@Serializable
data class MethodRecord(
    val id: String,
    val `class`: String,
    val name: String,
    val params: List<String>,
    val returnType: String,
    val access: List<String>,
    val hasCode: Boolean,
    val instructions: Int,
    val registers: Int,
    val callsOut: Int,
    val strings: Int,
)

@Serializable
data class FieldRecord(
    val id: String,
    val `class`: String,
    val name: String,
    val type: String,
    val access: List<String>,
    val initialValue: String?,
)

/** One line per distinct (caller, callee, kind). Offsets are in 16-bit code units, like dexdump; capped at [MAX_OFFSETS]. */
@Serializable
data class CallRecord(
    val caller: String,
    val callee: String,
    val kind: String,
    val count: Int,
    val offsets: List<Int>,
)

/** One line per distinct (string constant, method that loads it). */
@Serializable
data class StringRecord(
    val value: String,
    val method: String,
    val count: Int,
    val offsets: List<Int>,
)

/** One line per distinct (method, field, kind): `read` (iget/sget) or `write` (iput/sput). */
@Serializable
data class FieldRefRecord(
    val method: String,
    val field: String,
    val kind: String,
    val count: Int,
    val offsets: List<Int>,
)

/** One line per distinct (method, type, kind): `new`, `class` (const-class), `cast`, `instanceof`, `array`. */
@Serializable
data class TypeRefRecord(
    val method: String,
    val type: String,
    val kind: String,
    val count: Int,
    val offsets: List<Int>,
)

/**
 * A resource the code uses: [key] is the resource id loaded as a constant (`0x7f0e001c`) or the `R` field read
 * (`Lcom/app/R$string;->title:I`); [via] is the first call within 8 instructions after it, usually the one it is passed to.
 */
@Serializable
data class ResourceUseRecord(
    val method: String,
    val key: String,
    val via: String?,
    val count: Int,
    val offsets: List<Int>,
)

/** One annotation on a class, method, field or method parameter, with its element values. */
@Serializable
data class AnnotationRecord(
    /** The class, method or field descriptor it is on. */
    val target: String,
    /** class | method | field | parameter */
    val targetKind: String,
    /** For parameter annotations: the parameter index. */
    val parameter: Int?,
    /** `Lkotlin/Metadata;` */
    val type: String,
    /** build | runtime | system */
    val visibility: String,
    val elements: List<AnnotationElement>,
)

@Serializable
data class AnnotationElement(
    val name: String,
    /** Strings and numbers plain; arrays, types, enums and nested annotations in smali syntax. */
    val value: String,
)

/** What a method's code says beyond instructions: parameter names, the source line table, exception handlers. */
@Serializable
data class MethodDetailRecord(
    val method: String,
    /** From debug info; null where the build stripped it. */
    val parameterNames: List<String?>,
    /** (code offset, source line) pairs, in code order. */
    val lines: List<Pair<Int, Int>>,
    val handlers: List<HandlerRecord>,
)

/** Every instruction of one method, in code order (only built for sinks that want them, see CodeSink.wantsInstructions). */
@Serializable
data class MethodCodeRecord(
    val method: String,
    val instructions: List<InstructionRow>,
)

/** One instruction: its offset, opcode, literal or branch target, and what it references. */
@Serializable
data class InstructionRow(
    /** In 16-bit code units, as in calls.offsets and dexdump. */
    val offset: Int,
    /** Index into the opcodes table. */
    val op: Int,
    /** const* and other literal instructions. */
    val literal: Long?,
    /** Branches, switches and fill-array-data: the absolute offset they go to. */
    val target: Int?,
    /** method | field | type | string | call_site | proto | handle */
    val refKind: String?,
    /** Descriptor (method, field, type) or the string value. */
    val ref: String?,
)

/** A try block's handler: code units [start, end) jump to [handler] for [type] (null = catch-all / finally). */
@Serializable
data class HandlerRecord(
    val start: Int,
    val end: Int,
    val handler: Int,
    val type: String?,
)

@Serializable
data class CodeStats(
    val dexFiles: List<String>,
    val classes: Int,
    val methods: Int,
    val fields: Int,
    val callEdges: Int,
    val callSites: Int,
    val stringRefs: Int,
    val distinctStrings: Int,
    val fieldRefs: Int = 0,
    val typeRefs: Int = 0,
    val kotlinClasses: Int,
    val duplicateClasses: Int,
    val unreadableMethods: Int,
    val topPackages: List<PackageCount>,
    /** How much of the code was renamed by R8/ProGuard. */
    val obfuscation: Obfuscation? = null,
)

/** "heavy · 61% short class names · 2,301 in the default package · source files stripped (99%)", or null for older folders. */
fun CodeStats.obfuscationText(): String? {
    val o = obfuscation ?: return null

    fun pct(n: Int) = if (classes == 0) "0%" else "${n * 100 / classes}%"
    return buildList {
        add(o.level)
        add("${pct(o.shortNamedClasses)} short class names")
        if (o.defaultPackageClasses > 0) add("%,d in the default package".format(o.defaultPackageClasses))
        if (o.sourceFileStripped * 2 > classes) add("source files stripped (${pct(o.sourceFileStripped)})")
    }.joinToString(" · ")
}

/**
 * Signs of obfuscation, counted over every class. Short names (`a`, `yi0`) and classes moved to the default package
 * come from renaming; a stripped source file (`SourceFile`, `r8-map-id-…`) only hides file names.
 */
@Serializable
data class Obfuscation(
    val shortNamedClasses: Int,
    val defaultPackageClasses: Int,
    val sourceFileStripped: Int,
    /** none, light (5–30% short names) or heavy (over 30%). */
    val level: String,
)

@Serializable
data class PackageCount(
    val `package`: String,
    val classes: Int,
)

const val MAX_OFFSETS = 32
