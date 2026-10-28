package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.types.restrictTo
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.index.IndexReader
import io.github.rajumark.sextant.core.index.LinkRow
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** `sextant links <app> <thing>`: what anything connects to, across kinds (component, class, method, resource, …). */
class LinksCommand : CoreCliktCommand(name = "links") {
    private val target by argument(help = INDEX_INPUT).path()
    private val thing by argument(
        help = "A key (component:…, class:L…;, method:…, @string/x, permission:…, url:…) or a component, class or permission name",
    )
    private val depth by option("--depth", help = "Follow outgoing links this many steps (default 1, max 4)")
        .int()
        .restrictTo(1..4)
        .default(1)
    private val rel by option("--rel", help = "Only this relation, e.g. uses, implemented_by, instantiates, calls")
    private val limit by option("--limit", help = "Rows per relation (default 25)").int().default(25)
    private val json by option("--json", help = "One JSON object per link").flag()

    override fun help(context: Context) =
        "Show what anything links to and what links to it, across kinds: the app, components, classes, methods, " +
            "permissions, resources, URLs and native libraries. --depth follows the outgoing links further."

    override fun run() =
        guarded {
            openIndex(target).use { index ->
                val keys = index.linkKeys(thing)
                if (keys.isEmpty()) {
                    throw SextantException.input(
                        "nothing named '$thing'",
                        "use a key such as @string/app_name or class:Lcom/app/Main;, or `sextant find`",
                    )
                }
                for (key in keys) walk(index, key)
            }
        }

    private fun walk(
        index: IndexReader,
        root: String,
    ) {
        if (!json) println(root)
        val seen = hashSetOf(root)

        fun visit(
            key: String,
            level: Int,
        ) {
            val rows = index.links(key).filter { rel == null || it.rel == rel }
            val shown = rows.filter { it.direction == "out" || level == 1 }
            for ((group, links) in shown.groupBy { it.direction to it.rel }) {
                for (link in links.take(limit)) {
                    if (json) {
                        println(line(key, link, level))
                    } else {
                        val arrow = if (link.direction == "out") "→" else "←"
                        val via = link.via?.let { "  ($it)" } ?: ""
                        println("${"  ".repeat(level)}$arrow ${group.second.padEnd(14)} ${link.other}$via")
                    }
                    if (link.direction == "out" && level < depth && seen.add(link.other)) visit(link.other, level + 1)
                }
                if (links.size > limit && !json) println("${"  ".repeat(level)}  … ${links.size - limit} more ${group.second}")
            }
            if (level == 1 && shown.isEmpty() && !json) println("  (no links)")
        }
        visit(root, 1)
    }

    private fun line(
        from: String,
        link: LinkRow,
        level: Int,
    ) = JsonObject(
        mapOf(
            "from" to JsonPrimitive(from),
            "direction" to JsonPrimitive(link.direction),
            "rel" to JsonPrimitive(link.rel),
            "key" to JsonPrimitive(link.other),
            "via" to (link.via?.let(::JsonPrimitive) ?: JsonNull),
            "depth" to JsonPrimitive(level),
        ),
    ).toString()
}
