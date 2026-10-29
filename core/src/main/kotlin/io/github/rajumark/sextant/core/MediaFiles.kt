package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.ApkFile
import java.util.zip.ZipFile

/** One image file of the APK, anywhere (not only drawables): format and pixel size from its header. */
class ImageFile(
    val path: String,
    val format: String,
    val width: Int?,
    val height: Int?,
)

/** One font file: the names from its `name` table. */
class FontFile(
    val path: String,
    val family: String?,
    val subfamily: String?,
    val fullName: String?,
    val version: String?,
    val postscriptName: String?,
)

/** Reads every image and font file in the APK from their headers; pixels and glyphs are never decoded. */
object MediaFiles {
    private val IMAGES = setOf("png", "webp", "jpeg", "gif")

    // Headers are at the start; JPEG markers can sit after large EXIF blocks, so read up to this much.
    private const val MAX_HEADER = 256 * 1024
    private const val MAX_FONT_BYTES = 32L * 1024 * 1024

    fun images(
        zip: ZipFile,
        files: List<ApkFile>,
    ): List<ImageFile> =
        files.filter { it.magic in IMAGES }.mapNotNull { f ->
            val entry = zip.getEntry(f.path) ?: return@mapNotNull null
            val head = runCatching { zip.getInputStream(entry).use { it.readNBytes(MAX_HEADER) } }.getOrNull() ?: return@mapNotNull null
            val (w, h) = size(f.magic!!, head)
            ImageFile(f.path, f.magic, w, h)
        }

    fun fonts(
        zip: ZipFile,
        files: List<ApkFile>,
    ): List<FontFile> =
        files.filter { it.magic == "font" && it.sizeBytes <= MAX_FONT_BYTES }.mapNotNull { f ->
            val entry = zip.getEntry(f.path) ?: return@mapNotNull null
            val bytes = runCatching { zip.getInputStream(entry).use { it.readBytes() } }.getOrNull() ?: return@mapNotNull null
            runCatching { font(f.path, bytes) }.getOrNull()
        }

    /** Width and height from the header of a PNG, WebP, JPEG or GIF; nulls when the header is cut or unusual. */
    internal fun size(
        format: String,
        b: ByteArray,
    ): Pair<Int?, Int?> {
        fun u8(i: Int) = b[i].toInt() and 0xff

        fun u16be(i: Int) = (u8(i) shl 8) or u8(i + 1)

        fun u16le(i: Int) = u8(i) or (u8(i + 1) shl 8)
        return try {
            when (format) {
                "png" -> if (b.size >= 24) ((u16be(16) shl 16) or u16be(18)) to ((u16be(20) shl 16) or u16be(22)) else null to null
                "gif" -> if (b.size >= 10) u16le(6) to u16le(8) else null to null
                "webp" ->
                    if (b.size < 30) {
                        null to null
                    } else {
                        when (String(b, 12, 4, Charsets.US_ASCII)) {
                            "VP8X" ->
                                ((u8(24) or (u8(25) shl 8) or (u8(26) shl 16)) + 1) to
                                    ((u8(27) or (u8(28) shl 8) or (u8(29) shl 16)) + 1)
                            "VP8L" -> {
                                val bits = u8(21) or (u8(22) shl 8) or (u8(23) shl 16) or (u8(24) shl 24)
                                ((bits and 0x3fff) + 1) to (((bits shr 14) and 0x3fff) + 1)
                            }
                            "VP8 " -> (u16le(26) and 0x3fff) to (u16le(28) and 0x3fff)
                            else -> null to null
                        }
                    }
                "jpeg" -> {
                    // Walk the markers to the first start-of-frame (SOF0–SOF15, except DHT C4, JPG C8, DAC CC).
                    var i = 2
                    var result: Pair<Int?, Int?> = null to null
                    while (i + 9 < b.size) {
                        if (u8(i) != 0xff) break
                        val marker = u8(i + 1)
                        if (marker == 0xd8 || marker in 0xd0..0xd7 || marker == 0x01) {
                            i += 2
                            continue
                        }
                        val length = u16be(i + 2)
                        if (marker in 0xc0..0xcf && marker != 0xc4 && marker != 0xc8 && marker != 0xcc) {
                            result = u16be(i + 7) to u16be(i + 5)
                            break
                        }
                        if (marker == 0xda || length < 2) break
                        i += 2 + length
                    }
                    result
                }
                else -> null to null
            }
        } catch (e: IndexOutOfBoundsException) {
            null to null
        }
    }

    /** The `name` table of a TrueType / OpenType font (Windows Unicode records first, then Macintosh Roman). */
    internal fun font(
        path: String,
        b: ByteArray,
    ): FontFile? {
        fun u16(i: Int) = ((b[i].toInt() and 0xff) shl 8) or (b[i + 1].toInt() and 0xff)

        fun u32(i: Int) = (u16(i).toLong() shl 16) or u16(i + 2).toLong()
        if (b.size < 12) return null
        val tables = u16(4)
        var nameOffset = -1
        for (t in 0 until tables) {
            val r = 12 + t * 16
            if (r + 16 > b.size) return null
            if (String(b, r, 4, Charsets.ISO_8859_1) == "name") nameOffset = u32(r + 8).toInt()
        }
        if (nameOffset < 0 || nameOffset + 6 > b.size) return null
        val count = u16(nameOffset + 2)
        val strings = nameOffset + u16(nameOffset + 4)
        val names = HashMap<Int, Pair<Int, String>>()
        for (k in 0 until count) {
            val r = nameOffset + 6 + k * 12
            if (r + 12 > b.size) break
            val platform = u16(r)
            val encoding = u16(r + 2)
            val language = u16(r + 4)
            val id = u16(r + 6)
            val length = u16(r + 8)
            val start = strings + u16(r + 10)
            if (start + length > b.size) continue
            val (rank, text) =
                when {
                    platform == 3 && (encoding == 1 || encoding == 10) ->
                        (if (language == 0x409) 0 else 1) to
                            String(b, start, length, Charsets.UTF_16BE)
                    platform == 0 -> 2 to String(b, start, length, Charsets.UTF_16BE)
                    platform == 1 && encoding == 0 -> 3 to String(b, start, length, Charsets.ISO_8859_1)
                    else -> continue
                }
            if (names[id]?.first?.let { it <= rank } != true) names[id] = rank to text
        }

        fun name(id: Int) = names[id]?.second?.trim()?.ifEmpty { null }
        return FontFile(path, name(1), name(2), name(4), name(5), name(6))
    }
}
