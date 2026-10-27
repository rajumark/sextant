package io.github.rajumark.sextant.core.output

import io.github.rajumark.sextant.core.AsyncSink
import io.github.rajumark.sextant.core.CodeSink
import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.Extractor
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.index.IndexWriter
import io.github.rajumark.sextant.core.index.TeeSink
import io.github.rajumark.sextant.core.insights.Insights
import io.github.rajumark.sextant.core.model.AppMeta
import io.github.rajumark.sextant.core.model.Component
import io.github.rajumark.sextant.core.model.FeatureRecord
import io.github.rajumark.sextant.core.model.MetadataRecord
import io.github.rajumark.sextant.core.model.Permission
import io.github.rajumark.sextant.core.model.QueryRecord
import io.github.rajumark.sextant.core.model.ResourceHarvest
import io.github.rajumark.sextant.core.model.XmlNode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Output file names. Shared with readers (info, later versions). */
object OutputFiles {
    const val SUMMARY = "summary.md"
    const val META = "meta.json"
    const val MANIFEST = "manifest.json"
    const val PERMISSIONS = "permissions.jsonl"
    const val COMPONENTS = "components.jsonl"
    const val FEATURES = "features.jsonl"
    const val METADATA = "metadata.jsonl"
    const val QUERIES = "queries.jsonl"
    const val INDEX = "index.sqlite"
    const val LIBRARIES = "insights/libraries.jsonl"
    const val FRAMEWORKS = "insights/frameworks.jsonl"
    const val SDKS = "insights/sdks.jsonl"
    const val ENDPOINTS = "insights/endpoints.jsonl"
    const val RESOURCES = "resources/values.jsonl"
    const val NATIVE_LIBRARIES = "native/libraries.jsonl"
    const val NATIVE_SYMBOLS = "native/symbols.jsonl"
    const val FINDINGS = "insights/findings.jsonl"

    // Schema 8: res/ and assets/.
    const val RES_STRINGS = "resources/strings.jsonl"
    const val RES_IDS = "resources/ids.jsonl"
    const val RES_STYLES = "resources/styles.jsonl"
    const val RES_DRAWABLES = "resources/drawables.jsonl"
    const val RES_LAYOUTS = "resources/layouts.jsonl"
    const val RES_MENUS = "resources/menus.jsonl"
    const val RES_NAVIGATION = "resources/navigation.jsonl"
    const val RES_ASSETS = "resources/assets.jsonl"
    const val RES_XML = "resources/xml.jsonl"
    const val RES_REFS = "resources/refs.jsonl"
    val HARVEST = listOf(RES_STRINGS, RES_IDS, RES_STYLES, RES_DRAWABLES, RES_LAYOUTS, RES_MENUS, RES_NAVIGATION, RES_ASSETS, RES_XML, RES_REFS)
    val INSIGHTS = listOf(LIBRARIES, FRAMEWORKS, SDKS, ENDPOINTS, RESOURCES, NATIVE_LIBRARIES, NATIVE_SYMBOLS, FINDINGS)
    val ALL = listOf(SUMMARY, META, MANIFEST, PERMISSIONS, COMPONENTS, FEATURES, METADATA, QUERIES)
}

/**
 * Writes an [Extraction] deterministically: fixed key order, `\n` line endings, UTF-8, trailing newline,
 * no timestamps or absolute paths. Each file is written to a temp file first and then moved into place.
 */
object OutputWriter {
    /** Result of [extractTo]: the in-memory extraction plus every file written. */
    class Written(
        val extraction: Extraction,
        val files: List<Path>,
    )

    /**
     * Extracts [apk] into [dir]: code records are streamed to `code/` while the APK is read, then the
     * manifest-level files are written. With [includeCode] false, stale `code/` files from an earlier run are removed.
     */
    fun extractTo(
        apk: Path,
        dir: Path,
        force: Boolean,
        includeCode: Boolean = true,
        includeIndex: Boolean = true,
        writeCodeFiles: Boolean = true,
        extraCodeSink: CodeSink? = null,
    ): Written {
        prepare(dir, force)
        val index = if (includeIndex) IndexWriter(dir.resolve(OutputFiles.INDEX)) else null
        // The index is the slow consumer: feed it from a background thread while JSONL is written here.
        val indexFeed = if (index != null && includeCode) AsyncSink(index) else null
        // writeCodeFiles = false still reads the code (stats, index, extraCodeSink) but writes no code/*.jsonl.
        val codeFiles = if (includeCode && writeCodeFiles) FileCodeSink(dir) else null
        try {
            val sinks = listOfNotNull(codeFiles, indexFeed, extraCodeSink.takeIf { includeCode })
            val code =
                when {
                    !includeCode -> null
                    sinks.size == 1 -> sinks.single()
                    sinks.isEmpty() -> Extractor.COUNT_ONLY
                    else -> TeeSink(*sinks.toTypedArray())
                }
            val extraction = Extractor.extract(apk, code)
            indexFeed?.await()
            val written = mutableListOf<Path>()
            if (codeFiles != null) {
                written.addAll(codeFiles.commit())
            } else {
                CodeFiles.ALL.forEach { Files.deleteIfExists(dir.resolve(it)) }
            }
            if (index != null) {
                written.add(index.finish(extraction))
            } else {
                Files.deleteIfExists(dir.resolve(OutputFiles.INDEX))
            }
            return Written(extraction, write(extraction, dir, force = true, hasIndex = index != null) + written)
        } finally {
            indexFeed?.close()
            codeFiles?.close()
            index?.close()
        }
    }

