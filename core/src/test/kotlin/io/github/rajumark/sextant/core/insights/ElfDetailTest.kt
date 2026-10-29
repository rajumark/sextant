package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.CorpusApks
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test
import java.util.zip.ZipFile

class ElfDetailTest {
    private fun lib(path: String) =
        ZipFile(CorpusApks.require("binaryeye").toFile()).use { z -> z.getInputStream(z.getEntry(path)).use { it.readBytes() } }

    @Test
    fun `a real arm64 library is read in depth`() {
        val info = ElfDetail.read(lib("lib/arm64-v8a/libzxingcpp_android.so"))!!
        info.elfClass shouldBe "ELF64"
        info.machine shouldBe "arm64"
        info.needed shouldContain "libc.so"
        info.buildId!! shouldMatch Regex("[0-9a-f]{16,}")
        info.sections.map { it.name } shouldContain ".text"
        info.sections.map { it.name } shouldContain ".dynsym"
        info.symbols.count { it.defined && it.name.startsWith("Java_") } shouldBeGreaterThan 0
        info.symbols.count { !it.defined } shouldBeGreaterThan 0
        info.strings.size shouldBeGreaterThan 0
    }

    @Test
    fun `32-bit libraries and garbage`() {
        ElfDetail.read(lib("lib/armeabi-v7a/libzxingcpp_android.so"))!!.let {
            it.elfClass shouldBe "ELF32"
            it.machine shouldBe "arm"
            it.needed.size shouldNotBe 0
        }
        ElfDetail.read(ByteArray(100) { 7 }) shouldBe null
        ElfDetail.read(byteArrayOf(0x7f, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())) shouldBe null
    }
}
