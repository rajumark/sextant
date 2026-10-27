package io.github.rajumark.sextant.desktop

import io.github.rajumark.sextant.desktop.data.CoreRepository
import io.github.rajumark.sextant.desktop.data.MemoryStore
import io.github.rajumark.sextant.desktop.data.renderPng
import io.github.rajumark.sextant.desktop.data.testState
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The per-type component pages, rendered for a real APK. Corpus APKs are used when downloaded. */
class ComponentPagesTest {
    private val corpus = Path.of(System.getProperty("sextant.corpus") ?: "../fixtures/external")
    private val temps = mutableListOf<Path>()

    @AfterTest
    fun removeTempFolders() {
        temps.forEach { it.toFile().deleteRecursively() }
        temps.clear()
    }

    @Test
    fun `activities, services, receivers and aliases pages render`() {
        val source = corpus.resolve("signal.apk")
        if (!source.exists()) return println("skipped: signal.apk missing")
        val dir = Files.createTempDirectory("sextant-pages-test").also { temps.add(it) }
        val apk = source.copyTo(dir.resolve("signal.apk"))
        val a = CoreRepository(MemoryStore(), dir.resolve("fallback")).analyze(apk)
        val out = File(System.getProperty("sextant.shots") ?: "build/screenshots").apply { mkdirs() }
        assertEquals(a.components.count { it.type == ComponentType.Activity }, a.countOf(ComponentType.Activity))
        val pages = listOf(Destination.Activities, Destination.Services, Destination.Receivers, Destination.Aliases)
        for (page in pages) {
            val state =
                testState(a).apply {
                    themeMode = io.github.rajumark.sextant.desktop.theme.ThemeMode.Light
                    workspace!!.destination = page
                }
            File(out, "page-${page.name.lowercase()}.png").writeBytes(renderPng(frames = 60) { App(state) })
        }
    }
}
