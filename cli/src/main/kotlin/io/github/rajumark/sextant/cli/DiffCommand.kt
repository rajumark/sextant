package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.diff.DiffReport
import io.github.rajumark.sextant.core.diff.DiffSection
import io.github.rajumark.sextant.core.diff.Differ
import io.github.rajumark.sextant.core.output.SextantJson

/** `sextant diff <old> <new>`: what changed between two versions of an app. */
class DiffCommand : CoreCliktCommand(name = "diff") {
    private val old by argument(help = "The older version: an APK or a .sqlite file").path()
    private val new by argument(help = "The newer version: an APK or a .sqlite file").path()
    private val show by option("--show", help = "Items to list per section (default 15; totals are always exact)").int().default(15)
    private val json by option("--json", help = "The full report as JSON").flag()

    override fun help(context: Context) =
        "Compare two versions of an app: app facts, permissions, components, deep links, SDKs, libraries, hosts, URLs, " +
            "native code, packages, classes, strings and resources."

    override fun run() =
        guarded {
            val report = Differ.diff(indexFile(old), indexFile(new))
            if (json) println(SextantJson.compact.encodeToString(DiffReport.serializer(), report)) else print(render(report))
        }

    private fun render(r: DiffReport) =
        buildString {
            appendLine("# ${r.old}  →  ${r.new}")
            appendLine()
            val (changed, same) = r.sections.partition { !it.empty }
            for (s in changed) section(s)
            if (same.isNotEmpty()) appendLine("Unchanged: ${same.joinToString { it.name }}")
        }

    private fun StringBuilder.section(s: DiffSection) {
        appendLine("## ${s.name}  (+${s.addedTotal} −${s.removedTotal} ~${s.changedTotal})")
        s.note?.let { appendLine("   note: $it") }
        s.added.take(show).forEach { appendLine("  + ${it.oneLine()}") }
        if (s.addedTotal > show) appendLine("  + … ${s.addedTotal - show} more")
        s.removed.take(show).forEach { appendLine("  − ${it.oneLine()}") }
        if (s.removedTotal > show) appendLine("  − … ${s.removedTotal - show} more")
        s.changed.take(show).forEach { appendLine("  ~ ${it.item}: ${it.old.oneLine()}  →  ${it.new.oneLine()}") }
        if (s.changedTotal > show) appendLine("  ~ … ${s.changedTotal - show} more")
        appendLine()
    }

    private fun String.oneLine() = replace("\n", "\\n").let { if (it.length > 160) it.take(157) + "…" else it }
}
