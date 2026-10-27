package io.github.rajumark.sextant.desktop.data

import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.output.OutputFiles
import io.github.rajumark.sextant.core.output.OutputReader
import io.github.rajumark.sextant.core.output.OutputWriter
import io.github.rajumark.sextant.desktop.model.Analysis
import java.nio.file.Files
import java.nio.file.Path
import java.util.prefs.Preferences
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension

/** Where the desktop app gets its data. [CoreRepository] uses the core library in-process. */
interface SextantRepository {
    /** Runs the same extraction as `sextant extract` and returns the result. Blocking; call off the UI thread. */
    fun analyze(apk: Path): Analysis

    /** Reads an existing output folder, like `sextant info <dir>`. Blocking. */
    fun open(dir: Path): Analysis

    /** Output folders opened before, newest first. */
    fun recentFolders(): List<Path>

    fun remember(dir: Path)

    fun forget(dir: Path)

    fun clearRecent()

    /** Deletes an output folder written by Sextant (never the APK). Refuses folders that are not Sextant's. */
    fun delete(dir: Path)

    /** Where [analyze] writes for [apk]; shown before the run starts. */
    fun outputDirFor(apk: Path): Path
}

/** A user-facing failure: message plus an optional hint, as the CLI prints them. */
class AnalysisFailure(
    message: String,
    val hint: String?,
    cause: Throwable?,
) : Exception(message, cause)

class CoreRepository(
    private val store: RecentStore = PreferencesRecentStore(),
    private val fallbackRoot: Path = appDataDir().resolve("analyses"),
) : SextantRepository {
    override fun analyze(apk: Path): Analysis =
        failures {
            if (!Files.isRegularFile(apk)) throw SextantException.input("$apk is not a file")
            val dir = outputDirFor(apk)
            val started = System.nanoTime()
            // force only replaces Sextant's own files, and outputDirFor only picks empty or Sextant-owned folders.
            val written = OutputWriter.extractTo(apk, dir, force = true, includeCode = true, includeIndex = true)
            val ms = (System.nanoTime() - started) / 1_000_000
            store.push(dir)
            CoreMapper.map(written.extraction, dir, apk, System.currentTimeMillis(), ms)
        }

    override fun open(dir: Path): Analysis =
        failures {
            val extraction = OutputReader.read(dir)
            val at = runCatching { dir.resolve(OutputFiles.META).getLastModifiedTime().toMillis() }.getOrDefault(System.currentTimeMillis())
            store.push(dir)
            CoreMapper.map(extraction, dir, apkNextTo(dir), at, null)
        }

    override fun recentFolders(): List<Path> = store.all().filter { it.resolve(OutputFiles.META).exists() }

    override fun remember(dir: Path) = store.push(dir)

    override fun forget(dir: Path) = store.remove(dir)

    override fun clearRecent() = store.clear()

    override fun delete(dir: Path) =
        failures {
            if (!Files.isRegularFile(dir.resolve(OutputFiles.META))) {
                throw SextantException.output("$dir is not a Sextant output folder", "Nothing was deleted.")
            }
            Files.walk(dir).use { s -> s.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
            store.remove(dir)
        }

    /**
     * `<apk-name>.sextant/` next to the APK, like the CLI's default. Falls back to the app data folder when that
     * place is not writable or already holds something that is not a Sextant output folder.
     */
    override fun outputDirFor(apk: Path): Path {
        val name = "${apk.nameWithoutExtension}.sextant"
        val beside = apk.toAbsolutePath().parent?.resolve(name)
        if (beside != null && usable(beside)) return beside
        var n = 1
        while (true) {
            val candidate = fallbackRoot.resolve(if (n == 1) name else "${apk.nameWithoutExtension}-$n.sextant")
            if (usable(candidate)) return candidate
            n++
        }
    }

    private fun usable(dir: Path): Boolean {
        if (!dir.exists()) return dir.parent?.let { p -> Files.isWritable(p) || !p.exists() } ?: false
        if (!dir.isDirectory() || !Files.isWritable(dir)) return false
        val empty = Files.list(dir).use { it.findAny().isEmpty }
        return empty || dir.resolve(OutputFiles.META).exists()
    }

    /** The CLI writes `<name>.sextant/` beside `<name>.apk`; find that APK again so "Analyze again" works. */
    private fun apkNextTo(dir: Path): Path? {
        val apk = dir.parent?.resolve(dir.fileName.toString().removeSuffix(".sextant") + ".apk") ?: return null
        return apk.takeIf { Files.isRegularFile(it) }
    }

    private inline fun <T> failures(block: () -> T): T =
        try {
            block()
        } catch (e: SextantException) {
            throw AnalysisFailure(e.message ?: "Sextant failed", e.hint, e)
        } catch (e: OutOfMemoryError) {
            throw AnalysisFailure("Ran out of memory while reading this APK", "Very large APKs need more heap; this is a known limit", e)
        } catch (e: Exception) {
            throw AnalysisFailure(e.message ?: e.javaClass.simpleName, "This looks like a bug in Sextant; please report it with the APK", e)
        }

    companion object {
        fun appDataDir(): Path {
            val home = Path.of(System.getProperty("user.home"))
            val os = System.getProperty("os.name").lowercase()
            return when {
                os.startsWith("mac") -> home.resolve("Library/Application Support/Sextant")
                os.startsWith(
                    "windows",
                ) -> Path.of(System.getenv("APPDATA") ?: home.resolve("AppData/Roaming").toString()).resolve("Sextant")
                else -> Path.of(System.getenv("XDG_DATA_HOME") ?: home.resolve(".local/share").toString()).resolve("sextant")
            }
        }
    }
}

/** Remembers output folders between launches. */
interface RecentStore {
    fun all(): List<Path>

    fun push(dir: Path)

    fun remove(dir: Path)

    fun clear()
}

class PreferencesRecentStore(
    private val prefs: Preferences = Preferences.userRoot().node("io/github/rajumark/sextant/desktop"),
    private val limit: Int = 12,
) : RecentStore {
    override fun all(): List<Path> =
        prefs
            .get(KEY, "")
            .lines()
            .filter { it.isNotBlank() }
            .map { Path.of(it) }

    override fun push(dir: Path) = save(listOf(dir.toAbsolutePath().normalize()) + all().filter { it != dir.toAbsolutePath().normalize() })

    override fun remove(dir: Path) = save(all().filter { it != dir.toAbsolutePath().normalize() })

    override fun clear() = save(emptyList())

    private fun save(list: List<Path>) {
        prefs.put(KEY, list.take(limit).joinToString("\n"))
        runCatching { prefs.flush() }
    }

    private companion object {
        const val KEY = "recent"
    }
}
