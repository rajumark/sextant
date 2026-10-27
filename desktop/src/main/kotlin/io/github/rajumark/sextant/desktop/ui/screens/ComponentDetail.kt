package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.data.ManifestFormat
import io.github.rajumark.sextant.desktop.model.AppComponent
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.model.ExportedSource
import io.github.rajumark.sextant.desktop.model.IntentFilterSpec
import io.github.rajumark.sextant.desktop.theme.MonoFamily
import io.github.rajumark.sextant.desktop.ui.common.CodeBlock
import io.github.rajumark.sextant.desktop.ui.common.CopyButton
import io.github.rajumark.sextant.desktop.ui.common.IconBadge
import io.github.rajumark.sextant.desktop.ui.common.KeyValueRow
import io.github.rajumark.sextant.desktop.ui.common.MonoLine
import io.github.rajumark.sextant.desktop.ui.common.SubsectionLabel
import io.github.rajumark.sextant.desktop.ui.common.Syntax
import io.github.rajumark.sextant.desktop.ui.common.Tag
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.common.WebActions
import io.github.rajumark.sextant.desktop.ui.common.highlighted
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

/** Color of a core verdict tag: red for review-first risks, then exposure, entry points, roles, and plain state. */
fun tagTone(tag: String): Tone =
    when (tag) {
        "task-hijack", "sensitive-role" -> Tone.Error
        "hidden-entry", "unguarded", "exported-by-default", "lock-screen", "uri-grants" -> Tone.Tertiary
        "launcher", "app-link", "deep-link" -> Tone.Primary
        "system-role", "boot", "foreground", "file-provider", "isolated", "direct-boot" -> Tone.Secondary
        else -> Tone.Neutral
    }

/** Tags ordered for review: most worrying first. Core gives them in display order; this is for the "Risk first" sort. */
fun riskScore(c: AppComponent): Int =
    c.tags.sumOf { t ->
        when (tagTone(t)) {
            Tone.Error -> 100
            Tone.Tertiary -> 10
            else -> 0
        }
    } + if (c.exported) 1 else 0

