package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.ExitCode
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.index.SavedQueries

/** `sextant q [name] [app]`: saved queries for common questions. */
class QCommand : CoreCliktCommand(name = "q") {
    private val name by argument(help = "The saved query; leave out to list them").optional()
    private val target by argument(help = INDEX_INPUT).path().optional()
    private val args by option("--arg", help = "A parameter, NAME=VALUE (e.g. --arg name=app_name)").multiple()
    private val format by option(help = "Output format").switch("--json" to "json", "--csv" to "csv").default("table")
    private val limit by option("--limit", help = "Maximum rows to print (default 1000, 0 = no limit)").int().default(1000)

    override fun help(context: Context) =
        "Run a saved query for a common question (unused strings, password fields, exported components, …). " +
            "Without a name, list them; without an app, print the query's SQL."

    override fun run() =
        guarded {
            val queryName = name
            if (queryName == null) {
                print(
                    Table.render(
                        listOf("query", "params", "what it answers"),
                        SavedQueries.ALL.map {
                            listOf(it.name, it.params.joinToString(), it.description)
                        },
                    ),
                )
                System.err.println("Run one with: sextant q <query> app.apk")
                return@guarded
            }
            val query =
                SavedQueries.find(queryName)
                    ?: throw SextantException(ExitCode.USAGE, "no saved query named '$queryName'", "list them with: sextant q")
            val app = target
            if (app == null) {
                println(query.sql)
                return@guarded
            }
            val given = args.associate { a -> a.substringBefore('=') to a.substringAfter('=', "") }
            val missing = query.params.filter { it !in given }
            if (missing.isNotEmpty()) {
                throw SextantException(ExitCode.USAGE, "$queryName needs ${missing.joinToString { "--arg $it=…" }}", query.description)
            }
            openIndex(app).use { index ->
                printResult(
                    index.query(
                        query.sql,
                        if (limit <=
                            0
                        ) {
                            Int.MAX_VALUE
                        } else {
                            limit
                        },
                        query.params.map { given.getValue(it) },
                    ),
                    format,
                    limit,
                )
            }
        }
}
