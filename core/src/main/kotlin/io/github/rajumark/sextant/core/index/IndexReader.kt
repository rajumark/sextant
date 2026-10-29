package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.model.ApkFile
import io.github.rajumark.sextant.core.model.AppMeta
import io.github.rajumark.sextant.core.model.XmlNode
import io.github.rajumark.sextant.core.output.OutputFiles
import io.github.rajumark.sextant.core.output.SextantJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.sqlite.SQLiteConfig
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

/** Result of an SQL query: column names plus rows of nullable strings/numbers. */
class QueryResult(
    val columns: List<String>,
    val rows: List<List<Any?>>,
    val truncated: Boolean,
)

@Serializable
data class XrefRow(
    /** new | class | cast | instanceof | array | call <method> | read/write <field> */
    val relation: String,
    val method: String,
    val count: Int,
)

@Serializable
data class SearchHit(
    val kind: String,
    val ref: String,
    val text: String,
)

@Serializable
data class ClassReport(
    val descriptor: String,
    val name: String,
    val superclass: String?,
    val interfaces: List<String>,
    val access: String,
    val sourceFile: String?,
    val dex: String,
    val kotlin: Boolean,
    val subclasses: List<String>,
    val fields: List<FieldLine>,
    val methods: List<MethodLine>,
    val callers: List<CallerLine>,
    val strings: List<String>,
)

@Serializable
data class FieldLine(
    val name: String,
    val type: String,
    val access: String,
    val initialValue: String?,
)

@Serializable
data class MethodLine(
    val descriptor: String,
    val name: String,
    val access: String,
    val instructions: Int,
    val callsOut: Int,
    val calledFrom: Int,
)

@Serializable
data class CallerLine(
    val caller: String,
    val callee: String,
    val count: Int,
)

/** One row of `sextant coverage`: the zip entries of one type and how many a decoder read. */
@Serializable
data class CoverageRow(
    val type: String,
    val files: Int,
    val decoded: Int,
    val bytes: Long,
    val decodedBytes: Long,
    /** A few paths of this type that no decoder read. */
    val listedOnly: List<String>,
)

/** One element found by `sextant xml --tag/--attr/--value`. */
@Serializable
data class XmlHit(
    val path: String,
    val resource: String?,
    val tag: String,
    /** The matching attribute, or null when only the tag was asked for. */
    val attribute: String?,
    val value: String?,
)

/** One edge next to a key: `out` = key → [other], `in` = [other] → key. */
@Serializable
data class LinkRow(
    val direction: String,
    val rel: String,
    val other: String,
    val via: String?,
)

/** One matching line of a text file inside the APK. */
@Serializable
data class TextLine(
    val path: String,
    val line: Int,
    val text: String,
)

