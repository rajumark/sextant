package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.rajumark.sextant.desktop.data.AppIconImage
import io.github.rajumark.sextant.desktop.data.IconAssets
import io.github.rajumark.sextant.desktop.model.AdaptivePreview
import io.github.rajumark.sextant.desktop.model.IconFileInfo
import io.github.rajumark.sextant.desktop.model.IconPick
import io.github.rajumark.sextant.desktop.saveWithDialog
import io.github.rajumark.sextant.desktop.ui.common.CodeBlock
import io.github.rajumark.sextant.desktop.ui.common.CopyButton
import io.github.rajumark.sextant.desktop.ui.common.CopyTextButton
import io.github.rajumark.sextant.desktop.ui.common.EmptyState
import io.github.rajumark.sextant.desktop.ui.common.IconBadge
import io.github.rajumark.sextant.desktop.ui.common.KeyValueRow
import io.github.rajumark.sextant.desktop.ui.common.ListDetail
import io.github.rajumark.sextant.desktop.ui.common.SubsectionLabel
import io.github.rajumark.sextant.desktop.ui.common.Syntax
import io.github.rajumark.sextant.desktop.ui.common.Tag
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.common.formatBytes
import io.github.rajumark.sextant.desktop.ui.common.highlighted
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

/** One icon file with the role it plays. [key] identifies it in the selection. */
private data class IconEntry(
    val role: String,
    val resource: String,
    val file: IconFileInfo,
) {
    val key: String get() = "$role|${file.path}"
    val title: String get() = "${file.config.ifEmpty { "default" }} · $role"

    /**
     * The resource type: "mipmap" for @mipmap/ic_launcher, "drawable" for @drawable/... . Build tools rename files flat
     * (res/yn.png), so the resource name is the reliable source; the folder is used when there is none.
     */
    val kind: String
        get() =
            resource.substringAfter('@', "").substringBefore('/').ifEmpty {
                file.path
                    .substringAfter("res/", "")
                    .substringBefore('/')
                    .substringBefore('-')
                    .ifEmpty { "other" }
            }
}

/** Launcher pixel sizes per density. Launcher icons of another size are flagged. */
private val LauncherSizes =
    linkedMapOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)

private fun IconEntry.sizeWarning(): String? {
    if (role != "Launcher icon" && role != "Round icon") return null
    // Match whole qualifiers: "xhdpi" contains "hdpi", so a substring test picks the wrong density.
    val density = LauncherSizes.keys.firstOrNull { it in file.config.split('-') } ?: return null
    val width = file.width ?: return null
    val expected = LauncherSizes.getValue(density)
    return if (width != expected) "Expected $expected px for $density, found $width px" else null
}

/** The filter value for launcher files with the wrong pixel size. */
private const val SIZE_PROBLEMS = "Size problems"

/**
 * Densities with no bitmap launcher icon. Empty when the launcher icon has no bitmaps at all (vector only), since
 * then no density is missing.
 */
private fun missingLauncherDensities(all: List<IconEntry>): List<String> {
    val bitmaps = all.filter { it.role == "Launcher icon" && it.file.format.lowercase() in setOf("png", "webp", "jpg", "jpeg") }
    if (bitmaps.isEmpty()) return emptyList()
    return LauncherSizes.keys.filter { d ->
        bitmaps.none { b ->
            b.file.config
                .split('-')
                .contains(d)
        }
    }
}

/** One line per shown file, for a release checklist or a bug report. */
private fun iconReport(entries: List<IconEntry>): String =
    entries.joinToString("\n") { e ->
        listOf(
            e.role,
            e.file.config.ifEmpty { "default" },
            e.file.path,
            e.file.format,
            if (e.file.width != null) "${e.file.width}×${e.file.height} px" else "vector",
            formatBytes(e.file.sizeBytes),
        ).joinToString("\t") + (e.sizeWarning()?.let { "\t$it" } ?: "")
    }

/**
 * Every icon file the APK ships. The list has search, filter chips and a launcher preview button. Selecting a file
 * opens its details beside the list. Zoom, compare, save and the launcher preview open only when asked.
 */
