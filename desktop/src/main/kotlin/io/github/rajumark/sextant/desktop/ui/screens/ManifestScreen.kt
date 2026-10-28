package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.data.ManifestFormat
import io.github.rajumark.sextant.desktop.model.ManifestNode
import io.github.rajumark.sextant.desktop.theme.codeTextStyle
import io.github.rajumark.sextant.desktop.ui.common.CodeView
import io.github.rajumark.sextant.desktop.ui.common.CopyTextButton
import io.github.rajumark.sextant.desktop.ui.common.EmptyState
import io.github.rajumark.sextant.desktop.ui.common.KeyValueRow
import io.github.rajumark.sextant.desktop.ui.common.ListDetail
import io.github.rajumark.sextant.desktop.ui.common.SubsectionLabel
import io.github.rajumark.sextant.desktop.ui.common.Syntax
import io.github.rajumark.sextant.desktop.ui.common.Tag
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.common.rememberHighlighted
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import io.github.rajumark.sextant.desktop.ui.workspace.ManifestView
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState

private data class TreeRow(
    val path: String,
    val depth: Int,
    val node: ManifestNode,
)

private fun ManifestNode.at(path: String): ManifestNode? {
    if (path.isEmpty()) return this
    var n: ManifestNode = this
    for (i in path.split('/')) n = n.children.getOrNull(i.toInt()) ?: return null
    return n
}

private fun ManifestNode.matches(q: String): Boolean =
    tag.contains(q, true) || attributes.any { it.first.contains(q, true) || it.second.contains(q, true) }

/** Flattens the visible part of the tree. With a query, shows matches and their ancestors. */
private fun flatten(
    root: ManifestNode,
    expanded: Set<String>,
    query: String,
): List<TreeRow> {
    val out = mutableListOf<TreeRow>()

    fun hasMatch(n: ManifestNode): Boolean = n.matches(query) || n.children.any(::hasMatch)

    fun walk(
        n: ManifestNode,
        path: String,
        depth: Int,
    ) {
        if (query.isNotEmpty() && !hasMatch(n)) return
        out += TreeRow(path, depth, n)
        if (query.isNotEmpty() || path in expanded) {
            n.children.forEachIndexed { i, c -> walk(c, if (path.isEmpty()) "$i" else "$path/$i", depth + 1) }
        }
    }
    walk(root, "", 0)
    return out
}

@Composable
fun ManifestScreen(ws: WorkspaceState) {
    val root = ws.analysis.manifest
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)) {
            ListHeader("Manifest", "Decoded AndroidManifest.xml · written to manifest.json") {
                SingleChoiceSegmentedButtonRow {
                    ManifestView.entries.forEachIndexed { i, v ->
                        SegmentedButton(
                            selected = ws.manifestView == v,
                            onClick = { ws.manifestView = v },
                            shape = SegmentedButtonDefaults.itemShape(i, ManifestView.entries.size),
                            label = { Text(v.label) },
                        )
                    }
                }
            }
            if (ws.manifestView == ManifestView.Tree) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val q = ws.manifestQuery.trim()
                    val matches =
                        remember(root, q) { if (q.isEmpty()) emptyList() else flatten(root, emptySet(), q).filter { it.node.matches(q) } }

                    // Enter / Shift+Enter walk through the matches, selecting each one.
                    fun step(by: Int) {
                        if (matches.isEmpty()) return
                        val i = matches.indexOfFirst { it.path == ws.selectedNode }
                        ws.selectedNode = matches[if (i < 0) 0 else (i + by).mod(matches.size)].path
                    }
                    SearchField(
                        ws.manifestQuery,
                        { ws.manifestQuery = it },
                        "Filter tags, attributes and values · Enter jumps to the next match",
                        Modifier.weight(1f).onPreviewKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                                step(if (e.isShiftPressed) -1 else 1)
                                true
                            } else {
                                false
                            }
                        },
                    )
                    if (q.isNotEmpty()) {
                        Spacer(Modifier.width(12.dp))
                        val at = matches.indexOfFirst { it.path == ws.selectedNode }
                        Text(
                            when {
                                matches.isEmpty() -> "No matches"
                                at >= 0 -> "${at + 1} of ${matches.size}"
                                else -> "${matches.size} matches"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { step(-1) }, enabled = matches.isNotEmpty()) {
                            Icon(Icons.Outlined.KeyboardArrowUp, "Previous match")
                        }
                        IconButton(onClick = { step(1) }, enabled = matches.isNotEmpty()) {
                            Icon(Icons.Outlined.KeyboardArrowDown, "Next match")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { ws.expanded = WorkspaceState.allPaths(root) }) { Icon(Icons.Outlined.UnfoldMore, "Expand all") }
                    IconButton(onClick = { ws.expanded = setOf("") }) { Icon(Icons.Outlined.UnfoldLess, "Collapse all") }
                }
            }
        }
        when (ws.manifestView) {
            ManifestView.Tree -> ManifestTree(ws, root)
            ManifestView.Xml -> {
                val text = remember(root) { ManifestFormat.xml(root) }
                CodeView(rememberHighlighted(text, Syntax.Xml), Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 24.dp))
            }
            ManifestView.Json -> {
                val text = remember(root) { ManifestFormat.json(root) }
                CodeView(rememberHighlighted(text, Syntax.Json), Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 24.dp))
            }
        }
    }
}

