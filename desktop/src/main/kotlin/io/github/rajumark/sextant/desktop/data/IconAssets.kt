package io.github.rajumark.sextant.desktop.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.core.Bundles
import io.github.rajumark.sextant.core.ManifestDecoder
import io.github.rajumark.sextant.core.model.XmlNode
import io.github.rajumark.sextant.desktop.model.ManifestNode
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile

/**
 * Reads icon files out of an APK: raw bytes, bitmaps, and compiled vector drawables (converted to Compose vectors).
 * Each APK is opened once and kept open while the app runs.
 */
object IconAssets {
    private val zips = ConcurrentHashMap<String, ZipFile>()
    private val decoders = ConcurrentHashMap<String, (ByteArray) -> XmlNode?>()

    // A split bundle is read through its merged APK, where the icon files of every split are.
    private fun zip(apk: Path): ZipFile = zips.computeIfAbsent(apk.toString()) { ZipFile(Bundles.open(apk).apk.toFile()) }

    /** The file's bytes inside the APK, or null when it is not there. */
    fun bytes(
        apk: Path,
        path: String,
    ): ByteArray? =
        runCatching {
            val z = zip(apk)
            z.getEntry(path)?.let { entry -> z.getInputStream(entry).use { it.readBytes() } }
        }.getOrNull()

    /** A compiled vector drawable as a Compose vector, or null when it cannot be read. */
    fun vector(
        apk: Path,
        path: String,
    ): ImageVector? {
        val xml = bytes(apk, path) ?: return null
        val decode = decoderFor(apk) ?: return null
        val root = runCatching { decode(xml) }.getOrNull() ?: return null
        return vectorOf(root)
    }

    /** The XML source of a binary XML file in the APK, as readable text; null when it cannot be decoded. */
    fun xmlText(
        apk: Path,
        path: String,
    ): String? {
        val xml = bytes(apk, path) ?: return null
        val decode = decoderFor(apk) ?: return null
        val root = runCatching { decode(xml) }.getOrNull() ?: return null
        return ManifestFormat.xml(toManifestNode(root))
    }

    private fun toManifestNode(n: XmlNode): ManifestNode = ManifestNode(n.tag, n.attributes.toList(), n.children.map(::toManifestNode))

    /** The APK's binary-XML decoder, built from its manifest and resource table once. */
    private fun decoderFor(apk: Path): ((ByteArray) -> XmlNode?)? =
        decoders[apk.toString()] ?: run {
            val manifest = bytes(apk, "AndroidManifest.xml") ?: return null
            val table = bytes(apk, "resources.arsc")
            val decoded = runCatching { ManifestDecoder.decode(manifest, table, mutableListOf()) }.getOrNull() ?: return null
            decoders.putIfAbsent(apk.toString(), decoded.decodeXml)
            decoded.decodeXml
        }

    /** Converts a decoded `<vector>` into an ImageVector. Plain path fills only; gradients and groups are not drawn. */
    internal fun vectorOf(root: XmlNode): ImageVector? {
        if (root.tag != "vector") return null
        val viewW = root.attributes["android:viewportWidth"]?.toFloatOrNull() ?: 24f
        val viewH = root.attributes["android:viewportHeight"]?.toFloatOrNull() ?: 24f
        val width = dimension(root.attributes["android:width"]) ?: viewW
        val height = dimension(root.attributes["android:height"]) ?: viewH
        val builder = ImageVector.Builder(defaultWidth = width.dp, defaultHeight = height.dp, viewportWidth = viewW, viewportHeight = viewH)
        collectPaths(root, builder)
        return builder.build()
    }

    private fun collectPaths(
        node: XmlNode,
        builder: ImageVector.Builder,
    ) {
        node.children.forEach { child ->
            when (child.tag) {
                "path" -> {
                    val data = child.attributes["android:pathData"] ?: return@forEach
                    val fill = colorOf(child.attributes["android:fillColor"]) ?: Color.Black
                    builder.addPath(
                        pathData = PathParser().parsePathString(data).toNodes(),
                        fill = SolidColor(fill),
                        fillAlpha = child.attributes["android:fillAlpha"]?.toFloatOrNull() ?: 1f,
                    )
                }
                "group" -> collectPaths(child, builder)
            }
        }
    }

    private fun dimension(text: String?): Float? = text?.removeSuffix("dp")?.toFloatOrNull()

    /** `#RRGGBB` or `#AARRGGBB`; resource references are not resolved here. */
    fun colorOf(text: String?): Color? {
        val hex = text?.removePrefix("#") ?: return null
        val value = hex.toLongOrNull(16) ?: return null
        return when (hex.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> null
        }
    }
}
