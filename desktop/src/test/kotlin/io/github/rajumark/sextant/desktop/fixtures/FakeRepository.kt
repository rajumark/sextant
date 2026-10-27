package io.github.rajumark.sextant.desktop.data

import io.github.rajumark.sextant.desktop.AppState
import io.github.rajumark.sextant.desktop.model.Analysis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import kotlin.io.path.nameWithoutExtension

/** Serves the sample analyses. [hold] keeps analyze() running until released, to show the progress state. */
class FakeRepository(
    private val samples: List<Analysis> = SampleData.recent,
    val hold: CountDownLatch? = null,
    private val failWith: AnalysisFailure? = null,
) : SextantRepository {
    val remembered = mutableListOf<Path>()
    val deleted = mutableListOf<Path>()

    override fun analyze(apk: Path): Analysis {
        hold?.await()
        failWith?.let { throw it }
        return samples.firstOrNull { it.meta.file.name == apk.fileName.toString() } ?: samples.first()
    }

    override fun open(dir: Path): Analysis =
        samples.firstOrNull { Path.of(it.outputDir) == dir } ?: throw AnalysisFailure("$dir is not a Sextant output folder", null, null)

    override fun recentFolders(): List<Path> = samples.map { Path.of(it.outputDir) }

    override fun remember(dir: Path) {
        remembered.add(dir)
    }

    override fun forget(dir: Path) = Unit

    override fun clearRecent() = Unit

    override fun delete(dir: Path) {
        deleted.add(dir)
    }

    override fun outputDirFor(apk: Path): Path = Path.of("/samples/${apk.nameWithoutExtension}.sextant")
}

/** An AppState that runs its work inline, so tests and screenshots see results without waiting. */
fun testState(
    initial: Analysis? = null,
    repo: SextantRepository = FakeRepository(),
    io: CoroutineDispatcher = Dispatchers.Unconfined,
    pickApk: () -> Path? = { null },
): AppState = AppState(repo, CoroutineScope(Dispatchers.Unconfined), io, settings = MemorySettings(), pickApk = pickApk, initial = initial)

/** Recent folders kept in memory instead of user preferences. */
class MemoryStore : RecentStore {
    private val dirs = mutableListOf<Path>()

    override fun all() = dirs.toList()

    override fun push(dir: Path) {
        dirs.remove(dir)
        dirs.add(0, dir)
    }

    override fun remove(dir: Path) {
        dirs.remove(dir)
    }

    override fun clear() = dirs.clear()
}