/**
 * One component in layers: verdicts and why → properties and attributes → entry points → raw manifest entry.
 * Everything shown comes prepared from core, the same data `sextant component` prints.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentDetail(
    c: AppComponent,
    ws: WorkspaceState,
) {
    val f = c.facts
    val page = ws.componentPage(c.type)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        DetailHeader(
            c.simpleName,
            listOfNotNull(c.type.label, f.label).joinToString(" · "),
            leading = { IconBadge(c.type.icon(), c.type.tone(), 48.dp) },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { ws.showComponentInManifest(c.name) }) { Text("View in Manifest") }
                    CopyButton(c.name, "class name")
                }
            },
        )
        MonoLine(c.name, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        ExportedExplainer(c)

        if (f.tags.isNotEmpty()) {
            SubsectionLabel("Verdicts")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                f.tags.forEach { t ->
                    Row(verticalAlignment = Alignment.Top) {
                        Tag(t, tagTone(t), mono = true)
                        Spacer(Modifier.width(10.dp))
                        Text(f.reasons[t].orEmpty(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        SubsectionLabel("Properties")
        KeyValueRow("Enabled", if (c.enabled) "yes" else "no")
        KeyValueRow("Permission", c.permission, mono = true)
        if (c.type == ComponentType.Provider) {
            KeyValueRow("Read permission", c.readPermission, mono = true)
            KeyValueRow("Write permission", c.writePermission, mono = true)
        }
        KeyValueRow("Process", c.process, mono = true)
        if (c.type == ComponentType.ActivityAlias) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KeyValueRow("Target activity", c.targetActivity, mono = true)
            }
            c.targetActivity?.let { t ->
                if (ws.analysis.components.any { it.name == t }) {
                    TextButton(onClick = { ws.showComponent(t) }) { Text("Open target activity") }
                }
            }
        }
        f.attributes.forEach { (k, v) -> KeyValueRow(k, v, mono = true) }

        if (f.aliases.isNotEmpty()) {
            SubsectionLabel("Aliases pointing here · ${f.aliases.size}")
            f.aliases.forEach { a ->
                SelectableRow(
                    selected = false,
                    onClick = { ws.showComponent(a) },
                    headline = { Text(a.substringAfterLast('.')) },
                    supporting = { MonoLine(a) },
                    leading = { IconBadge(ComponentType.ActivityAlias.icon(), Tone.Primary, 32.dp) },
                    trailing = {},
                    inset = 0.dp,
                )
            }
        }

        SubsectionLabel("Entry points")
        if (f.entryPoints.isEmpty()) {
            Text(
                if (c.exported) "No intent filters: any app can still start it with an explicit intent." else "No intent filters: only this app can start it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        f.entryPoints.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }

        if (c.deepLinks.isNotEmpty()) {
            SubsectionLabel("Deep links · ${c.deepLinks.size}")
            c.deepLinks.forEach { uri ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Icon(Icons.Outlined.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(uri, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily), modifier = Modifier.weight(1f))
                    CopyButton(uri, "link")
                }
            }
        }
        if (f.appLinkHosts.isNotEmpty()) {
            SubsectionLabel("Verified App Link hosts")
            f.appLinkHosts.forEach { h ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Icon(Icons.Outlined.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    MonoLine("https://$h/.well-known/assetlinks.json", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    CopyButton("https://$h/.well-known/assetlinks.json", "assetlinks URL")
                }
            }
            Text(
                "Android checks each file lists this app's signing certificate before opening links without a chooser.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (c.authorities.isNotEmpty()) {
            SubsectionLabel("Authorities")
            c.authorities.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoLine("content://$a/", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    CopyButton("content://$a/", "content URI")
                }
            }
        }
        if (f.pathPermissions.isNotEmpty()) {
            SubsectionLabel("Path permissions")
            f.pathPermissions.forEach { MonoLine(it, color = MaterialTheme.colorScheme.onSurface) }
        }
        if (f.grantUriPaths.isNotEmpty()) {
            SubsectionLabel("URI grants")
            f.grantUriPaths.forEach { MonoLine(it, color = MaterialTheme.colorScheme.onSurface) }
        }

        if (f.adb.isNotEmpty()) {
            SubsectionLabel("adb")
            f.adb.forEach { (label, cmd) ->
                Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cmd, style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFamily), modifier = Modifier.weight(1f))
                        CopyButton(cmd, "adb command")
                    }
                }
            }
        }

        if (c.intentFilters.isNotEmpty()) {
            SubsectionLabel("Intent filters · ${c.intentFilters.size}")
            c.intentFilters.forEachIndexed { i, filter ->
                IntentFilterCard(i, filter, f.entryPoints.getOrNull(i))
                Spacer(Modifier.height(8.dp))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SubsectionLabel("Manifest entry", Modifier.weight(1f))
            SingleChoiceSegmentedButtonRow {
                listOf("XML", "JSON").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = page.rawJson == (i == 1),
                        onClick = { page.rawJson = i == 1 },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        icon = {},
                        label = { Text(label) },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val raw = if (page.rawJson) c.jsonLine.ifEmpty { "{}" } else f.manifestXml ?: ManifestFormat.componentSnippet(c)
        Row(verticalAlignment = Alignment.Top) {
            CodeBlock(highlighted(raw, if (page.rawJson) Syntax.Json else Syntax.Xml), Modifier.weight(1f))
            CopyButton(raw, if (page.rawJson) "JSON" else "manifest entry")
        }

        val pkg = ws.analysis.meta.packageName
        WebActions(
            searchQuery = "Android ${c.type.label.lowercase()} ${c.name}",
            question =
                "In the Android app $pkg, what does the ${c.type.label.lowercase()} ${c.name} likely do? " +
                    "It is ${if (c.exported) "exported" else "not exported"}" +
                    (if (c.tags.isEmpty()) "" else " and flagged: ${c.tags.joinToString()}") +
                    ". What should a security or privacy reviewer check?",
        )
    }
}

@Composable
private fun ExportedExplainer(c: AppComponent) {
    val title =
        when {
            c.exported && c.exportedSource == ExportedSource.Default -> "Exported by default"
            c.exported -> "Exported"
            else -> "Not exported"
        }
    val body =
        when {
            c.exportedSource == ExportedSource.Default && c.exported ->
                "android:exported is missing. Android exports it because it declares an intent filter."
            c.exportedSource == ExportedSource.Default -> "android:exported is missing. Android's default rule makes it private."
            c.exported && c.permission != null -> "Set explicitly. Callers must hold ${c.permission.substringAfterLast('.')}."
            c.exported -> "Set explicitly. Any app on the device can reach it."
            else -> "Set explicitly. Only this app (or the same UID) can reach it."
        }
    val (bg, fg) =
        if (c.exported) {
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
        }
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Outlined.Info, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntentFilterCard(
    index: Int,
    f: IntentFilterSpec,
    summary: String?,
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(MaterialTheme.colorScheme.outlineVariant)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(summary ?: "Filter ${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (f.autoVerify) Tag("autoVerify", Tone.Primary)
                if (f.priority != 0) {
                    Spacer(Modifier.width(6.dp))
                    Tag("priority ${f.priority}", mono = true)
                }
            }
            if (f.actions.isNotEmpty()) FilterLine("action", f.actions.map { it.removePrefix("android.intent.action.") })
            if (f.categories.isNotEmpty()) FilterLine("category", f.categories.map { it.removePrefix("android.intent.category.") })
            if (f.data.isNotEmpty()) {
                FilterLine("data", f.data.map { d -> d.uri ?: d.mimeType ?: listOfNotNull(d.scheme, d.host).joinToString("://").ifEmpty { "…" } })
            }
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
            val display = if (values.size > 6) values.take(6) + listOf("+${values.size - 6} more") else values
            display.forEach { Tag(it, Tone.Secondary, mono = true) }
        }
    }
}
