package io.github.rajumark.sextant.desktop

import io.github.rajumark.sextant.desktop.data.CoreRepository
import io.github.rajumark.sextant.desktop.data.MemoryStore
import io.github.rajumark.sextant.desktop.data.renderPng
import io.github.rajumark.sextant.desktop.data.testState
import io.github.rajumark.sextant.desktop.theme.ThemeMode
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Opens every golden output folder of the real-APK corpus (39 apps: Kotlin, Java, Flutter, React Native,
 * native, .NET, …) through the real repository and renders each screen, so odd real-world data cannot crash the UI.
 */
class RealCorpusUiTest {
    private val golden = Path.of(System.getProperty("sextant.corpus") ?: "../fixtures/external").resolveSibling("golden")
    private val outDir = File(System.getProperty("sextant.shots") ?: "build/screenshots", "corpus").apply { mkdirs() }

    /** A few varied apps also get their screenshots kept for review. */
    private val keep = setOf("signal", "fluffychat", "termuxapi", "bitwarden", "duckduckgo")

    @Test
    fun `every corpus app renders on every screen`() {
        val dirs =
            if (golden.isDirectory()) {
                Files.list(golden).use { s ->
                    s.filter { it.isDirectory() }.sorted().toList()
                }
            } else {
                emptyList()
            }
        if (dirs.isEmpty()) {
            println("skipped: no golden folders in $golden")
            return
        }
        val repo = CoreRepository(MemoryStore(), Files.createTempDirectory("sextant-fallback"))
        val failures = mutableListOf<String>()
        val openMillis = linkedMapOf<String, Long>()
        for (dir in dirs) {
            val started = System.nanoTime()
            val analysis =
                runCatching { repo.open(dir) }.getOrElse {
                    failures += "${dir.name}: open failed: ${it.message}"
                    null
                } ?: continue
            openMillis[dir.name] = (System.nanoTime() - started) / 1_000_000
            for (dest in Destination.entries) {
                runCatching {
                    val state = testState(analysis).apply { themeMode = ThemeMode.Light }
                    state.workspace!!.destination = dest
                    // Select the first item so the detail panes render real data too.
                    state.workspace!!.selectedPermission = analysis.permissions.firstOrNull()?.name
                    state.workspace!!.selectedComponent =
                        analysis.deepLinkComponents.firstOrNull()?.name ?: analysis.components.firstOrNull()?.name
                    state.workspace!!.selectedNode = "0"
                    val png = renderPng(frames = 20, frameNanos = 50_000_000L) { App(state) }
                    if (dir.name in keep) File(outDir, "${dir.name}-${dest.name.lowercase()}.png").writeBytes(png)
                }.onFailure { failures += "${dir.name}/${dest.name}: ${it::class.simpleName}: ${it.message}" }
            }
        }
        // Opening a folder should feel instant (checklist PF-03: < 1 s to first view).
        val slowest = openMillis.entries.sortedByDescending { it.value }.take(5)
        println("open() slowest: " + slowest.joinToString { "${it.key} ${it.value} ms" })
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
