package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.ExitCode
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.index.ClassReport
import io.github.rajumark.sextant.core.index.IndexSchema
import io.github.rajumark.sextant.core.index.QueryResult
import io.github.rajumark.sextant.core.index.SearchHit
import io.github.rajumark.sextant.core.output.SextantJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** `sextant sql <app> "<query>"`: read-only SQL over the app's SQLite file. */
class SqlCommand : CoreCliktCommand(name = "sql") {
    private val dir by argument(help = INDEX_INPUT).path()
    private val query by argument(help = "SQL to run (read-only)").optional()
    private val format by option(help = "Output format").switch("--json" to "json", "--csv" to "csv").default("table")
    private val schema by option("--schema", help = "Print the tables and views instead of running a query").flag()
    private val limit by option("--limit", help = "Maximum rows to print (default 1000, 0 = no limit)").int().default(1000)

    override fun help(context: Context) = "Run read-only SQL over the app's SQLite file. Use --schema to see the tables."

    override fun run() =
        guarded {
            openIndex(dir).use { index ->
                if (schema) {
                    print(index.schema())
                    return@use
                }
                val sql = query ?: throw SextantException(ExitCode.USAGE, "missing SQL query", "or pass --schema")
                printResult(index.query(sql, if (limit <= 0) Int.MAX_VALUE else limit), format, limit)
            }
        }
}

/** Prints a query result as a table (default), JSON lines or CSV, and the row count on stderr. Shared by `sql` and `q`. */
internal fun printResult(
    result: QueryResult,
    format: String,
    limit: Int,
) {
    when (format) {
        "json" -> result.rows.forEach { println(SqlJson.row(result.columns, it)) }
        "csv" -> {
            fun esc(v: Any?): String {
                val s = v?.toString() ?: ""
                return if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
            }
            println(result.columns.joinToString(",") { esc(it) })
            result.rows.forEach { row -> println(row.joinToString(",") { esc(it) }) }
        }
        else ->
            if (result.rows.isNotEmpty()) {
                print(
                    Table.render(
                        result.columns,
                        result.rows.map { row ->
                            row.map { it?.toString() ?: "NULL" }
                        },
                    ),
                )
            }
    }
    val note = if (result.truncated) " (stopped at --limit $limit)" else ""
    System.err.println("${result.rows.size} row${if (result.rows.size == 1) "" else "s"}$note")
}

/** `sextant find <dir> <text>`: substring search over every name and string constant. */
class FindCommand : CoreCliktCommand(name = "find") {
    private val dir by argument(help = INDEX_INPUT).path()
    private val text by argument(help = "Text to find (any part of a name or string)")
    private val kind by option("--kind", help = "Only this kind")
        .choice(*IndexSchema.KINDS.toTypedArray())
    private val limit by option("--limit", help = "Maximum results (default 50)").int().default(50)
    private val json by option("--json", help = "One JSON object per line").flag()

    override fun help(context: Context) =
        "Find anything containing the text: classes, methods, fields, strings, components, permissions, deep links, " +
            "resources, endpoints, SDKs, libraries and native symbols."

    override fun run() =
        guarded {
            openIndex(dir).use { index ->
                val hits = index.search(text, kind, limit)
                for (hit in hits) {
                    val users = if (hit.kind == "string") index.stringUsers(hit.ref, 3) else emptyList()
                    if (json) {
                        println(SextantJson.compact.encodeToString(FindHit.serializer(), FindHit.of(hit, users)))
                    } else {
                        println("%-10s %s".format(hit.kind, oneLine(hit.text)))
                        if (hit.kind != "string" && hit.ref != hit.text) println("           ${hit.ref}")
                        users.forEach { println("           ← $it") }
                    }
                }
                System.err.println(
                    "${hits.size} result${if (hits.size == 1) "" else "s"}${if (hits.size == limit) " (stopped at --limit $limit)" else ""}",
                )
            }
        }

    private fun oneLine(s: String) = s.replace("\n", "\\n").let { if (it.length > 160) it.take(157) + "…" else it }
}

@Serializable
data class FindHit(
    val kind: String,
    val ref: String,
    val text: String,
    val usedIn: List<String>,
) {
    companion object {
        fun of(
            hit: SearchHit,
            users: List<String>,
        ) = FindHit(hit.kind, if (hit.kind == "string") hit.text else hit.ref, hit.text, users)
    }
}

