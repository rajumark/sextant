package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.CorpusApks
import io.github.rajumark.sextant.core.ExitCode
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.model.AppMeta
import io.github.rajumark.sextant.core.output.OutputWriter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path

/** index.sqlite built from a real, obfuscated corpus APK (Binary Eye) must agree with the JSONL output. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IndexTest {
    private val dir: Path = Files.createTempDirectory("sextant-index")
    private val meta: AppMeta = OutputWriter.extractTo(CorpusApks.require("binaryeye"), dir, force = false).extraction.meta
    private val index = IndexReader.open(dir)

    private val onResume = "Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V"

    @AfterAll
    fun cleanup() {
        index.close()
        dir.toFile().deleteRecursively()
    }

    private fun scalar(sql: String) =
        index
            .query(sql)
            .rows
            .single()
            .single()

    @Test
    fun `row counts match the code stats`() {
        val code = meta.code!!
        scalar("SELECT count(*) FROM classes") shouldBe code.classes
        scalar("SELECT count(*) FROM methods WHERE defined = 1") shouldBe code.methods
        scalar("SELECT count(*) FROM fields") shouldBe code.fields
        scalar("SELECT count(*) FROM calls") shouldBe code.callEdges
        scalar("SELECT sum(count) FROM calls") shouldBe code.callSites
        scalar("SELECT sum(count) FROM string_refs") shouldBe code.stringRefs
        scalar("SELECT count(*) FROM strings") shouldBe code.distinctStrings
    }

    @Test
    fun `manifest tables are filled`() {
        scalar("SELECT value FROM meta WHERE key = 'package'") shouldBe "de.markusfisch.android.binaryeye"
        scalar("SELECT count(*) FROM components") shouldBe 18
        scalar("SELECT count(*) FROM permissions WHERE kind = 'implied'") shouldBe 1
        scalar("SELECT count(*) FROM deep_links") shouldBe 7
    }

    @Test
    fun `views give descriptor-based edges`() {
        val rows = index.query("SELECT callee, kind, count, offsets FROM call_edges WHERE caller = '$onResume'").rows
        rows shouldContain listOf("Landroid/app/Activity;->getIntent()Landroid/content/Intent;", "virtual", 1, "[84]")
        scalar("SELECT method FROM string_uses WHERE value = 'https://markusfisch.de/BinaryEye'") shouldBe onResume
    }

    @Test
    fun `external framework methods are referenced but not defined`() {
        scalar("SELECT defined FROM methods WHERE descriptor = 'Landroid/app/Activity;->getIntent()Landroid/content/Intent;'") shouldBe 0
        scalar("SELECT defined FROM methods WHERE descriptor = '$onResume'") shouldBe 1
    }

    @Test
    fun `search finds strings, methods and deep links and resolves them`() {
        val hits = index.search("markusfisch.de/BinaryEye")
        hits.map { it.kind }.toSet() shouldBe setOf("string", "deep_link", "endpoint")
        index.search("getIntent", kind = "method").map { it.ref } shouldContain
            "Landroid/app/Activity;->getIntent()Landroid/content/Intent;"
        index.search("CameraActivity", kind = "class").single().ref shouldBe "Lde/markusfisch/android/binaryeye/activity/CameraActivity;"
    }

    @Test
    fun `short queries fall back to LIKE`() {
        index.search("ss", kind = "permission").shouldNotBeEmpty()
    }

    @Test
    fun `class report`() {
        val r = index.classReport(index.findClasses("CameraActivity").single())
        r.methods.size shouldBe 23
        r.fields.size shouldBe 26
        r.strings shouldContain "https://markusfisch.de/BinaryEye"
    }

    @Test
    fun `the index is read-only`() {
        val e = shouldThrow<SextantException> { index.query("DELETE FROM classes") }
        e.exitCode shouldBe ExitCode.INPUT
        e.message!! shouldContain "readonly"
    }
}
