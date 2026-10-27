package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.copyToClipboard
import io.github.rajumark.sextant.desktop.data.ManifestFormat
import io.github.rajumark.sextant.desktop.data.PermissionBank
import io.github.rajumark.sextant.desktop.model.Permission
import io.github.rajumark.sextant.desktop.model.PermissionKind
import io.github.rajumark.sextant.desktop.openInBrowser
import io.github.rajumark.sextant.desktop.ui.common.CodeBlock
import io.github.rajumark.sextant.desktop.ui.common.CopyButton
import io.github.rajumark.sextant.desktop.ui.common.EmptyState
import io.github.rajumark.sextant.desktop.ui.common.IconBadge
import io.github.rajumark.sextant.desktop.ui.common.KeyValueRow
import io.github.rajumark.sextant.desktop.ui.common.ListDetail
import io.github.rajumark.sextant.desktop.ui.common.LocalNotifier
import io.github.rajumark.sextant.desktop.ui.common.MonoLine
import io.github.rajumark.sextant.desktop.ui.common.SubsectionLabel
import io.github.rajumark.sextant.desktop.ui.common.Syntax
import io.github.rajumark.sextant.desktop.ui.common.Tag
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.common.WebActions
import io.github.rajumark.sextant.desktop.ui.common.highlighted
import io.github.rajumark.sextant.desktop.ui.workspace.PermissionFilter
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState
import io.github.rajumark.sextant.desktop.urlQuery

private val Permission.selectionKey: String get() = "${kind.wire}|$name"

private fun Permission.matches(f: PermissionFilter): Boolean =
    when (f) {
        PermissionFilter.All -> true
        PermissionFilter.Dangerous -> dangerous
        PermissionFilter.Normal -> !dangerous && kind != PermissionKind.Declared
        PermissionFilter.Declared -> kind == PermissionKind.Declared
        PermissionFilter.Custom -> custom
    }

private fun Permission.tone(): Tone =
    when {
        dangerous -> Tone.Error
        kind == PermissionKind.Declared -> Tone.Tertiary
        else -> Tone.Neutral
    }

private fun Permission.icon() =
    when {
        dangerous -> Icons.Outlined.GppMaybe
        kind == PermissionKind.Declared -> Icons.Outlined.Policy
        else -> Icons.Outlined.Lock
    }

@Composable
fun PermissionsScreen(ws: WorkspaceState) {
    val all = ws.analysis.permissions
    val filtered =
        remember(all, ws.permissionQuery, ws.permissionFilter) {
            all.filter { it.matches(ws.permissionFilter) && it.name.contains(ws.permissionQuery.trim(), ignoreCase = true) }
        }
    // Selection holds "kind|name" (a name can appear as both declared and requested); a bare name also works.
    val selected = all.firstOrNull { it.selectionKey == ws.selectedPermission } ?: all.firstOrNull { it.name == ws.selectedPermission }
    val rows = remember(filtered) { withUniqueKeys(filtered) { it.selectionKey } }

    ListDetail(
        hasSelection = selected != null,
        onBack = { ws.selectedPermission = null },
        list = {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)) {
                    ListHeader(
                        "Permissions",
                        "${ws.analysis.requestedCount} requested · ${ws.analysis.dangerousCount} dangerous · ${ws.analysis.declaredCount} declared",
                    )
                    Spacer(Modifier.height(16.dp))
                    SearchField(ws.permissionQuery, { ws.permissionQuery = it }, "Search permissions", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PermissionFilter.entries.forEach { f ->
                            val n = all.count { it.matches(f) }
                            FilterChip(
                                selected = ws.permissionFilter == f,
                                onClick = { ws.permissionFilter = f },
                                label = { Text("${f.label}  $n") },
                            )
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    EmptyState(Icons.Outlined.SearchOff, "No permissions match", "Try a different search or filter.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding =
                            androidx.compose.foundation.layout
                                .PaddingValues(bottom = 16.dp),
                    ) {
                        items(rows, key = { it.first }) { (_, p) ->
                            SelectableRow(
                                selected = p == selected,
                                onClick = { ws.selectedPermission = p.selectionKey },
                                headline = { Text(p.shortName, style = MaterialTheme.typography.bodyLarge) },
                                supporting = { MonoLine(p.name) },
                                leading = { IconBadge(p.icon(), p.tone()) },
                                trailing = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (p.dangerous) Tag("dangerous", Tone.Error)
                                        p.protectionLevel?.let { Tag(it, Tone.Tertiary, mono = true) }
                                        if (p.kind != PermissionKind.UsesPermission) Tag(p.kind.wire, mono = true)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
        detail = {
            if (selected == null) {
                EmptyState(
                    Icons.Outlined.TouchApp,
                    "Select a permission",
                    "Its meaning, what it is used for and its manifest entry appear here.",
                )
            } else {
                PermissionDetail(ws, selected)
            }
        },
    )
}

@Composable
private fun PermissionDetail(
    ws: WorkspaceState,
    p: Permission,
) {
    val summary = PermissionBank.summary(p.name)
    val users = ws.analysis.components.filter { it.permission == p.name }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        DetailHeader(
            p.shortName,
            summary?.group ?: if (p.custom) "Custom permission" else "Platform permission",
            leading = { IconBadge(p.icon(), p.tone(), 48.dp) },
            trailing = { CopyButton(p.name, "permission name") },
        )
        MonoLine(p.name, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    summary != null -> {
                        Text(summary.meaning, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Used for: ${summary.usage}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    p.custom ->
                        Text(
                            "Declared by this app itself. Other apps can use it only as its protection level allows.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    else ->
                        Text("No plain-English summary for this permission yet.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        SubsectionLabel("Properties")
        KeyValueRow("Kind", p.kind.wire, mono = true)
        KeyValueRow("Dangerous", if (p.dangerous) "yes · needs runtime approval" else "no")
        KeyValueRow("Protection level", p.protectionLevel, mono = true)
        KeyValueRow("Max SDK", p.maxSdk?.toString())
        KeyValueRow("Custom", if (p.custom) "yes" else "no")
        p.impliedBy?.let { KeyValueRow("Implied by", it, mono = true) }
        if (p.kind == PermissionKind.Declared) {
            SubsectionLabel("Guarded components")
            if (users.isEmpty()) {
                Text(
                    "No component in this APK requires it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            users.forEach { c ->
                SelectableRow(
                    selected = false,
                    onClick = { ws.showComponent(c.name) },
                    headline = { Text(c.simpleName) },
                    supporting = { MonoLine(c.type.wire) },
                    leading = { IconBadge(c.type.icon(), Tone.Primary, 32.dp) },
                    trailing = {},
                )
            }
        }
        SubsectionLabel("Manifest entry")
        CodeBlock(highlighted(p.manifestXml ?: ManifestFormat.permissionSnippet(p), Syntax.Xml), Modifier.fillMaxWidth())
        PermissionWebActions(p)
    }
}

/** Look the permission up on the web, or ask an AI assistant about it. */
@Composable
private fun PermissionWebActions(p: Permission) =
    WebActions(
        searchQuery = "Android permission ${p.name}",
        question = "What does the Android permission ${p.name} allow an app to do, and when do apps need it?",
    )