@Composable
fun IconsScreen(ws: WorkspaceState) {
    val a = ws.analysis
    val apk = a.apkPath?.let { Path.of(it) }
    val all = a.meta.iconGroups.flatMap { g -> g.files.map { IconEntry(g.title, g.resource, it) } }
    val kinds = all.map { it.kind }.distinct().sorted()
    val query = ws.iconQuery.trim()
    val entries =
        all.filter { e ->
            (ws.iconFilter == "All" || e.kind == ws.iconFilter || (ws.iconFilter == SIZE_PROBLEMS && e.sizeWarning() != null)) &&
                (
                    query.isEmpty() ||
                        e.file.path.contains(query, true) ||
                        e.file.config.contains(query, true) ||
                        e.resource.contains(query, true)
                )
        }
    val selected = all.firstOrNull { it.key == ws.selectedIcon }
    var zoomOf by remember { mutableStateOf<IconEntry?>(null) }
    var compareOf by remember { mutableStateOf<IconEntry?>(null) }
    var showPreview by remember { mutableStateOf(false) }

    when {
        all.isEmpty() ->
            EmptyState(Icons.Outlined.Image, "No icons", "This APK declares no launcher icon.", Modifier.fillMaxSize())
        apk == null ->
            EmptyState(
                Icons.Outlined.Image,
                "APK not available",
                "Icons are read from the APK. Open the APK again to see them.",
                Modifier.fillMaxSize(),
            )
        else -> {
            ListDetail(
                hasSelection = selected != null,
                onBack = { ws.selectedIcon = null },
                list = {
                    IconList(
                        ws = ws,
                        apk = apk,
                        all = all,
                        entries = entries,
                        kinds = kinds,
                        canPreview = a.meta.adaptive != null || a.meta.icon != null,
                        onLauncherPreview = { showPreview = true },
                    )
                },
                detail = {
                    if (selected != null) {
                        IconDetail(
                            apk = apk,
                            e = selected,
                            sameRole = all.filter { it.role == selected.role },
                            onZoom = { zoomOf = selected },
                            onCompare = { compareOf = selected },
                            onSave = { saveIcon(apk, selected) },
                        )
                    } else {
                        EmptyState(Icons.Outlined.TouchApp, "Select an icon", "Its file, size and format appear here.")
                    }
                },
            )
            zoomOf?.let { e -> ZoomDialog(apk, e.file) { zoomOf = null } }
            compareOf?.let { e -> CompareDialog(apk, all.filter { it.role == e.role }, e.role) { compareOf = null } }
            if (showPreview) LauncherPreviewDialog(apk, a.meta.adaptive, a.meta.icon) { showPreview = false }
        }
    }
}

