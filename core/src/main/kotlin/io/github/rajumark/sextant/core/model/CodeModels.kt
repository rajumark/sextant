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
)

@Serializable
data class PackageCount(
    val `package`: String,
    val classes: Int,
)

const val MAX_OFFSETS = 32
