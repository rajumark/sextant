package io.github.rajumark.sextant.core.mcp

import io.github.rajumark.sextant.core.CorpusApks
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.StringWriter
import java.nio.file.Files

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpServerTest {
    private val echo =
        McpTool("echo", "Echoes text", listOf(McpTool.Param("text", "string", "Text", required = true))) { it.string("text")!! }
    private val server = McpServer(listOf(echo), "test server")

    private fun call(line: String): JsonObject = Json.parseToJsonElement(server.handle(line)!!).jsonObject

    private fun text(response: JsonObject) =
        response
            .getValue("result")
            .jsonObject
            .getValue("content")
            .jsonArray[0]
            .jsonObject
            .getValue("text")
            .jsonPrimitive.content

    @Test
    fun `initialize echoes the protocol version and announces tools`() {
        val r =
            call(
                """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26"}}""",
            ).getValue("result").jsonObject
        r.getValue("protocolVersion").jsonPrimitive.content shouldBe "2025-03-26"
        r
            .getValue("serverInfo")
            .jsonObject
            .getValue("name")
            .jsonPrimitive.content shouldBe "sextant"
        r.getValue("capabilities").jsonObject.keys shouldBe setOf("tools")
    }

    @Test
    fun `notifications get no response, unknown methods get -32601, bad JSON gets -32700`() {
        server.handle("""{"jsonrpc":"2.0","method":"notifications/initialized"}""") shouldBe null
        call(
            """{"jsonrpc":"2.0","id":2,"method":"resources/list"}""",
        ).getValue("error").jsonObject.getValue("code").jsonPrimitive.int shouldBe
            -32601
        call("{not json")
            .getValue("error")
            .jsonObject
            .getValue("code")
            .jsonPrimitive.int shouldBe -32700
    }

    @Test
    fun `tools list and call, with required-argument checks`() {
        val tools =
            call("""{"jsonrpc":"2.0","id":3,"method":"tools/list"}""")
                .getValue("result")
                .jsonObject
                .getValue("tools")
                .jsonArray
        tools[0]
            .jsonObject
            .getValue("inputSchema")
            .jsonObject
            .getValue("required")
            .jsonArray
            .map { it.jsonPrimitive.content } shouldBe
            listOf("text")
        text(call("""{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"echo","arguments":{"text":"hi"}}}""")) shouldBe "hi"
        val missing = call("""{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"echo","arguments":{}}}""")
        missing
            .getValue("result")
            .jsonObject
            .getValue("isError")
            .jsonPrimitive.boolean shouldBe true
    }

    @Test
    fun `serve loop answers line by line`() {
        val out = StringWriter()
        server.serve("""{"jsonrpc":"2.0","id":1,"method":"ping"}""".plus("\n\n").reader().buffered(), out)
        out.toString().trim() shouldBe """{"jsonrpc":"2.0","id":1,"result":{}}"""
    }

    // Sextant's own tools against a real APK.
    private val dir = Files.createTempDirectory("sextant-mcp")
    private val sextant by lazy { SextantTools(null, null).let { McpServer(it.tools, it.instructions) } }

    private fun tool(
        name: String,
        args: String,
    ) = Json
        .parseToJsonElement(
            sextant.handle("""{"jsonrpc":"2.0","id":9,"method":"tools/call","params":{"name":"$name","arguments":$args}}""")!!,
        ).jsonObject

    /** Windows paths have backslashes, which must be escaped inside a JSON string. */
    private fun jsonPath(path: Any) = path.toString().replace("\\", "\\\\")

    @AfterAll
    fun cleanup() {
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `sextant tools work after extract`() {
        val before = tool("summary", "{}")
        before
            .getValue("result")
            .jsonObject
            .getValue("isError")
            .jsonPrimitive.boolean shouldBe true
        text(
            tool("extract", """{"apk":"${jsonPath(CorpusApks.require("binaryeye"))}","out":"${jsonPath(dir.resolve("be"))}"}"""),
        ) shouldContain
            "de.markusfisch.android.binaryeye"
        text(tool("find", """{"text":"markusfisch.de/BinaryEye","kind":"string"}""")) shouldContain "CameraActivity;->onResume()V"
        text(tool("callees", """{"method":"CameraActivity.onResume","depth":1}""")) shouldContain "(external)"
        text(tool("sql", """{"query":"SELECT count(*) AS n FROM components"}""")) shouldBe """{"n":18}"""
        text(tool("sql", """{"query":"schema"}""")) shouldContain "CREATE TABLE methods"
        text(tool("class", """{"name":"CameraActivity"}""")) shouldContain "\"methods\""
        text(tool("source", """{"target":"Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V"}""")) shouldContain
            "welcome_shown"
        text(tool("insights", "{}")) shouldContain "## sdks"
        text(tool("endpoints", """{"host":"markusfisch"}""")).lines().map { it.substringBefore("  ") } shouldContainAll
            listOf("https://markusfisch.de/BinaryEye")
    }
}
