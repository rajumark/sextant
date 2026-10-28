package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.copyToClipboard
import io.github.rajumark.sextant.desktop.model.AppComponent
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.ui.common.EmptyState
import io.github.rajumark.sextant.desktop.ui.common.IconBadge
import io.github.rajumark.sextant.desktop.ui.common.ListDetail
import io.github.rajumark.sextant.desktop.ui.common.LocalNotifier
import io.github.rajumark.sextant.desktop.ui.common.MonoLine
import io.github.rajumark.sextant.desktop.ui.common.Tag
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.common.colors
import io.github.rajumark.sextant.desktop.ui.workspace.ComponentPageState
import io.github.rajumark.sextant.desktop.ui.workspace.ComponentSort
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState

/** The verdicts worth a summary chip on each page, in order. Counts and meanings come from core. */
private fun summaryTags(type: ComponentType): List<String> =
    when (type) {
        ComponentType.Activity ->
            listOf(
                "launcher",
                "deep-link",
                "app-link",
                "hidden-entry",
                "task-hijack",
                "lock-screen",
                "exported-by-default",
            )
        ComponentType.ActivityAlias -> listOf("launcher", "deep-link", "app-link", "hidden-entry", "exported-by-default")
        ComponentType.Service -> listOf("unguarded", "sensitive-role", "system-role", "foreground", "isolated", "direct-boot")
        ComponentType.Receiver -> listOf("unguarded", "sensitive-role", "system-role", "boot", "direct-boot", "exported-by-default")
        ComponentType.Provider -> listOf("unguarded", "file-provider", "uri-grants", "direct-boot")
    }

private fun AppComponent.matches(q: String) =
    name.contains(q, ignoreCase = true) ||
        deepLinks.any { it.contains(q, ignoreCase = true) } ||
        authorities.any { it.contains(q, ignoreCase = true) } ||
        listOfNotNull(permission, readPermission, writePermission, process, facts.label).any { it.contains(q, ignoreCase = true) } ||
        tags.any { it.equals(q, ignoreCase = true) }

private fun ComponentPageState.accepts(c: AppComponent): Boolean =
    (exported == null || c.exported == exported) &&
        (!guardedOnly || c.permission != null || c.readPermission != null || c.writePermission != null) &&
        (!disabledOnly || !c.enabled) &&
        (!customLaunchModeOnly || c.attribute("launchMode").let { it != null && it != "standard" }) &&
        (!customProcessOnly || c.process != null) &&
        c.tags.containsAll(tags)

private fun sorted(
    list: List<AppComponent>,
    sort: ComponentSort,
): List<AppComponent> =
    when (sort) {
        ComponentSort.Relevance ->
            list.sortedWith(
                compareByDescending<AppComponent> { it.launcher }
                    .thenByDescending { it.exported }
                    .thenByDescending { it.deepLinks.isNotEmpty() }
                    .thenByDescending { it.intentFilters.isNotEmpty() }
                    .thenBy { it.simpleName.lowercase() },
            )
        ComponentSort.Risk -> list.sortedWith(compareByDescending<AppComponent> { riskScore(it) }.thenBy { it.simpleName.lowercase() })
        ComponentSort.Name -> list.sortedBy { it.simpleName.lowercase() }
        ComponentSort.Package -> list.sortedWith(compareBy<AppComponent> { it.packagePath }.thenBy { it.simpleName.lowercase() })
        ComponentSort.Filters ->
            list.sortedWith(
                compareByDescending<AppComponent> { it.intentFilters.size }.thenBy { it.simpleName.lowercase() },
            )
    }

/** A row in the list: a package header (when grouping) or a component. */
private sealed interface Line {
    val key: String

    data class Header(
        val pkg: String,
        val count: Int,
        val open: Boolean,
    ) : Line {
        override val key = "pkg|$pkg"
    }

    data class Item(
        val c: AppComponent,
        override val key: String,
    ) : Line
}

