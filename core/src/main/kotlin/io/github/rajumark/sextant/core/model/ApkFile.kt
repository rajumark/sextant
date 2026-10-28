package io.github.rajumark.sextant.core.model

import kotlinx.serialization.Serializable

/** One entry of the APK zip: the bottom layer of the index, so every byte of the APK is accounted for. */
@Serializable
data class ApkFile(
    val path: String,
    /** manifest | resource-table | dex | resource | native-library | asset | signature | library-version | kotlin-metadata | service-loader | meta-inf | other */
    val type: String,
    val sizeBytes: Long,
    val compressedBytes: Long,
    /** stored | deflated */
    val method: String,
    /** CRC-32 from the zip header, hex */
    val crc32: String,
    /** Of the uncompressed bytes; null when the entry could not be read or is larger than the hashing cap. */
    val sha256: String?,
    /** What the bytes are, from their first bytes: dex | elf | zip | png | webp | jpeg | gif | binary-xml | resource-table | xml | json | font | text; null = unknown */
    val magic: String?,
    /** The resource this file is, from resources.arsc: `@layout/activity_main`. Shrunk apps rename files (`res/BW.xml`). */
    val resource: String? = null,
    /** Configuration of that resource: '' default, `xxxhdpi-v4`, `night`, … */
    val config: String? = null,
    /** The Sextant decoder that turned this file into rows: manifest, resource-table, code, layouts, menus, …; null = listed only. */
    val decodedBy: String? = null,
)
