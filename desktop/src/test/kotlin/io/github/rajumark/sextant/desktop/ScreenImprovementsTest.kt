package io.github.rajumark.sextant.desktop

import io.github.rajumark.sextant.desktop.data.CoreRepository
import io.github.rajumark.sextant.desktop.data.Insights
import io.github.rajumark.sextant.desktop.data.MemoryStore
import io.github.rajumark.sextant.desktop.data.renderPng
import io.github.rajumark.sextant.desktop.data.testState
import io.github.rajumark.sextant.desktop.model.Certificate
import io.github.rajumark.sextant.desktop.model.Signing
import io.github.rajumark.sextant.desktop.theme.ThemeMode
import io.github.rajumark.sextant.desktop.ui.screens.NOT_REFERENCED
import io.github.rajumark.sextant.desktop.ui.screens.ResSort
import io.github.rajumark.sextant.desktop.ui.screens.permissionReport
import io.github.rajumark.sextant.desktop.ui.screens.readableName
import io.github.rajumark.sextant.desktop.ui.screens.referenceCounts
import io.github.rajumark.sextant.desktop.ui.screens.signingSummary
import io.github.rajumark.sextant.desktop.ui.screens.signingVerdict
import io.github.rajumark.sextant.desktop.ui.screens.sortResources
import io.github.rajumark.sextant.desktop.ui.screens.summaryMarkdown
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import io.github.rajumark.sextant.desktop.ui.workspace.componentType
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The per-screen improvements (copy reports, readable signing, evidence links), checked on real corpus APKs. */
class ScreenImprovementsTest {
    private val corpus = Path.of(System.getProperty("sextant.corpus") ?: "../fixtures/external")
    private val out = File(System.getProperty("sextant.shots") ?: "build/screenshots", "improvements").apply { mkdirs() }
    private val temps = mutableListOf<Path>()

    @AfterTest
    fun removeTempFolders() {
        temps.forEach { it.toFile().deleteRecursively() }
        temps.clear()
    }

    private fun cert(
        subject: String,
        notAfter: String = "2050-12-31",
    ) = Certificate("aa", "bb", subject, subject, "2020-01-01", notAfter)

    @Test
    fun `distinguished names read as plain names, escaped commas kept`() {
        assertEquals(
            "Signal Messenger, LLC · Mountain View, California, US",
            readableName("CN=Signal Messenger\\, LLC,2.5.4.5=#1307,O=Signal Messenger\\, LLC,L=Mountain View,ST=California,C=US"),
        )
        assertEquals("Android Debug · Android · US", readableName("C=US,O=Android,CN=Android Debug"))
        assertEquals("2.5.4.5=#13", readableName("2.5.4.5=#13"))
    }

    @Test
    fun `signing verdict names debug keys, expiry and scheme gaps`() {
        val today = LocalDate.of(2026, 10, 28)
        val debug = signingVerdict(Signing(true, listOf("v2"), listOf(cert("CN=Android Debug,O=Android,C=US"))), 21, today)
        assertTrue(debug.any { it.first == "Android debug key" })
        assertTrue(debug.any { it.first.startsWith("No v1") })
        val expired = signingVerdict(Signing(true, listOf("v1", "v2"), listOf(cert("CN=Acme", "2025-01-01"))), 26, today)
        assertTrue(expired.any { it.first.startsWith("Certificate expired") })
        assertTrue(expired.any { it.first == "Schemes cover min SDK 26" })
        assertEquals(
            "Unsigned: Android will not install it",
            signingVerdict(Signing(false, emptyList(), emptyList()), 26, today).single().first,
        )
    }

    @Test
    fun `reports and evidence links hold for varied real apps`() {
        val apps = listOf("signal", "fluffychat", "termux", "vlc", "opencamera", "bitwarden").filter { corpus.resolve("$it.apk").exists() }
        if (apps.isEmpty()) return println("skipped: corpus not downloaded")
        for (name in apps) {
            val dir = Files.createTempDirectory("sextant-improvements").also { temps.add(it) }
            val a =
                CoreRepository(
                    MemoryStore(),
                    dir.resolve("fallback"),
                ).analyze(corpus.resolve("$name.apk").copyTo(dir.resolve("$name.apk")))
            val insights = Insights.of(a)

            val summary = summaryMarkdown(a, insights)
            assertTrue(a.meta.packageName in summary, name)
            insights.forEach { assertTrue(it.title in summary, "$name: ${it.title}") }

            val report = permissionReport(a.permissions)
            assertEquals(a.permissions.size + 2, report.trimEnd().lines().size, name)

            val sig = signingSummary(a.meta.signing)
            a.meta.signing.certificates
                .forEach { assertTrue(it.sha256 in sig, name) }
            a.meta.signing.certificates
                .forEach { assertFalse(readableName(it.subject).isBlank(), name) }

            val state = testState(a).apply { themeMode = ThemeMode.Light }
            val ws = state.workspace!!
            // "N exported components without a permission" opens a page whose filter shows components from that set.
            val open =
                a.components.filter {
                    it.exported &&
                        it.permission == null &&
                        it.readPermission == null &&
                        it.writePermission == null &&
                        !it.launcher
                }
            if (open.isNotEmpty()) {
                ws.showOpenComponents()
                val type = ws.destination.componentType!!
                assertTrue(ws.componentPage(type).openOnly, name)
                assertTrue(open.any { it.type == type }, name)
                File(out, "$name-open-components.png").writeBytes(renderPng(frames = 40) { App(state) })
            }
            ws.showApplicationInManifest()
            assertEquals(Destination.Manifest, ws.destination)
            assertEquals("application", a.manifest.children[ws.selectedNode!!.toInt()].tag, name)
            ws.manifestQuery = "intent-filter"
            File(out, "$name-manifest-matches.png").writeBytes(renderPng(frames = 40) { App(state) })
            // Reference counts only come from res and the manifest; every counted ref names a real `@type/name`.
            val refs = referenceCounts(a.resources.refs)
            assertEquals(a.resources.refs.size, refs.values.sum(), name)
            val layouts =
                a.resources.layouts
                    .map { it.name }
                    .distinct()
            val byRefs = sortResources(layouts, ResSort.MostReferenced, { it }, { refs["@layout/$it"] ?: 0 }, null)
            assertEquals(byRefs.map { refs["@layout/$it"] ?: 0 }.sortedDescending(), byRefs.map { refs["@layout/$it"] ?: 0 }, name)
            val assets = sortResources(a.resources.assets, ResSort.Largest, { it.path }, { 0 }, { it.bytes })
            assertEquals(assets.map { it.bytes }.sortedDescending(), assets.map { it.bytes }, name)
            for (d in listOf(Destination.Layouts, Destination.Drawables, Destination.Assets, Destination.Sdks)) {
                ws.resourcePage(d).sort = if (d == Destination.Assets) ResSort.Largest else ResSort.MostReferenced
                ws.destination = d
                File(out, "$name-${d.name.lowercase()}.png").writeBytes(renderPng(frames = 40) { App(state) })
            }
            ws.resourcePage(Destination.Layouts).filters = setOf(NOT_REFERENCED)
            ws.destination = Destination.Layouts
            File(out, "$name-layouts-not-referenced.png").writeBytes(renderPng(frames = 40) { App(state) })
            for (d in listOf(
                Destination.Overview,
                Destination.Permissions,
                Destination.Signing,
                Destination.Application,
                Destination.IconFiles,
                Destination.Output,
            )) {
                ws.destination = d
                File(out, "$name-${d.name.lowercase()}.png").writeBytes(renderPng(frames = 40) { App(state) })
            }
        }
    }
}
