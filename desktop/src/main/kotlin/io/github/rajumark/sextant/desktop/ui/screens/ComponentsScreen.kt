package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.model.AppComponent
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.model.ExportedSource
import io.github.rajumark.sextant.desktop.model.IntentFilterSpec
import io.github.rajumark.sextant.desktop.theme.MonoFamily
import io.github.rajumark.sextant.desktop.ui.common.CodeBlock
import io.github.rajumark.sextant.desktop.ui.common.CopyButton
import io.github.rajumark.sextant.desktop.ui.common.EmptyState
import io.github.rajumark.sextant.desktop.ui.common.IconBadge
import io.github.rajumark.sextant.desktop.ui.common.KeyValueRow
import io.github.rajumark.sextant.desktop.ui.common.ListDetail
import io.github.rajumark.sextant.desktop.ui.common.MonoLine
import io.github.rajumark.sextant.desktop.ui.common.SubsectionLabel
import io.github.rajumark.sextant.desktop.ui.common.Syntax
import io.github.rajumark.sextant.desktop.ui.common.Tag
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.common.highlighted
import io.github.rajumark.sextant.desktop.ui.workspace.ComponentGroup
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState

fun ComponentType.icon(): ImageVector =
    when (this) {
        ComponentType.Activity -> Icons.Outlined.Smartphone
        ComponentType.ActivityAlias -> Icons.Outlined.Link
        ComponentType.Service -> Icons.Outlined.Sync
        ComponentType.Receiver -> Icons.Outlined.Sensors
        ComponentType.Provider -> Icons.Outlined.Storage
    }

fun ComponentType.tone(): Tone =
    when (this) {
        ComponentType.Activity, ComponentType.ActivityAlias -> Tone.Primary
        ComponentType.Service -> Tone.Tertiary
        ComponentType.Receiver -> Tone.Secondary
        ComponentType.Provider -> Tone.Neutral
    }

