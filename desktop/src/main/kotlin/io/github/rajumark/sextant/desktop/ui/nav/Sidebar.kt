package io.github.rajumark.sextant.desktop.ui.nav

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.AppState
import io.github.rajumark.sextant.desktop.model.Analysis
import io.github.rajumark.sextant.desktop.ui.common.AppIcon
import io.github.rajumark.sextant.desktop.ui.workspace.Destination

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
            // The menu is a pane: compact rows and folded sections make it fit, but on a short window only it scrolls.
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                SidebarMenu.forEach { section ->
                    val title = section.title
                    // The section holding the open page never folds away, or the user would lose their place.
                    val open = title == null || title !in app.foldedMenuSections || ws.destination in section.items
                    if (title != null) {
                        SectionHeader(
                            title = title,
                            open = open,
                            canFold = ws.destination !in section.items,
                            onToggle = { app.toggleMenuSection(title) },
                        )
                    }
                    if (open) {
                        section.items.forEach { d ->
                            MenuRow(d, countFor(d), exportedFor(d), selected = ws.destination == d) { ws.destination = d }
                        }
                    }
                }
            }
            NavigationDrawerItem(
                label = { Text("Settings") },
                selected = false,
                onClick = { app.showSettings = true },
                icon = { Icon(Icons.Outlined.Settings, null, Modifier.size(20.dp)) },
                modifier = Modifier.height(40.dp),
            )
        }
    }
}

/** A section title; clicking it folds or opens the section. A section holding the open page can't fold. */
@Composable
private fun SectionHeader(
    title: String,
    open: Boolean,
    canFold: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = canFold, onClick = onToggle)
            .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (canFold) {
            Icon(
                if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                if (open) "Fold $title" else "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * One page in the menu, 40dp high so the whole menu fits a normal window. The count is on the right: red
 * "exported/total" when some components are exported. Pages with nothing in them are dimmed but still open.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuRow(
    d: Destination,
    count: Int?,
    exported: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val empty = count == 0
    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    NavigationDrawerItem(
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    d.label,
                    modifier = Modifier.weight(1f),
                    color = if (empty && !selected) dim else Color.Unspecified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (count != null && exported != null && exported > 0) {
                    TooltipArea(tooltip = { PlainTooltipBox("$exported exported of $count") }) {
                        Text("$exported/$count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                } else if (count != null) {
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (empty) dim else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        selected = selected,
        onClick = onClick,
        icon = { Icon(d.icon, null, Modifier.size(20.dp), tint = if (empty && !selected) dim else LocalContentColor.current) },
        modifier = Modifier.height(40.dp),
    )
}

@Composable
private fun PlainTooltipBox(text: String) {
    Surface(color = MaterialTheme.colorScheme.inverseSurface, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
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
