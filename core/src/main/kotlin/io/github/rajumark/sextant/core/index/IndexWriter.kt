package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.CodeSink
import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.RawXml
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.SigningDetail
import io.github.rajumark.sextant.core.insights.AppIcons
import io.github.rajumark.sextant.core.insights.ComponentReviews
import io.github.rajumark.sextant.core.insights.IconFile
import io.github.rajumark.sextant.core.insights.Insights
import io.github.rajumark.sextant.core.model.AnnotationRecord
import io.github.rajumark.sextant.core.model.ApkFile
import io.github.rajumark.sextant.core.model.CallRecord
import io.github.rajumark.sextant.core.model.ClassRecord
import io.github.rajumark.sextant.core.model.FieldRecord
import io.github.rajumark.sextant.core.model.FieldRefRecord
import io.github.rajumark.sextant.core.model.IntentFilter
import io.github.rajumark.sextant.core.model.MethodCodeRecord
import io.github.rajumark.sextant.core.model.MethodDetailRecord
import io.github.rajumark.sextant.core.model.MethodRecord
import io.github.rajumark.sextant.core.model.NavAction
import io.github.rajumark.sextant.core.model.PathPermission
import io.github.rajumark.sextant.core.model.ResValue
import io.github.rajumark.sextant.core.model.ResourceHarvest
import io.github.rajumark.sextant.core.model.ResourceUseRecord
import io.github.rajumark.sextant.core.model.StringRecord
import io.github.rajumark.sextant.core.model.TypeRefRecord
import io.github.rajumark.sextant.core.model.XmlNode
import io.github.rajumark.sextant.core.output.InfoReports
import io.github.rajumark.sextant.core.output.SextantJson
import io.github.rajumark.sextant.core.output.makeReadable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.zip.ZipFile

private const val MAX_CONTENT_BYTES = 1L shl 30

/**
 * Builds index.sqlite while code records stream in (one pass over the APK), then adds manifest-level tables,
 * indexes and the full-text table in [finish]. Written to a temp file and moved into place on success.
 */
