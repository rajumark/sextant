package io.github.rajumark.sextant.core.mcp

import io.github.rajumark.sextant.core.Sextant
import io.github.rajumark.sextant.core.SextantException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.io.Writer

/** One tool an MCP client can call. */
class McpTool(
    val name: String,
    val description: String,
    /** JSON Schema properties: name → (type, description). */
    val params: List<Param>,
    val run: (JsonObject) -> String,
) {
    class Param(
        val name: String,
        val type: String,
        val description: String,
        val required: Boolean = false,
    )
}

/**
 * Minimal Model Context Protocol server over stdio (newline-delimited JSON-RPC 2.0).
 * Implements initialize, ping, tools/list and tools/call; everything else returns "method not found".
 */
class McpServer(
    private val tools: List<McpTool>,
    private val instructions: String,
) {
    private val json = Json { encodeDefaults = true }

    fun serve(
        input: BufferedReader,
        output: Writer,
    ) {
        while (true) {
            val line = input.readLine() ?: return
            if (line.isBlank()) continue
            val response = handle(line) ?: continue
            output.write(response)
            output.write("\n")
            output.flush()
        }
    }

    /** Handles one message; returns the response line, or null for notifications. */
    fun handle(line: String): String? {
        val message =
            try {
                json.parseToJsonElement(line).jsonObject
            } catch (e: Exception) {
                return error(JsonNull, PARSE_ERROR, "invalid JSON: ${e.message}")
            }
        val id = message["id"]
        val method =
            message["method"]?.jsonPrimitive?.contentOrNull ?: return if (id == null) null else error(id, INVALID_REQUEST, "no method")
        if (id == null) return null // notification (e.g. notifications/initialized): no response
        val params = message["params"] as? JsonObject ?: JsonObject(emptyMap())
        return when (method) {
            "initialize" -> result(id, initialize(params))
            "ping" -> result(id, JsonObject(emptyMap()))
            "tools/list" -> result(id, listTools())
            "tools/call" -> result(id, callTool(params))
            else -> error(id, METHOD_NOT_FOUND, "method not found: $method")
        }
    }

    private fun initialize(params: JsonObject): JsonObject =
        buildJsonObject {
            // Answer with the client's version when it sends one we can serve; tools/list + tools/call are stable across versions.
            put("protocolVersion", params["protocolVersion"]?.jsonPrimitive?.contentOrNull ?: PROTOCOL_VERSION)
            putJsonObject("capabilities") { putJsonObject("tools") { put("listChanged", false) } }
            putJsonObject("serverInfo") {
                put("name", Sextant.NAME)
                put("version", Sextant.version)
            }
            put("instructions", instructions)
        }

    private fun listTools(): JsonObject =
        buildJsonObject {
            put(
                "tools",
                buildJsonArray {
                    for (tool in tools) {
                        add(
                            buildJsonObject {
                                put("name", tool.name)
                                put("description", tool.description)
                                putJsonObject("inputSchema") {
                                    put("type", "object")
                                    putJsonObject("properties") {
                                        for (p in tool.params) {
                                            putJsonObject(p.name) {
                                                put("type", p.type)
                                                put("description", p.description)
                                            }
                                        }
                                    }
                                    put(
                                        "required",
                                        buildJsonArray { tool.params.filter { it.required }.forEach { add(JsonPrimitive(it.name)) } },
                                    )
                                }
                            },
                        )
                    }
                },
            )
        }

    private fun callTool(params: JsonObject): JsonObject {
        val name = params["name"]?.jsonPrimitive?.contentOrNull
        val tool = tools.firstOrNull { it.name == name } ?: return toolResult("unknown tool: $name", isError = true)
        val args = params["arguments"] as? JsonObject ?: JsonObject(emptyMap())
        val missing = tool.params.filter { it.required && args[it.name] == null }.map { it.name }
        if (missing.isNotEmpty()) return toolResult("missing required argument(s): ${missing.joinToString()}", isError = true)
        return try {
            toolResult(tool.run(args), isError = false)
        } catch (e: SextantException) {
            toolResult(e.message + (e.hint?.let { "\nhint: $it" } ?: ""), isError = true)
        } catch (e: Exception) {
            toolResult("internal error: ${e.javaClass.simpleName}: ${e.message}", isError = true)
        }
    }

    private fun toolResult(
        text: String,
        isError: Boolean,
    ) = buildJsonObject {
        put(
            "content",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("type", "text")
                        put("text", text)
                    },
                )
            },
        )
        put("isError", isError)
    }

    private fun result(
        id: JsonElement,
        result: JsonElement,
    ) = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", id)
        put("result", result)
    }.toString()

    private fun error(
        id: JsonElement,
        code: Int,
        message: String,
    ) = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", id)
        putJsonObject("error") {
            put("code", code)
            put("message", message)
        }
    }.toString()

    companion object {
        const val PROTOCOL_VERSION = "2025-06-18"
        private const val PARSE_ERROR = -32700
        private const val INVALID_REQUEST = -32600
        private const val METHOD_NOT_FOUND = -32601
    }
}

/** Typed argument access for tool implementations. */
fun JsonObject.string(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull

fun JsonObject.int(
    name: String,
    default: Int,
): Int = (this[name] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: default
