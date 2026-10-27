package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.output.CodeFiles
import io.github.rajumark.sextant.core.output.OutputWriter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/** The in-memory digests golden tests use must equal hashing the real code files byte for byte. */
class CodeDigestSinkTest {
    @TempDir
    lateinit var tmp: Path

    @Test
    fun `digests match the written code files`() {
        val sink = CodeDigestSink()
        OutputWriter.extractTo(CorpusApks.require("binaryeye"), tmp, force = false, includeIndex = false, extraCodeSink = sink)
        val fromFiles =
            CodeFiles.ALL.joinToString("") { name ->
                val file = tmp.resolve(name)
                val lines = Files.newBufferedReader(file).use { r -> r.lines().count() }
                "${ApkReader.sha256(file)}  ${"%9d".format(lines)}  $name\n"
            }
        sink.lines() shouldBe fromFiles
    }
}
