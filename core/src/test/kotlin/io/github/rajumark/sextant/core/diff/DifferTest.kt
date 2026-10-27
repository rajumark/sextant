package io.github.rajumark.sextant.core.diff

import io.github.rajumark.sextant.core.CorpusApks
import io.github.rajumark.sextant.core.output.OutputWriter
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files

/** Binary Eye 1.75.3 → 1.75.5, both real F-Droid builds; facts checked by hand. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DifferTest {
    private val dir = Files.createTempDirectory("sextant-diff")
    private val report by lazy {
        OutputWriter.extractTo(CorpusApks.require("binaryeye-old"), dir.resolve("old"), force = false)
        OutputWriter.extractTo(CorpusApks.require("binaryeye"), dir.resolve("new"), force = false)
        Differ.diff(dir.resolve("old"), dir.resolve("new"))
    }

    private fun section(name: String) = report.sections.single { it.name == name }

    @AfterAll
    fun cleanup() {
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `labels name both versions`() {
        report.old shouldBe "de.markusfisch.android.binaryeye 1.75.3 (178)"
        report.new shouldBe "de.markusfisch.android.binaryeye 1.75.5 (180)"
    }

    @Test
    fun `app facts changed`() {
        section("app").changed shouldContain Change("version_code", "178", "180")
        section("app").changed shouldContain Change("android_gradle_plugin", "9.3.2", "9.4.1")
    }

    @Test
    fun `unchanged areas are empty`() {
        section("permissions").empty shouldBe true
        section("components").empty shouldBe true
    }

    @Test
    fun `removed library class is reported`() {
        section("classes").removed shouldContain "de.tsenger.vdstools.vds.tr03171.StatusIndicator"
    }

    @Test
    fun `comparing a version with itself finds nothing`() {
        Differ.diff(dir.resolve("new"), dir.resolve("new")).sections.all { it.empty } shouldBe true
    }
}