/**
 * One kind of manifest component (activities, aliases, services, receivers, providers) as its own page: verdict summary,
 * search and filters, then a keyboard-driven list on the left and the component's layered details on the right.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TypedComponentScreen(
    ws: WorkspaceState,
    type: ComponentType,
) {
    val page = ws.componentPage(type)
    val title = if (type == ComponentType.ActivityAlias) "Aliases" else type.plural
    val notify = LocalNotifier.current
    val windowInfo = LocalWindowInfo.current
    val base = remember(ws.analysis, type) { ws.analysis.components.filter { it.type == type } }
    val q = page.query.trim()
    val filtered =
        remember(
            base,
            q,
            page.exported,
            page.guardedOnly,
            page.disabledOnly,
            page.customLaunchModeOnly,
            page.customProcessOnly,
            page.tags,
            page.sort,
        ) {
            sorted(base.filter { (q.isEmpty() || it.matches(q)) && page.accepts(it) }, page.sort)
        }
    val lines =
        remember(filtered, page.groupByPackage, page.collapsed) {
            val keyed = withUniqueKeys(filtered) { it.name }
            if (!page.groupByPackage) {
                keyed.map { (k, c) -> Line.Item(c, k) }
            } else {
                keyed.groupBy { it.second.packagePath }.toSortedMap().flatMap { (pkg, rows) ->
                    val open = pkg !in page.collapsed
                    listOf(Line.Header(pkg, rows.size, open)) + if (open) rows.map { (k, c) -> Line.Item(c, k) } else emptyList()
                }
            }
        }
    val visible = lines.filterIsInstance<Line.Item>().map { it.c }
    val selected = base.firstOrNull { it.name == page.selected }
    val searchFocus = remember { FocusRequester() }
    val listFocus = remember { FocusRequester() }

    // Keep the selected row in view when it changes from the keyboard or another screen.
    LaunchedEffect(page.selected, lines) {
        val i = lines.indexOfFirst { it is Line.Item && it.c.name == page.selected }
        if (i >= 0 &&
            page.listState.layoutInfo.visibleItemsInfo
                .none { it.index == i && it.offset >= 0 }
        ) {
            page.listState.animateScrollToItem(i)
        }
    }

    fun select(c: AppComponent) {
        val mods = windowInfo.keyboardModifiers
        when {
            mods.isMetaPressed || mods.isCtrlPressed -> page.multi = if (c.name in page.multi) page.multi - c.name else page.multi + c.name
            mods.isShiftPressed && page.selected != null -> {
                val a = visible.indexOfFirst { it.name == page.selected }
                val b = visible.indexOf(c)
                if (a >= 0 && b >= 0) page.multi = page.multi + visible.subList(minOf(a, b), maxOf(a, b) + 1).map { it.name }
            }
            else -> page.selected = c.name
        }
        listFocus.requestFocus()
    }

    ListDetail(
        hasSelection = selected != null,
        onBack = { page.selected = null },
        initialDetailWidth = 520.dp,
        minListWidth = 560.dp,
        list = {
            Column(
                Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp).onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.F && (e.isMetaPressed || e.isCtrlPressed)) {
                        searchFocus.requestFocus()
                        true
                    } else {
                        false
                    }
                },
            ) {
                ListHeader(title, "${base.size} declared · ${base.count { it.exported }} exported · showing ${filtered.size}") {
                    SortMenu(page)
                    IconToggleButton(checked = page.groupByPackage, onCheckedChange = { page.groupByPackage = it }) {
                        Icon(Icons.Outlined.FolderOpen, if (page.groupByPackage) "Show as flat list" else "Group by package")
                    }
                }
                SummaryStrip(type, base, page)
                Spacer(Modifier.height(12.dp))
                SearchField(
                    page.query,
                    { page.query = it },
                    "Search class, link, authority, permission, process or tag   (${shortcutLabel("F")})",
                    Modifier.fillMaxWidth().focusRequester(searchFocus),
                )
                Spacer(Modifier.height(8.dp))
                FilterRow(type, base, page)
                if (page.multi.isNotEmpty()) MultiBar(page, base) { notify(it) }
                Spacer(Modifier.height(4.dp))
                if (filtered.isEmpty()) {
                    EmptyState(
                        Icons.Outlined.SearchOff,
                        if (base.isEmpty()) "No ${title.lowercase()} declared" else "No ${title.lowercase()} match",
                        if (base.isEmpty()) "This APK's manifest declares none." else "Clear filters or try another search.",
                    )
                } else {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .focusRequester(listFocus)
                            .focusable()
                            .onPreviewKeyEvent { e ->
                                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                val i = visible.indexOfFirst { it.name == page.selected }
                                when (e.key) {
                                    Key.DirectionDown ->
                                        visible
                                            .getOrNull(
                                                if (i <
                                                    0
                                                ) {
                                                    0
                                                } else {
                                                    (i + 1).coerceAtMost(visible.lastIndex)
                                                },
                                            )?.let { page.selected = it.name }
                                    Key.DirectionUp ->
                                        visible
                                            .getOrNull(
                                                if (i <
                                                    0
                                                ) {
                                                    0
                                                } else {
                                                    (i - 1).coerceAtLeast(0)
                                                },
                                            )?.let { page.selected = it.name }
                                    Key.MoveHome -> visible.firstOrNull()?.let { page.selected = it.name }
                                    Key.MoveEnd -> visible.lastOrNull()?.let { page.selected = it.name }
                                    Key.Escape -> if (page.multi.isNotEmpty()) page.multi = emptySet() else page.selected = null
                                    Key.Slash -> searchFocus.requestFocus()
                                    else -> return@onPreviewKeyEvent false
                                }
                                true
                            },
                        state = page.listState,
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(lines, key = { it.key }) { line ->
                            when (line) {
                                is Line.Header ->
                                    PackageHeader(line) {
                                        page.collapsed = if (line.open) page.collapsed + line.pkg else page.collapsed - line.pkg
                                    }
                                is Line.Item -> ComponentRow(ws, page, line.c, page.groupByPackage) { select(line.c) }
                            }
                        }
                    }
                }
            }
        },
        detail = {
            if (selected != null) {
                ComponentDetail(selected, ws)
            } else {
                EmptyState(
                    Icons.Outlined.TouchApp,
                    "Select ${if (type == ComponentType.ActivityAlias) "an alias" else "a " + type.label.lowercase()}",
                    "Its verdicts, attributes, entry points, adb commands and manifest entry appear here. Use ↑ ↓ to move.",
                )
            }
        },
    )
}

private fun shortcutLabel(key: String) = if (System.getProperty("os.name").orEmpty().startsWith("Mac")) "⌘$key" else "Ctrl+$key"

/** One chip per verdict with its count; clicking filters the list to it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryStrip(
    type: ComponentType,
    base: List<AppComponent>,
    page: ComponentPageState,
) {
    val counts = summaryTags(type).map { t -> t to base.count { t in it.tags } }.filter { it.second > 0 }
    if (counts.isEmpty()) return
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        counts.forEach { (t, n) ->
            val on = t in page.tags
            FilterChip(
                selected = on,
                onClick = { page.tags = if (on) page.tags - t else page.tags + t },
                label = { Text("$n  $t") },
                leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(tagTone(t).colors().content)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(
    type: ComponentType,
    base: List<AppComponent>,
    page: ComponentPageState,
) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ToggleChip("Exported ${base.count { it.exported }}", page.exported == true) { page.exported = if (it) true else null }
        ToggleChip("Not exported ${base.count { !it.exported }}", page.exported == false) { page.exported = if (it) false else null }
        val guarded = base.count { it.permission != null || it.readPermission != null || it.writePermission != null }
        if (guarded > 0) ToggleChip("Guarded $guarded", page.guardedOnly) { page.guardedOnly = it }
        val disabled = base.count { !it.enabled }
        if (disabled > 0) ToggleChip("Disabled $disabled", page.disabledOnly) { page.disabledOnly = it }
        if (type == ComponentType.Activity || type == ComponentType.ActivityAlias) {
            val modes = base.count { it.attribute("launchMode").let { m -> m != null && m != "standard" } }
            if (modes > 0) ToggleChip("Launch mode set $modes", page.customLaunchModeOnly) { page.customLaunchModeOnly = it }
        }
        val processes = base.count { it.process != null }
        if (processes > 0) ToggleChip("Own process $processes", page.customProcessOnly) { page.customProcessOnly = it }
        if (page.filtering || page.query.isNotEmpty()) {
            TextButton(onClick = {
                page.clearFilters()
                page.query = ""
            }) { Text("Clear all") }
        }
    }
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
        leadingIcon = if (selected) ({ Icon(Icons.Outlined.Check, null, Modifier.width(FilterChipDefaults.IconSize)) }) else null,
    )
}

@Composable
private fun SortMenu(page: ComponentPageState) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(page.sort.label)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ComponentSort.entries.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.label) },
                    leadingIcon = { if (s == page.sort) Icon(Icons.Outlined.Check, null) else Spacer(Modifier.size(24.dp)) },
                    onClick = {
                        page.sort = s
                        open = false
                    },
                )
            }
        }
    }
}

/** Shown while rows are picked with Cmd/Ctrl- or Shift-click: copy them for a report. */
@Composable
private fun MultiBar(
    page: ComponentPageState,
    base: List<AppComponent>,
    notify: (String) -> Unit,
) {
    val picked = base.filter { it.name in page.multi }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${picked.size} selected", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                copyToClipboard(Export.markdown(picked))
                notify("Copied ${picked.size} as a Markdown table")
            }) { Text("Markdown") }
            TextButton(onClick = {
                copyToClipboard(Export.csv(picked))
                notify("Copied ${picked.size} as CSV")
            }) { Text("CSV") }
            TextButton(onClick = {
                copyToClipboard(picked.joinToString("\n") { it.jsonLine })
                notify("Copied ${picked.size} as JSON lines")
            }) { Text("JSON") }
            TextButton(onClick = { page.multi = emptySet() }) { Text("Clear") }
        }
    }
}