@Composable
fun ComponentsScreen(ws: WorkspaceState) {
    val all = ws.analysis.components
    val filtered =
        remember(all, ws.componentQuery, ws.componentGroup, ws.exportedOnly, ws.deepLinksOnly) {
            val q = ws.componentQuery.trim()
            all.filter {
                it.type in ws.componentGroup.types &&
                    (!ws.exportedOnly || it.exported) &&
                    (!ws.deepLinksOnly || it.deepLinks.isNotEmpty()) &&
                    (q.isEmpty() || it.name.contains(q, ignoreCase = true) || it.deepLinks.any { l -> l.contains(q, ignoreCase = true) })
            }
        }
    val selected = all.firstOrNull { it.name == ws.selectedComponent }
    val listState = rememberLazyListState()
    LaunchedEffect(ws.selectedComponent) {
        val i = filtered.indexOfFirst { it.name == ws.selectedComponent }
        if (i >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == i }) listState.scrollToItem(i)
    }

    ListDetail(
        hasSelection = selected != null,
        onBack = { ws.selectedComponent = null },
        initialDetailWidth = 500.dp,
        minListWidth = 560.dp,
        list = {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)) {
                    ListHeader("Components", "${all.size} declared · ${ws.analysis.exportedCount} exported · showing ${filtered.size}")
                    Spacer(Modifier.height(16.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ComponentGroup.entries.forEachIndexed { i, g ->
                            val n = all.count { it.type in g.types }
                            SegmentedButton(
                                selected = ws.componentGroup == g,
                                onClick = { ws.componentGroup = g },
                                shape = SegmentedButtonDefaults.itemShape(i, ComponentGroup.entries.size),
                                icon = {},
                                label = { Text("${g.label} $n", maxLines = 1) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SearchField(ws.componentQuery, { ws.componentQuery = it }, "Search classes or deep links", Modifier.weight(1f))
                        Spacer(Modifier.width(4.dp))
                        ToggleChip("Exported", ws.exportedOnly) { ws.exportedOnly = it }
                        ToggleChip("Deep links", ws.deepLinksOnly) { ws.deepLinksOnly = it }
                    }
                }
                if (filtered.isEmpty()) {
                    EmptyState(Icons.Outlined.SearchOff, "No components match", "Clear a filter or try another search.")
                } else {
                    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(withUniqueKeys(filtered) { "${it.type.wire}|${it.name}" }, key = { it.first }) { (_, c) ->
                            SelectableRow(
                                selected = c.name == ws.selectedComponent,
                                onClick = { ws.selectedComponent = c.name },
                                headline = {
                                    Text(
                                        c.simpleName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        color = if (c.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                    )
                                },
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
            if (selected == null) {
                EmptyState(Icons.Outlined.TouchApp, "Select a component", "See why it is exported, its intent filters and deep links.")
            } else {
                ComponentDetail(selected)
            }
        },
    )
}

@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    onChange: (Boolean) -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = { onChange(!selected) },
        label = { Text(label) },
        leadingIcon =
            if (selected) {
                { Icon(Icons.Outlined.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
            } else {
                null
            },
    )
}

@Composable
fun ComponentTags(c: AppComponent) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (c.launcher) Tag("launcher", Tone.Primary)
        if (!c.enabled) Tag("disabled")
        if (c.deepLinks.isNotEmpty()) Tag("${c.deepLinks.size} links", Tone.Secondary)
        if (c.exported) Tag(if (c.exportedSource == ExportedSource.Default) "exported*" else "exported", Tone.Tertiary)
        if (c.permission != null) Tag("guarded")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentDetail(c: AppComponent) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        DetailHeader(
            c.simpleName,
            c.type.label,
            leading = { IconBadge(c.type.icon(), c.type.tone(), 48.dp) },
            trailing = { CopyButton(c.name, "class name") },
        )
        MonoLine(c.name, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        ExportedExplainer(c)

        SubsectionLabel("Properties")
        KeyValueRow("Enabled", if (c.enabled) "yes" else "no")
        KeyValueRow("Permission", c.permission, mono = true)
        KeyValueRow("Process", c.process, mono = true)
        if (c.type == ComponentType.ActivityAlias) KeyValueRow("Target activity", c.targetActivity, mono = true)
        if (c.type == ComponentType.Provider) KeyValueRow("Authorities", c.authorities.joinToString("\n").ifEmpty { null }, mono = true)
        KeyValueRow("Launcher", if (c.launcher) "yes" else "no")

        if (c.deepLinks.isNotEmpty()) {
            SubsectionLabel("Deep links")
            c.deepLinks.forEach { uri ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Icon(Icons.Outlined.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(uri, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily), modifier = Modifier.weight(1f))
                    CopyButton(uri, "link")
                }
            }
        }

        SubsectionLabel("Intent filters · ${c.intentFilters.size}")
        if (c.intentFilters.isEmpty()) {
            Text(
                "None. Only explicit intents can reach this component.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        c.intentFilters.forEachIndexed { i, f ->
            IntentFilterCard(i, f)
            Spacer(Modifier.height(8.dp))
        }

        SubsectionLabel("components.jsonl")
        CodeBlock(highlighted(c.jsonLine, Syntax.Json), Modifier.fillMaxWidth())
    }
}

@Composable
private fun ExportedExplainer(c: AppComponent) {
    val (tone, title) =
        when {
            c.exported && c.exportedSource == ExportedSource.Default -> Tone.Tertiary to "Exported by default"
            c.exported -> Tone.Tertiary to "Exported"
            else -> Tone.Neutral to "Not exported"
        }
    val body =
        when {
            c.exportedSource == ExportedSource.Default && c.exported ->
                "android:exported is missing. Android exports it because it declares an intent filter."
            c.exportedSource == ExportedSource.Default ->
                "android:exported is missing. Android's default rule makes it private."
            c.exported && c.permission != null -> "Set explicitly. Callers must hold ${c.permission.substringAfterLast('.')}."
            c.exported -> "Set explicitly. Any app on the device can start it."
            else -> "Set explicitly. Only this app (or the same UID) can start it."
        }
    val colors =
        when (tone) {
            Tone.Tertiary -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
        }
    Surface(color = colors.first, contentColor = colors.second, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Outlined.Info, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "exported_source: ${c.exportedSource.wire}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFamily),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntentFilterCard(
    index: Int,
    f: IntentFilterSpec,
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border =
            CardDefaults.outlinedCardBorder().copy(
                brush =
                    androidx.compose.ui.graphics
                        .SolidColor(MaterialTheme.colorScheme.outlineVariant),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filter ${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (f.autoVerify) Tag("autoVerify", Tone.Primary)
                if (f.priority != 0) {
                    Spacer(Modifier.width(6.dp))
                    Tag("priority ${f.priority}", mono = true)
                }
            }
            FilterLine("action", f.actions.map { it.removePrefix("android.intent.action.") })
            if (f.categories.isNotEmpty()) FilterLine("category", f.categories.map { it.removePrefix("android.intent.category.") })
            if (f.data.isNotEmpty()) FilterLine("data", f.data.map { it.uri ?: it.mimeType ?: "…" })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterLine(
    label: String,
    values: List<String>,
) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp).padding(top = 3.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { Tag(it, Tone.Secondary, mono = true) }
        }
    }
}
