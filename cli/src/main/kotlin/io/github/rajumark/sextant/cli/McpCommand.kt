package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.mcp.McpServer
import io.github.rajumark.sextant.core.mcp.SextantTools

/** `sextant mcp`: a Model Context Protocol server on stdin/stdout for AI agents. */
class McpCommand : CoreCliktCommand(name = "mcp") {
    private val dir by option("--dir", help = "Start with this extraction folder as the current app").path()
    private val apk by option("--apk", help = "The APK for `source` (and the default for `extract`)").path()

    override fun help(context: Context) =
        "Run an MCP server on stdin/stdout so AI agents can extract, search and query APKs. Tools: " +
            SextantTools(null, null).tools.joinToString { it.name } + "."

    override fun run() {
        val tools = SextantTools(dir, apk)
        // stdout carries protocol messages only; nothing else may be printed there.
        McpServer(tools.tools, tools.instructions).serve(System.`in`.bufferedReader(), System.out.writer())
    }
}
