package io.github.rajumark.sextant.desktop.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.AppState
import io.github.rajumark.sextant.desktop.ShortcutPrefix
import io.github.rajumark.sextant.desktop.isMac
import io.github.rajumark.sextant.desktop.model.Analysis
import io.github.rajumark.sextant.desktop.theme.ThemeMode
import io.github.rajumark.sextant.desktop.ui.common.AppIcon
import io.github.rajumark.sextant.desktop.ui.common.EmptyState
import io.github.rajumark.sextant.desktop.ui.common.MonoLine
import io.github.rajumark.sextant.desktop.ui.common.SextantLogo
import io.github.rajumark.sextant.desktop.ui.common.ShapeBadge
import io.github.rajumark.sextant.desktop.ui.common.Tag
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.common.relativeTime

@Composable
fun HomeScreen(app: AppState) {
    // The window never scrolls: two panes share the space; only the recent list scrolls, inside its pane.
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        HomeTopBar(app)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            val wide = maxWidth >= 860.dp
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StartPane(app, Modifier.weight(1f).fillMaxHeight())
                    RecentPane(app, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    StartPane(app, Modifier.weight(1f).fillMaxWidth())
                    RecentPane(app, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

/** Left half: how to start. Fills its half; the drop zone takes all the space that is left. */
@Composable
private fun StartPane(
    app: AppState,
    modifier: Modifier,
) {
    Column(modifier) {
        DropZone(app, Modifier.weight(1f).fillMaxWidth())
    }
}

/** Right half: recent analyses. Only this list scrolls. */
@Composable
private fun RecentPane(
    app: AppState,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.fillMaxSize().padding(top = 20.dp, bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text("Recent analyses", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (app.recent.isNotEmpty()) TextButton(onClick = app::clearRecent) { Text("Clear") }
            }
            Spacer(Modifier.height(8.dp))
            when {
                app.loadingRecent && app.recent.isEmpty() -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                app.recent.isEmpty() ->
                    EmptyState(
                        Icons.Outlined.History,
                        "No analyses yet",
                        "APKs you analyze and output folders you open appear here.",
                        Modifier.weight(1f),
                    )
                else ->
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp)) {
                        items(app.recent, key = { it.outputDir }) { analysis ->
                            RecentRow(
                                a = analysis,
                                tags = app.tags[analysis.outputDir].orEmpty(),
                                onOpen = { app.open(analysis) },
                                onReveal = { app.revealFolder(analysis.outputDir) },
                                onTag = { app.tagging = analysis },
                                onDelete = { app.deleting = analysis },
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun HomeTopBar(app: AppState) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        SextantLogo(40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Sextant", style = MaterialTheme.typography.titleLarge)
            Text(
                "APK intelligence",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val (icon, next) =
            when (app.themeMode) {
                ThemeMode.System -> Icons.Outlined.BrightnessAuto to ThemeMode.Light
                ThemeMode.Light -> Icons.Outlined.LightMode to ThemeMode.Dark
                ThemeMode.Dark -> Icons.Outlined.DarkMode to ThemeMode.System
            }
        IconButton(onClick = { app.themeMode = next }) { Icon(icon, "Theme: ${app.themeMode.name}") }
        IconButton(onClick = { app.showSettings = true }) { Icon(Icons.Outlined.Settings, "Settings") }
    }
}

/** Expressive shapes are experimental in this Compose version; the drop area is the one place that uses them. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DropZone(
    app: AppState,
    modifier: Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val c = MaterialTheme.colorScheme
    OutlinedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.outlinedCardColors(containerColor = if (hovered) c.surfaceContainerHigh else c.surfaceContainerLow),
        border = BorderStroke(if (hovered) 2.dp else 1.dp, if (hovered) c.primary else c.outlineVariant),
        modifier = modifier.hoverable(interaction),
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ShapeBadge(Icons.Outlined.UploadFile, Tone.Primary, MaterialShapes.Cookie9Sided.toShape(), 84.dp)
            Spacer(Modifier.height(16.dp))
            Text("Drop an APK here", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = app::openApk) {
                    Icon(Icons.Outlined.UploadFile, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Choose APK…")
                }
                OutlinedButton(onClick = app::openFolder) { Text("Open output folder…") }
            }
            Spacer(Modifier.height(12.dp))
            Text("${ShortcutPrefix}O to open", style = MaterialTheme.typography.labelMedium, color = c.outline)
        }
    }
}

/**
 * One recent analysis: name, package and version. Tag chips appear only when the analysis has tags.
 * Hovering shows three actions: reveal the output folder, add tags, delete.
 */
@Composable
private fun RecentRow(
    a: Analysis,
    tags: List<String>,
    onOpen: () -> Unit,
    onReveal: () -> Unit,
    onTag: () -> Unit,
    onDelete: () -> Unit,
) {
    val m = a.meta
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    // Actions fade in with the theme's effects spring: color and opacity never overshoot.
    val actionsAlpha by animateFloatAsState(
        if (hovered) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "actionsAlpha",
    )
    Surface(
        onClick = onOpen,
        interactionSource = interaction,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(a, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                MonoLine(m.packageName)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    m.versionName?.let { Tag(it, Tone.Secondary, mono = true) }
                    Tag("SDK ${m.minSdk ?: "?"}–${m.targetSdk ?: "?"}")
                    tags.forEach { Tag(it, Tone.Primary) }
                }
            }
            Text(
                relativeTime(a.analyzedAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            // Actions keep their space when hidden, so the row does not shift on hover.
            Row(Modifier.alpha(actionsAlpha)) {
                IconButton(onClick = onReveal, enabled = hovered, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.FolderOpen, if (isMac) "Reveal output folder in Finder" else "Open output folder")
                }
                IconButton(onClick = onTag, enabled = hovered, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.LocalOffer, "Add tags")
                }
                IconButton(onClick = onDelete, enabled = hovered, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, "Delete analysis", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
