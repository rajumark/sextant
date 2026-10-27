package io.github.rajumark.sextant.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.rajumark.sextant.desktop.data.CoreRepository
import io.github.rajumark.sextant.desktop.theme.ThemeMode
import io.github.rajumark.sextant.desktop.ui.common.SextantIcon
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import java.awt.Dimension
import java.awt.Frame

private fun shortcut(
    key: Key,
    shift: Boolean = false,
) = if (isMac) KeyShortcut(key, meta = true, shift = shift) else KeyShortcut(key, ctrl = true, shift = shift)

fun main() {
    // Name shown in the macOS menu bar; must be set before AWT starts.
    System.setProperty("apple.awt.application.name", "Sextant")
    // Dock/taskbar icon when running from Gradle; packaged installers carry the .icns/.ico/.png instead.
    runCatching {
        val taskbar = java.awt.Taskbar.getTaskbar()
        if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) SextantIcon.awtImage()?.let { taskbar.iconImage = it }
    }
    runApp()
}

private fun runApp() =
    application {
        val scope = rememberCoroutineScope()
        // The native dialogs need the window as their owner; it exists only once Window composes.
        val owner = remember { arrayOfNulls<Frame>(1) }
        val state =
            remember {
                AppState(
                    repo = CoreRepository(),
                    scope = scope,
                    pickApk = { chooseApkFile(owner[0]) },
                    pickFolder = { chooseOutputFolder(owner[0]) },
                    reveal = ::revealInFileManager,
                )
            }
        LaunchedEffect(Unit) { state.loadRecent() }
        val windowState = rememberWindowState(width = 1360.dp, height = 880.dp, position = WindowPosition.Aligned(Alignment.Center))
        val title =
            state.workspace
                ?.analysis
                ?.meta
                ?.label
                ?.let { "Sextant — $it" } ?: "Sextant"
        Window(onCloseRequest = ::exitApplication, state = windowState, title = title, icon = SextantIcon.windowPainter) {
            owner[0] = window
            window.minimumSize = Dimension(960, 640)
            MenuBar {
                Menu("File") {
                    Item("Open APK…", shortcut = shortcut(Key.O), onClick = state::openApk)
                    Item("Open output folder…", shortcut = shortcut(Key.O, shift = true), onClick = state::openFolder)
                    Item(
                        "Analyze again",
                        shortcut = shortcut(Key.R),
                        enabled = state.workspace?.analysis?.apkPath != null,
                        onClick = state::reanalyze,
                    )
                    Item("Reveal output folder", enabled = state.workspace != null, onClick = state::revealOutput)
                    Separator()
                    Item("Settings…", shortcut = shortcut(Key.Comma), onClick = { state.showSettings = true })
                    Separator()
                    Item("Close analysis", shortcut = shortcut(Key.W), enabled = state.workspace != null, onClick = state::closeWorkspace)
                }
                Menu("View") {
                    val keys = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven)
                    Destination.entries.forEachIndexed { i, d ->
                        Item(d.label, shortcut = keys.getOrNull(i)?.let { shortcut(it) }, enabled = state.workspace != null) {
                            state.workspace?.destination = d
                        }
                    }
                    Separator()
                    Menu("Theme") {
                        ThemeMode.entries.forEach { mode ->
                            RadioButtonItem(mode.name, selected = state.themeMode == mode) { state.themeMode = mode }
                        }
                    }
                }
            }
            App(state)
        }
    }
