package io.github.rajumark.sextant.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.theme.SextantTheme
import io.github.rajumark.sextant.desktop.ui.common.IconBadge
import io.github.rajumark.sextant.desktop.ui.common.LocalNotifier
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.home.DeleteDialog
import io.github.rajumark.sextant.desktop.ui.home.ExtractionDialog
import io.github.rajumark.sextant.desktop.ui.home.HomeScreen
import io.github.rajumark.sextant.desktop.ui.home.ProblemDialog
import io.github.rajumark.sextant.desktop.ui.home.SettingsDialog
import io.github.rajumark.sextant.desktop.ui.home.TagDialog
import io.github.rajumark.sextant.desktop.ui.nav.Sidebar
import io.github.rajumark.sextant.desktop.ui.workspace.Workspace
import kotlinx.coroutines.launch
import java.net.URI
import java.nio.file.Path

@Composable
fun App(state: AppState) {
    SextantTheme(state.themeMode, state.seed) {
        // Motion comes from the theme's expressive scheme: springs for movement, effects springs for opacity.
        val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val fastEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val notify: (String) -> Unit = { msg ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(msg)
            }
        }
        CompositionLocalProvider(LocalNotifier provides notify) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxSize()) {
                    // The sidebar is for an open analysis; the drop screen stands on its own.
                    if (state.workspace != null) Sidebar(state)
                    DropArea(onDrop = state::openPath, modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AnimatedContent(
                            targetState = state.workspace,
                            transitionSpec = { fadeIn(effects).togetherWith(fadeOut(fastEffects)) },
                            label = "root",
                        ) { ws ->
                            if (ws == null) HomeScreen(state) else Workspace(state, ws)
                        }
                        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
                    }
                }
                state.extraction?.let { ExtractionDialog(state, it) }
                state.problem?.let { ProblemDialog(it) { state.problem = null } }
                if (state.showSettings) SettingsDialog(state)
                state.tagging?.let { TagDialog(state, it) }
                state.deleting?.let { DeleteDialog(state, it) }
            }
        }
    }
}

/** Accepts an APK or an output folder dropped anywhere on the window. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun DropArea(
    onDrop: (Path) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    var hovering by remember { mutableStateOf(false) }
    val target =
        remember(onDrop) {
            object : DragAndDropTarget {
                override fun onEntered(event: DragAndDropEvent) {
                    hovering = true
                }

                override fun onExited(event: DragAndDropEvent) {
                    hovering = false
                }

                override fun onEnded(event: DragAndDropEvent) {
                    hovering = false
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    hovering = false
                    val files = event.dragData() as? DragData.FilesList ?: return false
                    val first = files.readFiles().firstOrNull() ?: return false
                    onDrop(Path.of(URI(first)))
                    return true
                }
            }
        }
    Box(
        modifier.dragAndDropTarget(shouldStartDragAndDrop = { true }, target = target),
    ) {
        content()
        AnimatedVisibility(hovering, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge),
                contentAlignment = Alignment.Center,
            ) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraLarge) {
                    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconBadge(Icons.Outlined.UploadFile, Tone.Primary, 64.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Drop to analyze", style = MaterialTheme.typography.headlineSmall)
                        Text("An .apk file, or a .sextant output folder", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
