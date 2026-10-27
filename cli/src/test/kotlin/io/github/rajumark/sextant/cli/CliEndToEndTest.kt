package io.github.rajumark.sextant.cli

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/** Runs the shipped fat jar exactly as a user would and checks stdout, stderr and exit codes. */
class CliEndToEndTest {
    @TempDir
    lateinit var tmp: Path

    private data class Result(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    )

    private val root: Path =
        generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.exists(it.resolve("settings.gradle.kts")) }

    private fun apk(id: String): String {
        val path = root.resolve("fixtures/external/$id.apk")
        check(Files.isRegularFile(path)) { "corpus APK '$id' missing: run scripts/fetch-apks.sh" }
        return path.toString()
    }

    private fun sextant(
        vararg args: String,
        cwd: Path = tmp,
    ): Result {
        val jar = System.getProperty("sextant.jar") ?: error("sextant.jar system property not set")
        val java = File(System.getProperty("java.home"), "bin/java").path
        val process =
            ProcessBuilder(listOf(java, "-jar", jar) + args)
                .directory(cwd.toFile())
                .redirectError(tmp.resolve("stderr.txt").toFile())
                .start()
        val stdout = process.inputStream.bufferedReader().readText()
        check(process.waitFor(120, TimeUnit.SECONDS)) { "sextant timed out" }
        return Result(process.exitValue(), stdout, Files.readString(tmp.resolve("stderr.txt")))
    }

    @Test
    fun `--version prints the tool name and version`() {
        val result = sextant("--version")
        result.exitCode shouldBe 0
        result.stdout shouldContain Regex("""^sextant version \d+\.\d+\.\d+""")
    }

    @Test
    fun `--help lists the commands`() {
        val result = sextant("--help")
        result.exitCode shouldBe 0
        result.stdout shouldContain "extract"
        result.stdout shouldContain "info"
    }

    @Test
    fun `extract writes manifest files, code files and the index, progress goes to stderr`() {
        val out = tmp.resolve("out")
        val result = sextant("extract", apk("binaryeye"), "-o", out.toString())
        result.exitCode shouldBe 0
        result.stdout.shouldBeEmpty()
        result.stderr shouldContain "de.markusfisch.android.binaryeye"
        Files.list(out).use { files -> files.map { it.fileName.toString() }.toList() } shouldContainExactlyInAnyOrder
            listOf(
                "summary.md",
                "meta.json",
                "manifest.json",
                "permissions.jsonl",
                "components.jsonl",
                "code",
                "index.sqlite",
                "insights",
                "resources",
                "native",
            )
        Files.list(out.resolve("code")).use { files -> files.map { it.fileName.toString() }.toList() } shouldContainExactlyInAnyOrder
            listOf("classes.jsonl", "methods.jsonl", "fields.jsonl", "calls.jsonl", "strings.jsonl", "field_refs.jsonl", "type_refs.jsonl")
        result.stderr shouldContain "Code "
    }

    @Test
    fun `--no-code skips code files and removes stale ones`() {
        val out = tmp.resolve("nocode")
        sextant("extract", "-q", apk("jitsi"), "-o", out.toString()).exitCode shouldBe 0
        Files.isRegularFile(out.resolve("code/calls.jsonl")) shouldBe true
        sextant("extract", "-q", "--force", "--no-code", apk("jitsi"), "-o", out.toString()).exitCode shouldBe 0
        Files.exists(out.resolve("code/calls.jsonl")) shouldBe false
        sextant("info", out.toString()).stdout shouldContain "not extracted"
    }

    @Test
    fun `extract defaults to apk-name dot sextant in the current directory`() {
        sextant("extract", "--quiet", apk("jitsi")).exitCode shouldBe 0
        Files.isRegularFile(tmp.resolve("jitsi.sextant/meta.json")) shouldBe true
    }

    @Test
    fun `--quiet prints nothing`() {
        val result = sextant("extract", "-q", apk("jitsi"), "-o", tmp.resolve("q").toString())
        result.exitCode shouldBe 0
        result.stdout.shouldBeEmpty()
        result.stderr.shouldBeEmpty()
    }

    @Test
    fun `non-empty output directory needs --force, and --force keeps foreign files`() {
        val out = Files.createDirectories(tmp.resolve("busy"))
        Files.writeString(out.resolve("mine.txt"), "keep me")
        val refused = sextant("extract", apk("jitsi"), "-o", out.toString())
        refused.exitCode shouldBe 3
        refused.stderr shouldContain "--force"

        sextant("extract", "-q", "--force", apk("jitsi"), "-o", out.toString()).exitCode shouldBe 0
        Files.readString(out.resolve("mine.txt")) shouldBe "keep me"
        Files.isRegularFile(out.resolve("meta.json")) shouldBe true
    }

    @Test
    fun `info on a folder and on an APK give the same answer`() {
        val out = tmp.resolve("seal")
        sextant("extract", "-q", apk("seal"), "-o", out.toString()).exitCode shouldBe 0
        val fromFolder = sextant("info", out.toString())
        val fromApk = sextant("info", apk("seal"))
        fromFolder.exitCode shouldBe 0
        fromFolder.stdout shouldContain "com.junkfood.seal"
        fromFolder.stdout shouldBe fromApk.stdout
    }

    @Test
    fun `info --json prints only one JSON object on stdout`() {
        val result = sextant("info", "--json", apk("newpipe"))
        result.exitCode shouldBe 0
        val json = Json.parseToJsonElement(result.stdout.trim()).jsonObject
        json
            .getValue("meta")
            .jsonObject
            .getValue("package")
            .jsonPrimitive.content shouldBe "org.schabi.newpipe"
        json
            .getValue("counts")
            .jsonObject
            .getValue("exported")
            .jsonPrimitive.int shouldBe 9
    }

    @Test
    fun `missing APK exits 2 with a message and no stack trace`() {
        val result = sextant("extract", tmp.resolve("missing.apk").toString(), "-o", tmp.resolve("x").toString())
        result.exitCode shouldBe 2
        result.stderr shouldContain "file not found"
        result.stderr shouldNotContain "\tat "
    }

    @Test
    fun `non-APK input exits 2`() {
        val fake = Files.writeString(tmp.resolve("fake.apk"), "not a zip")
        val result = sextant("info", fake.toString())
        result.exitCode shouldBe 2
        result.stderr shouldContain "not a valid APK"
    }

    @Test
    fun `info on a folder that is not an extraction exits 2 with a hint`() {
        val result = sextant("info", Files.createDirectories(tmp.resolve("empty")).toString())
        result.exitCode shouldBe 2
        result.stderr shouldContain "sextant extract"
    }

    @Test
    fun `usage errors exit 1`() {
        sextant("extract").exitCode shouldBe 1
        sextant("frobnicate").exitCode shouldBe 1
    }

    private fun extracted(id: String): String {
        val out = tmp.resolve("$id-x")
        if (!Files.exists(out)) sextant("extract", "-q", apk(id), "-o", out.toString()).exitCode shouldBe 0
        return out.toString()
    }

    @Test
    fun `sql prints a table, row count on stderr`() {
        val result = sextant("sql", extracted("binaryeye"), "SELECT type, count(*) AS n FROM components GROUP BY type ORDER BY type")
        result.exitCode shouldBe 0
        result.stdout.lines().first() shouldContain "type"
        result.stdout shouldContain "activity"
        result.stderr shouldContain "rows"
    }

    @Test
    fun `sql --json prints one object per row, --csv prints a header`() {
        val dir = extracted("binaryeye")
        val json = sextant("sql", dir, "SELECT value FROM meta WHERE key = 'package'", "--json")
        Json
            .parseToJsonElement(json.stdout.trim())
            .jsonObject
            .getValue("value")
            .jsonPrimitive.content shouldBe
            "de.markusfisch.android.binaryeye"
        sextant("sql", dir, "SELECT key FROM meta ORDER BY key LIMIT 1", "--csv").stdout shouldBe "key\nindex_schema_version\n"
    }

    @Test
    fun `sql --schema lists tables and views`() {
        val result = sextant("sql", extracted("binaryeye"), "--schema")
        result.exitCode shouldBe 0
        result.stdout shouldContain "CREATE TABLE methods"
        result.stdout shouldContain "CREATE VIEW call_edges"
    }

    @Test
    fun `sql errors and writes exit 2`() {
        val dir = extracted("binaryeye")
        sextant("sql", dir, "SELECT * FROM nope").exitCode shouldBe 2
        sextant("sql", dir, "DROP TABLE classes").stderr shouldContain "readonly"
        sextant("sql", dir).exitCode shouldBe 1
    }

    @Test
    fun `find shows a string and the method that loads it`() {
        val result = sextant("find", extracted("binaryeye"), "markusfisch.de/BinaryEye", "--kind", "string")
        result.exitCode shouldBe 0
        result.stdout shouldContain "https://markusfisch.de/BinaryEye"
        result.stdout shouldContain "CameraActivity;->onResume()V"
    }

    @Test
    fun `find --json gives used_in`() {
        val line = sextant("find", extracted("binaryeye"), "markusfisch.de/BinaryEye", "--kind", "string", "--json").stdout.lines().first()
        val json = Json.parseToJsonElement(line).jsonObject
        json.getValue("kind").jsonPrimitive.content shouldBe "string"
        json.getValue("used_in").toString() shouldContain "onResume"
    }

    @Test
    fun `class shows methods and strings, ambiguous names list candidates`() {
        val dir = extracted("binaryeye")
        val result = sextant("class", dir, "CameraActivity")
        result.exitCode shouldBe 0
        result.stdout shouldContain "Methods (23)"
        result.stdout shouldContain "https://markusfisch.de/BinaryEye"
        sextant("class", dir, "NoSuchClassAnywhere").exitCode shouldBe 2
    }

    @Test
    fun `query commands explain a missing index`() {
        val out = tmp.resolve("noindex")
        sextant("extract", "-q", "--no-index", apk("jitsi"), "-o", out.toString()).exitCode shouldBe 0
        val result = sextant("find", out.toString(), "jitsi")
        result.exitCode shouldBe 2
        result.stderr shouldContain "sextant extract"
    }

    @Test
    fun `callers and callees print trees`() {
        val dir = extracted("binaryeye")
        val callees = sextant("callees", dir, "CameraActivity.onResume", "--depth", "1")
        callees.exitCode shouldBe 0
        callees.stdout shouldContain "Landroid/app/Activity;->getIntent()Landroid/content/Intent;  ×1  (external)"
        val callers = sextant("callers", dir, "Landroid/app/Activity;->getIntent", "--depth", "1", "--json")
        callers.exitCode shouldBe 0
        callers.stdout shouldContain "PickActivity;->onCreate"
    }

    @Test
    fun `ambiguous method names list candidates, unknown ones exit 2`() {
        val dir = extracted("binaryeye")
        val result = sextant("callers", dir, "onResume")
        result.exitCode shouldBe 0
        result.stderr shouldContain "methods match"
        result.stdout shouldContain "CameraActivity;->onResume"
        sextant("callees", dir, "noSuchMethodAnywhere").exitCode shouldBe 2
    }

    @Test
    fun `source prints decompiled Java, unknown classes exit 2`() {
        val result = sextant("source", apk("binaryeye"), "Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V")
        result.exitCode shouldBe 0
        result.stdout shouldContain "welcome_shown"
        sextant("source", apk("binaryeye"), "com.example.Nope").exitCode shouldBe 2
    }

    @Test
    fun `insights and endpoints commands`() {
        val dir = extracted("binaryeye")
        val insights = sextant("insights", dir)
        insights.exitCode shouldBe 0
        insights.stdout shouldContain "## Frameworks"
        insights.stdout shouldContain "## Sdks"
        val json = Json.parseToJsonElement(sextant("insights", dir, "--json").stdout.trim()).jsonObject
        json.keys shouldContainExactlyInAnyOrder listOf("frameworks", "sdks", "top_hosts", "libraries", "native")
        val endpoints = sextant("endpoints", dir, "--host", "markusfisch.de")
        endpoints.exitCode shouldBe 0
        endpoints.stdout shouldContain "https://markusfisch.de/BinaryEye"
        endpoints.stdout shouldContain "CameraActivity;->onResume()V"
    }

    @Test
    fun `extract writes insight files`() {
        val dir = Paths.get(extracted("binaryeye"))
        for (f in listOf("insights/libraries.jsonl", "insights/sdks.jsonl", "insights/endpoints.jsonl", "resources/values.jsonl")) {
            Files.isRegularFile(dir.resolve(f)) shouldBe true
        }
    }

    @Test
    fun `mcp speaks JSON-RPC on stdio and keeps stdout clean`() {
        val jar = System.getProperty("sextant.jar")!!
        val java = File(System.getProperty("java.home"), "bin/java").path
        val process = ProcessBuilder(java, "-jar", jar, "mcp").redirectError(tmp.resolve("mcp-err.txt").toFile()).start()
        process.outputStream.bufferedWriter().use { w ->
            w.write("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18"}}""" + "\n")
            w.write("""{"jsonrpc":"2.0","method":"notifications/initialized"}""" + "\n")
            w.write("""{"jsonrpc":"2.0","id":2,"method":"tools/list"}""" + "\n")
        }
        val lines = process.inputStream.bufferedReader().readLines()
        process.waitFor(60, TimeUnit.SECONDS) shouldBe true
        lines.size shouldBe 2
        lines.forEach { Json.parseToJsonElement(it) }
        lines[1] shouldContain "\"callers\""
    }

    @Test
    fun `diff compares two versions`() {
        val old = extracted("binaryeye-old")
        val new = extracted("binaryeye")
        val result = sextant("diff", old, new, "--show", "3")
        result.exitCode shouldBe 0
        result.stdout shouldContain "1.75.3 (178)  →  de.markusfisch.android.binaryeye 1.75.5 (180)"
        result.stdout shouldContain "~ version_code: 178  →  180"
        result.stdout shouldContain "Unchanged:"
        val json = Json.parseToJsonElement(sextant("diff", old, new, "--json").stdout.trim()).jsonObject
        json.getValue("new").jsonPrimitive.content shouldContain "1.75.5"
    }

    @Test
    fun `findings command lists review points with evidence`() {
        val dir = extracted("binaryeye")
        val result = sextant("findings", dir)
        result.exitCode shouldBe 0
        result.stdout shouldContain "Components exported without permission"
        result.stderr shouldContain "not proven issues"
        val high = sextant("findings", dir, "--severity", "high", "--json")
        high.exitCode shouldBe 0
        high.stdout
            .lines()
            .filter { it.isNotBlank() }
            .forEach { Json.parseToJsonElement(it) }
    }

    @Test
    fun `xref, reach and smali commands`() {
        val dir = extracted("binaryeye")
        val xref = sextant("xref", dir, "Lde/markusfisch/android/binaryeye/activity/CameraActivity;->c1")
        xref.exitCode shouldBe 0
        xref.stdout shouldContain "write Lde/markusfisch/android/binaryeye/activity/CameraActivity;->c1:Z"
        val reach =
            sextant("reach", dir, "de.markusfisch.android.binaryeye.activity.CameraActivity", "SharedPreferences\$Editor.putBoolean")
        reach.exitCode shouldBe 0
        reach.stdout shouldContain "Path 1"
        val smali = sextant("smali", apk("binaryeye"), "Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V")
        smali.exitCode shouldBe 0
        smali.stdout shouldContain ".method public final onResume()V"
    }

    @Test
    fun `icon command lists files and saves the best bitmap`() {
        val result = sextant("icon", apk("signal"))
        result.exitCode shouldBe 0
        result.stdout shouldContain "res/o-.png  ← best"
        result.stdout shouldContain "adaptive icon  res/BW.xml"
        val out = tmp.resolve("icon.png")
        sextant("icon", apk("signal"), "-o", out.toString()).exitCode shouldBe 0
        Files.readAllBytes(out).take(4) shouldBe listOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
        sextant("icon", apk("binaryeye"), "--json").stdout shouldContain "\"resource\":\"@mipmap/ic_launcher\""
    }
}