    fun write(
        extraction: Extraction,
        dir: Path,
        force: Boolean,
        hasIndex: Boolean = false,
    ): List<Path> {
        prepare(dir, force)
        return listOf(
            writeText(dir, OutputFiles.SUMMARY, SummaryWriter.render(extraction, hasIndex)),
            writeText(dir, OutputFiles.META, pretty(extraction.meta)),
            writeText(dir, OutputFiles.MANIFEST, pretty(extraction.manifest)),
            writeText(dir, OutputFiles.PERMISSIONS, jsonl(extraction.permissions)),
            writeText(dir, OutputFiles.COMPONENTS, jsonl(extraction.components)),
            writeText(dir, OutputFiles.FEATURES, jsonl(extraction.features)),
            writeText(dir, OutputFiles.METADATA, jsonl(extraction.metadata)),
            writeText(dir, OutputFiles.QUERIES, jsonl(extraction.queries)),
        ) + writeResources(extraction.resources, dir) + (extraction.insights?.let { writeInsights(it, dir) } ?: emptyList())
    }

    private fun writeResources(
        r: ResourceHarvest,
        dir: Path,
    ): List<Path> =
        listOf(
            writeText(dir, OutputFiles.RES_STRINGS, jsonl(r.strings)),
            writeText(dir, OutputFiles.RES_IDS, jsonl(r.ids)),
            writeText(dir, OutputFiles.RES_STYLES, jsonl(r.styles)),
            writeText(dir, OutputFiles.RES_DRAWABLES, jsonl(r.drawables)),
            writeText(dir, OutputFiles.RES_LAYOUTS, jsonl(r.layouts)),
            writeText(dir, OutputFiles.RES_MENUS, jsonl(r.menus)),
            writeText(dir, OutputFiles.RES_NAVIGATION, jsonl(r.navigation)),
            writeText(dir, OutputFiles.RES_ASSETS, jsonl(r.assets)),
            writeText(dir, OutputFiles.RES_XML, jsonl(r.xml)),
            writeText(dir, OutputFiles.RES_REFS, jsonl(r.refs)),
        )

    private fun writeInsights(
        insights: Insights,
        dir: Path,
    ): List<Path> {
        for (sub in listOf("insights", "resources", "native")) Files.createDirectories(dir.resolve(sub))
        return listOf(
            writeText(dir, OutputFiles.LIBRARIES, jsonl(insights.libraries)),
            writeText(dir, OutputFiles.FRAMEWORKS, jsonl(insights.frameworks)),
            writeText(dir, OutputFiles.SDKS, jsonl(insights.sdks)),
            writeText(dir, OutputFiles.ENDPOINTS, jsonl(insights.endpoints)),
            writeText(dir, OutputFiles.RESOURCES, jsonl(insights.resources)),
            writeText(dir, OutputFiles.NATIVE_LIBRARIES, jsonl(insights.nativeLibraries)),
            writeText(dir, OutputFiles.NATIVE_SYMBOLS, jsonl(insights.nativeSymbols)),
            writeText(dir, OutputFiles.FINDINGS, jsonl(insights.findings)),
        )
    }

    /**
     * Refuses to write into a non-empty directory unless [force]. Even with [force] only Sextant's own
     * files are replaced; nothing else in the directory is touched or deleted.
     */
    fun prepare(
        dir: Path,
        force: Boolean,
    ) {
        if (Files.exists(dir)) {
            if (!Files.isDirectory(dir)) throw SextantException.output("$dir exists and is not a directory")
            val nonEmpty = Files.list(dir).use { it.findAny().isPresent }
            if (nonEmpty && !force) {
                throw SextantException.output("output directory $dir is not empty", "pass --force to overwrite Sextant's files in it")
            }
        } else {
            try {
                Files.createDirectories(dir)
            } catch (e: IOException) {
                throw SextantException.output("cannot create output directory $dir: ${e.message}", cause = e)
            }
        }
        if (!Files.isWritable(dir)) throw SextantException.output("output directory $dir is not writable")
    }

