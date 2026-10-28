package io.github.rajumark.sextant.desktop.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.AppState
import io.github.rajumark.sextant.desktop.model.Analysis
import io.github.rajumark.sextant.desktop.ui.common.AppIcon

/**
 * The left sidebar for an open analysis: an app card at the top (icon, name, version) with a dropdown that goes back to
 * the drop APK screen; the analysis screens below; Settings at the bottom.
 */
@Composable
fun Sidebar(app: AppState) {
    val ws = app.workspace ?: return
    val analysis = ws.analysis

    fun countFor(dest: io.github.rajumark.sextant.desktop.ui.workspace.Destination): Int? =
        when (dest) {
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Activities ->
                analysis.countOf(io.github.rajumark.sextant.desktop.model.ComponentType.Activity)
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Services ->
                analysis.countOf(io.github.rajumark.sextant.desktop.model.ComponentType.Service)
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Receivers ->
                analysis.countOf(io.github.rajumark.sextant.desktop.model.ComponentType.Receiver)
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Providers ->
                analysis.countOf(io.github.rajumark.sextant.desktop.model.ComponentType.Provider)
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Aliases ->
                analysis.countOf(io.github.rajumark.sextant.desktop.model.ComponentType.ActivityAlias)
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Permissions ->
                analysis.permissions.size
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Sdks -> analysis.sdks?.entries?.count { it.kind == "sdk" }
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Strings -> analysis.stringKeys.size
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Layouts ->
                analysis.resources.layouts
                    .distinctBy { it.name }
                    .size
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Menus ->
                analysis.resources.menus
                    .distinctBy { it.name }
                    .size
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Navigation -> analysis.resources.navigation.size
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Styles ->
                analysis.resources.styles
                    .distinctBy { it.name }
                    .size
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Drawables ->
                analysis.resources.drawables
                    .distinctBy {
                        it.type +
                            it.name
                    }.size
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.ResourceIds -> analysis.resources.ids.size
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Assets -> analysis.resources.assets.size
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.XmlConfigs ->
                analysis.resources.xml
                    .distinctBy { it.name }
                    .size
            else -> null
        }

    fun exportedFor(dest: io.github.rajumark.sextant.desktop.ui.workspace.Destination): Int? =
        when (dest) {
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Activities ->
                analysis.components.count { it.type == io.github.rajumark.sextant.desktop.model.ComponentType.Activity && it.exported }
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Services ->
                analysis.components.count { it.type == io.github.rajumark.sextant.desktop.model.ComponentType.Service && it.exported }
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Receivers ->
                analysis.components.count { it.type == io.github.rajumark.sextant.desktop.model.ComponentType.Receiver && it.exported }
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Providers ->
                analysis.components.count { it.type == io.github.rajumark.sextant.desktop.model.ComponentType.Provider && it.exported }
            io.github.rajumark.sextant.desktop.ui.workspace.Destination.Aliases ->
                analysis.components.count { it.type == io.github.rajumark.sextant.desktop.model.ComponentType.ActivityAlias && it.exported }
            else -> null
        }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.width(248.dp).fillMaxHeight(),
    ) {
        Column(Modifier.fillMaxHeight().padding(horizontal = 12.dp, vertical = 16.dp)) {
            AppCard(analysis, onOpenDropScreen = app::closeWorkspace)
            Spacer(Modifier.height(16.dp))
            // The menu is a pane: with the Resources group it can be taller than the window, so only it scrolls.
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                SidebarMenu.forEachIndexed { index, group ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    group.forEach { d ->
                        val c = countFor(d)
                        val e = exportedFor(d)
                        NavigationDrawerItem(
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(d.label, modifier = Modifier.weight(1f))
                                    if (c != null && e != null && e > 0) {
                                        Text("$e/$c", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    } else if (c != null) {
                                        Text(
                                            "$c",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            selected = ws.destination == d,
                            onClick = { ws.destination = d },
                            icon = { Icon(d.icon, null) },
                        )
                    }
                }
            }
            NavigationDrawerItem(
                label = { Text("Settings") },
                selected = false,
                onClick = { app.showSettings = true },
                icon = { Icon(Icons.Outlined.Settings, null) },
            )
        }
    }
}

/** One row: app icon, name and version, and a dropdown arrow. The whole card opens the drop APK screen. */
@Composable
private fun AppCard(
    analysis: Analysis,
    onOpenDropScreen: () -> Unit,
) {
    Surface(
        onClick = onOpenDropScreen,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(44.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppIcon(analysis, 36.dp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(analysis.meta.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    analysis.meta.versionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Icon(Icons.Outlined.ExpandMore, "Open another APK", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