@Composable
private fun IconList(
    ws: WorkspaceState,
    apk: Path,
    all: List<IconEntry>,
    entries: List<IconEntry>,
    kinds: List<String>,
    canPreview: Boolean,
    onLauncherPreview: () -> Unit,
) {
    val used =
        ws.analysis.meta.icon
            ?.path
    val groups = entries.groupBy { it.role }
    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Icons", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${all.size} icon files · select one to see its details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val missing = remember(all) { missingLauncherDensities(all) }
                if (missing.isNotEmpty()) {
                    Text(
                        "Launcher icon has no ${missing.joinToString()} bitmap; Android scales another density on those screens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            CopyTextButton("Copy list", "${entries.size} icon files") { iconReport(entries) }
            if (canPreview) {
                IconButton(onClick = onLauncherPreview) { Icon(Icons.Outlined.Layers, "Launcher preview") }
            }
        }
        Spacer(Modifier.height(12.dp))
        SearchField(ws.iconQuery, { ws.iconQuery = it }, "Search by path, density or resource", Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("All") + kinds).forEach { kind ->
                FilterChip(
                    selected = ws.iconFilter == kind,
                    onClick = { ws.iconFilter = kind },
                    label = { Text("$kind  ${if (kind == "All") all.size else all.count { it.kind == kind }}") },
                )
            }
            val wrong = all.count { it.sizeWarning() != null }
            if (wrong > 0) {
                FilterChip(
                    selected = ws.iconFilter == SIZE_PROBLEMS,
                    onClick = { ws.iconFilter = SIZE_PROBLEMS },
                    label = { Text("$SIZE_PROBLEMS  $wrong") },
                    leadingIcon = { Icon(Icons.Outlined.WarningAmber, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (entries.isEmpty()) {
            EmptyState(Icons.Outlined.Image, "No icons match", "Clear the search or the filter to see them all.", Modifier.fillMaxSize())
        } else {
            // Arrow keys move the selection through the visible icons.
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val step =
                            when (event.key) {
                                Key.DirectionDown -> 1
                                Key.DirectionUp -> -1
                                else -> return@onPreviewKeyEvent false
                            }
                        val index = entries.indexOfFirst { it.key == ws.selectedIcon }
                        val next = (if (index < 0) 0 else index + step).coerceIn(0, entries.lastIndex)
                        ws.selectedIcon = entries[next].key
                        true
                    },
            ) {
                groups.forEach { (role, items) ->
                    item(key = "header|$role") { SubsectionLabel("$role · ${items.size}") }
                    items(items, key = { it.key }) { e ->
                        SelectableRow(
                            selected = e.key == ws.selectedIcon,
                            onClick = { ws.selectedIcon = e.key },
                            headline = { Text(e.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1) },
                            supporting = { Text(e.file.path, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
                            leading = { IconThumb(apk, e.file, 44.dp) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (e.role == "Launcher icon" && e.file.path == used) Tag("in use", Tone.Primary)
                                    Text(formatBytes(e.file.sizeBytes), style = MaterialTheme.typography.labelMedium)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconDetail(
    apk: Path,
    e: IconEntry,
    sameRole: List<IconEntry>,
    onZoom: () -> Unit,
    onCompare: () -> Unit,
    onSave: () -> Unit,
) {
    val f = e.file
    val warning = e.sizeWarning()
    val canZoom = f.drawable || f.format == "xml"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        DetailHeader(
            e.title,
            e.resource,
            leading = { IconBadge(Icons.Outlined.Image, Tone.Primary, 48.dp) },
            trailing = { CopyButton(f.path, "file path") },
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().height(220.dp).clickable(enabled = canZoom) { onZoom() },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IconPicture(apk, f.path, f.format, 180.dp, "${f.path} preview")
            }
        }
        if (warning != null) {
            Spacer(Modifier.height(8.dp))
            Tag(warning, Tone.Tertiary)
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onZoom, enabled = canZoom) {
                Icon(Icons.Outlined.ZoomIn, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Zoom")
            }
            if (sameRole.size > 1) {
                OutlinedButton(onClick = onCompare) {
                    Icon(Icons.Outlined.Layers, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Compare densities")
                }
            }
            OutlinedButton(onClick = onSave) {
                Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save…")
            }
        }
        if (f.format == "xml") XmlSource(apk, f.path)
        SubsectionLabel("Properties")
        KeyValueRow("Role", e.role)
        KeyValueRow("Resource", e.resource, mono = true)
        KeyValueRow("File", f.path, mono = true, copyable = true)
        KeyValueRow("Config", f.config.ifEmpty { "default" })
        KeyValueRow("Format", f.format, mono = true)
        KeyValueRow("Dimensions", f.width?.let { w -> f.height?.let { h -> "$w × $h" } })
        KeyValueRow("Size", formatBytes(f.sizeBytes))
    }
}

/** Saves the file's original bytes, under the name it has in the APK. */
private fun saveIcon(
    apk: Path,
    e: IconEntry,
) {
    val bytes = IconAssets.bytes(apk, e.file.path) ?: return
    saveWithDialog(e.file.path.substringAfterLast('/'), bytes)
}

/** A small preview in a list row. */
@Composable
private fun IconThumb(
    apk: Path,
    file: IconFileInfo,
    size: Dp,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.size(size),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            IconPicture(apk, file.path, file.format, size * 0.8f, file.path)
        }
    }
}

/**
 * One icon file drawn at [size]: a bitmap for PNG, WebP and JPG, and a converted vector for XML. A spinner shows
 * while it loads, and nothing is drawn if the file cannot be read.
 */
@Composable
private fun IconPicture(
    apk: Path,
    path: String,
    format: String,
    size: Dp,
    description: String,
) {
    // Every load ends with a result (drawn, or "cannot draw"), so nothing spins forever.
    if (format == "xml") {
        val result by produceState<Pair<Boolean, ImageVector?>>(false to null, apk, path) {
            value = true to withContext(Dispatchers.IO) { IconAssets.vector(apk, path) }
        }
        val (done, vector) = result
        when {
            !done -> Loading(size)
            vector != null -> Image(rememberVectorPainter(vector), description, Modifier.size(size), contentScale = ContentScale.Fit)
            else -> CannotDraw(size)
        }
        return
    }
    val result by produceState<Pair<Boolean, ImageBitmap?>>(false to null, apk, path) {
        value = true to withContext(Dispatchers.IO) { AppIconImage.load(apk, IconPick(path, format)) }
    }
    val (done, bitmap) = result
    when {
        !done -> Loading(size)
        bitmap != null ->
            Image(
                bitmap = bitmap,
                contentDescription = description,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size),
            )
        else -> CannotDraw(size)
    }
}

@Composable
private fun CannotDraw(size: Dp) {
    Text("XML", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** The XML source of a binary XML icon, shown in the details. */
@Composable
private fun XmlSource(
    apk: Path,
    path: String,
) {
    val text by produceState<String?>(null, apk, path) {
        value = withContext(Dispatchers.IO) { IconAssets.xmlText(apk, path) ?: "Could not decode this XML file." }
    }
    SubsectionLabel("XML")
    val loaded = text
    if (loaded != null) {
        CodeBlock(highlighted(loaded, Syntax.Xml), Modifier.fillMaxWidth())
    } else {
        Loading(24.dp)
    }
}

@Composable
private fun Loading(size: Dp) {
    CircularProgressIndicator(Modifier.size(size * 0.25f), strokeWidth = 2.dp)
}

/** A large view of one icon. Zoom with the buttons; close with the close button or Escape. */
@Composable
private fun ZoomDialog(
    apk: Path,
    file: IconFileInfo,
    onClose: () -> Unit,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(640.dp, 600.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(file.path.substringAfterLast('/'), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { zoom = (zoom / 1.5f).coerceAtLeast(0.5f) }) { Icon(Icons.Outlined.ZoomOut, "Zoom out") }
                    Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { zoom = (zoom * 1.5f).coerceAtMost(4f) }) { Icon(Icons.Outlined.ZoomIn, "Zoom in") }
                    TextButton(onClick = { zoom = 1f }) { Text("Reset") }
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close") }
                }
                Box(Modifier.fillMaxSize().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                    IconPicture(apk, file.path, file.format, 320.dp * zoom, file.path)
                }
            }
        }
    }
}

/** The same icon role at every density, side by side, at one size. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompareDialog(
    apk: Path,
    files: List<IconEntry>,
    role: String,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(24.dp).width(560.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Compare densities · $role", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close") }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    files.forEach { e ->
                        Column(Modifier.width(120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.size(104.dp),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    IconPicture(apk, e.file.path, e.file.format, 84.dp, e.file.path)
                                }
                            }
                            Text(e.file.config.ifEmpty { "default" }, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            Text(
                                e.file.width?.let { w -> e.file.height?.let { h -> "$w × $h" } } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * How the launcher shows the icon: a circle and a rounded square. Each shows the background and foreground layers of
 * an adaptive icon, or the plain launcher icon when the app has no adaptive one.
 */
@Composable
private fun LauncherPreviewDialog(
    apk: Path,
    adaptive: AdaptivePreview?,
    launcher: IconPick?,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Launcher preview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    LauncherShape(apk, adaptive, launcher, CircleShape, "Circle")
                    LauncherShape(apk, adaptive, launcher, RoundedCornerShape(28.dp), "Rounded square")
                }
            }
        }
    }
}

@Composable
private fun LauncherShape(
    apk: Path,
    adaptive: AdaptivePreview?,
    launcher: IconPick?,
    shape: Shape,
    label: String,
) {
    val background = adaptive?.backgroundColor?.let { IconAssets.colorOf(it) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(144.dp).clip(shape).background(background ?: MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (adaptive != null) {
                adaptive.background?.let { IconPicture(apk, it.path, it.format, 144.dp, it.path) }
                adaptive.foreground?.let { IconPicture(apk, it.path, it.format, 144.dp, it.path) }
            } else {
                launcher?.let { IconPicture(apk, it.path, it.format, 112.dp, it.path) }
            }
        }
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
    }
}
