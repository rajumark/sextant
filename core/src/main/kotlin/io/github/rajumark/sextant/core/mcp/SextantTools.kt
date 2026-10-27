package io.github.rajumark.sextant.core.mcp

import io.github.rajumark.sextant.core.Extractor
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.SmaliPrinter
import io.github.rajumark.sextant.core.SourceDecompiler
import io.github.rajumark.sextant.core.index.CallTreeText
import io.github.rajumark.sextant.core.index.ClassReport
import io.github.rajumark.sextant.core.index.Direction
import io.github.rajumark.sextant.core.index.IndexReader
import io.github.rajumark.sextant.core.index.IndexSchema
import io.github.rajumark.sextant.core.index.SearchHit
import io.github.rajumark.sextant.core.insights.AppIcons
import io.github.rajumark.sextant.core.insights.Urls
import io.github.rajumark.sextant.core.mcp.McpTool.Param
import io.github.rajumark.sextant.core.output.OutputFiles
import io.github.rajumark.sextant.core.output.OutputWriter
import io.github.rajumark.sextant.core.output.SextantJson
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * The Sextant tools exposed over MCP. A session remembers the current extraction folder and APK, so after
 * `extract` the other tools need no paths; every tool also accepts an explicit `dir`.
 */
class SextantTools(
    private var dir: Path?,
    private var apk: Path?,
) {
    val instructions =
        """
        Sextant analyzes Android APKs. Start with `extract` (APK path) unless the server was started with a folder.
        Then read `summary` first. Use `find` for any text (names, strings, URLs, resources, SDKs, native symbols),
        `class` for one class, `callers`/`callees` to follow the call graph, `reach` for paths from an entry point to a target, `xref` for class/field uses, `insights` for frameworks/SDKs/libraries,
        `endpoints` for URLs with their sources, `findings` for review points, `source` for decompiled Java, `smali` for bytecode, and `sql` for anything else
        (call `sql` with query "schema" to see all tables). IDs are smali descriptors like Lcom/a/B;->m(I)V.
        """.trimIndent()

    private fun folder(args: JsonObject): Path {
        val d = args.string("dir")?.let { Paths.get(it) } ?: dir
        return d ?: throw SextantException.input("no extraction folder yet", "call `extract` with an APK path first, or pass `dir`")
    }

    private fun <T> index(
        args: JsonObject,
        block: (IndexReader) -> T,
    ): T = IndexReader.open(folder(args)).use(block)

    private val dirParam = Param("dir", "string", "Extraction folder (defaults to the current session folder)")

    val tools: List<McpTool> =
        listOf(
            McpTool(
                "extract",
                "Analyze an APK: writes structured files plus index.sqlite and makes it the session's current app. Returns summary.md.",
                listOf(
                    Param("apk", "string", "Path to the .apk file", required = true),
                    Param("out", "string", "Output folder (default: <apk-name>.sextant next to the APK)"),
                ),
            ) { args ->
                val apkPath = Paths.get(args.string("apk")!!)
                val out =
                    args.string("out")?.let { Paths.get(it) }
                        ?: apkPath.resolveSibling(apkPath.fileName.toString().substringBeforeLast('.') + ".sextant")
                OutputWriter.extractTo(apkPath, out, force = true)
                dir = out
                apk = apkPath
                "Extracted to $out\n\n" + Files.readString(out.resolve(OutputFiles.SUMMARY))
            },
            McpTool(
                "summary",
                "The app overview (summary.md): identity, counts, exported components, insights, files.",
                listOf(dirParam),
            ) { args ->
                Files.readString(folder(args).resolve(OutputFiles.SUMMARY))
            },
            McpTool(
                "find",
                "Substring search over classes, methods, fields, strings, components, permissions, deep links, resources, " +
                    "endpoints, SDKs, libraries and native symbols. Returns JSON lines {kind, ref, text, used_in}.",
                listOf(
                    Param("text", "string", "Text to find (3+ characters use the full-text index)", required = true),
                    Param("kind", "string", "Only this kind: ${IndexSchema.KINDS.joinToString()}"),
                    Param("limit", "integer", "Maximum results (default 30)"),
                    dirParam,
                ),
            ) { args ->
                index(args) { idx ->
                    val hits = idx.search(args.string("text")!!, args.string("kind"), args.int("limit", 30))
                    hits
                        .joinToString("\n") { hit -> hitJson(hit, if (hit.kind == "string") idx.stringUsers(hit.ref, 5) else emptyList()) }
                        .ifEmpty { "no results" }
                }
            },
            McpTool(
                "class",
                "Everything about one class as JSON: hierarchy, fields with constant values, methods with call counts, outside callers, strings.",
                listOf(Param("name", "string", "com.app.Login, Lcom/app/Login; or just Login", required = true), dirParam),
            ) { args ->
                index(args) { idx ->
                    val matches = idx.findClasses(args.string("name")!!)
                    when {
                        matches.isEmpty() -> throw SextantException.input(
                            "no class matches '${args.string("name")}'",
                            "use `find` with kind=class",
                        )
                        matches.size > 1 -> "Several classes match; call again with one of:\n" + matches.joinToString("\n")
                        else -> SextantJson.compact.encodeToString(ClassReport.serializer(), idx.classReport(matches.single()))
                    }
                }
            },
            callTool("callers", Direction.CALLERS, "Who calls a method, as an indented tree (static call edges)."),
            callTool("callees", Direction.CALLEES, "What a method calls, as an indented tree; external methods are leaves."),
            McpTool(
                "sql",
                "Read-only SQL over index.sqlite. Returns JSON lines, one per row. Pass query \"schema\" to get all CREATE statements.",
                listOf(
                    Param("query", "string", "SQL, or \"schema\"", required = true),
                    Param("limit", "integer", "Maximum rows (default 200)"),
                    dirParam,
                ),
            ) { args ->
                index(args) { idx ->
                    val q = args.string("query")!!
                    if (q.trim().equals("schema", ignoreCase = true)) return@index idx.schema()
                    val r = idx.query(q, args.int("limit", 200))
                    val rows =
                        r.rows.joinToString("\n") { row ->
                            JsonObject(
                                r.columns.zip(row).associate { (c, v) ->
                                    c to
                                        when (v) {
                                            null -> JsonNull
                                            is Number -> JsonPrimitive(v)
                                            else -> JsonPrimitive(v.toString())
                                        }
                                },
                            ).toString()
                        }
                    rows.ifEmpty { "no rows" } + if (r.truncated) "\n(stopped at limit ${args.int("limit", 200)})" else ""
                }
            },
            McpTool(
                "source",
                "Decompiled Java (jadx) for one class or method. Needs the APK (from `extract`, or pass `apk`).",
                listOf(
                    Param("target", "string", "com.a.B, Lcom/a/B;, Lcom/a/B;->m(I)V or com.a.B.m", required = true),
                    Param("apk", "string", "APK path (defaults to the session APK)"),
                ),
            ) { args ->
                val apkPath =
                    args.string("apk")?.let { Paths.get(it) } ?: apk
                        ?: throw SextantException.input("no APK in this session", "pass `apk`, or call `extract` first")
                SourceDecompiler.decompile(apkPath, args.string("target")!!).code
            },
            McpTool(
                "insights",
                "Frameworks, SDKs, exact library versions, top hosts and native libraries as JSON.",
                listOf(dirParam),
            ) { args ->
                index(args) { idx ->
                    listOf(
                        "frameworks" to "SELECT name, evidence FROM frameworks ORDER BY name",
                        "sdks" to "SELECT category, name, classes FROM sdks ORDER BY category, name",
                        "top_hosts" to
                            "SELECT host, count(*) AS urls FROM endpoints WHERE ${Urls.NETWORK_SQL} GROUP BY host ORDER BY urls DESC, host LIMIT 20",
                        "libraries" to "SELECT \"group\", artifact, version FROM libraries ORDER BY \"group\", artifact",
                        "native" to
                            "SELECT name, max(exported_symbols) AS exported, max(jni_methods) AS jni FROM native_libraries GROUP BY name ORDER BY jni DESC",
                    ).joinToString("\n\n") { (title, sql) ->
                        val r = idx.query(sql)
                        "## $title\n" + r.rows.joinToString("\n") { row -> r.columns.zip(row).joinToString(", ") { (c, v) -> "$c=$v" } }
                    }
                }
            },
            McpTool(
                "xref",
                "Where a class or field is used: instantiations, casts, instanceof, const-class, calls into its methods, field reads/writes.",
                listOf(
                    Param(
                        "target",
                        "string",
                        "Class (com.a.B, Lcom/a/B;) or field (Lcom/a/B;->f:I, Lcom/a/B;->f, com.a.B.f)",
                        required = true,
                    ),
                    Param("limit", "integer", "Maximum rows (default 100)"),
                    dirParam,
                ),
            ) { args ->
                index(args) { idx ->
                    idx
                        .xref(
                            args.string("target")!!,
                            args.int("limit", 100),
                        ).groupBy { it.relation }
                        .entries
                        .joinToString("\n") { (rel, rows) ->
                            rel + "\n" + rows.joinToString("\n") { "    ${it.method} ×${it.count}" }
                        }.ifEmpty { "no references" }
                }
            },
            McpTool(
                "reach",
                "Call paths from an entry point (component/class or method) to target methods, following virtual/interface dispatch " +
                    "into subclasses (generic signatures with >25 implementations are not expanded). Static approximation.",
                listOf(
                    Param("from", "string", "Entry point: component/class name or method", required = true),
                    Param(
                        "to",
                        "string",
                        "Target: Ljavax/crypto/Cipher;->getInstance, Cipher.getInstance, okhttp3/ or a method name",
                        required = true,
                    ),
                    Param("depth", "integer", "Maximum call depth (default 10)"),
                    Param("paths", "integer", "Maximum paths (default 3)"),
                    dirParam,
                ),
            ) { args ->
                index(args) { idx ->
                    val r =
                        idx.reach(
                            args.string("from")!!,
                            args.string("to")!!,
                            args.int("depth", 10).coerceIn(1, 50),
                            args.int("paths", 3).coerceIn(1, 50),
                        )
                    if (r.paths.isEmpty()) {
                        "No path found (${r.entries.size} entry methods, ${r.visited} methods visited)."
                    } else {
                        r.paths.joinToString("\n\n") { p ->
                            p.steps.joinToString("\n") { s ->
                                (if (s.via == null) "" else "→ ") + s.method +
                                    if (s.via == "override") " (via override)" else ""
                            }
                        }
                    }
                }
            },
            McpTool(
                "smali",
                "Smali (bytecode assembly) for a class or method; works where decompiling fails. #@ offsets are hex code units.",
                listOf(
                    Param("target", "string", "Class or method descriptor", required = true),
                    Param("apk", "string", "APK path (defaults to the session APK)"),
                ),
            ) { args ->
                val apkPath =
                    args.string("apk")?.let { Paths.get(it) } ?: apk
                        ?: throw SextantException.input("no APK in this session", "pass `apk`, or call `extract` first")
                SmaliPrinter.print(apkPath, args.string("target")!!)
            },
            McpTool(
                "icon",
                "The app's launcher icons: every file behind android:icon and android:roundIcon per density, adaptive icon layers " +
                    "(foreground/background/monochrome), pixel sizes and the best file. JSON.",
                listOf(Param("apk", "string", "APK path (defaults to the session APK)")),
            ) { args ->
                val apkPath =
                    args.string("apk")?.let { Paths.get(it) } ?: apk
                        ?: throw SextantException.input("no APK in this session", "pass `apk`, or call `extract` first")
                SextantJson.compact.encodeToString(AppIcons.serializer(), Extractor.icons(apkPath))
            },
            McpTool(
                "findings",
                "Review findings with evidence (manifest flags, exported components, WebView bridges, weak crypto, trust-all TLS, " +
                    "embedded secrets). Static observations to check, not proven vulnerabilities.",
                listOf(dirParam),
            ) { args ->
                index(args) { idx ->
                    val f = idx.query("SELECT id, severity, title, detail, count FROM findings ORDER BY severity, id")
                    f.rows
                        .joinToString("\n\n") { row ->
                            val ev =
                                idx.query(
                                    "SELECT kind, location, value FROM finding_evidence WHERE finding = '${row[0].toString().replace(
                                        "'",
                                        "''",
                                    )}' LIMIT 10",
                                )
                            "[${row[1]}] ${row[2]} (${row[4]}) · ${row[0]}\n${row[3]}\n" +
                                ev.rows.joinToString("\n") { e -> "- ${e[0]} ${e[1]}${e[2]?.let { "  = $it" } ?: ""}" }
                        }.ifEmpty { "no findings" }
                }
            },
            McpTool(
                "endpoints",
                "URLs found in code, resources, deep links, manifest values, assets and native libraries, with their sources.",
                listOf(
                    Param("host", "string", "Only hosts containing this text"),
                    Param("limit", "integer", "Maximum URLs (default 100)"),
                    dirParam,
                ),
            ) { args ->
                index(args) { idx ->
                    val filter = args.string("host")?.let { "WHERE e.host LIKE '%' || '${it.replace("'", "''")}' || '%'" } ?: ""
                    val r =
                        idx.query(
                            "SELECT e.url, group_concat(s.kind || ' ' || s.location, '; ') AS sources FROM endpoints e " +
                                "JOIN endpoint_sources s ON s.endpoint = e.id $filter GROUP BY e.id ORDER BY e.host, e.url",
                            args.int("limit", 100),
                        )
                    r.rows.joinToString("\n") { "${it[0]}  ←  ${it[1]}" }.ifEmpty { "no endpoints" }
                }
            },
        )

    private fun callTool(
        name: String,
        direction: Direction,
        description: String,
    ) = McpTool(
        name,
        "$description Method: Lcom/a/B;->m(I)V, Lcom/a/B;->m, com.a.B.m, B.m or m.",
        listOf(
            Param("method", "string", "The method", required = true),
            Param("depth", "integer", "Levels to follow (default 3)"),
            Param("fan_out", "integer", "Maximum children per method (default 15)"),
            dirParam,
        ),
    ) { args ->
        index(args) { idx ->
            val matches = idx.resolveMethods(args.string("method")!!)
            val groups = matches.groupBy { it.second.substringBefore('(') }
            when {
                matches.isEmpty() -> throw SextantException.input(
                    "no method matches '${args.string("method")}'",
                    "use `find` with kind=method",
                )
                groups.size > 1 -> "Several methods match; call again with one of:\n" + groups.keys.joinToString("\n")
                else ->
                    matches.joinToString("\n") { (id, _) ->
                        CallTreeText.render(
                            idx.callTree(id, direction, args.int("depth", 3).coerceIn(1, 10), args.int("fan_out", 15).coerceIn(1, 200)),
                        )
                    }
            }
        }
    }

    private fun hitJson(
        hit: SearchHit,
        users: List<String>,
    ) = buildString {
        append("{\"kind\":").append(JsonPrimitive(hit.kind))
        append(",\"ref\":").append(JsonPrimitive(if (hit.kind == "string") hit.text else hit.ref))
        append(",\"text\":").append(JsonPrimitive(hit.text.take(500)))
        if (users.isNotEmpty()) append(",\"used_in\":").append(users.joinToString(",", "[", "]") { JsonPrimitive(it).toString() })
        append("}")
    }
}
