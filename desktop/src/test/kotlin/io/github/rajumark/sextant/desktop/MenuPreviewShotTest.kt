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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** The Menus page with one real menu of each host kind selected: app bar, bottom navigation and navigation drawer. */
class MenuPreviewShotTest {
    private val corpus = Path.of(System.getProperty("sextant.corpus") ?: "../fixtures/external")
    private val outDir = File(System.getProperty("sextant.shots") ?: "build/screenshots", "menus").apply { mkdirs() }
    private val temps = mutableListOf<Path>()

    @AfterTest
    fun removeTempFolders() = temps.forEach { it.toFile().deleteRecursively() }

    @Test
    fun `menu previews render for app bar, bottom navigation and drawer menus`() {
        val cases =
            listOf(
                Triple("binaryeye", "activity_camera", ThemeMode.Light),
                Triple("anki", "bottom_nav_menu", ThemeMode.Light),
                Triple("anki", "reviewer", ThemeMode.Dark),
                Triple("nextcloud", "partial_drawer_entries", ThemeMode.Light),
            )
        if (cases.any { !Files.exists(corpus.resolve("${it.first}.apk")) }) return println("skipped: corpus APKs missing")
        val repo = CoreRepository(MemoryStore(), Files.createTempDirectory("sextant-menus").also { temps.add(it) })
        val analyses = cases.map { it.first }.distinct().associateWith { repo.analyze(corpus.resolve("$it.apk")) }
        for ((app, menu, mode) in cases) {
            val analysis = analyses.getValue(app)
            assertTrue(analysis.resources.menus.any { it.name == menu }, "$app has menu $menu")
            val state = testState(analysis).apply { themeMode = mode }
            state.workspace!!.destination = Destination.Menus
            state.workspace!!.resourcePage(Destination.Menus).selected = menu
            val png = renderPng(1360, 880, frames = 20, frameNanos = 50_000_000L) { App(state) }
            File(outDir, "$app-$menu.png").writeBytes(png)
            assertTrue(png.size > 10_000, "$app/$menu rendered")
        }
    }
}
