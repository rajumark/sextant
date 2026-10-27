package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.SextantException
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

/** Read-only access to index.sqlite. */
class IndexReader private constructor(
    private val conn: Connection,
) : AutoCloseable {
    companion object {
        private const val STRING_USERS =
            "SELECT m.descriptor FROM string_refs r JOIN methods m ON m.id = r.method WHERE r.string = ? LIMIT ?"
        private const val CLASS_SUFFIX =
            "SELECT descriptor FROM classes WHERE name LIKE ? ESCAPE '\\' OR name = ? ORDER BY descriptor LIMIT 50"

        fun open(dir: Path): IndexReader {
            val file = if (Files.isDirectory(dir)) dir.resolve(OutputFiles.INDEX) else dir
            if (!Files.isRegularFile(file)) {
                throw SextantException.input("no index at $file", "run `sextant extract <app.apk>` (0.3.0 or newer) to build index.sqlite")
            }
            val config =
                SQLiteConfig().apply {
                    setReadOnly(true)
                }
            return IndexReader(config.createConnection("jdbc:sqlite:$file"))
        }
    }

    /** Runs any read-only SQL. At most [limit] rows are returned. */
    fun query(
        sql: String,
        limit: Int = Int.MAX_VALUE,
    ): QueryResult =
        try {
            conn.createStatement().use { st ->
                st.executeQuery(sql).use { rs ->
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
        query("SELECT sql FROM sqlite_master WHERE sql IS NOT NULL AND name NOT LIKE 'search_%' ORDER BY rowid")
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
                else ->
                    "SELECT library, name || coalesce('  →  ' || jni_class || '.' || jni_method, '') FROM native_symbols WHERE rowid = ?"
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
