package io.github.rajumark.sextant.core

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import java.util.zip.ZipFile

class MediaFilesTest {
    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun `image sizes from headers`() {
        val png = ByteArray(24).also { b -> bytes(0x89, 'P'.code, 'N'.code, 'G'.code).copyInto(b) }
        png[18] = 0x01
        png[19] = 0x2c // width 300
        png[22] = 0x00
        png[23] = 0xc8.toByte() // height 200
        MediaFiles.size("png", png) shouldBe (300 to 200)
        MediaFiles.size("gif", "GIF89a".toByteArray() + bytes(0x40, 0x01, 0xf0, 0x00)) shouldBe (320 to 240)
        // SOI, an APP0 segment, then SOF0 with height 480 and width 640.
        val jpeg = bytes(0xff, 0xd8, 0xff, 0xe0, 0x00, 0x04, 0x00, 0x00, 0xff, 0xc0, 0x00, 0x11, 0x08, 0x01, 0xe0, 0x02, 0x80, 0x03)
        MediaFiles.size("jpeg", jpeg) shouldBe (640 to 480)
        MediaFiles.size("jpeg", bytes(0xff, 0xd8)) shouldBe (null to null)
    }

    @Test
    fun `font names from a real font`() {
        val font =
            ZipFile(CorpusApks.require("nextcloud").toFile()).use { z ->
                MediaFiles.font("res/RV.ttf", z.getInputStream(z.getEntry("res/RV.ttf")).use { it.readBytes() })
            }!!
        font.family shouldBe "Roboto Medium"
        font.subfamily shouldBe "Regular"
        font.version!! shouldStartWith "Version 2.137"
        MediaFiles.font("x", ByteArray(8)) shouldBe null
    }
}