@Composable
private fun ManifestTree(
    ws: WorkspaceState,
    root: ManifestNode,
) {
    val q = ws.manifestQuery.trim()
    val rows = remember(root, ws.expanded, q) { flatten(root, ws.expanded, q) }
    val selected = ws.selectedNode?.let { root.at(it) }
    val listState = rememberLazyListState()
    LaunchedEffect(ws.selectedNode, rows) {
        val i = rows.indexOfFirst { it.path == ws.selectedNode }
        if (i >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == i }) listState.animateScrollToItem(i)
    }
    ListDetail(
        hasSelection = selected != null,
        onBack = { ws.selectedNode = null },
        initialDetailWidth = 420.dp,
        list = {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            ) {
                items(rows, key = { it.path }) { row ->
                    TreeLine(
                        row = row,
                        expanded = q.isNotEmpty() || row.path in ws.expanded,
                        selected = row.path == ws.selectedNode,
                        highlight = q.isNotEmpty() && row.node.matches(q),
                        onToggle = { ws.expanded = if (row.path in ws.expanded) ws.expanded - row.path else ws.expanded + row.path },
                        onSelect = { ws.selectedNode = row.path },
                    )
                }
            }
        },
        detail = {
            if (selected == null) {
                EmptyState(
                    Icons.Outlined.TouchApp,
                    "Select an element",
                    "All attributes of the element appear here, with resource references kept as written.",
                )
            } else {
                NodeDetail(ws, selected)
            }
        },
    )
}

@Composable
private fun TreeLine(
    row: TreeRow,
    expanded: Boolean,
    selected: Boolean,
    highlight: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val n = row.node
    val bg =
        when {
            selected -> c.secondaryContainer
            highlight -> c.tertiaryContainer.copy(alpha = 0.5f)
            else -> c.surface
        }
    Row(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(MaterialTheme.shapes.small)
            .background(bg)
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(row.depth) {
            Box(Modifier.width(20.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(c.outlineVariant))
            }
        }
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            if (n.children.isNotEmpty()) {
                IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowDown else Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        val name = n.attr("android:name") ?: n.attr("package")
        val label =
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.primary)) { append("<${n.tag}") }
                if (name != null) {
                    append(" ")
                    withStyle(SpanStyle(color = c.tertiary)) { append(name) }
                }
                val rest = n.attributes.filter { it.first != "android:name" && it.first != "package" }
                rest.take(2).forEach { (k, v) ->
                    append(" ")
                    withStyle(SpanStyle(color = c.secondary)) { append(k.removePrefix("android:")) }
                    withStyle(SpanStyle(color = c.onSurfaceVariant)) { append("=$v") }
                }
                if (rest.size > 2) withStyle(SpanStyle(color = c.outline)) { append(" …") }
                withStyle(SpanStyle(color = c.primary)) { append(if (n.children.isEmpty()) " />" else ">") }
            }
        Text(label, style = codeTextStyle(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (n.children.isNotEmpty() && !expanded) {
            Tag("${n.children.size}", mono = true, modifier = Modifier.padding(end = 8.dp))
        }
    }
}

@Composable
private fun NodeDetail(
    ws: WorkspaceState,
    n: ManifestNode,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        DetailHeader(
            "<${n.tag}>",
            "${n.attributes.size} attributes · ${n.children.size} children",
            leading = { Tag(n.tag, Tone.Primary, mono = true) },
        )
        val open = pageFor(ws, n)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (open != null) FilledTonalButton(onClick = open.second) { Text(open.first) }
            CopyTextButton("Copy as XML", "<${n.tag}> as XML") { ManifestFormat.xml(n) }
        }
        SubsectionLabel("Attributes")
        if (n.attributes.isEmpty()) {
            Text("No attributes.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        n.attributes.forEach { (k, v) -> KeyValueRow(k, v, mono = true, copyable = true, keyWidth = 190.dp) }
        if (n.children.isNotEmpty()) {
            SubsectionLabel("Children")
            n.children.groupingBy { it.tag }.eachCount().forEach { (tag, count) ->
                KeyValueRow("<$tag>", count.toString(), mono = true, keyWidth = 190.dp)
            }
        }
    }
}

/** The page that explains this element in depth, when there is one: label and how to get there. */
private fun pageFor(
    ws: WorkspaceState,
    n: ManifestNode,
): Pair<String, () -> Unit>? {
    val name = n.attr("android:name")
    return when (n.tag) {
        "activity", "activity-alias", "service", "receiver", "provider" -> {
            val pkg = ws.analysis.meta.packageName
            val full = if (name != null && name.startsWith(".")) pkg + name else name
            val c =
                ws.analysis.components.firstOrNull { it.name == full }
                    ?: ws.analysis.components.firstOrNull { name != null && it.name.endsWith(".$name") }
            c?.let { "Open ${it.type.label.lowercase()} page" to { ws.showComponent(it.name) } }
        }
        "uses-permission", "uses-permission-sdk-23", "permission" ->
            name?.takeIf { p -> ws.analysis.permissions.any { it.name == p } }?.let { "Open permission page" to { ws.showPermission(it) } }
        "application" -> "Open Application page" to { ws.destination = Destination.Application }
        else -> null
    }
}
