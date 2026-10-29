package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.insights.ElfDetail
import io.github.rajumark.sextant.core.insights.ElfInfo
import io.github.rajumark.sextant.core.model.ApkFile
import io.github.rajumark.sextant.core.model.XmlNode
import java.util.zip.ZipFile

/** One binary XML file of the APK, decoded: the raw layer under every resource table. */
class XmlDoc(
    val path: String,
    val root: XmlNode,
)

/** One ELF file of the APK (native library, or ELF bytes anywhere else), read in depth. */
class ElfFile(
    val path: String,
    val info: ElfInfo,
)

/** The raw layer of an extraction: every binary XML file node by node, the resource table in every configuration, every ELF file. */
class RawLayer(
    val xml: List<XmlDoc> = emptyList(),
    val resourceTable: List<RawResource> = emptyList(),
    val elf: List<ElfFile> = emptyList(),
    val text: List<TextFile> = emptyList(),
    val signing: SigningDetail = SigningDetail(),
)

/** Reads every file whose bytes are ELF (lib/ and anything hidden elsewhere) with [ElfDetail]. */
object RawElf {
    // ELF files larger than this are listed and hashed, not parsed.
    private const val MAX_ELF_BYTES = 256L * 1024 * 1024

    fun read(
        zip: ZipFile,
        files: List<ApkFile>,
        warnings: MutableList<String>,
    ): List<ElfFile> {
        var failed = 0
        val out = mutableListOf<ElfFile>()
        for (f in files) {
            if (f.magic != "elf" || f.sizeBytes > MAX_ELF_BYTES) continue
            val entry = zip.getEntry(f.path) ?: continue
            val info = runCatching { zip.getInputStream(entry).use { ElfDetail.read(it.readBytes()) } }.getOrNull()
            if (info == null) failed++ else out += ElfFile(f.path, info)
        }
        if (failed > 0) warnings += "$failed ELF file${if (failed == 1) "" else "s"} could not be read"
        return out
    }
}

/**
 * Decodes every binary XML file in the APK (manifest, layouts, menus, drawables, color state lists, animations,
 * interpolators, res/xml configs, …), not only the kinds the resource harvester summarizes, so any view or attribute
 * can be queried. Files that do not decode are skipped and counted in a warning.
 */
object RawXml {
    // Same cap as the resource harvester: larger "XML" files are not real resources.
    private const val MAX_XML_BYTES = 8L * 1024 * 1024

    fun decode(
        zip: ZipFile,
        manifest: XmlNode,
        files: List<ApkFile>,
        decodeXml: (ByteArray) -> XmlNode?,
        warnings: MutableList<String>,
    ): List<XmlDoc> {
        var failed = 0
        val docs = mutableListOf(XmlDoc("AndroidManifest.xml", manifest))
        for (f in files) {
            if (f.magic != "binary-xml" || f.path == "AndroidManifest.xml" || f.sizeBytes > MAX_XML_BYTES) continue
            val entry = zip.getEntry(f.path) ?: continue
            val root = runCatching { zip.getInputStream(entry).use { decodeXml(it.readBytes()) } }.getOrNull()
            if (root == null) failed++ else docs += XmlDoc(f.path, root)
        }
        if (failed > 0) warnings += "$failed binary XML file${if (failed == 1) "" else "s"} could not be decoded"
        return docs
    }

    /**
     * A resource reference in an attribute value, normalized: `@+id/x` → `@id/x`. Theme attributes (`?attr/x`) and
     * framework resources (`@android:color/white`) are references too; plain values (`16.0dip`, `#ff000000`) are not.
     */
    fun ref(value: String): String? =
        when {
            value.startsWith("@+") -> "@" + value.removePrefix("@+")
            (value.startsWith("@") || value.startsWith("?")) && value.contains('/') -> value
            else -> null
        }
}
