package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.ResourceFile
import io.github.rajumark.sextant.core.ResourceResolver
import io.github.rajumark.sextant.core.android
import io.github.rajumark.sextant.core.child
import io.github.rajumark.sextant.core.model.XmlNode
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/** The app's launcher icons, resolved from the manifest through resources.arsc to real files in the APK. */
@Serializable
data class AppIcons(
    /** `android:icon` of the application. */
    val icon: IconRef?,
    /** `android:roundIcon` of the application. */
    val roundIcon: IconRef?,
)

@Serializable
data class IconRef(
    /** The manifest value, e.g. `@mipmap/ic_launcher`. */
    val resource: String,
    /** Best file to show: the largest bitmap, else a vector/XML drawable. Null when nothing resolves. */
    val best: String?,
    /** Every file behind the resource, one per configuration. */
    val files: List<IconFile>,
    /** Set when the API 26+ file is an `<adaptive-icon>`. */
    val adaptive: AdaptiveIcon?,
)

@Serializable
data class IconFile(
    val path: String,
    /** Resource configuration, e.g. `xxxhdpi-v4` or `anydpi-v26`; empty for the default. */
    val config: String,
    /** Density in dpi (mdpi = 160 … xxxhdpi = 640); null for anydpi/nodpi. */
    val density: Int?,
    /** png | webp | jpg | gif | xml */
    val format: String,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long,
)

@Serializable
data class AdaptiveIcon(
    /** The `<adaptive-icon>` XML file. */
    val path: String,
    val background: IconLayer?,
    val foreground: IconLayer?,
    val monochrome: IconLayer?,
)

@Serializable
data class IconLayer(
    /** The drawable reference, e.g. `@mipmap/ic_launcher_foreground` or `@color/ic_launcher_background`. */
    val resource: String,
    /** For color layers: the color value, e.g. `#ff3a76f0`. */
    val color: String?,
    val best: String?,
    val files: List<IconFile>,
)

object Icons {
    private val DENSITIES =
        listOf(
            "ldpi" to 120,
            "mdpi" to 160,
            "tvdpi" to 213,
            "hdpi" to 240,
            "xhdpi" to 320,
            "xxhdpi" to 480,
            "xxxhdpi" to 640,
        )
    private val BITMAPS = setOf("png", "webp", "jpg", "gif")

    fun resolve(
        zip: ZipFile,
        manifest: XmlNode,
        resources: ResourceResolver,
        decodeXml: (ByteArray) -> XmlNode?,
    ): AppIcons {
        val app = manifest.child("application")
        return AppIcons(
            icon = app?.android("icon")?.let { ref(it, zip, resources, decodeXml) },
            roundIcon = app?.android("roundIcon")?.let { ref(it, zip, resources, decodeXml) },
        )
    }

    private fun ref(
        resource: String,
        zip: ZipFile,
        resources: ResourceResolver,
        decodeXml: (ByteArray) -> XmlNode?,
    ): IconRef {
        val files = files(resources.files(resource), zip)
        val adaptive =
            files.firstOrNull { it.format == "xml" }?.let { xmlFile ->
                val root = zip.getEntry(xmlFile.path)?.let { e -> zip.getInputStream(e).use { decodeXml(it.readBytes()) } }
                if (root?.tag != "adaptive-icon") return@let null
                AdaptiveIcon(
                    path = xmlFile.path,
                    background = root.child("background")?.let { layer(it, zip, resources) },
                    foreground = root.child("foreground")?.let { layer(it, zip, resources) },
                    monochrome = root.child("monochrome")?.let { layer(it, zip, resources) },
                )
            }
        return IconRef(resource, best(files), files, adaptive)
    }

    private fun layer(
        node: XmlNode,
        zip: ZipFile,
        resources: ResourceResolver,
    ): IconLayer? {
        // The drawable is either on the layer itself or on a wrapper inside it (<inset>, <bitmap>, <layer-list><item>).
        val drawable = node.android("drawable") ?: firstDrawable(node) ?: return null
        val files = files(resources.files(drawable), zip)
        val color = if (files.isEmpty()) resources.resolve(drawable)?.takeIf { it.startsWith("#") } else null
        return IconLayer(drawable, color, best(files), files)
    }

    private fun firstDrawable(node: XmlNode): String? {
        for (child in node.children) return child.android("drawable") ?: firstDrawable(child) ?: continue
        return null
    }

    /** The largest bitmap by pixel area (then density), else the first XML (vector) drawable. */
    fun best(files: List<IconFile>): String? =
        files
            .filter { it.format in BITMAPS }
            .maxWithOrNull(compareBy<IconFile>({ (it.width ?: 0).toLong() * (it.height ?: 0) }, { it.density ?: 0 }, { it.sizeBytes }))
            ?.path
            ?: files.firstOrNull { it.format == "xml" }?.path

    private fun files(
        resolved: List<ResourceFile>,
        zip: ZipFile,
    ): List<IconFile> =
        resolved.mapNotNull { f ->
            val entry = zip.getEntry(f.path) ?: return@mapNotNull null
            val format =
                f.path
                    .substringAfterLast('.', "")
                    .lowercase()
                    .let { if (it == "jpeg") "jpg" else it }
            val head = zip.getInputStream(entry).use { it.readNBytes(64) }
            val (w, h) = dimensions(head)
            IconFile(f.path, f.config, density(f.config), format, w, h, entry.size)
        }

    fun density(config: String): Int? {
        val parts = config.split('-')
        if ("anydpi" in parts || "nodpi" in parts) return null
        DENSITIES.firstOrNull { (name, _) -> name in parts }?.let { return it.second }
        Regex("""^(\d+)dpi$""")
            .let { re ->
                parts.firstNotNullOfOrNull {
                    re
                        .matchEntire(it)
                        ?.groupValues
                        ?.get(1)
                        ?.toInt()
                }
            }?.let { return it }
        return 160 // no density qualifier: the default (mdpi) bucket
    }

    /** Pixel size from the header of a PNG or WebP (lossy, lossless, extended); null for anything else. */
    fun dimensions(head: ByteArray): Pair<Int?, Int?> {
        val b = ByteBuffer.wrap(head)

        fun ascii(
            at: Int,
            len: Int,
        ) = if (head.size >= at + len) String(head, at, len, Charsets.ISO_8859_1) else ""
        return when {
            head.size >= 24 && head[0] == 0x89.toByte() && ascii(1, 3) == "PNG" -> b.order(ByteOrder.BIG_ENDIAN).getInt(16) to b.getInt(20)
            ascii(0, 4) == "RIFF" && ascii(8, 4) == "WEBP" -> {
                b.order(ByteOrder.LITTLE_ENDIAN)
                when (ascii(12, 4)) {
                    "VP8 " ->
                        if (head.size >=
                            30
                        ) {
                            (b.getShort(26).toInt() and 0x3fff) to (b.getShort(28).toInt() and 0x3fff)
                        } else {
                            null to null
                        }
                    "VP8L" ->
                        if (head.size >= 25) {
                            val bits = b.getInt(21)
                            ((bits and 0x3fff) + 1) to (((bits shr 14) and 0x3fff) + 1)
                        } else {
                            null to null
                        }
                    "VP8X" ->
                        if (head.size >= 30) {
                            fun u24(at: Int) =
                                (head[at].toInt() and 0xff) or ((head[at + 1].toInt() and 0xff) shl 8) or
                                    ((head[at + 2].toInt() and 0xff) shl 16)
                            (u24(24) + 1) to (u24(27) + 1)
                        } else {
                            null to null
                        }
                    else -> null to null
                }
            }
            else -> null to null
        }
    }
}
