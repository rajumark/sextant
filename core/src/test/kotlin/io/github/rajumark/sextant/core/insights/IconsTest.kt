package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.CorpusApks
import io.github.rajumark.sextant.core.Extractor
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class IconsTest {
    private fun png(
        w: Int,
        h: Int,
    ) = ByteBuffer
        .allocate(32)
        .order(ByteOrder.BIG_ENDIAN)
        .apply {
            put(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
            putInt(13)
            put("IHDR".toByteArray())
            putInt(w)
            putInt(h)
        }.array()

    private fun webp(
        chunk: String,
        body: ByteArray,
    ) = "RIFF".toByteArray() + ByteArray(4) + "WEBP".toByteArray() + chunk.toByteArray() + ByteArray(4) + body

    @Test
    fun `pixel sizes from PNG and all three WebP flavours`() {
        Icons.dimensions(png(192, 144)) shouldBe (192 to 144)
        // VP8 (lossy): 3-byte frame tag, start code 9d 01 2a, then 14-bit width/height.
        val vp8 =
            byteArrayOf(0, 0, 0, 0x9d.toByte(), 0x01, 0x2a) +
                ByteBuffer
                    .allocate(4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putShort(108)
                    .putShort(108)
                    .array()
        Icons.dimensions(webp("VP8 ", vp8)) shouldBe (108 to 108)
        // VP8L (lossless): signature 0x2f, then width-1 (14 bits) and height-1 (14 bits).
        val bits = (64 - 1) or ((48 - 1) shl 14)
        Icons.dimensions(
            webp(
                "VP8L",
                byteArrayOf(0x2f) +
                    ByteBuffer
                        .allocate(4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(bits)
                        .array(),
            ),
        ) shouldBe
            (64 to 48)
        // VP8X (extended): 4 flag bytes, then 24-bit width-1 and height-1.
        val vp8x = ByteArray(4) + byteArrayOf(0xff.toByte(), 0x01, 0) + byteArrayOf(0x7f, 0, 0)
        Icons.dimensions(webp("VP8X", vp8x)) shouldBe (512 to 128)
        Icons.dimensions("<vector/>".toByteArray()) shouldBe (null to null)
    }

    @Test
    fun `density buckets`() {
        Icons.density("xxxhdpi-v4") shouldBe 640
        Icons.density("xhdpi") shouldBe 320
        Icons.density("anydpi-v26") shouldBe null
        Icons.density("") shouldBe 160
        Icons.density("480dpi") shouldBe 480
    }

    @Test
    fun `best is the largest bitmap, else a vector`() {
        val files =
            listOf(
                IconFile("res/a.xml", "anydpi-v26", null, "xml", null, null, 500),
                IconFile("res/m.png", "mdpi", 160, "png", 48, 48, 2000),
                IconFile("res/x.webp", "xxxhdpi", 640, "webp", 192, 192, 9000),
            )
        Icons.best(files) shouldBe "res/x.webp"
        Icons.best(files.take(1)) shouldBe "res/a.xml"
        Icons.best(emptyList()) shouldBe null
    }

    @Test
    fun `real APKs - obfuscated paths, adaptive layers, color background, inset foreground`() {
        val signal = Extractor.icons(CorpusApks.require("signal")).icon!!
        signal.best shouldBe "res/o-.png"
        signal.files.single { it.path == "res/o-.png" }.let { (it.width to it.height) shouldBe (192 to 192) }
        signal.adaptive!!.path shouldBe "res/BW.xml"
        Extractor
            .icons(CorpusApks.require("bitwarden"))
            .icon!!
            .adaptive!!
            .background!!
            .color shouldBe "#175ddc"
        Extractor
            .icons(CorpusApks.require("fluffychat"))
            .icon!!
            .adaptive!!
            .foreground!!
            .best shouldBe "res/CH.png"
    }
}