class IndexWriter(
    private val target: Path,
    /** Store every instruction (`instructions` table); false keeps only calls, strings, field and type uses. */
    private val instructions: Boolean = true,
) : CodeSink,
    AutoCloseable {
    override val wantsInstructions: Boolean get() = instructions
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
    private val insertAnnotation: PreparedStatement
    private val insertAnnotationValue: PreparedStatement
    private val insertMethodLines: PreparedStatement
    private val insertMethodParam: PreparedStatement
    private val insertHandler: PreparedStatement
    private val insertInstruction: PreparedStatement
    private var nextAnnotationId = 1L

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
        insertAnnotation = conn.prepareStatement("INSERT INTO annotations VALUES (?,?,?,?,?,?)")
        insertAnnotationValue = conn.prepareStatement("INSERT INTO annotation_values VALUES (?,?,?)")
        insertMethodLines = conn.prepareStatement("INSERT INTO method_lines VALUES (?,?,?,?)")
        insertMethodParam = conn.prepareStatement("INSERT INTO method_params VALUES (?,?,?)")
        insertHandler = conn.prepareStatement("INSERT INTO exception_handlers VALUES (?,?,?,?,?)")
        insertInstruction = conn.prepareStatement("INSERT INTO instructions VALUES (?,?,?,?,?,?,?,?,?)")
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
    override fun annotation(record: AnnotationRecord) {
        val id = nextAnnotationId++
        insertAnnotation.apply {
            setLong(1, id)
            setString(2, record.targetKind)
            setString(3, record.target)
            record.parameter?.let { setInt(4, it) } ?: setNull(4, java.sql.Types.INTEGER)
            setString(5, record.type)
            setString(6, record.visibility)
        }
        batch(insertAnnotation)
        for (e in record.elements) {
            insertAnnotationValue.apply {
                setLong(1, id)
                setString(2, e.name)
                setString(3, e.value)
            }
            batch(insertAnnotationValue)
        }
    }

    override fun methodDetail(record: MethodDetailRecord) {
        val method = methodId(record.method)
        if (record.lines.isNotEmpty()) {
            insertMethodLines.apply {
                setLong(1, method)
                setInt(2, record.lines.minOf { it.second })
                setInt(3, record.lines.maxOf { it.second })
                setString(4, record.lines.joinToString(" ") { (offset, line) -> "$offset:$line" })
            }
            batch(insertMethodLines)
        }
        record.parameterNames.forEachIndexed { i, name ->
            if (name == null) return@forEachIndexed
            insertMethodParam.apply {
                setLong(1, method)
                setInt(2, i)
                setString(3, name)
            }
            batch(insertMethodParam)
        }
        for (h in record.handlers) {
            insertHandler.apply {
                setLong(1, method)
                setInt(2, h.start)
                setInt(3, h.end)
                setInt(4, h.handler)
                setString(5, h.type)
            }
            batch(insertHandler)
        }
    }

    override fun methodCode(record: MethodCodeRecord) {
        val method = methodId(record.method)
        for (r in record.instructions) {
            insertInstruction.apply {
                setLong(1, method)
                setInt(2, r.offset)
                setInt(3, r.op)
                r.literal?.let { setLong(4, it) } ?: setNull(4, java.sql.Types.INTEGER)
                r.target?.let { setInt(5, it) } ?: setNull(5, java.sql.Types.INTEGER)
                // Methods and strings point at their rows by id; fields, types and the rest keep their text.
                val methodRef = if (r.refKind == "method") r.ref?.let(::methodId) else null
                val stringRef = if (r.refKind == "string") r.ref?.let { stringIds[it] } else null
                methodRef?.let { setLong(6, it) } ?: setNull(6, java.sql.Types.INTEGER)
                stringRef?.let { setLong(7, it) } ?: setNull(7, java.sql.Types.INTEGER)
                setString(8, if (methodRef != null || stringRef != null) null else r.ref)
                setString(9, r.registers)
            }
            batch(insertInstruction)
        }
    }

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
            insertAnnotation,
            insertAnnotationValue,
            insertMethodLines,
            insertMethodParam,
            insertHandler,
            insertInstruction,
        )) {
            st.executeBatch()
        }
        pending = 0
    }

    /** Adds the manifest-level tables, builds indexes and the search table, and moves the file into place. */

    /** Where file contents come from: the (merged) APK, and whether to store every file or only the dex files. */
    class Contents(
        val apk: Path,
        val all: Boolean,
    )

    fun finish(
        extraction: Extraction,
        contents: Contents? = null,
    ): Path {
        try {
            profile("stream (code records)")
            flush()
            insertManifest(extraction)
            contents?.let { insertContents(extraction.files, it) }
            profile("flush + manifest tables")
            conn.createStatement().use { st ->
                IndexSchema.statements(IndexSchema.INDEXES).forEach(st::execute)
                profile("b-tree indexes")
                Links.BUILD.forEach(st::execute)
                Links.INDEXES.forEach(st::execute)
                profile("links")
                for ((kind, select) in MANIFEST_SEARCH) {
                    // Grouped selects (native_string) take each group's first row, as the search_items view does.
                    val rowid = if ("GROUP BY" in select) "min(rowid)" else "rowid"
                    st.execute("INSERT INTO search(rowid, text) SELECT ($rowid << 4) + ${IndexSchema.kindCode(kind)}, $select")
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
        conn.prepareStatement("INSERT INTO components VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)").use { st ->
            val reviews = ComponentReviews.of(extraction.components, m.`package`)
            for ((c, review) in extraction.components.zip(reviews)) {
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
                st.setString(22, json(review.tags))
                val attrs = ComponentReviews.attributes(c)
                st.setString(
                    23,
                    if (attrs.isEmpty()) null else JsonObject(attrs.associate { (k, v) -> k to JsonPrimitive(v) }).toString(),
                )
                st.addBatch()
            }
            st.executeBatch()
        }
        insertResources(extraction.resources)
        extraction.insights?.let(::insertInsights)
        insertIcons(m.icons)
        insertFiles(extraction.files)
        insertRaw(extraction)
        Records.write(conn, extraction)
        conn.prepareStatement("INSERT INTO schema_docs VALUES (?,?,?,?)").use { st ->
            for (d in SchemaDocs.ALL) {
                st.setString(1, d.table)
                st.setString(2, d.column)
                st.setString(3, d.type)
                st.setString(4, d.description)
                st.addBatch()
            }
            st.executeBatch()
        }
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

    private fun insertContents(
        files: List<ApkFile>,
        contents: Contents,
    ) {
        ZipFile(contents.apk.toFile()).use { zip ->
            conn.prepareStatement("INSERT INTO file_contents VALUES (?,?)").use { st ->
                var pendingBytes = 0L
                files.forEachIndexed { i, f ->
                    if (!contents.all && f.type != "dex") return@forEachIndexed
                    // Same zip-bomb guard as hashing: entries over 1 GB are listed, not stored.
                    if (f.sha256 == null || f.sizeBytes > MAX_CONTENT_BYTES) return@forEachIndexed
                    val entry = zip.getEntry(f.path) ?: return@forEachIndexed
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    st.setLong(1, i + 1L)
                    st.setBytes(2, bytes)
                    st.addBatch()
                    pendingBytes += bytes.size
                    if (pendingBytes > 64L * 1024 * 1024) {
                        st.executeBatch()
                        pendingBytes = 0
                    }
                }
                st.executeBatch()
            }
        }
    }

    private fun insertMedia(extraction: Extraction) {
        val fileIds = extraction.files.withIndex().associate { (i, f) -> f.path to i + 1L }

        fun PreparedStatement.file(
            i: Int,
            path: String,
        ) = fileIds[path]?.let { setLong(i, it) } ?: setNull(i, java.sql.Types.INTEGER)
        conn.prepareStatement("INSERT INTO images VALUES (?,?,?,?,?)").use { st ->
            for (img in extraction.raw.images) {
                st.setString(1, img.path)
                st.file(2, img.path)
                st.setString(3, img.format)
                img.width?.let { st.setInt(4, it) } ?: st.setNull(4, java.sql.Types.INTEGER)
                img.height?.let { st.setInt(5, it) } ?: st.setNull(5, java.sql.Types.INTEGER)
                st.addBatch()
            }
            st.executeBatch()
        }
        conn.prepareStatement("INSERT INTO fonts VALUES (?,?,?,?,?,?,?)").use { st ->
            for (f in extraction.raw.fonts) {
                st.setString(1, f.path)
                st.file(2, f.path)
                st.setString(3, f.family)
                st.setString(4, f.subfamily)
                st.setString(5, f.fullName)
                st.setString(6, f.version)
                st.setString(7, f.postscriptName)
                st.addBatch()
            }
            st.executeBatch()
        }
    }

    private fun insertSigning(d: SigningDetail) {
        conn.prepareStatement("INSERT INTO signing_signers VALUES (?,?,?,?,?,?)").use { st ->
            for (s in d.signers) {
                st.setString(1, s.scheme)
                st.setInt(2, s.index)
                st.setString(3, s.name)
                s.minSdk?.let { st.setInt(4, it) } ?: st.setNull(4, java.sql.Types.INTEGER)
                s.maxSdk?.let { st.setInt(5, it) } ?: st.setNull(5, java.sql.Types.INTEGER)
                st.setString(6, s.certSha256)
                st.addBatch()
            }
            d.sourceStamp?.let {
                st.setString(1, "source-stamp")
                st.setInt(2, 0)
                st.setNull(3, java.sql.Types.VARCHAR)
                st.setNull(4, java.sql.Types.INTEGER)
                st.setNull(5, java.sql.Types.INTEGER)
                st.setString(6, it)
                st.addBatch()
            }
            st.executeBatch()
        }
        conn.prepareStatement("INSERT INTO signing_digests VALUES (?,?,?,?)").use { st ->
            for (s in d.signers) {
                for ((algorithm, digest) in s.digests) {
                    st.setString(1, s.scheme)
                    st.setInt(2, s.index)
                    st.setString(3, algorithm)
                    st.setString(4, digest)
                    st.addBatch()
                }
            }
            st.executeBatch()
        }
        conn.prepareStatement("INSERT INTO signing_certificates VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)").use { st ->
            for (c in d.certificates) {
                listOf(
                    c.sha256,
                    c.sha1,
                    c.md5,
                    c.subject,
                    c.issuer,
                    c.serial,
                    c.notBefore,
                    c.notAfter,
                    c.signatureAlgorithm,
                    c.keyAlgorithm,
                ).forEachIndexed { i, v -> st.setString(i + 1, v) }
                c.keySize?.let { st.setInt(11, it) } ?: st.setNull(11, java.sql.Types.INTEGER)
                st.setString(12, c.publicKeySha256)
                st.setBytes(13, c.der)
                st.addBatch()
            }
            st.executeBatch()
        }
        conn.prepareStatement("INSERT INTO signing_lineage VALUES (?,?)").use { st ->
            d.lineage.forEachIndexed { i, cert ->
                st.setInt(1, i)
                st.setString(2, cert)
                st.addBatch()
            }
            st.executeBatch()
        }
        conn.prepareStatement("INSERT INTO signing_issues VALUES (?,?,?,?)").use { st ->
            for (i in d.issues) {
                st.setString(1, i.level)
                st.setString(2, i.scope)
                st.setString(3, i.issue)
                st.setString(4, i.message)
                st.addBatch()
            }
            st.executeBatch()
        }
        conn.prepareStatement("INSERT INTO signing_manifest VALUES (?,?,?)").use { st ->
            for ((path, algorithm, digest) in d.manifestDigests) {
                st.setString(1, path)
                st.setString(2, algorithm)
                st.setString(3, digest)
                st.addBatch()
            }
            st.executeBatch()
        }
    }

    private fun insertText(extraction: Extraction) {
        val fileIds = extraction.files.withIndex().associate { (i, f) -> f.path to i + 1L }
        conn.prepareStatement("INSERT INTO file_text VALUES (?,?,?,?)").use { text ->
            conn.prepareStatement("INSERT INTO file_properties VALUES (?,?,?,?)").use { props ->
                conn.prepareStatement("INSERT INTO services VALUES (?,?,?)").use { services ->
                    for (t in extraction.raw.text) {
                        text.setString(1, t.path)
                        fileIds[t.path]?.let { text.setLong(2, it) } ?: text.setNull(2, java.sql.Types.INTEGER)
                        text.setInt(3, t.text.lines().size)
                        text.setString(4, t.text)
                        text.addBatch()
                        t.properties.forEachIndexed { i, (k, v) ->
                            props.setString(1, t.path)
                            props.setInt(2, i)
                            props.setString(3, k)
                            props.setString(4, v)
                            props.addBatch()
                        }
                        for (impl in t.services) {
                            services.setString(1, t.path.removePrefix("META-INF/services/"))
                            services.setString(2, impl)
                            services.setString(3, t.path)
                            services.addBatch()
                        }
                    }
                    text.executeBatch()
                    props.executeBatch()
                    services.executeBatch()
                }
            }
        }
        // External-content FTS: built from file_text in one pass.
        conn.createStatement().use { it.execute("INSERT INTO file_text_search(file_text_search) VALUES ('rebuild')") }
    }

    private fun insertElf(extraction: Extraction) {
        val fileIds = extraction.files.withIndex().associate { (i, f) -> f.path to i + 1L }
        val elfPaths =
            extraction.raw.elf
                .map { it.path }
                .toSet()
        val files = conn.prepareStatement("INSERT INTO elf_files VALUES (?,?,?,?,?,?,?,?,?,?,?)")
        val sections = conn.prepareStatement("INSERT INTO elf_sections VALUES (?,?,?,?,?,?,?)")
        val needed = conn.prepareStatement("INSERT INTO elf_needed VALUES (?,?,?,?)")
        val symbols = conn.prepareStatement("INSERT INTO elf_symbols VALUES (?,?,?,?,?)")
        val strings = conn.prepareStatement("INSERT INTO elf_strings VALUES (?,?,?)")
        try {
            for (e in extraction.raw.elf) {
                val i = e.info
                files.setString(1, e.path)
                fileIds[e.path]?.let { files.setLong(2, it) } ?: files.setNull(2, java.sql.Types.INTEGER)
                files.setString(3, i.elfClass)
                files.setString(4, i.machine)
                files.setString(5, i.soname)
                files.setString(6, i.buildId)
                files.setString(7, i.runpath)
                files.setInt(8, i.symbols.count { it.defined })
                files.setInt(9, i.symbols.count { !it.defined })
                files.setInt(10, i.strings.size)
                files.setInt(11, if (i.stringsCapped) 1 else 0)
                files.addBatch()
                i.sections.forEachIndexed { k, s ->
                    sections.setString(1, e.path)
                    sections.setInt(2, k)
                    sections.setString(3, s.name)
                    sections.setString(4, s.type)
                    sections.setLong(5, s.offset)
                    sections.setLong(6, s.size)
                    sections.setString(7, s.flags)
                    sections.addBatch()
                }
                // A needed library resolves to the one with that name in the same folder (lib/arm64-v8a/), if the APK has it.
                val dir = e.path.substringBeforeLast('/', "")
                i.needed.forEachIndexed { k, n ->
                    needed.setString(1, e.path)
                    needed.setInt(2, k)
                    needed.setString(3, n)
                    needed.setString(4, (if (dir.isEmpty()) n else "$dir/$n").takeIf { it in elfPaths })
                    needed.addBatch()
                }
                for (s in i.symbols) {
                    symbols.setString(1, e.path)
                    symbols.setString(2, s.name)
                    symbols.setString(3, s.type)
                    symbols.setString(4, s.binding)
                    symbols.setInt(5, if (s.defined) 1 else 0)
                    symbols.addBatch()
                }
                for (s in i.strings) {
                    strings.setString(1, e.path)
                    strings.setLong(2, s.offset)
                    strings.setString(3, s.value)
                    strings.addBatch()
                }
            }
            listOf(files, sections, needed, symbols, strings).forEach { it.executeBatch() }
        } finally {
            listOf(files, sections, needed, symbols, strings).forEach { it.close() }
        }
    }

    private fun insertRaw(extraction: Extraction) {
        val fileIds = extraction.files.withIndex().associate { (i, f) -> f.path to i + 1L }
        var nextNode = 1L
        conn.prepareStatement("INSERT INTO xml_nodes VALUES (?,?,?,?,?,?,?,?)").use { nodes ->
            conn.prepareStatement("INSERT INTO xml_attrs VALUES (?,?,?,?)").use { attrs ->
                for (doc in extraction.raw.xml) {
                    var seq = 0

                    fun visit(
                        n: XmlNode,
                        parent: Long?,
                        depth: Int,
                    ) {
                        val id = nextNode++
                        nodes.setLong(1, id)
                        fileIds[doc.path]?.let { nodes.setLong(2, it) } ?: nodes.setNull(2, java.sql.Types.INTEGER)
                        nodes.setString(3, doc.path)
                        parent?.let { nodes.setLong(4, it) } ?: nodes.setNull(4, java.sql.Types.INTEGER)
                        nodes.setInt(5, depth)
                        nodes.setInt(6, seq++)
                        nodes.setString(7, n.tag)
                        nodes.setString(8, n.attributes["#text"])
                        nodes.addBatch()
                        for ((name, value) in n.attributes) {
                            if (name == "#text") continue
                            attrs.setLong(1, id)
                            attrs.setString(2, name)
                            attrs.setString(3, value)
                            attrs.setString(4, RawXml.ref(value))
                            attrs.addBatch()
                        }
                        n.children.forEach { visit(it, id, depth + 1) }
                    }
                    visit(doc.root, null, 0)
                }
                nodes.executeBatch()
                attrs.executeBatch()
            }
        }
        insertElf(extraction)
        insertText(extraction)
        insertSigning(extraction.raw.signing)
        insertMedia(extraction)
        conn.prepareStatement("INSERT INTO opcodes VALUES (?,?,?)").use { st ->
            for (op in com.android.tools.smali.dexlib2.Opcode.entries) {
                st.setInt(1, op.ordinal)
                st.setString(2, op.name)
                st.setString(3, op.format.name)
                st.addBatch()
            }
            st.executeBatch()
        }
        conn.prepareStatement("INSERT INTO resource_values VALUES (?,?,?,?,?,?)").use { values ->
            conn.prepareStatement("INSERT INTO resource_bag_items VALUES (?,?,?,?,?)").use { items ->
                for (r in extraction.raw.resourceTable) {
                    val id = "0x%08x".format(r.id)
                    values.setString(1, id)
                    values.setString(2, r.type)
                    values.setString(3, r.name)
                    values.setString(4, r.config)
                    values.setString(5, r.value)
                    values.setString(6, r.parent)
                    values.addBatch()
                    r.items.forEachIndexed { i, (key, value) ->
                        items.setString(1, id)
                        items.setString(2, r.config)
                        items.setInt(3, i)
                        items.setString(4, key)
                        items.setString(5, value)
                        items.addBatch()
                    }
                }
                values.executeBatch()
                items.executeBatch()
            }
        }
    }

    private fun insertFiles(files: List<ApkFile>) {
        conn.prepareStatement("INSERT INTO files VALUES (?,?,?,?,?,?,?,?,?,?,?,?)").use { st ->
            files.forEachIndexed { i, f ->
                st.setLong(1, i + 1L)
                st.setString(2, f.path)
                st.setString(3, f.type)
                st.setLong(4, f.sizeBytes)
                st.setLong(5, f.compressedBytes)
                st.setString(6, f.method)
                st.setString(7, f.crc32)
                st.setString(8, f.sha256)
                st.setString(9, f.magic)
                st.setString(10, f.resource)
                st.setString(11, f.config)
                st.setString(12, f.decodedBy)
                st.addBatch()
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

    private fun insertResources(r: ResourceHarvest) {
        fun <T> rows(
            table: String,
            columns: Int,
            items: List<T>,
            bind: (PreparedStatement, T) -> Unit,
        ) {
            if (items.isEmpty()) return
            conn.prepareStatement("INSERT INTO $table VALUES (" + List(columns) { "?" }.joinToString(",") + ")").use { st ->
                for (item in items) {
                    bind(st, item)
                    st.addBatch()
                }
                st.executeBatch()
            }
        }

        fun PreparedStatement.int(
            i: Int,
            v: Int?,
        ) = if (v == null) setNull(i, java.sql.Types.INTEGER) else setInt(i, v)
        val values = ListSerializer(ResValue.serializer())
        rows("string_resources", 6, r.strings) { st, s ->
            st.setString(1, s.name)
            st.setString(2, s.kind)
            st.setString(3, s.config)
            st.setString(4, s.locale)
            st.setString(5, s.item)
            st.setString(6, s.value)
        }
        rows("resource_ids", 4, r.ids) { st, x ->
            st.setString(1, x.id)
            st.setString(2, x.type)
            st.setString(3, x.name)
            st.setString(4, json(x.configs))
        }
        rows("styles", 5, r.styles) { st, x ->
            st.setString(1, x.name)
            st.setString(2, x.config)
            st.setString(3, x.parent)
            st.setString(4, SextantJson.compact.encodeToString(values, x.items))
            st.setString(5, json(x.usedBy))
        }
        rows("drawables", 8, r.drawables) { st, x ->
            st.setString(1, x.type)
            st.setString(2, x.name)
            st.setString(3, x.config)
            st.setString(4, x.path)
            st.setString(5, x.format)
            st.setLong(6, x.bytes)
            st.int(7, x.width)
            st.int(8, x.height)
        }
        rows("layouts", 9, r.layouts) { st, x ->
            st.setString(1, x.name)
            st.setString(2, x.config)
            st.setString(3, x.path)
            st.setString(4, x.root)
            st.setInt(5, x.viewCount)
            st.setString(6, json(x.ids))
            st.setString(7, json(x.customViews))
            st.setString(8, json(x.fragments))
            st.setString(9, json(x.includes))
        }
        rows("menus", 9, r.menus.flatMap { m -> m.items.map { m to it } }) { st, (m, i) ->
            st.setString(1, m.name)
            st.setString(2, m.config)
            st.setInt(3, i.depth)
            st.setString(4, i.tag)
            st.setString(5, i.id)
            st.setString(6, i.title?.raw)
            st.setString(7, i.title?.resolved)
            st.setString(8, i.icon)
            st.setString(9, i.showAsAction)
        }
        rows("navigation_destinations", 7, r.navigation.flatMap { g -> g.destinations.map { g to it } }) { st, (g, d) ->
            st.setString(1, g.name)
            st.setString(2, d.id)
            st.setString(3, d.kind)
            st.setString(4, d.className)
            st.setString(5, d.label?.let { it.resolved ?: it.raw })
            st.setString(6, json(d.deepLinks))
            st.setString(7, SextantJson.compact.encodeToString(ListSerializer(NavAction.serializer()), d.actions))
        }
        rows("assets", 5, r.assets) { st, x ->
            st.setString(1, x.path)
            st.setString(2, x.kind)
            st.setLong(3, x.bytes)
            st.setString(4, x.magic)
            st.setInt(5, if (x.hiddenCode) 1 else 0)
        }
        rows("xml_configs", 6, r.xml) { st, x ->
            st.setString(1, x.name)
            st.setString(2, x.config)
            st.setString(3, x.path)
            st.setString(4, x.kind)
            st.setString(5, JsonObject(x.summary.mapValues { JsonPrimitive(it.value) }).toString())
            st.setString(6, x.xml)
        }
        rows("resource_refs", 4, r.refs) { st, x ->
            st.setString(1, x.ref)
            st.setString(2, x.fromKind)
            st.setString(3, x.from)
            st.setString(4, x.attribute)
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
                "string_resource" to "substr(name || ' = ' || value, 1, ${IndexSchema.MAX_SEARCH_TEXT}) FROM string_resources",
                // The same string often sits in every ABI's copy of a library: index each value once.
                "native_string" to "substr(value, 1, ${IndexSchema.MAX_SEARCH_TEXT}) FROM elf_strings GROUP BY value",
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

    override fun resourceUse(record: ResourceUseRecord) = sinks.forEach { it.resourceUse(record) }

    override fun annotation(record: AnnotationRecord) = sinks.forEach { it.annotation(record) }

    override fun methodDetail(record: MethodDetailRecord) = sinks.forEach { it.methodDetail(record) }

    override val wantsInstructions: Boolean get() = sinks.any { it.wantsInstructions }

    override fun methodCode(record: MethodCodeRecord) = sinks.forEach { if (it.wantsInstructions) it.methodCode(record) }
}
