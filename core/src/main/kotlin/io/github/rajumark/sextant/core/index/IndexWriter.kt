package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.CodeSink
import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.insights.AppIcons
import io.github.rajumark.sextant.core.insights.IconFile
import io.github.rajumark.sextant.core.insights.Insights
import io.github.rajumark.sextant.core.model.CallRecord
import io.github.rajumark.sextant.core.model.ClassRecord
import io.github.rajumark.sextant.core.model.FieldRecord
import io.github.rajumark.sextant.core.model.FieldRefRecord
import io.github.rajumark.sextant.core.model.IntentFilter
import io.github.rajumark.sextant.core.model.MethodRecord
import io.github.rajumark.sextant.core.model.PathPermission
import io.github.rajumark.sextant.core.model.StringRecord
import io.github.rajumark.sextant.core.model.TypeRefRecord
import io.github.rajumark.sextant.core.output.InfoReports
import io.github.rajumark.sextant.core.output.SextantJson
import io.github.rajumark.sextant.core.output.makeReadable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

/**
 * Builds index.sqlite while code records stream in (one pass over the APK), then adds manifest-level tables,
 * indexes and the full-text table in [finish]. Written to a temp file and moved into place on success.
 */
class IndexWriter(
    private val target: Path,
) : CodeSink,
    AutoCloseable {
    private val tmp: Path = target.resolveSibling(".${target.fileName}.tmp")
    private val conn: Connection
    private val insertClass: PreparedStatement
    private val insertMethod: PreparedStatement
    private val defineMethod: PreparedStatement
    private val insertField: PreparedStatement
    private val insertCall: PreparedStatement
    private val insertString: PreparedStatement
    private val insertStringRef: PreparedStatement
    private val insertSearch: PreparedStatement
    private val insertFieldRef: PreparedStatement
    private val insertTypeRef: PreparedStatement

    private val methodIds = HashMap<String, Long>()
    private val stringIds = HashMap<String, Long>()
    private var nextClassId = 1L
    private var nextFieldId = 1L
    private var pending = 0
    private var committed = false

    init {
        try {
            Files.deleteIfExists(tmp)
            conn = DriverManager.getConnection("jdbc:sqlite:$tmp")
            conn.createStatement().use { st ->
                // Bulk-load settings: the file is rebuilt from scratch on every run, so durability is not needed.
                st.execute("PRAGMA journal_mode = OFF")
                st.execute("PRAGMA synchronous = OFF")
                st.execute("PRAGMA page_size = 4096")
                st.execute("PRAGMA cache_size = -262144") // 256 MB page cache: index builds stay in memory
                st.execute("PRAGMA temp_store = MEMORY")
                st.execute("PRAGMA locking_mode = EXCLUSIVE")
                IndexSchema.statements(IndexSchema.DDL).forEach(st::execute)
                // Bulk load: merge segments less often (default 4); one optimize runs at the end.
                st.execute("INSERT INTO search(search, rank) VALUES ('automerge', 16)")
            }
            conn.autoCommit = false
        } catch (e: Exception) {
            throw SextantException.output("cannot create index $target: ${e.message}", cause = e)
        }
        insertClass = conn.prepareStatement("INSERT INTO classes VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")
        insertMethod = conn.prepareStatement("INSERT INTO methods (id, descriptor, class, name, defined) VALUES (?,?,?,?,0)")
        defineMethod =
            conn.prepareStatement(
                "UPDATE methods SET defined = 1, params = ?, return_type = ?, access = ?, has_code = ?, instructions = ?, " +
                    "registers = ?, calls_out = ?, string_loads = ? WHERE id = ?",
            )
        insertField = conn.prepareStatement("INSERT INTO fields VALUES (?,?,?,?,?,?,?)")
        insertCall = conn.prepareStatement("INSERT INTO calls VALUES (?,?,?,?,?)")
        insertString = conn.prepareStatement("INSERT INTO strings VALUES (?,?)")
        insertStringRef = conn.prepareStatement("INSERT INTO string_refs VALUES (?,?,?,?)")
        insertSearch = conn.prepareStatement("INSERT INTO search(rowid, text) VALUES (?, ?)")
        insertFieldRef = conn.prepareStatement("INSERT INTO field_refs VALUES (?,?,?,?,?)")
        insertTypeRef = conn.prepareStatement("INSERT INTO type_refs VALUES (?,?,?,?,?)")
    }

    override fun classRecord(record: ClassRecord) {
        val id = nextClassId++
        insertClass.apply {
            setLong(1, id)
            setString(2, record.id)
            setString(3, record.name)
            setString(4, record.`package`)
            setString(5, record.superclass)
            setString(6, json(record.interfaces))
            setString(7, record.access.joinToString(" "))
            setString(8, record.sourceFile)
            setString(9, record.dex)
            setInt(10, if (record.kotlin) 1 else 0)
            setString(11, json(record.annotations))
            setInt(12, record.methods)
            setInt(13, record.fields)
        }
        batch(insertClass)
        search(id, "class", record.name)
    }

    override fun method(record: MethodRecord) {
        val id = methodId(record.id)
        defineMethod.apply {
            setString(1, json(record.params))
            setString(2, record.returnType)
            setString(3, record.access.joinToString(" "))
            setInt(4, if (record.hasCode) 1 else 0)
            setInt(5, record.instructions)
            setInt(6, record.registers)
            setInt(7, record.callsOut)
            setInt(8, record.strings)
            setLong(9, id)
        }
        batch(defineMethod)
    }

    override fun field(record: FieldRecord) {
        val id = nextFieldId++
        insertField.apply {
            setLong(1, id)
            setString(2, record.id)
            setString(3, record.`class`)
            setString(4, record.name)
            setString(5, record.type)
            setString(6, record.access.joinToString(" "))
            setString(7, record.initialValue)
        }
        batch(insertField)
        search(id, "field", simpleName(record.`class`) + "." + record.name + (record.initialValue?.let { " = $it" } ?: ""))
    }

    // Resolve ids before setting parameters: resolving can flush batches, which clears pending parameters.
    override fun call(record: CallRecord) {
        val caller = methodId(record.caller)
        val callee = methodId(record.callee)
        insertCall.apply {
            setLong(1, caller)
            setLong(2, callee)
            setString(3, record.kind)
            setInt(4, record.count)
            setString(5, record.offsets.joinToString(",", "[", "]"))
        }
        batch(insertCall)
    }

    override fun string(record: StringRecord) {
        val id =
            stringIds.getOrPut(record.value) {
                val newId = stringIds.size + 1L
                insertString.setLong(1, newId)
                insertString.setString(2, record.value)
                batch(insertString)
                search(newId, "string", record.value)
                newId
            }
        val method = methodId(record.method)
        insertStringRef.apply {
            setLong(1, id)
            setLong(2, method)
            setInt(3, record.count)
            setString(4, record.offsets.joinToString(",", "[", "]"))
        }
        batch(insertStringRef)
    }

    override fun fieldRef(record: FieldRefRecord) {
        val method = methodId(record.method)
        insertFieldRef.apply {
            setLong(1, method)
            setString(2, record.field)
            setString(3, record.kind)
            setInt(4, record.count)
            setString(5, record.offsets.joinToString(",", "[", "]"))
        }
        batch(insertFieldRef)
    }

    override fun typeRef(record: TypeRefRecord) {
        val method = methodId(record.method)
        insertTypeRef.apply {
            setLong(1, method)
            setString(2, record.type)
            setString(3, record.kind)
            setInt(4, record.count)
            setString(5, record.offsets.joinToString(",", "[", "]"))
        }
        batch(insertTypeRef)
    }

    /** Ids are assigned in first-seen order, which is deterministic because records arrive sorted. */
    private fun methodId(descriptor: String): Long =
        methodIds.getOrPut(descriptor) {
            val id = methodIds.size + 1L
            insertMethod.apply {
                setLong(1, id)
                setString(2, descriptor)
                setString(3, descriptor.substringBefore("->"))
                setString(4, descriptor.substringAfter("->").substringBefore('('))
            }
            batch(insertMethod)
            search(id, "method", simpleName(descriptor.substringBefore("->")) + "." + descriptor.substringAfter("->").substringBefore('('))
            id
        }

    /** Adds a full-text row; the rowid encodes the source row so hits can be resolved without storing text twice. */
    private fun search(
        id: Long,
        kind: String,
        text: String,
    ) {
        insertSearch.setLong(1, (id shl IndexSchema.KIND_BITS) + IndexSchema.kindCode(kind))
        insertSearch.setString(2, text.take(IndexSchema.MAX_SEARCH_TEXT))
        batch(insertSearch)
    }

    /** `Lcom/app/Foo$Bar;` → `Foo$Bar` */
    private fun simpleName(descriptor: String) = descriptor.removeSuffix(";").substringAfterLast('/').removePrefix("L")

    private fun batch(statement: PreparedStatement) {
        statement.addBatch()
        if (++pending >= BATCH) flush()
    }

    private fun flush() {
        // Order matters: method rows must exist before they are updated.
        for (st in listOf(
            insertMethod,
            insertClass,
            defineMethod,
            insertField,
            insertCall,
            insertString,
            insertStringRef,
            insertSearch,
            insertFieldRef,
            insertTypeRef,
        )) {
            st.executeBatch()
        }
        pending = 0
    }

    /** Adds the manifest-level tables, builds indexes and the search table, and moves the file into place. */
    fun finish(extraction: Extraction): Path {
        try {
            profile("stream (code records)")
            flush()
            insertManifest(extraction)
            profile("flush + manifest tables")
            conn.createStatement().use { st ->
                IndexSchema.statements(IndexSchema.INDEXES).forEach(st::execute)
                profile("b-tree indexes")
                for ((kind, select) in MANIFEST_SEARCH) {
                    st.execute("INSERT INTO search(rowid, text) SELECT (rowid << 4) + ${IndexSchema.kindCode(kind)}, $select")
                }
                st.execute("INSERT INTO search(search) VALUES ('optimize')")
                profile("search optimize")
            }
            conn.commit()
            profile("commit")
            conn.close()
            tmp.makeReadable()
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            committed = true
            return target
        } catch (e: SextantException) {
            throw e
        } catch (e: Exception) {
            throw SextantException.output("cannot build index $target: ${e.message}", cause = e)
        }
    }

    private fun insertManifest(extraction: Extraction) {
        val m = extraction.meta
        conn.prepareStatement("INSERT INTO meta VALUES (?, ?)").use { st ->
            val rows =
                listOf(
                    "index_schema_version" to IndexSchema.VERSION.toString(),
                    "schema_version" to m.schemaVersion.toString(),
                    "package" to m.`package`,
                    "version_name" to m.versionName,
                    "version_code" to m.versionCode?.toString(),
                    "min_sdk" to m.minSdk?.toString(),
                    "target_sdk" to m.targetSdk?.toString(),
                    "label" to m.label,
                    "sha256" to m.file.sha256,
                    "meta_json" to
                        SextantJson.compact.encodeToString(
                            io.github.rajumark.sextant.core.model.AppMeta
                                .serializer(),
                            m,
                        ),
                )
            for ((k, v) in rows) {
                st.setString(1, k)
                st.setString(2, v)
                st.addBatch()
            }
            st.executeBatch()
        }
        conn.prepareStatement("INSERT INTO permissions VALUES (?,?,?,?,?,?,?)").use { st ->
            for (p in extraction.permissions) {
                st.setString(1, p.name)
                st.setString(2, p.kind)
                p.maxSdk?.let { st.setInt(3, it) } ?: st.setNull(3, java.sql.Types.INTEGER)
                st.setInt(4, if (p.dangerous) 1 else 0)
                st.setString(5, p.protectionLevel)
                st.setInt(6, if (p.custom) 1 else 0)
                st.setString(7, p.impliedBy)
                st.addBatch()
            }
            st.executeBatch()
        }
        val filters = ListSerializer(IntentFilter.serializer())
        conn.prepareStatement("INSERT INTO components VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)").use { st ->
            for (c in extraction.components) {
                st.setString(1, c.type)
                st.setString(2, c.name)
                st.setInt(3, if (c.exported) 1 else 0)
                st.setString(4, c.exportedSource)
                st.setInt(5, if (c.enabled) 1 else 0)
                st.setString(6, c.permission)
                st.setString(7, c.process)
                st.setInt(8, if (c.launcher) 1 else 0)
                st.setString(9, c.targetActivity)
                st.setString(10, json(c.authorities))
                st.setString(11, SextantJson.compact.encodeToString(filters, c.intentFilters))
                st.setString(12, c.readPermission)
                st.setString(13, c.writePermission)
                c.grantUriPermissions?.let { st.setInt(14, if (it) 1 else 0) } ?: st.setNull(14, java.sql.Types.INTEGER)
                st.setString(15, SextantJson.compact.encodeToString(ListSerializer(PathPermission.serializer()), c.pathPermissions))
                st.setString(16, c.launchMode)
                st.setString(17, c.taskAffinity)
                st.setString(18, c.foregroundServiceType)
                c.isolatedProcess?.let { st.setInt(19, if (it) 1 else 0) } ?: st.setNull(19, java.sql.Types.INTEGER)
                c.directBootAware?.let { st.setInt(20, if (it) 1 else 0) } ?: st.setNull(20, java.sql.Types.INTEGER)
                st.setString(21, c.label)
                st.addBatch()
            }
            st.executeBatch()
        }
        extraction.insights?.let(::insertInsights)
        insertIcons(m.icons)
        conn.prepareStatement("INSERT INTO features VALUES (?,?,?,?)").use { st ->
            for (f in extraction.features) {
                st.setString(1, f.kind)
                st.setString(2, f.name)
                f.required?.let { st.setInt(3, if (it) 1 else 0) } ?: st.setNull(3, java.sql.Types.INTEGER)
                st.setString(4, f.version)
                st.addBatch()
            }
            st.executeBatch()
        }
        conn.prepareStatement("INSERT INTO metadata VALUES (?,?,?,?,?)").use { st ->
            for (md in extraction.metadata) {
                st.setString(1, md.scope)
                st.setString(2, md.kind)
                st.setString(3, md.name)
                st.setString(4, md.value)
                st.setString(5, md.resource)
                st.addBatch()
            }
            st.executeBatch()
        }
        val dataList =
            ListSerializer(
                io.github.rajumark.sextant.core.model.IntentData
                    .serializer(),
            )
        conn.prepareStatement("INSERT INTO queries VALUES (?,?,?,?,?)").use { st ->
            for (q in extraction.queries) {
                st.setString(1, q.kind)
                st.setString(2, q.name)
                st.setString(3, json(q.actions))
                st.setString(4, json(q.categories))
                st.setString(5, SextantJson.compact.encodeToString(dataList, q.data))
                st.addBatch()
            }
            st.executeBatch()
        }
        conn.prepareStatement("INSERT INTO deep_links VALUES (?,?)").use { st ->
            for (c in extraction.components) {
                for (url in InfoReports.deepLinks(c)) {
                    st.setString(1, c.name)
                    st.setString(2, url)
                    st.addBatch()
                }
            }
            st.executeBatch()
        }
    }

    private fun insertIcons(icons: AppIcons?) {
        if (icons == null) return
        val rows = mutableListOf<Pair<String, Triple<String, IconFile, Boolean>>>()

        fun add(
            role: String,
            resource: String,
            files: List<IconFile>,
            best: String?,
        ) = files.forEach { rows += role to Triple(resource, it, it.path == best) }
        for ((role, ref) in listOf("icon" to icons.icon, "round_icon" to icons.roundIcon)) {
            if (ref == null) continue
            add(role, ref.resource, ref.files, ref.best)
            ref.adaptive?.let { a ->
                for ((layer, l) in listOf("background" to a.background, "foreground" to a.foreground, "monochrome" to a.monochrome)) {
                    if (l != null) add("$role.$layer", l.resource, l.files, l.best)
                }
            }
        }
        conn.prepareStatement("INSERT INTO icons VALUES (?,?,?,?,?,?,?,?,?,?)").use { st ->
            for ((role, t) in rows) {
                val (resource, f, best) = t
                st.setString(1, role)
                st.setString(2, resource)
                st.setString(3, f.path)
                st.setString(4, f.config)
                f.density?.let { st.setInt(5, it) } ?: st.setNull(5, java.sql.Types.INTEGER)
                st.setString(6, f.format)
                f.width?.let { st.setInt(7, it) } ?: st.setNull(7, java.sql.Types.INTEGER)
                f.height?.let { st.setInt(8, it) } ?: st.setNull(8, java.sql.Types.INTEGER)
                st.setLong(9, f.sizeBytes)
                st.setInt(10, if (best) 1 else 0)
                st.addBatch()
            }
            st.executeBatch()
        }
    }

    private fun insertInsights(insights: Insights) {
        fun <T> rows(
            sql: String,
            items: List<T>,
            bind: PreparedStatement.(T) -> Unit,
        ) = conn.prepareStatement(sql).use { st ->
            for (item in items) {
                st.bind(item)
                st.addBatch()
            }
            st.executeBatch()
        }
        rows("INSERT INTO libraries VALUES (?,?,?,?)", insights.libraries) {
            setString(1, it.group)
            setString(2, it.artifact)
            setString(3, it.version)
            setString(4, it.evidence)
        }
        rows("INSERT INTO frameworks VALUES (?,?)", insights.frameworks) {
            setString(1, it.name)
            setString(2, json(it.evidence))
        }
        rows("INSERT INTO sdks VALUES (?,?,?,?,?)", insights.sdks) {
            setString(1, it.name)
            setString(2, it.category)
            setInt(3, it.classes)
            setString(4, json(it.packages))
            setString(5, json(it.manifest))
        }
        rows("INSERT INTO endpoints VALUES (?,?,?,?)", insights.endpoints.withIndex().toList()) { (i, e) ->
            setLong(1, i + 1L)
            setString(2, e.url)
            setString(3, e.scheme)
            setString(4, e.host)
        }
        val sources = insights.endpoints.withIndex().flatMap { (i, e) -> e.sources.map { i + 1L to it } }
        rows("INSERT INTO endpoint_sources VALUES (?,?,?)", sources) { (id, src) ->
            setLong(1, id)
            setString(2, src.kind)
            setString(3, src.where)
        }
        rows("INSERT INTO resources VALUES (?,?,?)", insights.resources) {
            setString(1, it.type)
            setString(2, it.name)
            setString(3, it.value)
        }
        rows("INSERT INTO native_libraries VALUES (?,?,?,?,?,?,?,?)", insights.nativeLibraries) {
            setString(1, it.path)
            setString(2, it.abi)
            setString(3, it.name)
            setLong(4, it.sizeBytes)
            setString(5, it.sha256)
            setString(6, it.elfClass)
            setInt(7, it.exportedSymbols)
            setInt(8, it.jniMethods)
        }
        rows("INSERT INTO findings VALUES (?,?,?,?,?)", insights.findings) {
            setString(1, it.id)
            setString(2, it.severity)
            setString(3, it.title)
            setString(4, it.detail)
            setInt(5, it.count)
        }
        rows(
            "INSERT INTO finding_evidence VALUES (?,?,?,?)",
            insights.findings.flatMap { f ->
                f.evidence.map { f.id to it }
            },
        ) { (id, ev) ->
            setString(1, id)
            setString(2, ev.kind)
            setString(3, ev.where)
            setString(4, ev.value)
        }
        rows("INSERT INTO native_symbols VALUES (?,?,?,?)", insights.nativeSymbols) {
            setString(1, it.library)
            setString(2, it.name)
            setString(3, it.jniClass)
            setString(4, it.jniMethod)
        }
    }

    override fun close() {
        if (committed) return
        runCatching { conn.close() }
        runCatching { Files.deleteIfExists(tmp) }
    }

    private val profiling = System.getProperty("sextant.profile") == "true"
    private var lastMark = System.nanoTime()

    private fun profile(phase: String) {
        if (!profiling) return
        val now = System.nanoTime()
        System.err.println("[profile] %-26s %6d ms".format(phase, (now - lastMark) / 1_000_000))
        lastMark = now
    }

    private fun json(values: List<String>) = SextantJson.compact.encodeToString(ListSerializer(String.serializer()), values)

    private companion object {
        const val BATCH = 20_000

        /** Search rows filled with SQL after the manifest-level and insight tables exist: kind to "text FROM table". */
        val MANIFEST_SEARCH =
            listOf(
                "component" to "type || ' ' || name FROM components",
                "permission" to "name FROM permissions",
                "deep_link" to "url FROM deep_links",
                "resource" to "substr(name || ' = ' || value, 1, ${IndexSchema.MAX_SEARCH_TEXT}) FROM resources",
                "endpoint" to "url FROM endpoints",
                "sdk" to "name || ' (' || category || ')' FROM sdks",
                "library" to "\"group\" || ':' || artifact || ':' || version FROM libraries",
                "native_symbol" to "name FROM native_symbols",
                "finding" to "severity || ' ' || title || ' ' || id FROM findings",
            )
    }
}

/** Sends every record to several sinks (JSONL files and the SQLite index in one pass). */
class TeeSink(
    private vararg val sinks: CodeSink,
) : CodeSink {
    override fun classRecord(record: ClassRecord) = sinks.forEach { it.classRecord(record) }

    override fun method(record: MethodRecord) = sinks.forEach { it.method(record) }

    override fun field(record: FieldRecord) = sinks.forEach { it.field(record) }

    override fun call(record: CallRecord) = sinks.forEach { it.call(record) }

    override fun string(record: StringRecord) = sinks.forEach { it.string(record) }

    override fun fieldRef(record: FieldRefRecord) = sinks.forEach { it.fieldRef(record) }

    override fun typeRef(record: TypeRefRecord) = sinks.forEach { it.typeRef(record) }
}
