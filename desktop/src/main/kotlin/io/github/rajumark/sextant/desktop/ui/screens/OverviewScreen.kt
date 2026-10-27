package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.data.Insight
import io.github.rajumark.sextant.desktop.data.Insights
import io.github.rajumark.sextant.desktop.data.Severity
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.theme.MonoFamily
import io.github.rajumark.sextant.desktop.ui.common.AppIcon
import io.github.rajumark.sextant.desktop.ui.common.CopyButton
import io.github.rajumark.sextant.desktop.ui.common.MonoLine
import io.github.rajumark.sextant.desktop.ui.common.SectionCard
import io.github.rajumark.sextant.desktop.ui.common.StatTile
import io.github.rajumark.sextant.desktop.ui.common.Tag
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.common.formatBytes
import io.github.rajumark.sextant.desktop.ui.common.relativeTime
import io.github.rajumark.sextant.desktop.ui.common.shortHash
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import io.github.rajumark.sextant.desktop.ui.workspace.PermissionFilter
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewScreen(ws: WorkspaceState) {
    val a = ws.analysis
    val m = a.meta
    val insights = remember(a) { Insights.of(a) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        // Short windows keep the panels readable: the stat tiles repeat numbers the panels show, so they step aside.
        val tall = maxHeight >= 720.dp
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 1240.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ListHeader("Overview", "What this APK is, what it asks for, and what it exposes")
                HeroCard(ws)

                val tiles: List<@Composable (Modifier) -> Unit> =
                    listOf(
                        { mod ->
                            StatTile(
                                "Permissions",
                                a.requestedCount.toString(),
                                "${a.dangerousCount} dangerous · ${a.declaredCount} declared",
                                Icons.Outlined.Shield,
                                Tone.Error,
                                mod,
                            ) {
                                ws.permissionFilter = PermissionFilter.Dangerous
                                ws.destination = Destination.Permissions
                            }
                        },
                        { mod ->
                            StatTile(
                                "Components",
                                a.components.size.toString(),
                                "${a.exportedCount} exported",
                                Icons.Outlined.Widgets,
                                Tone.Primary,
                                mod,
                            ) {
                                ws.exportedOnly = true
                                ws.destination = Destination.Components
                            }
                        },
                        { mod ->
                            StatTile(
                                "Deep links",
                                a.deepLinkComponents.sumOf { it.deepLinks.size }.toString(),
                                "in ${a.deepLinkComponents.size} component${if (a.deepLinkComponents.size == 1) "" else "s"}",
                                Icons.Outlined.Link,
                                Tone.Tertiary,
                                mod,
                            ) {
                                ws.deepLinksOnly = true
                                ws.destination = Destination.Components
                            }
                        },
                        { mod ->
                            StatTile(
                                "Code",
                                m.code?.let { "%,d".format(it.classes) } ?: "${m.layout.dexFiles} dex",
                                m.code?.let { "classes · %,d methods · ${m.layout.dexFiles} dex".format(it.methods) }
                                    ?: "${m.layout.nativeAbis.size} native ABIs",
                                Icons.Outlined.DeveloperBoard,
                                Tone.Secondary,
                                mod,
                            )
                        },
                    )
                if (tall) {
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { tiles.forEach { it(Modifier.weight(1f)) } }
                    } else {
                        tiles.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { pair.forEach { it(Modifier.weight(1f)) } }
                        }
                    }
                }

                // Each panel scrolls inside its own space; the page does not.
                val left: @Composable (Modifier) -> Unit = { mod ->
                    Column(mod.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        InsightsCard(insights)
                        FlagsCard(ws)
                    }
                }
                val right: @Composable (Modifier) -> Unit = { mod ->
                    Column(mod.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        EntryPointsCard(ws)
                        ComponentMixCard(ws)
                        if (m.warnings.isNotEmpty()) WarningsCard(m.warnings)
                    }
                }
                if (wide) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        left(Modifier.weight(1.1f).fillMaxHeight())
                        right(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        left(Modifier.weight(1f).fillMaxWidth())
                        right(Modifier.weight(1f).fillMaxWidth())
                    }
                }
                Text(
                    "Analyzed ${relativeTime(a.analyzedAtMillis)}" + (a.durationMs?.let { " in %.1f s".format(it / 1000.0) } ?: "") +
                        " · sextant ${m.toolVersion} · schema ${m.schemaVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(ws: WorkspaceState) {
    val m = ws.analysis.meta
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraLarge) {
        Row(Modifier.fillMaxWidth().padding(28.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(ws.analysis, 72.dp)
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Text(m.label, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    m.packageName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFamily),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroChip("Version ${m.versionText}")
                    m.minSdk?.let { HeroChip("min SDK $it") }
                    m.targetSdk?.let { HeroChip("target SDK $it") }
                    m.compileSdk?.let { HeroChip("compile SDK $it") }
                    HeroChip("Signed ${m.signing.schemes.joinToString(" · ")}")
                }
            }
            Spacer(Modifier.width(24.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.widthIn(min = 260.dp, max = 340.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("APK file", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MonoLine(m.file.name, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(10.dp))
                    Text("Size", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatBytes(m.file.sizeBytes), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Text("SHA-256", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoLine(shortHash(m.file.sha256), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        CopyButton(m.file.sha256, "SHA-256")
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f), shape = MaterialTheme.shapes.small) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun InsightsCard(insights: List<Insight>) {
    SectionCard("Worth a look", icon = Icons.Outlined.Lightbulb) {
        if (insights.isEmpty()) {
            Text("Nothing stands out.", style = MaterialTheme.typography.bodyMedium)
        }
        insights.forEachIndexed { i, ins ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 10.dp))
            Row {
                val (icon, tint) =
                    when (ins.severity) {
                        Severity.Risk -> Icons.Outlined.ErrorOutline to MaterialTheme.colorScheme.error
                        Severity.Notice -> Icons.Outlined.WarningAmber to MaterialTheme.colorScheme.tertiary
                        Severity.Info -> Icons.Outlined.Info to MaterialTheme.colorScheme.secondary
                    }
                Icon(icon, ins.severity.name, tint = tint, modifier = Modifier.size(20.dp).padding(top = 1.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(ins.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                    Text(ins.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FlagsCard(ws: WorkspaceState) {
    val f = ws.analysis.meta.flags
    SectionCard("Application flags", icon = Icons.Outlined.Flag) {
        FlagRow("debuggable", f.debuggable, riskyWhen = true)
        FlagRow("allowBackup", f.allowBackup, riskyWhen = true)
        FlagRow("usesCleartextTraffic", f.usesCleartextTraffic, riskyWhen = true)
        FlagRow("extractNativeLibs", f.extractNativeLibs, riskyWhen = null)
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "networkSecurityConfig",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily),
                modifier = Modifier.weight(1f),
            )
            if (f.networkSecurityConfig != null) Tag(f.networkSecurityConfig, Tone.Secondary, mono = true) else Tag("not set")
        }
        Text(
            "\"not set\" means the attribute is absent from the manifest. Sextant never guesses a value.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun FlagRow(
    name: String,
    value: Boolean?,
    riskyWhen: Boolean?,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily), modifier = Modifier.weight(1f))
        when (value) {
            null -> Tag("not set")
            else -> Tag(value.toString(), if (riskyWhen != null && value == riskyWhen) Tone.Error else Tone.Primary, mono = true)
        }
    }
}

@Composable
private fun EntryPointsCard(ws: WorkspaceState) {
    val a = ws.analysis
    SectionCard("Entry points", icon = Icons.Outlined.RocketLaunch) {
        Text("Launcher activity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val launcher = a.launcher
        if (launcher != null) {
            LinkRow(launcher.simpleName, launcher.name) { ws.showComponent(launcher.name) }
        } else {
            Text("None", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        Text("Deep links", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        a.deepLinkComponents.forEach { c ->
            c.deepLinks.forEach { uri -> LinkRow(uri, c.simpleName, mono = true) { ws.showComponent(c.name) } }
        }
        if (a.deepLinkComponents.isEmpty()) Text("None declared", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    mono: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style =
                    MaterialTheme.typography.bodyLarge.let {
                        if (mono) {
                            it.copy(
                                fontFamily = MonoFamily,
                            )
                        } else {
                            it.copy(fontWeight = FontWeight.Medium)
                        }
                    },
                color = MaterialTheme.colorScheme.onSurface,
            )
            MonoLine(subtitle)
        }
        Icon(
            Icons.AutoMirrored.Outlined.OpenInNew,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ComponentMixCard(ws: WorkspaceState) {
    val a = ws.analysis
    val c = MaterialTheme.colorScheme
    val parts: List<Triple<String, Int, Color>> =
        listOf(
            Triple("Activities", a.countOf(ComponentType.Activity) + a.countOf(ComponentType.ActivityAlias), c.primary),
            Triple("Services", a.countOf(ComponentType.Service), c.tertiary),
            Triple("Receivers", a.countOf(ComponentType.Receiver), c.secondary),
            Triple("Providers", a.countOf(ComponentType.Provider), c.outline),
        )
    val total = parts.sumOf { it.second }.coerceAtLeast(1)
    SectionCard("Component mix", icon = Icons.Outlined.PieChart, action = {
        TextButton(onClick = { ws.destination = Destination.Components }) { Text("View all") }
    }) {
        Row(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            parts.filter { it.second > 0 }.forEach { (_, n, color) ->
                Box(Modifier.weight(n.toFloat() / total).fillMaxHeight().background(color))
            }
        }
        Spacer(Modifier.height(14.dp))
        parts.forEach { (label, n, color) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(10.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(n.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Text(
                    "  ${n * 100 / total}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Native ABIs", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            a.meta.layout.nativeAbis
                .forEach { Tag(it, Tone.Secondary, mono = true) }
        }
    }
}

@Composable
private fun WarningsCard(warnings: List<String>) {
    SectionCard("Decoder warnings", icon = Icons.Outlined.WarningAmber) {
        Text(
            "Sextant skipped these broken pieces and kept going. They are also in meta.json → warnings[].",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        warnings.forEach {
            Row(Modifier.padding(vertical = 4.dp)) {
                Icon(
                    Icons.Outlined.WarningAmber,
                    null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFamily))
            }
        }
    }
}
