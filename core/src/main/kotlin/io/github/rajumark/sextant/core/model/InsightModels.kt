package io.github.rajumark.sextant.core.model

import kotlinx.serialization.Serializable

// Version 5: insights derived from the APK, each with the evidence it was based on.

/** A library with exact Maven coordinates, from `META-INF/<group>_<artifact>.version` files shipped in the APK. */
@Serializable
data class LibraryRecord(
    val group: String,
    val artifact: String,
    val version: String,
    val evidence: String,
)

/** An app framework or engine (Flutter, React Native, .NET MAUI, Unity, ...). */
@Serializable
data class FrameworkRecord(
    val name: String,
    val evidence: List<String>,
)

/** A known SDK matched by package prefix and/or manifest entries. */
@Serializable
data class SdkRecord(
    val name: String,
    val category: String,
    val classes: Int,
    val packages: List<String>,
    val manifest: List<String>,
)

/** A URL found in the app, with where it was found. */
@Serializable
data class EndpointRecord(
    val url: String,
    val scheme: String,
    val host: String,
    val sources: List<EndpointSource>,
)

@Serializable
data class EndpointSource(
    /** code | resource | deep_link | manifest | asset | native */
    val kind: String,
    /** Method descriptor, resource name (@string/x), component name, meta-data name or file path. */
    val where: String,
)

/** A simple resource value from the default configuration. */
@Serializable
data class ResourceValue(
    val type: String,
    val name: String,
    val value: String,
)

@Serializable
data class NativeLibrary(
    val path: String,
    val abi: String,
    val name: String,
    val sizeBytes: Long,
    val sha256: String,
    val elfClass: String?,
    val exportedSymbols: Int,
    val jniMethods: Int,
)

/** An exported, defined function symbol of a native library. JNI symbols are mapped back to Java. */
@Serializable
data class NativeSymbol(
    val library: String,
    val name: String,
    val jniClass: String?,
    val jniMethod: String?,
)

@Serializable
data class InsightStats(
    val libraries: Int,
    val frameworks: List<String>,
    val sdks: Int,
    val sdkCategories: Map<String, Int>,
    val endpoints: Int,
    val hosts: Int,
    val topHosts: List<HostCount>,
    val resourceValues: Int,
    val locales: List<String>,
    val nativeLibraries: Int,
    val jniMethods: Int,
    val androidGradlePlugin: String?,
    /** Review findings per severity (high, medium, low, info). */
    val findings: Map<String, Int> = emptyMap(),
)

@Serializable
data class HostCount(
    val host: String,
    val urls: Int,
)