    fun writeText(
        dir: Path,
        name: String,
        content: String,
    ): Path {
        val target = dir.resolve(name)
        try {
            Files.createDirectories(target.parent)
            val tmp = Files.createTempFile(target.parent, ".${target.fileName}.", ".tmp")
            val text = if (content.isEmpty() || content.endsWith("\n")) content else content + "\n"
            Files.writeString(tmp, text.escapeLoneSurrogates())
            tmp.makeReadable()
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            throw SextantException.output("cannot write $target: ${e.message}", cause = e)
        }
        return target
    }

    inline fun <reified T> pretty(value: T): String = SextantJson.pretty.encodeToString(serializer<T>(), value)

    inline fun <reified T> jsonl(records: List<T>): String = jsonl(records, serializer())

    fun <T> jsonl(
        records: List<T>,
        serializer: KSerializer<T>,
    ): String = records.joinToString("") { SextantJson.compact.encodeToString(serializer, it) + "\n" }
}

/** Reads an extraction folder back into memory (used by `info`). */
object OutputReader {
    fun read(dir: Path): Extraction {
        val meta = file(dir, OutputFiles.META)
        return Extraction(
            meta = SextantJson.reader.decodeFromString<AppMeta>(Files.readString(meta)),
            manifest = SextantJson.reader.decodeFromString<XmlNode>(Files.readString(file(dir, OutputFiles.MANIFEST))),
            permissions = lines(file(dir, OutputFiles.PERMISSIONS)).map { SextantJson.reader.decodeFromString<Permission>(it) },
            components = lines(file(dir, OutputFiles.COMPONENTS)).map { SextantJson.reader.decodeFromString<Component>(it) },
            // Added in schema 7: older folders simply have none.
            features = optionalLines(dir, OutputFiles.FEATURES).map { SextantJson.reader.decodeFromString<FeatureRecord>(it) },
            metadata = optionalLines(dir, OutputFiles.METADATA).map { SextantJson.reader.decodeFromString<MetadataRecord>(it) },
            queries = optionalLines(dir, OutputFiles.QUERIES).map { SextantJson.reader.decodeFromString<QueryRecord>(it) },
            // Added in schema 8.
            resources =
                ResourceHarvest(
                    ids = optionalLines(dir, OutputFiles.RES_IDS).map { SextantJson.reader.decodeFromString(it) },
                    strings = optionalLines(dir, OutputFiles.RES_STRINGS).map { SextantJson.reader.decodeFromString(it) },
                    styles = optionalLines(dir, OutputFiles.RES_STYLES).map { SextantJson.reader.decodeFromString(it) },
                    drawables = optionalLines(dir, OutputFiles.RES_DRAWABLES).map { SextantJson.reader.decodeFromString(it) },
                    layouts = optionalLines(dir, OutputFiles.RES_LAYOUTS).map { SextantJson.reader.decodeFromString(it) },
                    menus = optionalLines(dir, OutputFiles.RES_MENUS).map { SextantJson.reader.decodeFromString(it) },
                    navigation = optionalLines(dir, OutputFiles.RES_NAVIGATION).map { SextantJson.reader.decodeFromString(it) },
                    assets = optionalLines(dir, OutputFiles.RES_ASSETS).map { SextantJson.reader.decodeFromString(it) },
                    xml = optionalLines(dir, OutputFiles.RES_XML).map { SextantJson.reader.decodeFromString(it) },
                    refs = optionalLines(dir, OutputFiles.RES_REFS).map { SextantJson.reader.decodeFromString(it) },
                ),
        )
    }

    private fun file(
        dir: Path,
        name: String,
    ): Path {
        val path = dir.resolve(name)
        if (!Files.isRegularFile(path)) {
            throw SextantException.input("$dir is not a Sextant output folder (missing $name)", "run `sextant extract <app.apk>` first")
        }
        return path
    }

    private fun optionalLines(
        dir: Path,
        name: String,
    ) = dir.resolve(name).let { if (Files.isRegularFile(it)) lines(it) else emptyList() }

    private fun lines(path: Path) = Files.readAllLines(path).filter { it.isNotBlank() }
}
