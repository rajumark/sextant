package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.CodeGrep
import io.github.rajumark.sextant.core.GrepHit
import io.github.rajumark.sextant.core.ProguardMapping
import io.github.rajumark.sextant.core.output.SextantJson

/** `sextant grep <app.apk> <text>`: text search inside decompiled Java, like jadx-gui's code search. */
class GrepCommand : CoreCliktCommand(name = "grep") {
    private val apk by argument(help = "The APK or split bundle (searching needs the bytecode)").path()
    private val text by argument(help = "Text to find in the code (case-insensitive), or a regular expression with --regex")
    private val regex by option("--regex", help = "Treat TEXT as a regular expression").flag()
    private val caseSensitive by option("--case", help = "Match case").flag()
    private val pkg by option("--package", help = "Only classes whose name starts with this, e.g. com.app (much faster on big apps)")
    private val limit by option("--limit", help = "Stop after this many matching lines (default 200)").int().default(200)
    private val mapping by option("--mapping", help = "ProGuard/R8 mapping.txt: search and show original names").path()
    private val json by option("--json", help = "One JSON object per match").flag()
    private val noCache by option("--no-cache", help = "Don't read or write the local cache of decompiled code").flag()

    override fun help(context: Context) =
        "Search decompiled Java for text: every matching line with its class, method and line number. " +
            "Finds what `find` can't (code bodies, field use, API calls). The first search decompiles the app and keeps the code " +
            "in a local cache, so later searches are quick; --package narrows a first search on big apps."

    override fun run() =
        guarded {
            if (limit < 1) throw UsageError("--limit must be at least 1")
            val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val pattern =
                try {
                    if (regex) Regex(text, options) else Regex(Regex.escape(text), options)
                } catch (e: IllegalArgumentException) {
                    throw UsageError("not a valid regular expression: ${e.message}")
                }
            val map = mapping?.let { ProguardMapping.read(it) }
            val started = System.nanoTime()
            val result = CodeGrep.search(apk, pattern, pkg, limit, map, cache = !noCache)
            result.hits.forEach { h ->
                if (json) println(SextantJson.compact.encodeToString(GrepHit.serializer(), h)) else println(line(h))
            }
            val seconds = (System.nanoTime() - started) / 1e9
            System.err.println(
                "${result.hits.size} matching lines in ${result.classesSearched} classes (%.1f s)".format(seconds) +
                    (if (result.truncated) " · stopped at --limit $limit" else "") +
                    if (result.failed.isEmpty()) "" else " · ${result.failed.size} classes could not be decompiled (try `sextant smali`)",
            )
        }

    private fun line(h: GrepHit): String {
        val cls =
            h.classDescriptor
                .removePrefix("L")
                .removeSuffix(";")
                .replace('/', '.')
        val where =
            h.method
                ?.substringAfter("->")
                ?.substringBefore('(')
                ?.let { "$cls.$it" } ?: cls
        return "$where:${h.line}: ${h.text}"
    }
}

/** `sextant deobf <mapping.txt> <name>`: translate names between an R8/ProGuard mapping's two sides. */
class DeobfCommand : CoreCliktCommand(name = "deobf") {
    private val mappingFile by argument(help = "The mapping.txt R8 or ProGuard wrote for this build").path()
    private val name by argument(help = "Any part of an original or obfuscated class name, or a member name")
    private val limit by option("--limit", help = "Maximum lines (default 100)").int().default(100)

    override fun help(context: Context) =
        "Look up names in a ProGuard/R8 mapping.txt, both ways: original -> obfuscated for classes, fields and methods. " +
            "Pass the same file as --mapping to `source` and `grep` to read the code with original names."

    override fun run() =
        guarded {
            val mapping = ProguardMapping.read(mappingFile)
            val lines = mapping.lookup(name)
            lines.take(limit).forEach(::println)
            System.err.println(
                "${lines.size} matches in ${mapping.size} mapped classes" + if (lines.size > limit) " · showing $limit" else "",
            )
        }
}
