package io.github.rajumark.sextant.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.data.FakeRepository
import io.github.rajumark.sextant.desktop.data.SampleData
import io.github.rajumark.sextant.desktop.data.testState
import io.github.rajumark.sextant.desktop.theme.ThemeMode
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import kotlinx.coroutines.Dispatchers
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class UiFlowTest {
    private val northwind = SampleData.recent.first { it.meta.label == "Northwind Notes" }

    @Test
    fun `home lists recent analyses and opens one`() =
        runComposeUiTest {
            val state = testState().also { it.loadRecent() }
            // The app's minimum window (1024×700) is what the Overview must fit in.
            setContent { Box(Modifier.size(1024.dp, 700.dp)) { App(state) } }
            onNodeWithText("Drop an APK here").assertIsDisplayed()
            onNodeWithText("Orbit Chat").performClick()
            waitForIdle()
            assertEquals(
                "Orbit Chat",
                state.workspace
                    ?.analysis
                    ?.meta
                    ?.label,
            )
            assertEquals(Destination.Overview, state.workspace?.destination)
            onNodeWithText("Worth a look").assertIsDisplayed()
        }

    @Test
    fun `rail switches destinations`() =
        runComposeUiTest {
            val state = testState(northwind)
            setContent { App(state) }
            Destination.entries.drop(1).forEach { d ->
                // The menu scrolls when it is taller than the window (resource pages): bring the item into view first.
                onAllNodesWithText(d.label)[0].performScrollTo().performClick()
                waitForIdle()
                assertEquals(d, state.workspace?.destination)
            }
        }

    @Test
    fun `permission filter and search narrow the list`() =
        runComposeUiTest {
            val state = testState(northwind)
            state.workspace!!.destination = Destination.Permissions
            setContent { App(state) }
            onNodeWithText("Dangerous", substring = true).performClick()
            waitForIdle()
            onNodeWithText("android.permission.CAMERA").assertIsDisplayed()
            assertEquals(0, onAllNodesWithText("android.permission.INTERNET").fetchSemanticsNodes().size)

            onNodeWithText("Search permissions").performTextInput("camera")
            waitForIdle()
            onNodeWithText("android.permission.CAMERA").performClick()
            waitForIdle()
            assertEquals("uses-permission|android.permission.CAMERA", state.workspace?.selectedPermission)
            onNodeWithText("Use the camera.").assertIsDisplayed()
        }

    @Test
    fun `component detail explains default export`() =
        runComposeUiTest {
            val state = testState(northwind)
            setContent { App(state) }
            state.workspace!!.showComponent("com.northwind.notes.deeplink.DeepLinkActivity")
            waitForIdle()
            onNodeWithText("Exported by default").assertIsDisplayed()
            assertTrue(onAllNodesWithText("https://notes.northwind.example/open*").fetchSemanticsNodes().isNotEmpty())
        }

    @Test
    fun `manifest switches to XML view`() =
        runComposeUiTest {
            val state = testState(northwind)
            state.workspace!!.destination = Destination.Manifest
            setContent { App(state) }
            onNodeWithText("XML").performClick()
            waitForIdle()
            onNode(hasText("<manifest", substring = true)).assertIsDisplayed()
        }

    @Test
    fun `choosing an APK runs the extraction and opens the result`() =
        runComposeUiTest {
            val hold = CountDownLatch(1)
            val apk = Path.of("/samples/${northwind.meta.file.name}")
            val state = testState(repo = FakeRepository(hold = hold), io = Dispatchers.IO, pickApk = { apk })
            setContent { App(state) }
            onNodeWithText("Choose APK…").performClick()
            waitForIdle()
            assertNotNull(state.extraction)
            onNodeWithText("Analyzing ${apk.fileName}").assertIsDisplayed()
            hold.countDown()
            waitUntil(timeoutMillis = 10_000) { state.workspace != null }
            assertNull(state.extraction)
            assertEquals(
                "Northwind Notes",
                state.workspace
                    ?.analysis
                    ?.meta
                    ?.label,
            )
        }

    @Test
    fun `dropping a non-APK file explains the problem`() =
        runComposeUiTest {
            val state = testState()
            setContent { App(state) }
            state.openPath(Path.of("/tmp/notes.txt"))
            waitForIdle()
            onNodeWithText("Not an APK").assertIsDisplayed()
        }

    @Test
    fun `dropping a split bundle says how to proceed`() =
        runComposeUiTest {
            val state = testState()
            setContent { App(state) }
            state.openPath(Path.of("/tmp/app.xapk"))
            waitForIdle()
            onNodeWithText("Split APK bundles aren't supported yet").assertIsDisplayed()
        }

    @Test
    fun `settings dialog toggles theme`() =
        runComposeUiTest {
            val state = testState()
            setContent { App(state) }
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            onNodeWithText("Dark").performClick()
            waitForIdle()
            assertEquals(ThemeMode.Dark, state.themeMode)
        }
}
