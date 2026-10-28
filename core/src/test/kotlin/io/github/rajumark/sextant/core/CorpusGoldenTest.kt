package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.index.IndexReader
import io.github.rajumark.sextant.core.insights.MenuPreviews
import io.github.rajumark.sextant.core.output.OutputFiles
import io.github.rajumark.sextant.core.output.OutputReader
import io.github.rajumark.sextant.core.output.OutputWriter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Golden-file tests over the real-APK corpus.
 * - Manifest-level files must match fixtures/golden/<id>/ byte for byte.
 * - Insight files that stay small (libraries, frameworks, SDKs, endpoints, native libraries) are compared byte for byte too.
 * - Large files (code, resource values, native symbols) are pinned by line count and sha256 in fixtures/golden/<id>/digests.sha256.
 * - index.sqlite is pinned by row counts per table in fixtures/golden/<id>/index.counts (stable across SQLite versions).
 * Regenerate with `./gradlew updateGolden` and review the diff.
 */
class CorpusGoldenTest {
    @TempDir
    lateinit var tmp: Path

    private val update = System.getProperty("sextant.updateGolden") == "true"

    // The tool version changes every release; keep it out of the golden files. Only where the tool writes it:
    // a plain replace would also hit library versions that happen to be equal (androidx 1.0.0 vs sextant 1.0.0).
    private fun normalize(text: String) =
        text
            .replace(Regex("""("name": "sextant",\s*"version": ")${Regex.escape(Sextant.version)}""""), "$1<version>\"")
            .replace("sextant ${Sextant.version} ·", "sextant <version> ·")

    private val compared =
        OutputFiles.ALL +
            listOf(OutputFiles.LIBRARIES, OutputFiles.FRAMEWORKS, OutputFiles.SDKS, OutputFiles.ENDPOINTS, OutputFiles.NATIVE_LIBRARIES)
    private val digestedFiles = listOf(OutputFiles.RESOURCES, OutputFiles.NATIVE_SYMBOLS) + OutputFiles.HARVEST

    private fun digests(
        out: Path,
        code: CodeDigestSink,
    ) = code.lines() +
        digestedFiles.joinToString("") { name ->
            val file = out.resolve(name)
            val lines = Files.newBufferedReader(file).use { r -> r.lines().count() }
            "${ApkReader.sha256(file)}  ${"%9d".format(lines)}  $name\n"
        }

    private fun indexCounts(out: Path): String =
        IndexReader.open(out).use { index ->
            val tables = index.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'search%' ORDER BY name").rows
            val counts = tables.map { (name) -> "$name ${index.query("SELECT count(*) FROM \"$name\"").rows.single().single()}" }
            val search = "search ${index.query("SELECT count(*) FROM search_docsize").rows.single().single()}"
            (counts + search).joinToString("\n", postfix = "\n")
        }

    /** Every menu laid out as its host shows it: one reviewable line per menu. */
    private fun menuPreviews(out: Path): String {
        val r = OutputReader.read(out).resources
        return r.menus.joinToString("") { m ->
            val p = MenuPreviews.of(m, r.layouts)
            val bar = p.toolbar.joinToString(",") { it.id?.substringAfter('/') ?: "?" }
            "${m.name}${m.config.takeIf { it.isNotEmpty() }?.let { "-$it" } ?: ""}  ${p.host}  bar=[$bar]  " +
                "overflow=${p.overflow.size}  hidden=${p.hidden.size}  untitled=${p.untitled}\n"
        }
    }

    @TestFactory
    fun `output matches golden files`(): List<DynamicTest> =
        // Some golden folders are kept out of git (Signal embeds a Google API key verbatim); skip those when absent.
        CorpusApks.ids.filter { update || Files.isDirectory(CorpusApks.goldenDir.resolve(it)) }.map { id ->
            DynamicTest.dynamicTest(id) {
                // One APK at a time: its own folder, deleted right after the check, and code hashed instead of written,
                // so the whole corpus needs about one APK's index on disk (a class-level folder used to hold all 46).
                val out = Files.createTempDirectory(tmp, id)
                val code = CodeDigestSink()
                val actual =
                    try {
                        OutputWriter.extractTo(CorpusApks.require(id), out, force = false, writeCodeFiles = false, extraCodeSink = code)
                        compared.associate { it.replace('/', '_') to normalize(out.resolve(it).readText()) } +
                            ("digests.sha256" to digests(out, code)) + ("index.counts" to indexCounts(out)) +
                            ("menu_previews.txt" to menuPreviews(out))
                    } finally {
                        out.toFile().deleteRecursively()
                    }
                val golden = CorpusApks.goldenDir.resolve(id)
                for ((name, text) in actual) {
                    val expectedFile = golden.resolve(name)
                    if (update) {
                        Files.createDirectories(golden)
                        Files.writeString(expectedFile, text)
                    } else {
                        check(Files.exists(expectedFile)) { "no golden file $expectedFile: run ./gradlew updateGolden" }
                        withClue("$id/$name differs from golden (run ./gradlew updateGolden and review the diff)") {
                            text shouldBe expectedFile.readText()
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `two runs produce byte-identical output`(): List<DynamicTest> =
        listOf("binaryeye", "jitsi", "minetest").map { id ->
            DynamicTest.dynamicTest(id) {
                val apk = CorpusApks.require(id)
                val a = tmp.resolve("$id-a")
                val b = tmp.resolve("$id-b/nested")
                try {
                    val first = OutputWriter.extractTo(apk, a, force = false).files
                    val second = OutputWriter.extractTo(apk, b, force = false).files
                    first.size shouldBe second.size
                    first.zip(second).forEach { (x, y) -> withClue("${x.fileName}") { Files.mismatch(x, y) shouldBe -1L } }
                } finally {
                    a.toFile().deleteRecursively()
                    tmp.resolve("$id-b").toFile().deleteRecursively()
                }
            }
        }
}
