package io.github.rajumark.sextant.core

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** The downloaded real-APK corpus (scripts/fetch-apks.sh). */
object CorpusApks {
    val root: Path =
        generateSequence(
            Paths.get("").toAbsolutePath(),
        ) { it.parent }.first { Files.exists(it.resolve("settings.gradle.kts")) }

    val dir: Path = root.resolve("fixtures/external")

    val goldenDir: Path = root.resolve("fixtures/golden")

    /** Every id from the lock file. */
    val ids: List<String> by lazy {
        Files
            .readAllLines(root.resolve("fixtures/external.lock"))
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.substringBefore('\t') }
    }

    /** Path to a corpus APK, or null if it has not been downloaded (the corpus test then fails loudly). */
    fun path(id: String): Path? = dir.resolve("$id.apk").takeIf { Files.isRegularFile(it) }

    private val MIN_FREE_BYTES = (System.getenv("SEXTANT_MIN_FREE_GB")?.toLongOrNull() ?: 3L) * 1024 * 1024 * 1024

    /** Fails early with a clear message instead of filling the disk halfway through a corpus run. */
    fun requireFreeSpace(at: Path = Paths.get(System.getProperty("java.io.tmpdir"))) {
        val free = Files.getFileStore(at).usableSpace
        check(free >= MIN_FREE_BYTES) {
            "only ${free / 1024 / 1024 / 1024} GB free in $at (need ${MIN_FREE_BYTES / 1024 / 1024 / 1024}); free space or set SEXTANT_MIN_FREE_GB"
        }
    }

    /** The corpus APK, after checking there is room to extract it. */
    fun require(id: String): Path {
        requireFreeSpace()
        return path(id) ?: error("corpus APK '$id' is missing: run scripts/fetch-apks.sh (APKs are downloaded, never committed)")
    }
}
