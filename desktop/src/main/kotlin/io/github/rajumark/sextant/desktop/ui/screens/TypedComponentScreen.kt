package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.ui.common.EmptyState
import io.github.rajumark.sextant.desktop.ui.common.IconBadge
import io.github.rajumark.sextant.desktop.ui.common.ListDetail
import io.github.rajumark.sextant.desktop.ui.common.MonoLine
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState

/**
 * One kind of manifest component (activities, services, receivers, aliases) as its own page: a searchable list on the
 * left and the component's details on the right, in the same layout as the Permissions page.
 */
@Composable
fun TypedComponentScreen(
    ws: WorkspaceState,
    type: ComponentType,
    title: String,
) {
    val all = remember(ws.analysis) { ws.analysis.components.filter { it.type == type } }
    val query = ws.componentQuery.trim()
    val filtered =
        remember(all, query) {
            all.filter { it.name.contains(query, ignoreCase = true) || it.deepLinks.any { l -> l.contains(query, ignoreCase = true) } }
        }
    val selected = all.firstOrNull { it.name == ws.selectedComponent }
    val exported = all.count { it.exported }

    ListDetail(
        hasSelection = selected != null,
        onBack = { ws.selectedComponent = null },
        initialDetailWidth = 500.dp,
        minListWidth = 560.dp,
        list = {
            Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
                ListHeader(title, "${all.size} declared · $exported exported")
                Spacer(Modifier.height(16.dp))
                SearchField(ws.componentQuery, { ws.componentQuery = it }, "Search by class name or deep link", Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                if (filtered.isEmpty()) {
                    EmptyState(
                        Icons.Outlined.SearchOff,
                        if (all.isEmpty()) "No ${title.lowercase()} declared" else "No ${title.lowercase()} match",
                        if (all.isEmpty()) "This APK's manifest declares none." else "Clear the search to see them all.",
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(withUniqueKeys(filtered) { it.name }, key = { it.first }) { (_, c) ->
                            SelectableRow(
                                selected = c == selected,
                                onClick = { ws.selectedComponent = c.name },
                                headline = { Text(c.simpleName, style = MaterialTheme.typography.bodyLarge, maxLines = 1) },
                                supporting = { MonoLine(c.deepLinks.firstOrNull() ?: c.packagePath) },
                                leading = { IconBadge(c.type.icon(), c.type.tone()) },
                                trailing = { ComponentTags(c) },
                            )
                        }
                    }
                }
            }
        },
        detail = {
            if (selected != null) {
                ComponentDetail(selected)
            } else {
                EmptyState(
                    Icons.Outlined.TouchApp,
                    "Select a ${title.lowercase().removeSuffix("s")}",
                    "Its exported state, intent filters and deep links appear here.",
                )
            }
        },
    )
}
