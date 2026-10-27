package io.github.rajumark.sextant.desktop

import androidx.compose.runtime.Composable
import io.github.rajumark.sextant.desktop.data.AnalysisFailure
import io.github.rajumark.sextant.desktop.data.FakeRepository
import io.github.rajumark.sextant.desktop.data.SampleData
import io.github.rajumark.sextant.desktop.data.renderPng
import io.github.rajumark.sextant.desktop.data.testState
import io.github.rajumark.sextant.desktop.theme.ThemeMode
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import io.github.rajumark.sextant.desktop.ui.workspace.ManifestView
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders every screen off-screen to PNG so the design can be reviewed without a display.
 * Output goes to $SEXTANT_SHOTS_DIR (or build/screenshots).
 */
class ScreenshotTest {
    private val outDir = File(System.getProperty("sextant.shots") ?: "build/screenshots").apply { mkdirs() }
    private val northwind = SampleData.recent.first { it.meta.label == "Northwind Notes" }

    private fun shot(
        name: String,
        width: Int = 1360,
        height: Int = 880,
        content: @Composable () -> Unit,
    ) {
        val png = renderPng(width, height, frames = 90, content = content)
        File(outDir, "$name.png").writeBytes(png)
        assertTrue(png.size > 10_000, "$name rendered")
    }

    private fun workspace(
        dest: Destination,
        mode: ThemeMode = ThemeMode.Light,
        setup: (AppState) -> Unit = {},
    ): AppState =
        testState(northwind).also {
            it.themeMode = mode
            it.workspace!!.destination = dest
            setup(it)
        }

    @Test
    fun home() {
        shot("01-home-light") {
            App(
                testState().apply {
                    themeMode = ThemeMode.Light
                    loadRecent()
                },
            )
        }
        shot("02-home-dark") {
            App(
                testState().apply {
                    themeMode = ThemeMode.Dark
                    loadRecent()
                },
            )
        }
    }

    @Test
    fun overview() {
        shot("03-overview-light") { App(workspace(Destination.Overview)) }
        shot("04-overview-dark") { App(workspace(Destination.Overview, ThemeMode.Dark)) }
        val lumen = SampleData.recent.first { it.meta.label == "Lumen Camera" }
        shot("05-overview-risky-app") { App(testState(lumen).apply { themeMode = ThemeMode.Light }) }
    }

    @Test
    fun permissions() {
        shot("06-permissions") {
            App(workspace(Destination.Permissions) { it.workspace!!.selectedPermission = "android.permission.CAMERA" })
        }
        shot("07-permissions-declared-dark") {
            App(
                workspace(Destination.Permissions, ThemeMode.Dark) {
                    it.workspace!!.selectedPermission = "com.northwind.notes.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
                },
            )
        }
    }

    @Test
    fun components() {
        shot("08-components") {
            App(workspace(Destination.Components) { it.workspace!!.selectedComponent = "com.northwind.notes.deeplink.DeepLinkActivity" })
        }
        shot("09-components-narrow", width = 980, height = 760) {
            App(workspace(Destination.Components) { it.workspace!!.selectedComponent = "com.northwind.notes.ui.LaunchActivity" })
        }
    }

    @Test
    fun manifest() {
        shot("10-manifest-tree") {
            App(workspace(Destination.Manifest) { it.workspace!!.selectedNode = "0" })
        }
        shot("11-manifest-xml-dark") {
            App(workspace(Destination.Manifest, ThemeMode.Dark) { it.workspace!!.manifestView = ManifestView.Xml })
        }
    }

    @Test
    fun signingAndOutput() {
        shot("12-signing") { App(workspace(Destination.Signing)) }
        shot("13-output") { App(workspace(Destination.Output)) }
        shot(
            "14-output-jsonl-dark",
        ) { App(workspace(Destination.Output, ThemeMode.Dark) { it.workspace!!.selectedFile = "components.jsonl" }) }
    }

    @Test
    fun dialogs() {
        val apk = Path.of("/samples/orbit-chat-7.0.4.apk")
        val running = CountDownLatch(1)
        shot("15-extraction-running") {
            App(
                testState(repo = FakeRepository(hold = running), io = Dispatchers.IO).apply {
                    themeMode = ThemeMode.Light
                    analyze(apk)
                },
            )
        }
        running.countDown()
        val failing = FakeRepository(failWith = AnalysisFailure("not a zip file: broken.apk", "Pass an .apk file", null))
        shot("17-extraction-failed") {
            App(
                testState(repo = failing).apply {
                    themeMode = ThemeMode.Light
                    analyze(apk)
                },
            )
        }
        shot("16-settings") {
            App(
                testState().apply {
                    themeMode = ThemeMode.Light
                    showSettings = true
                },
            )
        }
    }
}
