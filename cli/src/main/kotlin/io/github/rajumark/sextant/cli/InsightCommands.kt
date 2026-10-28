package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.ExitCode
import io.github.rajumark.sextant.core.Sextant
import io.github.rajumark.sextant.core.index.IndexReader
import io.github.rajumark.sextant.core.insights.Finding
import io.github.rajumark.sextant.core.insights.FindingEvidence
import io.github.rajumark.sextant.core.insights.Sarif
import io.github.rajumark.sextant.core.insights.Urls
import io.github.rajumark.sextant.core.output.SextantJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** `sextant insights <dir>`: frameworks, SDKs, libraries, hosts and native code at a glance. */
class InsightsCommand : CoreCliktCommand(name = "insights") {
    private val dir by argument(help = INDEX_INPUT).path()
    private val json by option("--json", help = "One JSON object").flag()

    override fun help(context: Context) = "Show what the app is built with: frameworks, SDKs, libraries, endpoints and native code."

    override fun run() =
        guarded {
            openIndex(dir).use { index ->
                val sections =
                    linkedMapOf(
                        "frameworks" to "SELECT name, evidence FROM frameworks ORDER BY name",
                        "sdks" to "SELECT category, name, classes FROM sdks ORDER BY category, name",
                        "top_hosts" to
                            "SELECT host, count(*) AS urls FROM endpoints WHERE ${Urls.NETWORK_SQL} GROUP BY host ORDER BY urls DESC, host LIMIT 15",
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
    private val dir by argument(help = INDEX_INPUT).path()
    private val host by option("--host", help = "Only URLs whose host contains this text")
    private val limit by option("--limit", help = "Maximum URLs (default 200)").int().default(200)
    private val json by option("--json", help = "One JSON object per URL").flag()

    override fun help(context: Context) = "List URLs found in code strings, resources, deep links and manifest values, with their sources."

    override fun run() =
        guarded {
            openIndex(dir).use { index ->
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

/** `sextant findings <dir>`: review points with their evidence; `--sarif` for code scanning, `--fail-on` for CI. */
class FindingsCommand : CoreCliktCommand(name = "findings") {
    private val dir by argument(help = INDEX_INPUT).path()
    private val severity by option(
        "--severity",
        help = "Minimum severity to show",
    ).choice(*LEVELS.reversed().toTypedArray()).default("info")
    private val json by option("--json", help = "One JSON object per finding").flag()
    private val sarif by option("--sarif", help = "SARIF 2.1.0 for GitHub code scanning and other dashboards").flag()
    private val failOn by option(
        "--fail-on",
        help = "Exit with code 5 if any finding is at or above this severity (for CI); the output is unchanged",
    ).choice(*LEVELS.reversed().toTypedArray())

    override fun help(context: Context) =
        "Show review findings (debuggable, cleartext, exported components, WebView bridges, weak crypto, trust-all TLS, " +
            "embedded secrets, …) with evidence. They are static observations to check, not proven vulnerabilities."

    override fun run() =
        guarded {
            if (json && sarif) throw UsageError("--json and --sarif can't be used together")
            val all = openIndex(dir).use { index -> readFindings(index) to apkName(index) }
            val (findings, apk) = all
            val shown = findings.filter { atLeast(it.severity, severity) }
            when {
                sarif -> println(Sarif.encode(Sarif.build(shown, Sextant.version, apk)))
                json -> shown.forEach { println(SextantJson.compact.encodeToString(Finding.serializer(), it)) }
                else ->
                    for (f in shown) {
                        println("[${f.severity}] ${f.title}  (${f.count})  · ${f.id}")
                        println("    ${f.detail}")
                        f.evidence.take(5).forEach { e -> println("    - ${e.kind} ${e.where}${e.value?.let { "  = $it" } ?: ""}") }
                        if (f.evidence.size > 5) println("    - … ${f.count - 5} more")
                    }
            }
            val plural = if (shown.size == 1) "" else "s"
            System.err.println("${shown.size} finding$plural (static observations to review, not proven issues)")
            val threshold = failOn
            if (threshold != null) {
                val failing = findings.filter { atLeast(it.severity, threshold) }
                if (failing.isNotEmpty()) {
                    throw CliktError(
                        "fail: ${failing.size} finding${if (failing.size == 1) "" else "s"} at or above $threshold " +
                            "(${failing.joinToString { it.id }})",
                        statusCode = ExitCode.FINDINGS.code,
                    )
                }
            }
        }

    private fun readFindings(index: IndexReader): List<Finding> {
        val rows =
            index.query(
                "SELECT id, severity, title, detail, count FROM findings " +
                    "ORDER BY CASE severity WHEN 'high' THEN 0 WHEN 'medium' THEN 1 WHEN 'low' THEN 2 ELSE 3 END, id",
            )
        return rows.rows.map { row ->
            val id = row[0].toString()
            val evidence =
                index
                    .query("SELECT kind, location, value FROM finding_evidence WHERE finding = ${SqlJson.literal(id)}")
                    .rows
                    .map { FindingEvidence(it[0].toString(), it[1].toString(), it[2]?.toString()) }
            Finding(id, row[1].toString(), row[2].toString(), row[3].toString(), evidence, row[4].toString().toInt())
        }
    }

    /** The APK file name from meta.json, for SARIF's artifact list. */
    private fun apkName(index: IndexReader): String? =
        index
            .query("SELECT value FROM meta WHERE key = 'meta_json'")
            .rows
            .firstOrNull()
            ?.get(0)
            ?.let {
                runCatching {
                    Json
                        .parseToJsonElement(
                            it.toString(),
                        ).jsonObject["file"]
                        ?.jsonObject
                        ?.get("name")
                        ?.jsonPrimitive
                        ?.content
                }.getOrNull()
            }

    private companion object {
        /** Most severe first. */
        val LEVELS = listOf("high", "medium", "low", "info")

        fun atLeast(
            severity: String,
            threshold: String,
        ) = LEVELS.indexOf(severity).let { it >= 0 && it <= LEVELS.indexOf(threshold) }
    }
}
