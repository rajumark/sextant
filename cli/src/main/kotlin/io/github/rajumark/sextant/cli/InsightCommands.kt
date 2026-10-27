package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.index.IndexReader

/** `sextant insights <dir>`: frameworks, SDKs, libraries, hosts and native code at a glance. */
class InsightsCommand : CoreCliktCommand(name = "insights") {
    private val dir by argument(help = "An extracted folder").path()
    private val json by option("--json", help = "One JSON object").flag()

    override fun help(context: Context) = "Show what the app is built with: frameworks, SDKs, libraries, endpoints and native code."

    override fun run() =
        guarded {
            IndexReader.open(dir).use { index ->
                val sections =
                    linkedMapOf(
                        "frameworks" to "SELECT name, evidence FROM frameworks ORDER BY name",
                        "sdks" to "SELECT category, name, classes FROM sdks ORDER BY category, name",
                        "top_hosts" to "SELECT host, count(*) AS urls FROM endpoints GROUP BY host ORDER BY urls DESC, host LIMIT 15",
                        "libraries" to "SELECT \"group\", artifact, version FROM libraries ORDER BY \"group\", artifact",
                        "native" to
                            "SELECT name, count(*) AS abis, max(exported_symbols) AS exported, max(jni_methods) AS jni " +
                            "FROM native_libraries GROUP BY name ORDER BY jni DESC, exported DESC, name",
                    )
                if (json) {
                    val obj =
                        sections.entries.joinToString(",", "{", "}") { (key, sql) ->
                            val r = index.query(sql)
                            "\"$key\":" + r.rows.joinToString(",", "[", "]") { row -> SqlJson.row(r.columns, row) }
                        }
                    println(obj)
                    return@use
                }
                for ((key, sql) in sections) {
                    val r = index.query(sql)
                    println("## ${key.replace('_', ' ').replaceFirstChar { it.uppercase() }} (${r.rows.size})")
                    if (r.rows.isEmpty()) {
                        println("none")
                    } else {
                        print(
                            Table.render(
                                r.columns,
                                r.rows.map { row ->
                                    row.map {
                                        it?.toString()
                                            ?: ""
                                    }
                                },
                            ),
                        )
                    }
                    println()
                }
            }
        }
}

/** `sextant endpoints <dir>`: every URL with where it comes from. */
class EndpointsCommand : CoreCliktCommand(name = "endpoints") {
    private val dir by argument(help = "An extracted folder").path()
    private val host by option("--host", help = "Only URLs whose host contains this text")
    private val limit by option("--limit", help = "Maximum URLs (default 200)").int().default(200)
    private val json by option("--json", help = "One JSON object per URL").flag()

    override fun help(context: Context) = "List URLs found in code strings, resources, deep links and manifest values, with their sources."

    override fun run() =
        guarded {
            IndexReader.open(dir).use { index ->
                val filter = host?.let { "WHERE e.host LIKE '%' || ${SqlJson.literal(it)} || '%'" } ?: ""
                val r =
                    index.query(
                        """
                        SELECT e.url, e.host, group_concat(s.kind || ' ' || s.location, char(10)) AS sources
                        FROM endpoints e JOIN endpoint_sources s ON s.endpoint = e.id $filter
                        GROUP BY e.id ORDER BY e.host, e.url
                        """.trimIndent(),
                        limit,
                    )
                for (row in r.rows) {
                    if (json) {
                        println(SqlJson.row(r.columns, row))
                    } else {
                        println(row[0])
                        row[2].toString().lines().forEach { println("    $it") }
                    }
                }
                System.err.println(
                    "${r.rows.size} URL${if (r.rows.size == 1) "" else "s"}${if (r.truncated) " (stopped at --limit $limit)" else ""}",
                )
            }
        }
}

/** `sextant findings <dir>`: review points with their evidence. */
class FindingsCommand : CoreCliktCommand(name = "findings") {
    private val dir by argument(help = "An extracted folder").path()
    private val severity by option("--severity", help = "Minimum severity to show").choice("info", "low", "medium", "high").default("info")
    private val json by option("--json", help = "One JSON object per finding").flag()

    override fun help(context: Context) =
        "Show review findings (debuggable, cleartext, exported components, WebView bridges, weak crypto, trust-all TLS, " +
            "embedded secrets, …) with evidence. They are static observations to check, not proven vulnerabilities."

    override fun run() =
        guarded {
            IndexReader.open(dir).use { index ->
                val levels = listOf("high", "medium", "low", "info")
                val allowed = levels.take(levels.indexOf(severity) + 1).joinToString(",") { "'$it'" }
                val findings =
                    index.query(
                        "SELECT id, severity, title, detail, count FROM findings WHERE severity IN ($allowed) " +
                            "ORDER BY CASE severity WHEN 'high' THEN 0 WHEN 'medium' THEN 1 WHEN 'low' THEN 2 ELSE 3 END, id",
                    )
                for (row in findings.rows) {
                    val id = row[0].toString()
                    val evidence = index.query("SELECT kind, location, value FROM finding_evidence WHERE finding = ${SqlJson.literal(id)}")
                    if (json) {
                        val ev = evidence.rows.joinToString(",", "[", "]") { SqlJson.row(evidence.columns, it) }
                        println(SqlJson.row(findings.columns, row).dropLast(1) + ",\"evidence\":$ev}")
                    } else {
                        println("[${row[1]}] ${row[2]}  (${row[4]})  · $id")
                        println("    ${row[3]}")
                        evidence.rows.take(5).forEach { e -> println("    - ${e[0]} ${e[1]}${e[2]?.let { "  = $it" } ?: ""}") }
                        if (evidence.rows.size > 5) println("    - … ${row[4].toString().toInt() - 5} more")
                    }
                }
                val plural = if (findings.rows.size == 1) "" else "s"
                System.err.println("${findings.rows.size} finding$plural (static observations to review, not proven issues)")
            }
        }
}
