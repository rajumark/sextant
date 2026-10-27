package io.github.rajumark.sextant.desktop

import io.github.rajumark.sextant.desktop.data.AppIconImage
import io.github.rajumark.sextant.desktop.data.CoreRepository
import io.github.rajumark.sextant.desktop.data.MemoryStore
import io.github.rajumark.sextant.desktop.data.renderPng
import io.github.rajumark.sextant.desktop.data.testState
import io.github.rajumark.sextant.desktop.theme.ThemeMode
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Icons come from the real engine. Corpus APKs are used when they are downloaded; otherwise the test is skipped. */
class AppIconTest {
    private val corpus = Path.of(System.getProperty("sextant.corpus") ?: "../fixtures/external")

    /** Each analysis copies its APK and writes an index; these folders are large, so they are removed after each test. */
    private val temps = mutableListOf<Path>()

    @AfterTest
    fun removeTempFolders() {
        temps.forEach { it.toFile().deleteRecursively() }
        temps.clear()
    }

    private fun analyze(id: String): io.github.rajumark.sextant.desktop.model.Analysis? {
        val source = corpus.resolve("$id.apk")
        if (!source.exists()) return null
        val dir = Files.createTempDirectory("sextant-icon-test").also { temps.add(it) }
        val apk = source.copyTo(dir.resolve("$id.apk"))
        return CoreRepository(MemoryStore(), dir.resolve("fallback")).analyze(apk)
    }

    @Test
    fun `a PNG launcher icon is picked and decodes`() {
        val a = analyze("signal") ?: return println("skipped: signal.apk missing")
        val pick = assertNotNull(a.meta.icon, "signal has a drawable icon")
        assertEquals("png", pick.format)
        assertNotNull(AppIconImage.load(Path.of(a.apkPath!!), pick))
    }

    @Test
    fun `a vector-only icon uses its adaptive foreground when that is a bitmap, or nothing`() {
        val a = analyze("duckduckgo") ?: return println("skipped: duckduckgo.apk missing")
        val pick = a.meta.icon
        if (pick != null) {
            assertEquals(true, pick.drawable)
            assertNotNull(AppIconImage.load(Path.of(a.apkPath!!), pick))
        } else {
            assertNull(pick)
        }
    }

    /** A real analyzed APK, so the icon is visible in review (the golden-folder screenshots have no APK). */
    @Test
    fun `overview of an analyzed APK shows its icon`() {
        val a = analyze("signal") ?: return println("skipped: signal.apk missing")
        val dir = File(System.getProperty("sextant.shots") ?: "build/screenshots").apply { mkdirs() }
        val png = renderPng(frames = 120) { App(testState(a).apply { themeMode = ThemeMode.Light }) }
        File(dir, "icon-signal-overview.png").writeBytes(png)
        assertNotNull(a.meta.icon)
    }

    /** The Icons page: every icon file, by role, rendered for a real APK. */
    @Test
    fun `icons page lists every icon file and renders it`() {
        val a = analyze("signal") ?: return println("skipped: signal.apk missing")
        assertNotNull(a.meta.iconGroups.firstOrNull { it.title == "Launcher icon" }, "launcher group present")
        val dir = File(System.getProperty("sextant.shots") ?: "build/screenshots").apply { mkdirs() }
        val state =
            testState(a).apply {
                themeMode = ThemeMode.Light
                workspace!!.destination = io.github.rajumark.sextant.desktop.ui.workspace.Destination.IconFiles
            }
        File(dir, "icons-signal.png").writeBytes(renderPng(frames = 120) { App(state) })
        // Select the first raster icon: the list and the details shown side by side, then the narrow one-pane view.
        val first =
            a.meta.iconGroups
                .flatMap { g -> g.files.map { g.title to it } }
                .first { it.second.drawable }
        state.workspace!!.selectedIcon = "${first.first}|${first.second.path}"
        File(dir, "icons-signal-selected.png").writeBytes(renderPng(frames = 120) { App(state) })
        File(dir, "icons-signal-selected-narrow.png").writeBytes(renderPng(width = 800, height = 700, frames = 120) { App(state) })
    }
}