/** Read-only access to index.sqlite. */
class IndexReader private constructor(
    private val conn: Connection,
) : AutoCloseable {
    companion object {
        private const val STRING_USERS =
            "SELECT m.descriptor FROM string_refs r JOIN methods m ON m.id = r.method WHERE r.string = ? LIMIT ?"
        private const val CLASS_SUFFIX =
            "SELECT descriptor FROM classes WHERE name LIKE ? ESCAPE '\\' OR name = ? ORDER BY descriptor LIMIT 50"

        /**
         * Opens an extracted folder, an index.sqlite file, or an APK. An APK is read through [IndexCache]: built once,
         * then reused; [onBuild] is told before a build starts.
         */
        fun open(
            dir: Path,
            onBuild: (String) -> Unit = {},
        ): IndexReader {
            val file =
                when {
                    Files.isDirectory(dir) -> dir.resolve(OutputFiles.INDEX)
                    Files.isRegularFile(dir) && !isSqlite(dir) -> IndexCache.index(dir, onBuild)
                    else -> dir
                }
            if (!Files.isRegularFile(file)) {
                throw SextantException.input("no index at $file", "run `sextant extract <app.apk>` (0.3.0 or newer) to build index.sqlite")
            }
            val config =
                SQLiteConfig().apply {
                    setReadOnly(true)
                }
            return IndexReader(config.createConnection("jdbc:sqlite:$file"))
        }

        /** True when [file] starts with the SQLite header (an index.sqlite rather than an APK). */
        private fun isSqlite(file: Path): Boolean =
            Files.newInputStream(file).use { input -> String(input.readNBytes(16), Charsets.ISO_8859_1) == "SQLite format 3\u0000" }
    }

    /** Runs any read-only SQL. At most [limit] rows are returned. [params] bind, in order, SQLite's numbered and named parameters. */
    fun query(
        sql: String,
        limit: Int = Int.MAX_VALUE,
        params: List<String> = emptyList(),
    ): QueryResult =
        try {
            conn.prepareStatement(sql).use { st ->
                params.forEachIndexed { i, p -> st.setString(i + 1, p) }
                // execute(), not executeQuery(): a write then reaches SQLite and fails as "readonly", the real reason.
                if (!st.execute()) return@use QueryResult(emptyList(), emptyList(), false)
                st.resultSet.use { rs ->
                    val meta = rs.metaData
                    val columns = (1..meta.columnCount).map { meta.getColumnLabel(it) }
                    val rows = mutableListOf<List<Any?>>()
                    var truncated = false
                    while (rs.next()) {
                        if (rows.size >= limit) {
                            truncated = true
                            break
                        }
                        rows += columns.indices.map { rs.getObject(it + 1) }
                    }
                    QueryResult(columns, rows, truncated)
                }
            }
        } catch (e: SQLException) {
            throw SextantException.input(
                "SQL error: ${e.message?.removePrefix("[SQLITE_ERROR] SQL error or missing database ")}",
                cause = e,
            )
        }

    /** The CREATE statements of every table and view, for `sql --schema`. */
    fun schema(): String =
        query("SELECT sql FROM sqlite_master WHERE sql IS NOT NULL AND name NOT GLOB '*search_*' ORDER BY rowid")
            .rows
            .joinToString(";\n\n", postfix = ";\n") { it[0].toString() }

    /**
     * Substring search over names and strings. Three or more characters use the trigram full-text index;
     * shorter queries fall back to a LIKE scan.
     */
    fun search(
        text: String,
        kind: String? = null,
        limit: Int = 50,
    ): List<SearchHit> {
        val kindFilter = if (kind != null) "AND (rowid & 15) = ${IndexSchema.kindCode(kind)}" else ""
        val rowids =
            try {
                if (text.length >= 3) {
                    conn.prepareStatement("SELECT rowid FROM search WHERE search MATCH ? $kindFilter ORDER BY rank LIMIT ?").use { st ->
                        st.setString(1, "\"" + text.replace("\"", "\"\"") + "\"")
                        st.setInt(2, limit)
                        st.executeQuery().use { rs -> rs.map { it.getLong(1) } }
                    }
                } else {
                    // Trigram needs 3 characters; shorter text falls back to a LIKE scan of the source tables.
                    val itemFilter = kindFilter.replace("rowid", "search_rowid")
                    val sql = "SELECT search_rowid FROM search_items WHERE text LIKE ? ESCAPE '\\' $itemFilter LIMIT ?"
                    conn.prepareStatement(sql).use { st ->
                        st.setString(1, "%" + likeEscape(text) + "%")
                        st.setInt(2, limit)
                        st.executeQuery().use { rs -> rs.map { it.getLong(1) } }
                    }
                }
            } catch (e: SQLException) {
                throw SextantException.input("search failed: ${e.message}", cause = e)
            }
        return rowids.mapNotNull(::resolve)
    }

    /** Turns a search rowid back into the row it was built from. */
    private fun resolve(rowid: Long): SearchHit? {
        val id = rowid shr IndexSchema.KIND_BITS
        val kind = IndexSchema.KINDS.getOrNull((rowid and 15).toInt() - 1) ?: return null
        val sql =
            when (kind) {
                "class" -> "SELECT descriptor, name FROM classes WHERE id = ?"
                "method" -> "SELECT descriptor, descriptor FROM methods WHERE id = ?"
                "field" -> "SELECT descriptor, descriptor || coalesce(' = ' || initial_value, '') FROM fields WHERE id = ?"
                "string" -> "SELECT id, value FROM strings WHERE id = ?"
                "component" -> "SELECT name, type || ' ' || name FROM components WHERE rowid = ?"
                "permission" -> "SELECT name, kind || ' ' || name FROM permissions WHERE rowid = ?"
                "deep_link" -> "SELECT component, url FROM deep_links WHERE rowid = ?"
                "resource" -> "SELECT '@' || type || '/' || name, value FROM resources WHERE rowid = ?"
                "endpoint" -> "SELECT url, url FROM endpoints WHERE id = ?"
                "sdk" -> "SELECT name, name || ' (' || category || ', ' || classes || ' classes)' FROM sdks WHERE rowid = ?"
                "library" ->
                    "SELECT \"group\" || ':' || artifact, \"group\" || ':' || artifact || ':' || version FROM libraries WHERE rowid = ?"
                "finding" -> "SELECT id, severity || ' · ' || title || ' (' || count || ')' FROM findings WHERE rowid = ?"
                "native_symbol" ->
                    "SELECT library, name || coalesce('  →  ' || jni_class || '.' || jni_method, '') FROM native_symbols WHERE rowid = ?"
                "string_resource" ->
                    "SELECT '@' || kind || '/' || name, name || ' = ' || value || coalesce('  (' || nullif(config, '') || ')', '') FROM string_resources WHERE rowid = ?"
                "native_string" -> "SELECT path, value FROM elf_strings WHERE rowid = ?"
                else -> return null
            }
        return conn.prepareStatement(sql).use { st ->
            st.setLong(1, id)
            st.executeQuery().use { rs -> if (rs.next()) SearchHit(kind, rs.getString(1), rs.getString(2)) else null }
        }
    }

    /** Methods that load a string constant (for search hits of kind `string`). */
    fun stringUsers(
        stringId: String,
        limit: Int,
    ): List<String> =
        conn.prepareStatement(STRING_USERS).use { st ->
            st.setLong(1, stringId.toLong())
            st.setInt(2, limit)
            st.executeQuery().use { rs -> rs.map { it.getString(1) } }
        }

    /** Finds classes by descriptor, dotted name, or simple-name suffix. */
    fun findClasses(query: String): List<String> {
        val descriptor = if (query.startsWith("L") && query.endsWith(";")) query else null
        conn.prepareStatement("SELECT descriptor FROM classes WHERE descriptor = ? OR name = ? ORDER BY descriptor").use { st ->
            st.setString(1, descriptor ?: "L${query.replace('.', '/')};")
            st.setString(2, query)
            val exact = st.executeQuery().use { rs -> rs.map { it.getString(1) } }
            if (exact.isNotEmpty()) return exact
        }
        conn.prepareStatement(CLASS_SUFFIX).use { st ->
            st.setString(1, "%." + likeEscape(query))
            st.setString(2, query)
            return st.executeQuery().use { rs -> rs.map { it.getString(1) } }
        }
    }

    fun classReport(
        descriptor: String,
        maxCallers: Int = 25,
        maxStrings: Int = 25,
    ): ClassReport {
        val base =
            conn.prepareStatement("SELECT * FROM classes WHERE descriptor = ?").use { st ->
                st.setString(1, descriptor)
                st.executeQuery().use { rs ->
                    if (!rs.next()) throw SextantException.input("class $descriptor not found")
                    ClassReport(
                        descriptor = descriptor,
                        name = rs.getString("name"),
                        superclass = rs.getString("superclass"),
                        interfaces = jsonList(rs.getString("interfaces")),
                        access = rs.getString("access"),
                        sourceFile = rs.getString("source_file"),
                        dex = rs.getString("dex"),
                        kotlin = rs.getInt("kotlin") == 1,
                        subclasses = emptyList(),
                        fields = emptyList(),
                        methods = emptyList(),
                        callers = emptyList(),
                        strings = emptyList(),
                    )
                }
            }
        val subclasses =
            strings("SELECT descriptor FROM classes WHERE superclass = ? ORDER BY descriptor LIMIT 50", descriptor)
        val fields =
            conn.prepareStatement("SELECT name, type, access, initial_value FROM fields WHERE class = ? ORDER BY descriptor").use { st ->
                st.setString(1, descriptor)
                st.executeQuery().use { rs -> rs.map { FieldLine(it.getString(1), it.getString(2), it.getString(3), it.getString(4)) } }
            }
        val methods =
            conn
                .prepareStatement(
                    """
                    SELECT m.descriptor, m.name, m.access, m.instructions, m.calls_out,
                           (SELECT coalesce(sum(c.count), 0) FROM calls c WHERE c.callee = m.id)
                    FROM methods m WHERE m.class = ? AND m.defined = 1 ORDER BY m.descriptor
                    """.trimIndent(),
                ).use { st ->
                    st.setString(1, descriptor)
                    st.executeQuery().use { rs ->
                        rs.map { MethodLine(it.getString(1), it.getString(2), it.getString(3), it.getInt(4), it.getInt(5), it.getInt(6)) }
                    }
                }
        val callers =
            conn
                .prepareStatement(
                    """
                    SELECT a.descriptor, b.descriptor, c.count FROM calls c
                    JOIN methods b ON b.id = c.callee JOIN methods a ON a.id = c.caller
                    WHERE b.class = ? AND a.class != ? ORDER BY c.count DESC, a.descriptor LIMIT ?
                    """.trimIndent(),
                ).use { st ->
                    st.setString(1, descriptor)
                    st.setString(2, descriptor)
                    st.setInt(3, maxCallers)
                    st.executeQuery().use { rs -> rs.map { CallerLine(it.getString(1), it.getString(2), it.getInt(3)) } }
                }
        val strings =
            conn
                .prepareStatement(
                    """
                    SELECT DISTINCT s.value FROM string_refs r JOIN strings s ON s.id = r.string JOIN methods m ON m.id = r.method
                    WHERE m.class = ? ORDER BY s.value LIMIT ?
                    """.trimIndent(),
                ).use { st ->
                    st.setString(1, descriptor)
                    st.setInt(2, maxStrings)
                    st.executeQuery().use { rs -> rs.map { it.getString(1) } }
                }
        return base.copy(subclasses = subclasses, fields = fields, methods = methods, callers = callers, strings = strings)
    }

    private fun strings(
        sql: String,
        arg: String,
    ): List<String> =
        conn.prepareStatement(sql).use { st ->
            st.setString(1, arg)
            st.executeQuery().use { rs -> rs.map { it.getString(1) } }
        }

    /**
     * Resolves a method query to method ids. Accepts a full descriptor (`Lcom/a/B;->m(I)V`), a descriptor without
     * signature (`Lcom/a/B;->m`), `com.a.B.m`, `B.m`, or just `m`. Returns all matches; overloads of one method
     * (same class and name) are returned together.
     */
    fun resolveMethods(query: String): List<Pair<Long, String>> {
        fun run(
            sql: String,
            vararg args: String,
        ) = conn.prepareStatement("$sql ORDER BY defined DESC, descriptor LIMIT 200").use { st ->
            args.forEachIndexed { i, a -> st.setString(i + 1, a) }
            st.executeQuery().use { rs -> rs.map { it.getLong(1) to it.getString(2) } }
        }
        val base = "SELECT id, descriptor FROM methods WHERE"
        if ("->" in query) {
            return if ("(" in query) {
                run("$base descriptor = ?", query)
            } else {
                run("$base class = ? AND name = ?", query.substringBefore("->"), query.substringAfter("->"))
            }
        }
        if ('.' !in query) return run("$base name = ? AND defined = 1", query)
        val owner = query.substringBeforeLast('.')
        val name = query.substringAfterLast('.')
        val exact = run("$base class = ? AND name = ?", "L${owner.replace('.', '/')};", name)
        if (exact.isNotEmpty()) return exact
        return run("$base name = ? AND (class LIKE ? ESCAPE '\\' OR class = ?)", name, "%/" + likeEscape(owner) + ";", "L$owner;")
    }

    /** Builds a caller or callee tree from [root] (a method id from [resolveMethods]). */
    fun callTree(
        root: Long,
        direction: Direction,
        depth: Int,
        fanOut: Int,
    ): CallNode {
        val builder = CallTreeBuilder(conn, direction, depth, fanOut)
        try {
            return builder.build(root)
        } finally {
            builder.close()
        }
    }

    /**
     * Cross-references. A field target (`Lcom/a/B;->f:I`, `Lcom/a/B;->f`, `com.a.B.f`) lists methods that read/write it;
     * a class target lists methods that instantiate, cast, check, reference it, call its methods or use its fields.
     * Each row is (relation, method, count).
     */
    fun xref(
        target: String,
        limit: Int,
    ): List<XrefRow> {
        val fieldPattern =
            when {
                "->" in target && ':' in target -> target
                "->" in target -> target + ":%"
                else -> null
            }
        if (fieldPattern != null) return fieldXref(fieldPattern, limit)
        val classes = findClasses(target)
        if (classes.size == 1) return classXref(classes.single(), limit)
        if (classes.size >
            1
        ) {
            throw SextantException.input("several classes match '$target': ${classes.take(5).joinToString()}", "pass the full name")
        }
        // `com.a.B.f` → field f of com.a.B
        if ('.' in target) {
            val owner = "L" + target.substringBeforeLast('.').replace('.', '/') + ";"
            val rows = fieldXref("$owner->${target.substringAfterLast('.')}:%", limit)
            if (rows.isNotEmpty()) return rows
        }
        throw SextantException.input("nothing matches '$target'", "use a class name or a field like Lcom/a/B;->name")
    }

    private fun fieldXref(
        pattern: String,
        limit: Int,
    ) = conn
        .prepareStatement(
            "SELECT r.kind || ' ' || r.field, m.descriptor, r.count FROM field_refs r JOIN methods m ON m.id = r.method " +
                "WHERE r.field LIKE ? ESCAPE '\\' ORDER BY r.field, r.kind, m.descriptor LIMIT ?",
        ).use { st ->
            st.setString(1, likeEscape(pattern).replace("\\%", "%"))
            st.setInt(2, limit)
            st.executeQuery().use { rs -> rs.map { XrefRow(it.getString(1), it.getString(2), it.getInt(3)) } }
        }

    private fun classXref(
        descriptor: String,
        limit: Int,
    ): List<XrefRow> {
        val sql =
            """
            SELECT r.kind, m.descriptor, r.count FROM type_refs r JOIN methods m ON m.id = r.method WHERE r.type = ?1
            UNION ALL
            SELECT 'call ' || substr(b.descriptor, length(?1) + 3), a.descriptor, c.count
              FROM calls c JOIN methods b ON b.id = c.callee JOIN methods a ON a.id = c.caller WHERE b.class = ?1 AND a.class != ?1
            UNION ALL
            SELECT f.kind || ' ' || substr(f.field, length(?1) + 3), m.descriptor, f.count
              FROM field_refs f JOIN methods m ON m.id = f.method WHERE f.field LIKE ?2 ESCAPE '\' AND m.class != ?1
            ORDER BY 1, 2 LIMIT ?3
            """.trimIndent()
        return conn.prepareStatement(sql).use { st ->
            st.setString(1, descriptor)
            st.setString(2, likeEscape(descriptor) + "->%")
            st.setInt(3, limit)
            st.executeQuery().use { rs -> rs.map { XrefRow(it.getString(1), it.getString(2), it.getInt(3)) } }
        }
    }

    /**
     * Call paths from [from] (a component or class name, or a method) to methods matching [to]. See [ReachGraph].
     */
    fun reach(
        from: String,
        to: String,
        maxDepth: Int,
        maxPaths: Int,
    ): ReachResult {
        val graph = ReachGraph(conn)
        val classes = findClasses(from)
        val entries =
            when {
                classes.size == 1 -> graph.entriesOfClass(classes.single())
                classes.size > 1 -> throw SextantException.input(
                    "several classes match '$from': ${classes.take(5).joinToString()}",
                    "pass the full name",
                )
                else -> graph.idsOf(resolveMethods(from).map { it.second })
            }
        if (entries.isEmpty()) throw SextantException.input("no entry point matches '$from'", "pass a component/class name or a method")
        return graph.search(entries, reachTarget(to), maxDepth, maxPaths)
    }

    /** Zip entries, sorted by path (or by size, largest first). [decoded]: true = decoded only, false = listed only. */
    fun files(
        type: String? = null,
        decoded: Boolean? = null,
        search: String? = null,
        bySize: Boolean = false,
        limit: Int = Int.MAX_VALUE,
    ): Pair<List<ApkFile>, Int> {
        requireFiles()
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        type?.let {
            where += "type = ?"
            args += it
        }
        decoded?.let { where += if (it) "decoded_by IS NOT NULL" else "decoded_by IS NULL" }
        search?.let {
            where += "(path LIKE ? ESCAPE '\\' OR resource LIKE ? ESCAPE '\\')"
            val like = "%" + likeEscape(it) + "%"
            args += like
            args += like
        }
        val filter = if (where.isEmpty()) "" else "WHERE " + where.joinToString(" AND ")
        val total =
            conn.prepareStatement("SELECT count(*) FROM files $filter").use { st ->
                args.forEachIndexed { i, a -> st.setString(i + 1, a) }
                st.executeQuery().use {
                    it.next()
                    it.getInt(1)
                }
            }
        val order = if (bySize) "size_bytes DESC, path" else "path"
        val rows =
            conn.prepareStatement("SELECT * FROM files $filter ORDER BY $order LIMIT ?").use { st ->
                args.forEachIndexed { i, a -> st.setString(i + 1, a) }
                st.setInt(args.size + 1, limit)
                st.executeQuery().use { rs ->
                    rs.map {
                        ApkFile(
                            path = it.getString("path"),
                            type = it.getString("type"),
                            sizeBytes = it.getLong("size_bytes"),
                            compressedBytes = it.getLong("compressed_bytes"),
                            method = it.getString("method"),
                            crc32 = it.getString("crc32"),
                            sha256 = it.getString("sha256"),
                            magic = it.getString("magic"),
                            resource = it.getString("resource"),
                            config = it.getString("config"),
                            decodedBy = it.getString("decoded_by"),
                        )
                    }
                }
            }
        return rows to total
    }

    /** Per file type: how many zip entries and bytes a decoder read. Largest types first. */
    fun coverage(examples: Int = 3): List<CoverageRow> {
        requireFiles()
        val sql =
            "SELECT type, count(*), count(decoded_by), sum(size_bytes), " +
                "coalesce(sum(CASE WHEN decoded_by IS NOT NULL THEN size_bytes END), 0) FROM files GROUP BY type ORDER BY count(*) DESC, type"
        val rows =
            conn.createStatement().use { st ->
                st.executeQuery(sql).use { rs ->
                    rs.map { CoverageRow(it.getString(1), it.getInt(2), it.getInt(3), it.getLong(4), it.getLong(5), emptyList()) }
                }
            }
        return conn
            .prepareStatement(
                "SELECT path FROM files WHERE type = ? AND decoded_by IS NULL ORDER BY size_bytes DESC, path LIMIT ?",
            ).use { st ->
                rows.map { row ->
                    if (row.decoded == row.files) return@map row
                    st.setString(1, row.type)
                    st.setInt(2, examples)
                    row.copy(listedOnly = st.executeQuery().use { rs -> rs.map { it.getString(1) } })
                }
            }
    }

    /** The whole extraction (manifest, resources, insights, files), rebuilt from this index alone. Code stays in its tables. */
    fun extraction(): Extraction {
        val metaJson = meta("meta_json") ?: throw SextantException.input("this index has no meta_json", "re-extract the APK")
        val meta = SextantJson.reader.decodeFromString(AppMeta.serializer(), metaJson)
        val files = if (hasTable("files")) files().first else emptyList()
        return Records.read(conn, meta, files)
    }

    private fun hasTable(name: String): Boolean =
        conn.prepareStatement("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?").use { st ->
            st.setString(1, name)
            st.executeQuery().use { it.next() }
        }

    /**
     * Paths of the binary XML files that match [target]: a path inside the APK (`res/BW.xml`, `AndroidManifest.xml`)
     * or a resource (`@layout/activity_main`, every configuration, default first).
     */
    fun xmlPaths(target: String): List<String> {
        requireTable("xml_nodes", "XML nodes need 10")
        val sql =
            if (target.startsWith("@")) {
                "SELECT DISTINCT n.path FROM xml_nodes n JOIN files f ON f.id = n.file WHERE f.resource = ? ORDER BY f.config != '', f.config"
            } else {
                "SELECT DISTINCT path FROM xml_nodes WHERE path = ?"
            }
        return conn.prepareStatement(sql).use { st ->
            st.setString(1, target)
            st.executeQuery().use { rs -> rs.map { it.getString(1) } }
        }
    }

    /** The file at [path], rebuilt from xml_nodes and xml_attrs. */
    fun xmlTree(path: String): XmlNode? {
        requireTable("xml_nodes", "XML nodes need 10")

        class Row(
            val id: Long,
            val parent: Long?,
            val tag: String,
            val text: String?,
        )
        val rows =
            conn.prepareStatement("SELECT id, parent, tag, text FROM xml_nodes WHERE path = ? ORDER BY seq").use { st ->
                st.setString(1, path)
                st.executeQuery().use { rs ->
                    rs.map { Row(it.getLong(1), it.getLong(2).takeUnless { _ -> it.wasNull() }, it.getString(3), it.getString(4)) }
                }
            }
        if (rows.isEmpty()) return null
        val attrs = HashMap<Long, MutableMap<String, String>>()
        conn
            .prepareStatement(
                "SELECT a.node, a.name, a.value FROM xml_attrs a JOIN xml_nodes n ON n.id = a.node WHERE n.path = ? ORDER BY a.rowid",
            ).use { st ->
                st.setString(1, path)
                st.executeQuery().use { rs ->
                    while (rs.next()) {
                        attrs.getOrPut(rs.getLong(1)) { linkedMapOf() }[rs.getString(2)] =
                            rs.getString(3)
                    }
                }
            }
        val children = rows.groupBy { it.parent }

        fun build(r: Row): XmlNode {
            val a = attrs[r.id].orEmpty().toMutableMap()
            r.text?.let { a["#text"] = it }
            return XmlNode(r.tag, a, children[r.id].orEmpty().map(::build))
        }
        return build(rows.first { it.parent == null })
    }

    /**
     * Elements across every XML file: by [tag] (exact, or ending in it: EditText finds TextInputEditText), [attribute] name and a
     * substring of its [value]. Without an attribute or value, one row per element.
     */
    fun xmlFind(
        tag: String?,
        attribute: String?,
        value: String?,
        limit: Int,
    ): Pair<List<XmlHit>, Int> {
        requireTable("xml_nodes", "XML nodes need 10")
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        tag?.let {
            where += "(n.tag = ? OR n.tag LIKE ? ESCAPE '\\')"
            args += it
            args += "%" + likeEscape(it)
        }
        val withAttr = attribute != null || value != null
        attribute?.let {
            where += "a.name = ?"
            args += it
        }
        value?.let {
            where += "a.value LIKE ? ESCAPE '\\'"
            args += "%" + likeEscape(it) + "%"
        }
        val from =
            "FROM xml_nodes n LEFT JOIN files f ON f.id = n.file" + if (withAttr) " JOIN xml_attrs a ON a.node = n.id" else ""
        val filter = if (where.isEmpty()) "" else "WHERE " + where.joinToString(" AND ")
        val total =
            conn.prepareStatement("SELECT count(*) $from $filter").use { st ->
                args.forEachIndexed { i, a -> st.setString(i + 1, a) }
                st.executeQuery().use {
                    it.next()
                    it.getInt(1)
                }
            }
        val columns = if (withAttr) "n.path, f.resource, n.tag, a.name, a.value" else "n.path, f.resource, n.tag, NULL, NULL"
        val hits =
            conn.prepareStatement("SELECT $columns $from $filter ORDER BY n.path, n.seq LIMIT ?").use { st ->
                args.forEachIndexed { i, a -> st.setString(i + 1, a) }
                st.setInt(args.size + 1, limit)
                st.executeQuery().use { rs ->
                    rs.map { XmlHit(it.getString(1), it.getString(2), it.getString(3), it.getString(4), it.getString(5)) }
                }
            }
        return hits to total
    }

    private fun requireTable(
        name: String,
        need: String,
    ) {
        if (!hasTable(name)) {
            throw SextantException.input(
                "this index has no $name table (index schema ${meta("index_schema_version") ?: "?"}; $need)",
                "run `sextant extract <app.apk> --force` again with this version, or pass the APK",
            )
        }
    }

    /**
     * Keys in `links` that [query] names: a key as written (`component:…`, `@string/x`, `class:L…;`), a class
     * descriptor, a method descriptor, a URL, or a component, class, permission, SDK, host or package name (a
     * component suffix like `.MainActivity` works too).
     */
    fun linkKeys(query: String): List<String> {
        requireTable("links", "links need 11")
        val q = query.trim()
        val prefixes =
            listOf(
                "app",
                "@",
                "?",
                "component:",
                "class:",
                "method:",
                "permission:",
                "url:",
                "native:",
                "file:",
                "package:",
                "sdk:",
                "host:",
                "certificate:",
                "system:",
            )
        if (prefixes.any { q == it || (it != "app" && q.startsWith(it)) }) return listOf(q)
        if (q.contains("->")) return listOf("method:$q")
        if (q.startsWith("L") && q.endsWith(";")) return listOf("class:$q")
        if (q.contains("://")) return listOf("url:$q")
        val keys = mutableListOf<String>()

        fun add(
            sql: String,
            vararg args: String,
        ) = conn.prepareStatement(sql).use { st ->
            args.forEachIndexed { i, a -> st.setString(i + 1, a) }
            st.executeQuery().use { rs -> while (rs.next()) keys += rs.getString(1) }
        }
        add(
            "SELECT 'component:' || name FROM components WHERE name = ? OR name LIKE ? ESCAPE '\\' ORDER BY name LIMIT 20",
            q,
            "%" + likeEscape(if (q.startsWith(".")) q else ".$q"),
        )
        add("SELECT 'class:' || descriptor FROM classes WHERE name = ?", q)
        add("SELECT 'permission:' || name FROM permissions WHERE name = ? LIMIT 1", q)
        add("SELECT 'sdk:' || name FROM sdks WHERE name = ? COLLATE NOCASE", q)
        add("SELECT DISTINCT 'host:' || host FROM endpoints WHERE host = ?", q)
        add("SELECT DISTINCT 'package:' || package FROM classes WHERE package = ? LIMIT 1", q)
        return keys.distinct()
    }

    /** Links next to [key]: the stored ones plus code edges added on the fly (superclass, class members, calls). */
    fun links(key: String): List<LinkRow> {
        requireTable("links", "links need 11")
        val rows = mutableListOf<LinkRow>()

        fun query(
            sql: String,
            direction: String,
            arg: String,
        ) = conn.prepareStatement(sql).use { st ->
            st.setString(1, arg)
            st.executeQuery().use { rs -> while (rs.next()) rows += LinkRow(direction, rs.getString(1), rs.getString(2), rs.getString(3)) }
        }
        query("SELECT rel, dst, via FROM links WHERE src = ? ORDER BY rel, dst", "out", key)
        query("SELECT rel, src, via FROM links WHERE dst = ? ORDER BY rel, src", "in", key)
        when {
            key.startsWith("class:") -> {
                val d = key.removePrefix("class:")
                query(
                    "SELECT 'extends', 'class:' || superclass, NULL FROM classes WHERE descriptor = ? AND superclass IS NOT NULL",
                    "out",
                    d,
                )
                query(
                    "SELECT 'defines', 'method:' || descriptor, NULL FROM methods WHERE class = ? AND defined = 1 ORDER BY name",
                    "out",
                    d,
                )
                query("SELECT 'extends', 'class:' || descriptor, NULL FROM classes WHERE superclass = ? ORDER BY descriptor", "in", d)
            }
            key.startsWith("method:") -> {
                val d = key.removePrefix("method:")
                query("SELECT 'defines', 'class:' || class, NULL FROM methods WHERE descriptor = ?", "in", d)
                query("SELECT 'calls', 'method:' || callee, kind FROM call_edges WHERE caller = ? ORDER BY callee", "out", d)
                query("SELECT 'calls', 'method:' || caller, kind FROM call_edges WHERE callee = ? ORDER BY caller", "in", d)
            }
        }
        return rows.distinct()
    }

    /** Every stored file, in path order: its path, whether it was stored (not deflated) in the APK, and its bytes. */
    fun fileContents(each: (path: String, stored: Boolean, bytes: ByteArray) -> Unit) {
        if (!hasTable("file_contents")) return
        conn.createStatement().use { st ->
            st.executeQuery("SELECT f.path, f.method, c.bytes FROM file_contents c JOIN files f ON f.id = c.file ORDER BY f.path").use { rs ->
                while (rs.next()) each(rs.getString(1), rs.getString(2) == "stored", rs.getBytes(3))
            }
        }
    }

    /** The bytes of the file at [path] inside the APK, or null when it is not stored. */
    fun fileContent(path: String): ByteArray? {
        if (!hasTable("file_contents")) return null
        return conn.prepareStatement("SELECT c.bytes FROM file_contents c JOIN files f ON f.id = c.file WHERE f.path = ?").use { st ->
            st.setString(1, path)
            st.executeQuery().use { rs -> if (rs.next()) rs.getBytes(1) else null }
        }
    }

    /**
     * Lines containing [text] (case-insensitive) in the text files of the APK (`file_text`). The full-text index finds
     * the files (3+ characters); shorter text scans them. At most [limit] lines.
     */
    fun textGrep(
        text: String,
        limit: Int,
    ): List<TextLine> {
        requireTable("file_text", "text files need 15")
        val sql =
            if (text.length >= 3) {
                "SELECT t.path, t.text FROM file_text_search s JOIN file_text t ON t.rowid = s.rowid WHERE file_text_search MATCH ? ORDER BY t.path"
            } else {
                "SELECT path, text FROM file_text WHERE text LIKE ? ESCAPE '\\' ORDER BY path"
            }
        val out = mutableListOf<TextLine>()
        conn.prepareStatement(sql).use { st ->
            st.setString(1, if (text.length >= 3) "\"" + text.replace("\"", "\"\"") + "\"" else "%" + likeEscape(text) + "%")
            st.executeQuery().use { rs ->
                while (rs.next() && out.size < limit) {
                    val path = rs.getString(1)
                    rs.getString(2).lineSequence().forEachIndexed { i, line ->
                        if (out.size < limit && line.contains(text, ignoreCase = true)) out += TextLine(path, i + 1, line.trim())
                    }
                }
            }
        }
        return out
    }

    /** One value from the meta table, or null. */
    fun meta(key: String): String? =
        conn.prepareStatement("SELECT value FROM meta WHERE key = ?").use { st ->
            st.setString(1, key)
            st.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
        }

    private fun requireFiles() {
        val has =
            conn.createStatement().use { st ->
                st.executeQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'files'").use { it.next() }
            }
        if (!has) {
            throw SextantException.input(
                "this index has no files table (index schema ${meta("index_schema_version") ?: "?"}, files need 8)",
                "run `sextant extract <app.apk> --force` again with this version",
            )
        }
    }

    override fun close() = conn.close()

    private fun likeEscape(text: String) = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun jsonList(json: String?): List<String> =
        json?.let { SextantJson.reader.decodeFromString(ListSerializer(String.serializer()), it) }.orEmpty()
}

private inline fun <T> ResultSet.map(row: (ResultSet) -> T): List<T> {
    val out = mutableListOf<T>()
    while (next()) out += row(this)
    return out
}