/** Report-ready text for picked components. Columns match `sextant components`. */
internal object Export {
    private fun cols(c: AppComponent) =
        listOf(
            c.type.wire,
            c.name,
            if (!c.exported) {
                "no"
            } else if (c.exportedSource.wire == "default") {
                "default"
            } else {
                "yes"
            },
            c.tags.joinToString(","),
            c.deepLinks.firstOrNull() ?: c.authorities.firstOrNull() ?: c.facts.entryPoints
                .firstOrNull()
                .orEmpty(),
        )

    private val HEADER = listOf("type", "name", "exported", "tags", "entry")

    fun markdown(list: List<AppComponent>): String =
        (listOf(HEADER, HEADER.map { "---" }) + list.map(::cols)).joinToString("\n") { row ->
            row.joinToString(" | ", "| ", " |") { it.replace("|", "\\|") }
        }

    fun csv(list: List<AppComponent>): String =
        (listOf(HEADER) + list.map(::cols)).joinToString("\n") { row ->
            row.joinToString(",") { v -> if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v }
        }
}

@Composable
private fun PackageHeader(
    h: Line.Header,
    onToggle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (h.open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        MonoLine(h.pkg.ifEmpty { "(default package)" }, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Tag(h.count.toString())
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ComponentRow(
    ws: WorkspaceState,
    page: ComponentPageState,
    c: AppComponent,
    indented: Boolean,
    onClick: () -> Unit,
) {
    val notify = LocalNotifier.current
    ContextMenuArea(items = {
        buildList {
            add(ContextMenuItem("Copy class name") { copyToClipboard(c.name).also { notify("Copied class name") } })
            c.facts.adb.firstOrNull()?.let { (_, cmd) ->
                add(ContextMenuItem("Copy adb command") { copyToClipboard(cmd).also { notify("Copied adb command") } })
            }
            c.deepLinks.firstOrNull()?.let { l ->
                add(ContextMenuItem("Copy first deep link") { copyToClipboard(l).also { notify("Copied link") } })
            }
            add(ContextMenuItem("View in Manifest") { ws.showComponentInManifest(c.name) })
            c.targetActivity?.let { t -> add(ContextMenuItem("Open target activity") { ws.showComponent(t) }) }
            add(
                ContextMenuItem(if (c.name in page.multi) "Remove from selection" else "Add to selection") {
                    page.multi = if (c.name in page.multi) page.multi - c.name else page.multi + c.name
                },
            )
        }
    }) {
        TooltipArea(
            tooltip = {
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.widthIn(max = 480.dp),
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(c.name, style = MaterialTheme.typography.bodySmall)
                        c.attribute("launchMode")?.let { Text("launchMode $it", style = MaterialTheme.typography.bodySmall) }
                        c.tags.forEach { t -> Text("$t: ${c.facts.reasons[t].orEmpty()}", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
            delayMillis = 500,
        ) {
            SelectableRow(
                selected = c.name == page.selected || c.name in page.multi,
                onClick = onClick,
                inset = if (indented) 24.dp else 12.dp,
                headline = {
                    Text(
                        c.simpleName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (c.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    )
                },
                supporting = {
                    MonoLine(
                        c.deepLinks.firstOrNull()
                            ?: c.authorities.firstOrNull()?.let { "content://$it" }
                            ?: c.targetActivity?.let { "→ $it" }
                            ?: c.facts.entryPoints.firstOrNull()
                            ?: c.packagePath,
                    )
                },
                leading = { IconBadge(c.type.icon(), c.type.tone()) },
                trailing = { RowTags(c) },
            )
        }
    }
}

/** Up to three verdicts on the row (the riskiest first), then a count of the rest; sample data falls back to exported. */
@Composable
private fun RowTags(c: AppComponent) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        val tags =
            c.tags.sortedBy {
                when (tagTone(it)) {
                    Tone.Error -> 0
                    Tone.Tertiary -> 1
                    Tone.Primary -> 2
                    Tone.Secondary -> 3
                    else -> 4
                }
            }
        tags.take(3).forEach { Tag(it, tagTone(it), mono = true) }
        if (tags.size > 3) Tag("+${tags.size - 3}")
        if (tags.isEmpty() && c.exported) Tag("exported", Tone.Tertiary)
    }
}
