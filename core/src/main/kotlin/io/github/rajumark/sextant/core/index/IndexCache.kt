package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.ApkReader
import io.github.rajumark.sextant.core.AsyncSink
import io.github.rajumark.sextant.core.Extractor
import io.github.rajumark.sextant.core.Sextant
import io.github.rajumark.sextant.core.SextantException
import kotlinx.serialization.Serializable
import org.sqlite.SQLiteConfig
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.name

/** One cached index, for `sextant cache`. */
@Serializable
data class CacheEntry(
    val sha256: String,
    val path: String,
    val sizeBytes: Long,
    val label: String?,
    val `package`: String?,
    val versionName: String?,
    /** False when it was built by another Sextant version or index schema: it is rebuilt on next use. */
    val current: Boolean,
)

/**
 * Indexes built from APKs, keyed by the APK's SHA-256, so any command given an APK reads a ready-made index.sqlite
 * instead of re-reading the APK. The first use builds it (one full extract, SQLite only); later uses only hash the APK.
 * An index from another Sextant version or index schema is rebuilt, so cached answers always match a fresh extract.
 */
object IndexCache {
    private const val SUFFIX = ".sqlite"
    private val INDEX_FILE = Regex("""[0-9a-f]{64}\.sqlite""")

    /** `-Dsextant.index.cache`, else `SEXTANT_CACHE`, else the platform's user cache folder. */
    fun root(): Path {
        System.getProperty("sextant.index.cache")?.let { return Path.of(it) }
        System.getenv("SEXTANT_CACHE")?.takeIf { it.isNotBlank() }?.let { return Path.of(it) }
        val home = Path.of(System.getProperty("user.home"))
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> home.resolve("Library/Caches/sextant")
            os.contains("win") ->
                (System.getenv("LOCALAPPDATA")?.let { Path.of(it) } ?: home.resolve("AppData/Local")).resolve("sextant/cache")
            else ->
                (
                    System
                        .getenv(
                            "XDG_CACHE_HOME",
                        )?.takeIf { it.isNotBlank() }
                        ?.let { Path.of(it) } ?: home.resolve(".cache")
                ).resolve("sextant")
        }.resolve("index")
    }

    /** The index of [apk], building it first when there is no current one. [onBuild] is told before a build starts. */
    fun index(
        apk: Path,
        onBuild: (String) -> Unit = {},
    ): Path {
        if (!Files.isRegularFile(apk)) throw SextantException.input("file not found: $apk")
        val head = Files.newInputStream(apk).use { it.readNBytes(2) }
        if (!head.contentEquals(byteArrayOf('P'.code.toByte(), 'K'.code.toByte()))) {
            throw SextantException.input(
                "$apk is not a valid APK (not a zip archive)",
                "pass an APK, a split bundle, an extracted folder or an index.sqlite",
            )
        }
        val sha = ApkReader.sha256(apk)
        val file = root().resolve(sha + SUFFIX)
        if (isCurrent(file)) return file
        onBuild("Indexing ${apk.fileName} once; later commands on this APK reuse ${file.parent}")
        build(apk, file)
        return file
    }

    /** The index of [apk] if one is already cached and current; never builds. */
    fun cached(apk: Path): Path? = root().resolve(ApkReader.sha256(apk) + SUFFIX).takeIf { isCurrent(it) }

    fun entries(): List<CacheEntry> {
        val dir = root()
        if (!Files.isDirectory(dir)) return emptyList()
        return Files
            .list(dir)
            .use { files ->
                files.filter { INDEX_FILE.matches(it.name) }.toList()
            }.map { f ->
                val meta = runCatching { meta(f) }.getOrDefault(emptyMap())
                CacheEntry(
                    sha256 = f.name.removeSuffix(SUFFIX),
                    path = f.toString(),
                    sizeBytes = Files.size(f),
                    label = meta["label"],
                    `package` = meta["package"],
                    versionName = meta["version_name"],
                    current = current(meta),
                )
            }.sortedWith(compareBy({ it.label ?: it.`package` ?: "" }, { it.versionName }, { it.sha256 }))
    }

    /** Deletes every cached index (and leftovers of interrupted builds). Returns the bytes freed. */
    fun clear(): Long {
        val dir = root()
        if (!Files.isDirectory(dir)) return 0
        var freed = 0L
        Files
            .list(dir)
            .use { files ->
                files.filter { it.name.endsWith(SUFFIX) || it.name.endsWith(".tmp") }.toList()
            }.forEach { f ->
                freed += runCatching { Files.size(f) }.getOrDefault(0)
                Files.deleteIfExists(f)
            }
        return freed
    }

    private fun isCurrent(file: Path) = Files.isRegularFile(file) && runCatching { current(meta(file)) }.getOrDefault(false)

    private fun current(meta: Map<String, String>) =
        meta["index_schema_version"] == IndexSchema.VERSION.toString() && meta["tool_version"] == Sextant.version

    private fun meta(file: Path): Map<String, String> {
        val config = SQLiteConfig().apply { setReadOnly(true) }
        return config.createConnection("jdbc:sqlite:$file").use { conn ->
            conn.createStatement().use { st ->
                val sql =
                    "SELECT key, value FROM meta WHERE key IN ('index_schema_version', 'label', 'package', 'version_name') " +
                        "UNION ALL SELECT 'tool_version', json_extract(value, '$.tool.version') FROM meta WHERE key = 'meta_json'"
                st.executeQuery(sql).use { rs ->
                    buildMap { while (rs.next()) rs.getString(2)?.let { put(rs.getString(1), it) } }
                }
            }
        }
    }

    /** One full extract into SQLite only. Built under a per-process name and moved into place, so parallel runs never clash. */
    private fun build(
        apk: Path,
        file: Path,
    ) {
        try {
            Files.createDirectories(file.parent)
        } catch (e: Exception) {
            throw SextantException.output("cannot create the index cache ${file.parent}: ${e.message}", cause = e)
        }
        val staging = file.resolveSibling("${file.name.removeSuffix(SUFFIX)}.${ProcessHandle.current().pid()}$SUFFIX")
        try {
            IndexWriter(staging).use { index ->
                AsyncSink(index).use { feed ->
                    val extraction = Extractor.extract(apk, feed, listFiles = true)
                    feed.await()
                    index.finish(extraction)
                }
            }
            Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(staging)
        }
    }
}