/** `sextant class <dir> <name>`: everything about one class. */
class ClassCommand : CoreCliktCommand(name = "class") {
    private val dir by argument(help = INDEX_INPUT).path()
    private val name by argument(help = "Class name: com.app.Login, Lcom/app/Login; or just Login")
    private val json by option("--json", help = "Machine-readable JSON").flag()

    override fun help(context: Context) = "Show a class: hierarchy, fields, methods, who calls it and the strings it uses."

    override fun run() =
        guarded {
            openIndex(dir).use { index ->
                val matches = index.findClasses(name)
                when {
                    matches.isEmpty() -> throw SextantException.input(
                        "no class matches '$name'",
                        "try `sextant find $dir $name --kind class`",
                    )
                    matches.size > 1 -> {
                        if (json) {
                            println(SextantJson.compact.encodeToString(ListSerializer(String.serializer()), matches))
                        } else {
                            System.err.println("${matches.size} classes match '$name'; pass the full name:")
                            matches.forEach { println(it) }
                        }
                    }
                    else -> {
                        val report = index.classReport(matches.single())
                        if (json) println(SextantJson.compact.encodeToString(ClassReport.serializer(), report)) else print(human(report))
                    }
                }
            }
        }

    private fun human(r: ClassReport) =
        buildString {
            appendLine("class ${r.name}  (${r.descriptor})")
            appendLine("  access      ${r.access}")
            r.superclass?.let { appendLine("  extends     $it") }
            if (r.interfaces.isNotEmpty()) appendLine("  implements  ${r.interfaces.joinToString()}")
            appendLine("  source      ${r.sourceFile ?: "?"} · ${r.dex}${if (r.kotlin) " · Kotlin" else ""}")
            if (r.subclasses.isNotEmpty()) appendLine("  subclasses  ${r.subclasses.joinToString()}")
            appendLine()
            appendLine("Fields (${r.fields.size})")
            r.fields.forEach { f -> appendLine("  ${f.access} ${f.name}: ${f.type}${f.initialValue?.let { " = ${quote(it)}" } ?: ""}") }
            appendLine()
            appendLine("Methods (${r.methods.size})")
            r.methods.forEach { m ->
                val sig = m.descriptor.substringAfter("->")
                appendLine("  ${m.access} $sig  · ${m.instructions} insns · ${m.callsOut} calls out · called from ${m.calledFrom} sites")
            }
            if (r.callers.isNotEmpty()) {
                appendLine()
                appendLine("Called from outside the class (top ${r.callers.size})")
                r.callers.forEach { c -> appendLine("  ${c.caller}  → ${c.callee.substringAfter("->")}  ×${c.count}") }
            }
            if (r.strings.isNotEmpty()) {
                appendLine()
                appendLine("Strings (${r.strings.size}${if (r.strings.size == 25) "+" else ""})")
                r.strings.forEach { appendLine("  ${quote(it)}") }
            }
        }

    private fun quote(s: String) =
        "\"" +
            s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .let { if (it.length > 120) it.take(117) + "…" else it } +
            "\""
}

/** Plain aligned text table; cells are cut at [MAX_CELL] characters. */
object Table {
    private const val MAX_CELL = 80

    fun render(
        columns: List<String>,
        rows: List<List<String>>,
    ): String {
        val cut =
            rows.map { row ->
                row.map { cell ->
                    cell.replace("\n", "\\n").let {
                        if (it.length >
                            MAX_CELL
                        ) {
                            it.take(MAX_CELL - 1) + "…"
                        } else {
                            it
                        }
                    }
                }
            }
        val widths = columns.indices.map { i -> maxOf(columns[i].length, cut.maxOfOrNull { it[i].length } ?: 0) }
        return buildString {
            appendLine(columns.indices.joinToString("  ") { columns[it].padEnd(widths[it]) }.trimEnd())
            appendLine(widths.joinToString("  ") { "-".repeat(it) })
            cut.forEach { row -> appendLine(row.indices.joinToString("  ") { row[it].padEnd(widths[it]) }.trimEnd()) }
        }
    }
}

/** SQL rows as JSON objects, and safe SQL string literals. */
object SqlJson {
    fun row(
        columns: List<String>,
        row: List<Any?>,
    ) = JsonObject(
        columns.zip(row).associate { (c, v) ->
            c to
                when (v) {
                    null -> JsonNull
                    is Number -> JsonPrimitive(v)
                    else -> JsonPrimitive(v.toString())
                }
        },
    ).toString()

    fun literal(text: String) = "'" + text.replace("'", "''") + "'"
}
