package io.github.rajumark.sextant.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Broken or hostile inputs must fail with a clear message and exit code 2, never a stack trace. */
class BadInputTest {
    @TempDir
    lateinit var dir: Path

    private fun expectInputError(path: Path): SextantException {
        val e = shouldThrow<SextantException> { Extractor.extract(path) }
        e.exitCode shouldBe ExitCode.INPUT
        return e
    }

    private fun zip(
        name: String,
        vararg entries: Pair<String, ByteArray>,
    ): Path {
        val path = dir.resolve(name)
        ZipOutputStream(Files.newOutputStream(path)).use { zip ->
            for ((entry, bytes) in entries) {
                zip.putNextEntry(ZipEntry(entry))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return path
    }

    @Test
    fun `missing file`() {
        expectInputError(dir.resolve("nope.apk")).message!! shouldContain "not found"
    }

    @Test
    fun `directory instead of file`() {
        expectInputError(dir).message!! shouldContain "directory"
    }

    @Test
    fun `empty file`() {
        expectInputError(Files.createFile(dir.resolve("empty.apk"))).message!! shouldContain "not a valid APK"
    }

    @Test
    fun `text file renamed to apk`() {
        expectInputError(Files.writeString(dir.resolve("notes.apk"), "hello, I am not an APK")).message!! shouldContain "not a valid APK"
    }

    @Test
    fun `zip without a manifest`() {
        expectInputError(zip("plain.apk", "readme.txt" to "hi".toByteArray())).message!! shouldContain "no AndroidManifest.xml"
    }

    @Test
    fun `manifest that is not binary XML`() {
        expectInputError(zip("junk.apk", "AndroidManifest.xml" to ByteArray(4096) { (it * 31).toByte() })).message!! shouldContain
            "could not be decoded"
    }

    @Test
    fun `zip bomb manifest is rejected before it is fully inflated`() {
        // 40 MB of zeros compresses to ~40 KB; the cap is 32 MB.
        val bomb = zip("bomb.apk", "AndroidManifest.xml" to ByteArray(40 * 1024 * 1024))
        Files.size(bomb) shouldBeLessThanMb 1
        expectInputError(bomb).message!! shouldContain "zip bomb"
    }

    @Test
    fun `truncated real APK`() {
        val source = CorpusApks.path("binaryeye") ?: return
        val bytes = Files.readAllBytes(source)
        val truncated = Files.write(dir.resolve("truncated.apk"), bytes.copyOf(bytes.size / 2))
        expectInputError(truncated)
    }

    private infix fun Long.shouldBeLessThanMb(mb: Int) = (this < mb * 1024L * 1024L) shouldBe true
}
