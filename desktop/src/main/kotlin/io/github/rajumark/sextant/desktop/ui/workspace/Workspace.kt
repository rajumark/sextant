package io.github.rajumark.sextant.desktop.ui.workspace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.AppState
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.ui.screens.ComponentsScreen
import io.github.rajumark.sextant.desktop.ui.screens.IconsScreen
import io.github.rajumark.sextant.desktop.ui.screens.ManifestScreen
import io.github.rajumark.sextant.desktop.ui.screens.OutputScreen
import io.github.rajumark.sextant.desktop.ui.screens.OverviewScreen
import io.github.rajumark.sextant.desktop.ui.screens.PermissionsScreen
import io.github.rajumark.sextant.desktop.ui.screens.SigningScreen
import io.github.rajumark.sextant.desktop.ui.screens.TypedComponentScreen

@Composable
fun Workspace(
    app: AppState,
    ws: WorkspaceState,
) {
    // Motion comes from the theme's expressive scheme: springs for movement, effects springs for opacity.
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fastEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    // No page top bar: each screen carries its own title, and the actions live in the menu bar.
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.weight(1f).padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                AnimatedContent(
                    targetState = ws.destination,
                    transitionSpec = {
                        (fadeIn(effects) + slideInVertically(spatial) { it / 24 }).togetherWith(fadeOut(fastEffects))
                    },
                    label = "destination",
                ) { dest ->
                    Box(Modifier.fillMaxSize()) {
                        when (dest) {
                            Destination.Overview -> OverviewScreen(ws)
                            Destination.Permissions -> PermissionsScreen(ws)
                            Destination.Components -> ComponentsScreen(ws)
                            Destination.Manifest -> ManifestScreen(ws)
                            Destination.Signing -> SigningScreen(ws)
                            Destination.Output -> OutputScreen(ws, app::revealOutput)
                            Destination.IconFiles -> IconsScreen(ws)
                            Destination.Activities -> TypedComponentScreen(ws, ComponentType.Activity, "Activities")
                            Destination.Services -> TypedComponentScreen(ws, ComponentType.Service, "Services")
                            Destination.Receivers -> TypedComponentScreen(ws, ComponentType.Receiver, "Receivers")
                            Destination.Aliases -> TypedComponentScreen(ws, ComponentType.ActivityAlias, "Aliases")
                        }
                    }
                }
            }
        }
    }
}
