package io.github.rajumark.sextant.cli

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeGreaterThan
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
                .also { it.environment()["SEXTANT_CACHE"] = tmp.resolve("index-cache").toString() }
                .directory(cwd.toFile())
                .redirectError(tmp.resolve("stderr.txt").toFile())
                .start()
        val stdout = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        check(process.waitFor(120, TimeUnit.SECONDS)) { "sextant timed out" }
        // The CLI prints platform line endings (CRLF on Windows); compare content, not line endings.
        return Result(
            process.exitValue(),
            stdout.replace("\r\n", "\n"),
            Files.readString(tmp.resolve("stderr.txt")).replace("\r\n", "\n"),
        )
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
    fun `extract writes one SQLite file and nothing else, progress goes to stderr`() {
        val out = tmp.resolve("out.sqlite")
        val result = sextant("extract", apk("binaryeye"), "-o", out.toString())
        result.exitCode shouldBe 0
        result.stdout.shouldBeEmpty()
        result.stderr shouldContain "de.markusfisch.android.binaryeye"
        result.stderr shouldContain "Code "
        result.stderr shouldContain "Wrote $out"
        Files.isRegularFile(out) shouldBe true
        Files.list(tmp).use { files ->
            files.map { it.fileName.toString() }.filter { it != "stderr.txt" }.toList()
        } shouldContainExactlyInAnyOrder
            listOf("out.sqlite")
        sextant("sql", out.toString(), "SELECT count(*) AS n FROM records WHERE kind = 'component'").stdout shouldContain "18"
    }

    @Test
    fun `--no-code writes no code tables`() {
        val out = tmp.resolve("nocode.sqlite")
        sextant("extract", "-q", apk("jitsi"), "-o", out.toString()).exitCode shouldBe 0
        sextant("sql", out.toString(), "SELECT count(*) > 0 AS has FROM classes").stdout shouldContain "1"
        sextant("extract", "-q", "--force", "--no-code", apk("jitsi"), "-o", out.toString()).exitCode shouldBe 0
        sextant("sql", out.toString(), "SELECT count(*) AS n FROM classes").stdout.lines()[2].trim() shouldBe "0"
        sextant("info", out.toString()).stdout shouldContain "not extracted"
    }

    @Test
    fun `extract defaults to apk-name dot sqlite in the current directory`() {
        sextant("extract", "--quiet", apk("jitsi")).exitCode shouldBe 0
        Files.isRegularFile(tmp.resolve("jitsi.sqlite")) shouldBe true
        Files.exists(tmp.resolve("jitsi.sextant")) shouldBe false
    }

    @Test
    fun `--quiet prints nothing`() {
        val result = sextant("extract", "-q", apk("jitsi"), "-o", tmp.resolve("q.sqlite").toString())
        result.exitCode shouldBe 0
        result.stdout.shouldBeEmpty()
        result.stderr.shouldBeEmpty()
    }

    @Test
    fun `an existing output file needs --force, and a folder is refused`() {
        val out = tmp.resolve("busy.sqlite")
        Files.writeString(out, "old")
        val refused = sextant("extract", apk("jitsi"), "-o", out.toString())
        refused.exitCode shouldBe 3
        refused.stderr shouldContain "--force"
        Files.readString(out) shouldBe "old"

        sextant("extract", "-q", "--force", apk("jitsi"), "-o", out.toString()).exitCode shouldBe 0
        sextant("info", out.toString()).stdout shouldContain "org.jitsi.meet"

        val folder = sextant("extract", apk("jitsi"), "-o", Files.createDirectories(tmp.resolve("dir")).toString())
        folder.exitCode shouldBe 3
        folder.stderr shouldContain "is a folder"
    }

    @Test
    fun `files and coverage account for every zip entry`() {
        val out = tmp.resolve("files.sqlite")
        sextant("extract", "-q", apk("binaryeye"), "-o", out.toString()).exitCode shouldBe 0
        val entries =
            java.util.zip
                .ZipFile(apk("binaryeye"))
                .use { z -> z.entries().asSequence().count { !it.isDirectory } }

        val coverage = sextant("coverage", out.toString())
        coverage.exitCode shouldBe 0
        coverage.stdout shouldContain "Binary Eye 1.75.5: $entries files"
        coverage.stdout shouldContain "native-library"

        val all = sextant("files", out.toString(), "--json", "--limit", "100000")
        all.exitCode shouldBe 0
        val rows =
            all.stdout
                .lines()
                .filter { it.isNotBlank() }
                .map { Json.parseToJsonElement(it).jsonObject }
        rows.size shouldBe entries
        rows.first { it["path"]!!.jsonPrimitive.content == "AndroidManifest.xml" }["decoded_by"]!!.jsonPrimitive.content shouldBe "manifest"
        // Shrunk res/ file names map back to their resource through resources.arsc.
        rows.filter { it["type"]!!.jsonPrimitive.content == "resource" }.all {
            it["resource"]!!.jsonPrimitive.content.startsWith(
                "@",
            )
        } shouldBe
            true

        val listed = sextant("files", out.toString(), "--status", "listed", "--type", "kotlin-metadata")
        listed.stdout shouldContain "kotlin/kotlin.kotlin_builtins"
        listed.stdout shouldNotContain "classes.dex"
    }

    @Test
    fun `an APK given to an index command is indexed once and then read from the cache`() {
        val first = sextant("find", apk("binaryeye"), "Cipher", "--limit", "1")
        first.exitCode shouldBe 0
        first.stderr shouldContain "Indexing binaryeye.apk once"
        val second = sextant("coverage", apk("binaryeye"))
        second.exitCode shouldBe 0
        second.stderr shouldNotContain "Indexing"
        second.stdout shouldContain "Binary Eye 1.75.5"

        val cache = sextant("cache")
        cache.stdout shouldContain "Binary Eye"
        cache.stdout shouldContain "ready"
        sextant("cache", "--clear").exitCode shouldBe 0
        sextant("cache").stdout shouldContain "No cached indexes yet"

        val notApk = tmp.resolve("notes.txt").also { Files.writeString(it, "hello") }
        val bad = sextant("info", notApk.toString())
        bad.exitCode shouldBe 2
        bad.stderr shouldNotContain "Indexing"
    }

    @Test
    fun `info on a folder and on an APK give the same answer`() {
        val out = tmp.resolve("seal.sqlite")
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
        val out = tmp.resolve("$id.sqlite")
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
        val result = sextant("find", Files.createDirectories(tmp.resolve("empty")).toString(), "jitsi")
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
    fun `code is a resource referrer, even in an R8 build without R classes`() {
        val file = extracted("binaryeye")
        val count = sextant("sql", file, "SELECT count(*) AS n FROM resource_refs WHERE from_kind = 'code'").stdout.lines()[2].trim()
        count.toInt() shouldBeGreaterThan 500
        val detail = sextant("res", file, "strings", "error_no_content")
        detail.exitCode shouldBe 0
        detail.stdout shouldContain "code Lde/markusfisch/android/binaryeye/activity/PickActivity;->onCreate"
    }

    @Test
    fun `xml prints any binary XML file and finds elements across all of them`() {
        val file = extracted("binaryeye")
        val menu = sextant("xml", file, "@menu/fragment_encode")
        menu.exitCode shouldBe 0
        menu.stdout shouldContain "android:title=\"@string/pick_file\""
        sextant("xml", file, "AndroidManifest.xml").stdout shouldContain "package=\"de.markusfisch.android.binaryeye\""
        val found = sextant("xml", file, "--tag", "vector", "--json", "--limit", "3")
        found.exitCode shouldBe 0
        found.stdout
            .lines()
            .filter { it.isNotBlank() }
            .size shouldBe 3
        found.stderr shouldContain "of"
        sextant("xml", file).exitCode shouldBe 1
        sextant("xml", file, "@layout/nope").exitCode shouldBe 2
    }

    @Test
    fun `links walks across kinds, from a component to its class and from a menu to the code that inflates it`() {
        val file = extracted("binaryeye")
        val component = sextant("links", file, ".PickActivity", "--depth", "2")
        component.exitCode shouldBe 0
        component.stdout shouldContain "component:de.markusfisch.android.binaryeye.activity.PickActivity"
        component.stdout shouldContain "→ implemented_by class:Lde/markusfisch/android/binaryeye/activity/PickActivity;"
        component.stdout shouldContain "→ defines        method:Lde/markusfisch/android/binaryeye/activity/PickActivity;->onCreate"
        val menu = sextant("links", file, "@menu/fragment_encode")
        menu.stdout shouldContain "→ uses           @drawable/ic_action_pick_file  (android:icon)"
        menu.stdout shouldContain "← uses           method:Lde/markusfisch/android/binaryeye/activity/EncodeActivity;"
        val json = sextant("links", file, "@menu/fragment_encode", "--json")
        json.stdout
            .lines()
            .filter { it.isNotBlank() }
            .forEach {
                Json
                    .parseToJsonElement(it)
                    .jsonObject["rel"]!!
                    .jsonPrimitive.content
            }
        sextant("links", file, "no.such.Thing").exitCode shouldBe 2
    }

    @Test
    fun `q lists and runs saved queries, and the file documents itself`() {
        val file = extracted("binaryeye")
        val list = sextant("q")
        list.exitCode shouldBe 0
        list.stdout shouldContain "unused-strings"
        list.stdout shouldContain "password-fields"
        sextant("q", "hosts").stdout shouldContain "SELECT"
        val hosts = sextant("q", "hosts", file, "--json")
        hosts.exitCode shouldBe 0
        hosts.stdout shouldContain "markusfisch.de"
        sextant("q", "string-users", file).exitCode shouldBe 1
        sextant("q", "string-users", file, "--arg", "name=pick_file").stdout shouldContain "@menu/fragment_encode"
        sextant("q", "no-such-query").exitCode shouldBe 1
        sextant(
            "sql",
            file,
            "SELECT description FROM schema_docs WHERE \"table\" = 'files' AND \"column\" = 'decoded_by'",
        ).stdout shouldContain
            "manifest"
    }

    @Test
    fun `the SQLite file holds every byte, so code commands, icon -o and files --save need no APK`() {
        val file = extracted("binaryeye")
        val smali = sextant("smali", file, "Lde/markusfisch/android/binaryeye/activity/PickActivity;->onCreate")
        smali.exitCode shouldBe 0
        smali.stdout shouldContain ".method public final onCreate(Landroid/os/Bundle;)V"
        sextant("source", file, "de.markusfisch.android.binaryeye.activity.PickActivity").stdout shouldContain "class PickActivity"
        val png = tmp.resolve("icon.png")
        sextant("icon", file, "-o", png.toString()).exitCode shouldBe 0
        Files.readAllBytes(png).take(4) shouldBe listOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
        val csv = tmp.resolve("cc.csv")
        sextant("files", file, "--save", "countrycodes.csv", "-o", csv.toString()).exitCode shouldBe 0
        Files.readString(csv) shouldContain "Afghanistan"

        val slim = tmp.resolve("slim.sqlite")
        sextant("extract", "-q", "--no-content", apk("binaryeye"), "-o", slim.toString()).exitCode shouldBe 0
        (Files.size(slim) < Files.size(Paths.get(file))) shouldBe true
        sextant("smali", slim.toString(), "Lde/markusfisch/android/binaryeye/activity/PickActivity;->onCreate").exitCode shouldBe 0
        sextant("icon", slim.toString(), "-o", tmp.resolve("no.png").toString()).stderr shouldContain "--no-content"
    }

    @Test
    fun `native libraries are read in depth, searchable and linked`() {
        val file = extracted("binaryeye")
        val libs = sextant("q", "native-libraries", file, "--json")
        libs.exitCode shouldBe 0
        libs.stdout
            .lines()
            .filter { it.isNotBlank() }
            .size shouldBe 12
        libs.stdout shouldContain "\"machine\":\"arm64\""
        sextant("find", file, "ZXing", "--kind", "native_string", "--limit", "1").stdout shouldContain "libzxingcpp_android.so"
        sextant("links", file, "native:lib/arm64-v8a/libzxingcpp_android.so").stdout shouldContain "→ needs          system:libc.so"
        sextant("q", "native-sensitive-imports", file).exitCode shouldBe 0
    }

    @Test
    fun `text files are rows, searchable with files --grep and queryable as JSON`() {
        val file = extracted("binaryeye")
        val grep = sextant("files", file, "--grep", "Germany")
        grep.exitCode shouldBe 0
        grep.stdout shouldContain "countrycodes.csv:85: Germany,DE,DEU"
        sextant(
            "sql",
            file,
            "SELECT json_valid(text) AS ok FROM file_text WHERE path = 'IdbMessageTypes.json'",
        ).stdout.lines()[2].trim() shouldBe
            "1"
        sextant("q", "service-providers", file).stdout shouldContain "META-INF/services/"
        sextant("coverage", file).stdout shouldContain "decoded: 918 of 930 files"
    }

    @Test
    fun `signing is stored as rows, with signers per scheme, certificates and manifest digests`() {
        val file = extracted("binaryeye")
        val signing = sextant("q", "signing", file, "--json")
        signing.exitCode shouldBe 0
        signing.stdout shouldContain "\"scheme\":\"v3\",\"min_sdk\":24"
        signing.stdout shouldContain "CN=FDroid"
        sextant("sql", file, "SELECT key_size FROM signing_certificates").stdout shouldContain "2048"
        sextant("sql", file, "SELECT count(*) > 900 AS ok FROM signing_manifest").stdout.lines()[2].trim() shouldBe "1"
        sextant("links", file, "app", "--rel", "signed_by").stdout shouldContain "certificate:9E:CD:BF:06"
    }

    @Test
    fun `code details are stored, with annotations, line tables and exception handlers`() {
        val file = extracted("binaryeye")

        fun count(table: String) =
            sextant("sql", file, "SELECT count(*) AS n FROM $table")
                .stdout
                .lines()[2]
                .trim()
                .toInt()
        count("annotations") shouldBeGreaterThan 100
        count("method_lines") shouldBeGreaterThan 10_000
        count("exception_handlers") shouldBeGreaterThan 1_000
        val deprecated = sextant("q", "annotated", file, "--arg", "type=Ljava/lang/Deprecated;")
        deprecated.exitCode shouldBe 0
        deprecated.stderr shouldContain "rows"
        sextant("q", "broad-catches", file).exitCode shouldBe 0
        val lines =
            sextant(
                "sql",
                file,
                "SELECT first_line FROM method_lines l JOIN methods m ON m.id = l.method WHERE m.descriptor LIKE 'Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume%'",
            )
        lines.stdout
            .lines()[2]
            .trim()
            .toInt() shouldBeGreaterThan 0
    }

    @Test
    fun `links reach a class's SDK, a screen's layouts and a host's URLs`() {
        val file = extracted("binaryeye")
        val host = sextant("links", file, "markusfisch.de")
        host.exitCode shouldBe 0
        host.stdout shouldContain "← on_host        url:https://markusfisch.de/BinaryEye"
        val screens = sextant("q", "activity-layouts", file, "--json")
        screens.stdout shouldContain "\"layout\":\"@layout/"
        sextant("sql", file, "SELECT count(*) > 0 AS ok FROM links WHERE rel = 'part_of'").stdout.lines()[2].trim() shouldBe "1"
        sextant("sql", file, "SELECT count(*) > 0 AS ok FROM links WHERE rel = 'in_package'").stdout.lines()[2].trim() shouldBe "1"
    }

    @Test
    fun `extract stores insights and resources in the SQLite file`() {
        val file = extracted("binaryeye")
        for (table in listOf("libraries", "sdks", "endpoints", "resources", "string_resources", "layouts")) {
            sextant("sql", file, "SELECT count(*) > 0 AS has FROM $table").stdout.lines()[2].trim() shouldBe "1"
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
    fun `findings --fail-on exits 5 for CI, --sarif prints SARIF 2_1_0`() {
        val dir = extracted("binaryeye")
        val failing = sextant("findings", dir, "--fail-on", "medium")
        failing.exitCode shouldBe 5
        failing.stderr shouldContain "at or above medium (cleartext)"
        failing.stdout shouldContain "Cleartext HTTP allowed"
        sextant("findings", dir, "--fail-on", "high").exitCode shouldBe 0
        val sarif = sextant("findings", dir, "--sarif")
        sarif.exitCode shouldBe 0
        val doc = Json.parseToJsonElement(sarif.stdout).jsonObject
        doc.getValue("version").jsonPrimitive.content shouldBe "2.1.0"
        sarif.stdout shouldContain "\"uri\": \"AndroidManifest.xml\""
        sarif.stdout shouldContain "\"uri\": \"smali/de/markusfisch/android/binaryeye/activity/DecodeActivity.smali\""
        sextant("findings", dir, "--sarif", "--json").exitCode shouldBe 1
    }

    @Test
    fun `top hosts count only network URLs, not custom-scheme deep links`() {
        val dir = extracted("binaryeye")
        val json = Json.parseToJsonElement(sextant("insights", dir, "--json").stdout.trim()).jsonObject
        val hosts = json.getValue("top_hosts").toString()
        hosts shouldContain "markusfisch.de"
        hosts shouldNotContain "\"scan\""
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
    fun `grep searches decompiled code, deobf reads a mapping, info shows obfuscation`() {
        val hits = sextant("grep", apk("binaryeye"), "V().X", "--no-cache")
        hits.exitCode shouldBe 0
        hits.stdout shouldContain "yi0.S:36: if (this.j.e && !V().X.keySet().contains(strE)) {"
        hits.stderr shouldContain "3 matching lines in"
        sextant("grep", apk("binaryeye"), "(", "--regex", "--no-cache").exitCode shouldBe 1
        val mapping = tmp.resolve("mapping.txt")
        Files.writeString(mapping, "kotlinx.serialization.json.internal.JsonTreeDecoder -> yi0:\n    int position -> m\n")
        sextant("deobf", mapping.toString(), "yi0").stdout shouldContain "kotlinx.serialization.json.internal.JsonTreeDecoder -> yi0"
        val src =
            sextant("source", apk("binaryeye"), "kotlinx.serialization.json.internal.JsonTreeDecoder", "--mapping", mapping.toString())
        src.exitCode shouldBe 0
        src.stdout shouldContain "public int position;"
        sextant("info", apk("binaryeye")).stdout shouldContain "Obfuscation  heavy"
    }

    @Test
    fun `a split bundle reads like its base APK`() {
        val bundle = tmp.resolve("binaryeye.apkm")
        java.util.zip.ZipOutputStream(Files.newOutputStream(bundle)).use { out ->
            out.putNextEntry(java.util.zip.ZipEntry("base.apk"))
            Files.copy(Path.of(apk("binaryeye")), out)
            out.closeEntry()
        }
        val info = sextant("info", bundle.toString())
        info.exitCode shouldBe 0
        info.stdout shouldContain "de.markusfisch.android.binaryeye"
        info.stdout shouldContain "Bundle       apkm · base base.apk · 0 splits"
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

    @Test
    fun `components lists verdicts and component explains one, from an APK and a folder alike`() {
        val list = sextant("components", apk("bitwarden"), "--type", "activity", "--tag", "launcher")
        list.exitCode shouldBe 0
        list.stdout shouldContain "com.x8bit.bitwarden.MainActivity"
        list.stderr shouldContain "of 33 components"
        val one = sextant("component", extracted("bitwarden"), ".MainActivity")
        one.exitCode shouldBe 0
        one.stdout shouldContain "launcher: Has a MAIN/LAUNCHER filter"
        one.stdout shouldContain "launchMode     singleTask"
        one.stdout shouldContain "## Manifest entry"
        sextant("component", apk("bitwarden"), "NoSuchActivity").exitCode shouldBe 1
    }

    @Test
    fun `app shows the startup order and the SDKs it starts, from an APK or its SQLite file`() {
        val fromApk = sextant("app", apk("bitwarden"))
        fromApk.exitCode shouldBe 0
        fromApk.stdout shouldContain "## Startup order"
        fromApk.stdout shouldContain "## SDKs reached at startup"
        val fromFolder = sextant("app", "--json", extracted("bitwarden"))
        Json
            .parseToJsonElement(fromFolder.stdout)
            .jsonObject["code_indexed"]!!
            .jsonPrimitive.content shouldBe "true"
    }

    @Test
    fun `res lists resources with resolved references and who uses them`() {
        val menus = sextant("res", apk("binaryeye"), "menus")
        menus.exitCode shouldBe 0
        menus.stdout shouldContain "Compose barcode"
        val name = sextant("res", apk("binaryeye"), "strings", "app_name")
        name.stdout shouldContain "Binary Eye"
        name.stdout shouldContain "manifest application"
        sextant("res", apk("bitwarden"), "xml").stdout shouldContain "base-cleartext=true user-certs=trusted"
    }

    @Test
    fun `sdks shows roles and versions, and filters apps by version across several`() {
        val one = sextant("sdks", extracted("bitwarden"))
        one.exitCode shouldBe 0
        one.stdout shouldContain "Gson"
        one.stdout shouldContain "2.10.1"
        val across = sextant("sdks", extracted("bitwarden"), extracted("binaryeye"), "--sdk", "Gson", "--below", "99")
        across.exitCode shouldBe 0
        across.stderr shouldContain "of 2 apps match"
    }
}
