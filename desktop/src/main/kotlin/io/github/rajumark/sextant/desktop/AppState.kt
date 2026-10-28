package io.github.rajumark.sextant.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import io.github.rajumark.sextant.desktop.data.AnalysisFailure
import io.github.rajumark.sextant.desktop.data.AppSettings
import io.github.rajumark.sextant.desktop.data.PreferencesSettings
import io.github.rajumark.sextant.desktop.data.SextantRepository
import io.github.rajumark.sextant.desktop.data.parseHexColor
import io.github.rajumark.sextant.desktop.data.toHex
import io.github.rajumark.sextant.desktop.model.Analysis
import io.github.rajumark.sextant.desktop.theme.DefaultSeed
import io.github.rajumark.sextant.desktop.theme.ThemeMode
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.name

/** One running `sextant extract`. Core has no progress callback yet, so the dialog shows elapsed time until done. */
class ExtractionJob(
    val apk: Path,
    val outputDir: Path,
) {
    val startedAtMillis: Long = System.currentTimeMillis()
    var result by mutableStateOf<Analysis?>(null)
    var failure by mutableStateOf<AnalysisFailure?>(null)
}

/** A problem to show in a dialog. */
data class Problem(
    val title: String,
    val message: String,
    val hint: String?,
)

/** Whole-app UI state, backed by a [SextantRepository]. */
class AppState(
    private val repo: SextantRepository,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val settings: AppSettings = PreferencesSettings(),
    /** Native file dialogs; replaced in tests. */
    private val pickApk: () -> Path? = { null },
    private val pickFolder: () -> Path? = { null },
    /** Opens a folder in Finder / Explorer; replaced in tests. */
    private val reveal: (Path) -> Unit = {},
    initial: Analysis? = null,
) {
    private var theme by mutableStateOf(settings.themeMode() ?: ThemeMode.System)

    /** Light, dark or system. The choice is saved, so the next launch starts the same way. */
    var themeMode: ThemeMode
        get() = theme
        set(value) {
            theme = value
            settings.saveThemeMode(value)
        }

    /** The seed the whole Material 3 scheme is generated from. Saved between launches. */
    var seed by mutableStateOf(settings.seedHex()?.let(::parseHexColor) ?: DefaultSeed)
        private set

    fun changeSeed(color: Color?) {
        seed = color ?: DefaultSeed
        settings.saveSeedHex(color?.takeIf { it != DefaultSeed }?.toHex())
    }

    /** Sidebar sections the user folded. Resources starts folded: most reviews start elsewhere. Saved between launches. */
    var foldedMenuSections by mutableStateOf(settings.foldedMenuSections() ?: setOf("Resources"))
        private set

    fun toggleMenuSection(title: String) {
        foldedMenuSections = if (title in foldedMenuSections) foldedMenuSections - title else foldedMenuSections + title
        settings.saveFoldedMenuSections(foldedMenuSections)
    }

    var showSettings by mutableStateOf(false)
    val recent = mutableStateListOf<Analysis>()
    var loadingRecent by mutableStateOf(false)
        private set
    var problem by mutableStateOf<Problem?>(null)

    /** Tags per output folder, shown on recent rows. Only folders with tags have an entry. */
    val tags = mutableStateMapOf<String, List<String>>()

    /** The analysis whose tag dialog is open, and the one waiting for delete confirmation. */
    var tagging by mutableStateOf<Analysis?>(null)
    var deleting by mutableStateOf<Analysis?>(null)

    var workspace by mutableStateOf(initial?.let(::WorkspaceState))
        private set
    var extraction by mutableStateOf<ExtractionJob?>(null)
        private set

    /** Reads the remembered output folders. Missing or unreadable ones are dropped from the list. */
    fun loadRecent() {
        loadingRecent = true
        scope.launch {
            val loaded =
                withContext(io) {
                    repo.recentFolders().mapNotNull { dir ->
                        runCatching { repo.open(dir) }.getOrElse {
                            repo.forget(dir)
                            null
                        }
                    }
                }
            recent.clear()
            recent.addAll(loaded)
            tags.clear()
            loaded.forEach { a -> settings.tags(a.outputDir).takeIf { it.isNotEmpty() }?.let { tags[a.outputDir] = it } }
            loadingRecent = false
        }
    }

    /** Shows the native file dialog, then analyzes the chosen APK. */
    fun openApk() {
        pickApk()?.let(::analyze)
    }

    fun openFolder() {
        val dir = pickFolder() ?: return
        scope.launch {
            try {
                show(withContext(io) { repo.open(dir) })
            } catch (e: AnalysisFailure) {
                problem = Problem("Could not open ${dir.name}", e.message ?: "", e.hint)
            }
        }
    }

    /** Opens an APK or an output folder dropped on the window. */
    fun openPath(path: Path) {
        when {
            path.toFile().isDirectory -> {
                scope.launch {
                    try {
                        show(withContext(io) { repo.open(path) })
                    } catch (e: AnalysisFailure) {
                        problem = Problem("Could not open ${path.name}", e.message ?: "", e.hint)
                    }
                }
            }
            path.name.endsWith(".apk", ignoreCase = true) -> analyze(path)
            path.name.substringAfterLast('.').lowercase() in setOf("xapk", "apks", "apkm", "aab") ->
                problem =
                    Problem(
                        "Split APK bundles aren't supported yet",
                        "${path.name} is a bundle of several APKs. Sextant reads one .apk at a time.",
                        "Unzip the bundle and open base.apk (it holds the manifest and most of the code).",
                    )
            else -> problem = Problem("Not an APK", "${path.name} is not an .apk file or a Sextant output folder.", null)
        }
    }

    fun analyze(apk: Path) {
        val job = ExtractionJob(apk, repo.outputDirFor(apk))
        extraction = job
        scope.launch {
            try {
                job.result = withContext(io) { repo.analyze(apk) }
            } catch (e: AnalysisFailure) {
                job.failure = e
            }
        }
    }

    fun reanalyze() {
        val apk = workspace?.analysis?.apkPath ?: return
        analyze(Path.of(apk))
    }

    /** Stops waiting. Core has no cancel hook yet, so the extraction finishes in the background and is not opened. */
    fun cancelExtraction() {
        extraction = null
    }

    fun finishExtraction() {
        val result = extraction?.result ?: return
        extraction = null
        show(result)
    }

    fun dismissFailure() {
        extraction = null
    }

    fun open(analysis: Analysis) {
        show(analysis)
        repo.remember(Path.of(analysis.outputDir))
    }

    private fun show(
        analysis: Analysis,
        destination: Destination = Destination.Overview,
    ) {
        workspace = WorkspaceState(analysis).also { it.destination = destination }
        recent.removeAll { it.outputDir == analysis.outputDir }
        recent.add(0, analysis)
    }

    /** Replaces the tags of one analysis. Blank and duplicate tags are dropped; at most eight are kept. */
    fun setTags(
        dir: String,
        list: List<String>,
    ) {
        val clean =
            list
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(MAX_TAGS)
        if (clean.isEmpty()) tags.remove(dir) else tags[dir] = clean
        settings.saveTags(dir, clean)
    }

    fun revealFolder(dir: String) {
        reveal(Path.of(dir))
    }

    /** Deletes Sextant's output folder for [analysis] and forgets it. The APK is never touched. */
    fun deleteAnalysis(analysis: Analysis) {
        deleting = null
        scope.launch {
            try {
                withContext(io) { repo.delete(Path.of(analysis.outputDir)) }
                setTags(analysis.outputDir, emptyList())
                recent.removeAll { it.outputDir == analysis.outputDir }
                if (workspace?.analysis?.outputDir == analysis.outputDir) workspace = null
            } catch (e: AnalysisFailure) {
                problem = Problem("Could not delete ${analysis.meta.label}", e.message ?: "", e.hint)
            }
        }
    }

    fun revealOutput() {
        workspace?.analysis?.outputDir?.let { reveal(Path.of(it)) }
    }

    fun closeWorkspace() {
        workspace = null
    }

    fun clearRecent() {
        recent.clear()
        repo.clearRecent()
    }
}

private const val MAX_TAGS = 8
