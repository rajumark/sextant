package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.core.insights.ResourceSource
import io.github.rajumark.sextant.core.insights.SdkEntry
import io.github.rajumark.sextant.core.insights.SourceBlock
import io.github.rajumark.sextant.core.insights.StringKey
import io.github.rajumark.sextant.core.model.AssetRecord
import io.github.rajumark.sextant.core.model.DrawableRecord
import io.github.rajumark.sextant.core.model.LayoutRecord
import io.github.rajumark.sextant.core.model.MenuRecord
import io.github.rajumark.sextant.core.model.NavigationRecord
import io.github.rajumark.sextant.core.model.ResValue
import io.github.rajumark.sextant.core.model.ResourceIdRecord
import io.github.rajumark.sextant.core.model.ResourceRef
import io.github.rajumark.sextant.core.model.StyleRecord
import io.github.rajumark.sextant.core.model.XmlConfigRecord
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
import io.github.rajumark.sextant.desktop.ui.common.WebActions
import io.github.rajumark.sextant.desktop.ui.common.formatBytes
import io.github.rajumark.sextant.desktop.ui.common.highlighted
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import io.github.rajumark.sextant.desktop.ui.workspace.ResourcePageState
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState

/** All configurations of one named resource (layout, menu, style, drawable, XML config). */
class ResGroup<T>(
    val name: String,
    val records: List<T>,
    val configs: List<String>,
) {
    /** The default configuration, else the first one. */
    val main: T get() = records.first()
}

private fun <T> grouped(
    records: List<T>,
    name: (T) -> String,
    config: (T) -> String,
): List<ResGroup<T>> =
    records.groupBy(name).map { (n, rs) ->
        val sorted = rs.sortedWith(compareBy<T> { config(it).isNotEmpty() }.thenBy { config(it) })
        ResGroup(n, sorted, sorted.map { config(it).ifEmpty { "default" } })
    }

/** How one resource page lists, filters and explains its entries. Everything shown is prepared by core. */
private class ResSpec<T>(
    val title: String,
    val noun: String,
    val icon: ImageVector,
    val items: List<T>,
    val key: (T) -> String,
    val headline: (T) -> String,
    val supporting: (T) -> String,
    val tags: (T) -> List<Pair<String, Tone>> = { emptyList() },
    val searchText: (T) -> String,
    /** Filter chips: label → predicate. Chips with no matches are hidden. */
    val filters: List<Pair<String, (T) -> Boolean>> = emptyList(),
    /** The `@type/name` refs that point at an entry, for "Used by". */
    val refs: (T) -> List<String> = { emptyList() },
    val detail: @Composable (T) -> Unit,
    /** The entry as written: its own file or its lines in res/values, one block per configuration (prepared by core). */
    val source: (T) -> List<SourceBlock> = { emptyList() },
    val webQuery: (T) -> Pair<String, String>,
    /** Replaces the default "Manifest entry" block (resources referenced from the manifest). */
    val manifestEntry: (@Composable (T) -> Unit)? = null,
)

@Composable
fun ResourceScreen(
    ws: WorkspaceState,
    dest: Destination,
) {
    val a = ws.analysis
    val r = a.resources
    val page = ws.resourcePage(dest)
    when (dest) {
        Destination.Strings -> ResourcePage(ws, page, stringsSpec(ws, page, a.stringKeys))
        Destination.Layouts -> ResourcePage(ws, page, layoutsSpec(grouped(r.layouts, { it.name }, { it.config })))
        Destination.Menus -> ResourcePage(ws, page, menusSpec(grouped(r.menus, { it.name }, { it.config })))
        Destination.Navigation -> ResourcePage(ws, page, navigationSpec(r.navigation))
        Destination.Styles -> ResourcePage(ws, page, stylesSpec(grouped(r.styles, { it.name }, { it.config })))
        Destination.Drawables -> ResourcePage(ws, page, drawablesSpec(grouped(r.drawables, { "${it.type}/${it.name}" }, { it.config })))
        Destination.ResourceIds -> ResourcePage(ws, page, idsSpec(r.ids))
        Destination.Assets -> ResourcePage(ws, page, assetsSpec(r.assets))
        Destination.XmlConfigs -> ResourcePage(ws, page, xmlSpec(grouped(r.xml, { it.name }, { it.config })))
        Destination.Sdks -> ResourcePage(ws, page, sdkSpec(ws))
        else -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ResourcePage(
    ws: WorkspaceState,
    page: ResourcePageState,
    spec: ResSpec<T>,
) {
    val icon = ws.destination.icon
    val q = page.query.trim()
    val active = spec.filters.filter { it.first in page.filters }
    val shown =
        remember(spec.items, q, page.filters) {
            spec.items.filter { item ->
                (q.isEmpty() || spec.searchText(item).contains(q, ignoreCase = true)) &&
                    active.all { it.second(item) }
            }
        }
    val selected = spec.items.firstOrNull { spec.key(it) == page.selected }
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(page.selected, shown) {
        val i = shown.indexOfFirst { spec.key(it) == page.selected }
        if (i >= 0 &&
            page.listState.layoutInfo.visibleItemsInfo
                .none { it.index == i }
        ) {
            page.listState.animateScrollToItem(i)
        }
    }
    ListDetail(
        hasSelection = selected != null,
        onBack = { page.selected = null },
        initialDetailWidth = 540.dp,
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
                ListHeader(spec.title, "${spec.items.size} ${spec.noun} · showing ${shown.size}") {
                    if (spec.title == "Strings") LocaleMenu(ws, page)
                }
                Spacer(Modifier.height(12.dp))
                SearchField(page.query, { page.query = it }, "Search ${spec.noun}", Modifier.fillMaxWidth().focusRequester(searchFocus))
                val counts = spec.filters.map { (label, p) -> Triple(label, p, spec.items.count(p)) }.filter { it.third > 0 }
                if (counts.isNotEmpty()) {
                    FlowRow(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        counts.forEach { (label, _, n) ->
                            val on = label in page.filters
                            FilterChip(
                                selected = on,
                                onClick = { page.filters = if (on) page.filters - label else page.filters + label },
                                label = { Text("$label $n") },
                                leadingIcon =
                                    if (on) {
                                        (
                                            {
                                                Icon(
                                                    Icons.Outlined.Check,
                                                    null,
                                                    Modifier.width(FilterChipDefaults.IconSize),
                                                )
                                            }
                                        )
                                    } else {
                                        null
                                    },
                            )
                        }
                        if (page.filters.isNotEmpty() || page.query.isNotEmpty()) {
                            TextButton(onClick = {
                                page.filters = emptySet()
                                page.query = ""
                            }) { Text("Clear all") }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (shown.isEmpty()) {
                    EmptyState(
                        Icons.Outlined.SearchOff,
                        if (spec.items.isEmpty()) "No ${spec.noun}" else "No ${spec.noun} match",
                        if (spec.items.isEmpty()) {
                            "This APK has none, or it was opened from a folder made before schema 8."
                        } else {
                            "Clear filters or try another search."
                        },
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().focusable().onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            val i = shown.indexOfFirst { spec.key(it) == page.selected }
                            when (e.key) {
                                Key.DirectionDown ->
                                    shown.getOrNull((i + 1).coerceAtMost(shown.lastIndex))?.let {
                                        page.selected =
                                            spec.key(it)
                                    }
                                Key.DirectionUp -> shown.getOrNull((i - 1).coerceAtLeast(0))?.let { page.selected = spec.key(it) }
                                Key.Escape -> page.selected = null
                                Key.Slash -> searchFocus.requestFocus()
                                else -> return@onPreviewKeyEvent false
                            }
                            true
                        },
                        state = page.listState,
                    ) {
                        items(withUniqueKeys(shown, spec.key), key = { it.first }) { (_, item) ->
                            SelectableRow(
                                selected = spec.key(item) == page.selected,
                                onClick = { page.selected = spec.key(item) },
                                headline = { Text(spec.headline(item), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supporting = { MonoLine(spec.supporting(item)) },
                                leading = { IconBadge(icon, Tone.Secondary) },
                                trailing = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        spec.tags(item).take(3).forEach { (t, tone) -> Tag(t, tone) }
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
                    "Select an entry",
                    "Its values, where it is used and its declaration appear here. Use ↑ ↓ to move.",
                )
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                    DetailHeader(
                        spec.headline(selected),
                        spec.title,
                        leading = { IconBadge(icon, Tone.Secondary, 48.dp) },
                        trailing = { CopyButton(spec.key(selected), "name") },
                    )
                    spec.detail(selected)
                    if (spec.refs(selected).isNotEmpty()) UsedBy(ws, a = ws.analysis.resources.refs, refs = spec.refs(selected))
                    val custom = spec.manifestEntry
                    if (custom != null) {
                        SubsectionLabel("Manifest entry")
                        custom(selected)
                    } else {
                        val manifestUses =
                            ws.analysis.resources.refs
                                .filter { it.fromKind == "manifest" && it.ref in spec.refs(selected) }
                        if (manifestUses.isNotEmpty()) {
                            SubsectionLabel("In AndroidManifest.xml · ${manifestUses.size}")
                            manifestUses.forEach { KeyValueRow(it.attribute.substringAfter(':'), it.from, mono = true, keyWidth = 240.dp) }
                        }
                    }
                    val key = spec.key(selected)
                    SourceSection(remember(spec.items, key) { spec.source(selected) }, key)
                    val (search, question) = spec.webQuery(selected)
                    WebActions(searchQuery = search, question = question)
                }
            }
        },
    )
}

/**
 * The entry as the developer wrote it: its own file (menu, layout, drawable XML) or its lines in res/values
 * (strings, styles, ids). One chip per configuration when there are several; the path inside the APK is shown
 * when shrinking renamed the file.
 */
@Composable
private fun SourceSection(
    blocks: List<SourceBlock>,
    key: String,
) {
    if (blocks.isEmpty()) return
    var pick by remember(key) { mutableStateOf(0) }
    val b = blocks[pick.coerceIn(blocks.indices)]
    SubsectionLabel(if (blocks.size > 1) "Source · ${blocks.size} configurations" else "Source")
    if (blocks.size > 1) {
        FlowRow(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            blocks.forEachIndexed { i, x ->
                FilterChip(selected = i == pick, onClick = { pick = i }, label = { Text(x.config.ifEmpty { "default" }) })
            }
        }
    }
    MonoLine(b.file + if (b.inApk != b.file) "  ·  in APK: ${b.inApk}" else "")
    Spacer(Modifier.height(6.dp))
    val text = b.text
    if (text == null) {
        Text(b.note.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        val syntax =
            when (b.language) {
                "xml" -> Syntax.Xml
                "json" -> Syntax.Json
                else -> Syntax.Plain
            }
        Row(verticalAlignment = Alignment.Top) {
            CodeBlock(highlighted(text, syntax), Modifier.weight(1f))
            CopyButton(text, "source")
        }
    }
}

/** Where a resource is referenced; each entry opens that resource's page when it has one. */
@Composable
private fun UsedBy(
    ws: WorkspaceState,
    a: List<ResourceRef>,
    refs: List<String>,
) {
    val users = a.filter { it.ref in refs && it.fromKind != "manifest" }
    SubsectionLabel("Used by · ${users.size}")
    if (users.isEmpty()) {
        Text(
            "Nothing in resources references it. Code can still use it through its resource id.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    users.take(200).forEach { u ->
        val target =
            when (u.fromKind) {
                "layout" -> Destination.Layouts
                "menu" -> Destination.Menus
                "navigation" -> Destination.Navigation
                "xml" -> Destination.XmlConfigs
                "style" -> Destination.Styles
                else -> null
            }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Tag(u.fromKind, Tone.Secondary)
            Spacer(Modifier.width(8.dp))
            MonoLine(u.from.substringAfter('/'), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            MonoLine(u.attribute)
            if (target != null) TextButton(onClick = { ws.showResource(target, u.from.substringAfter('/')) }) { Text("Open") }
        }
    }
    if (users.size > 200) Text("… ${users.size - 200} more", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun LocaleMenu(
    ws: WorkspaceState,
    page: ResourcePageState,
) {
    val locales =
        remember(ws.analysis) {
            ws.analysis.stringKeys
                .flatMap { it.locales }
                .distinct()
                .sorted()
        }
    if (locales.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.Outlined.Language, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(page.locale ?: "Default")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (listOf<String?>(null) + locales).forEach { l ->
                DropdownMenuItem(text = { Text(l ?: "Default") }, onClick = {
                    page.locale = l
                    open = false
                })
            }
        }
    }
}

@Composable
private fun ValueRow(v: ResValue) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        MonoLine(v.name.substringAfter(':'), modifier = Modifier.width(150.dp))
        Column(Modifier.weight(1f)) {
            Text(v.resolved ?: v.raw, style = MaterialTheme.typography.bodyMedium)
            if (v.resolved != null) MonoLine(v.raw)
        }
    }
}

// ---- Strings: the page AppSec, release and privacy reviewers search most, so it leads with meaning, not files.

private val FLAG_LABELS =
    linkedMapOf(
        "Used in UI" to "used",
        "Not referenced" to "unused",
        "URLs" to "url",
        "Emails" to "email",
        "Looks like a key" to "secret-like",
        "Format args" to "format",
        "HTML" to "html",
        "Missing translations" to "untranslated",
        "Empty" to "empty",
    )

private fun flagTone(f: String) =
    when (f) {
        "secret-like" -> Tone.Error
        "url", "email" -> Tone.Tertiary
        "untranslated", "empty" -> Tone.Neutral
        else -> Tone.Secondary
    }

private fun stringsSpec(
    ws: WorkspaceState,
    page: ResourcePageState,
    keys: List<StringKey>,
): ResSpec<StringKey> {
    val configOf =
        page.locale?.let { l ->
            keys.flatMap { k -> k.values.keys }.firstOrNull {
                ws.analysis.resources.strings.any { s ->
                    s.config ==
                        it &&
                        s.locale == l
                }
            }
        }

    fun shownValue(k: StringKey) = configOf?.let { k.values[it] } ?: k.value
    return ResSpec(
        title = "Strings",
        noun = "string keys",
        icon = Icons.Outlined.Language,
        items = keys,
        key = { it.name },
        headline = { shownValue(it).ifBlank { "(empty)" } },
        supporting = { it.name + if (it.kind != "string") " · ${it.kind}" else "" },
        tags = { k -> k.flags.filter { it != "used" && it != "unused" }.map { it to flagTone(it) } },
        searchText = { k -> k.name + "\n" + k.values.values.joinToString("\n") },
        filters =
            FLAG_LABELS.map { (label, flag) -> label to { k: StringKey -> flag in k.flags } } +
                listOf("Plurals" to { k: StringKey -> k.kind == "plurals" }, "Arrays" to { k: StringKey -> k.kind == "array" }),
        refs = { listOf("@string/${it.name}", "@plurals/${it.name}", "@array/${it.name}") },
        detail = { k ->
            MonoLine("@${if (k.kind == "string") "string" else k.kind}/${k.name}", color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SelectionContainer {
                    Text(
                        k.value.ifBlank { "(empty)" },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            if (k.flags.isNotEmpty()) {
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) { k.flags.forEach { Tag(it, flagTone(it)) } }
            }
            SubsectionLabel("Values · ${k.values.size} configurations")
            k.values.forEach { (config, text) -> KeyValueRow(config.ifEmpty { "default" }, text, keyWidth = 110.dp) }
            if (k.missingLocales.isNotEmpty()) {
                SubsectionLabel("Missing in ${k.missingLocales.size} app locales")
                Text(
                    k.missingLocales.joinToString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (k.kind != "string") {
                SubsectionLabel(if (k.kind == "plurals") "Quantities (default)" else "Items (default)")
                ws.analysis.resources.strings
                    .filter { it.name == k.name && it.config == "" }
                    .forEach { KeyValueRow(it.item.orEmpty(), it.value, keyWidth = 110.dp) }
            }
        },
        source = { k ->
            ResourceSource.strings(
                ws.analysis.resources.strings
                    .filter { it.name == k.name },
            )
        },
        webQuery = { k ->
            "\"${k.value.take(80)}\" android" to
                "In the Android app ${ws.analysis.meta.packageName}, the string resource ${k.name} reads: \"${k.value.take(
                    300,
                )}\". Where is it likely shown, and does it reveal anything a reviewer should check?"
        },
    )
}

// ---- Layouts, menus, navigation.

private fun layoutsSpec(groups: List<ResGroup<LayoutRecord>>) =
    ResSpec(
        title = "Layouts",
        noun = "layouts",
        icon = Icons.Outlined.TouchApp,
        items = groups,
        key = { it.name },
        headline = { it.name },
        supporting = { g ->
            "${g.main.root ?: "?"} · ${g.main.viewCount} views" +
                if (g.configs.size > 1) " · ${g.configs.joinToString()}" else ""
        },
        tags = { g ->
            buildList {
                if (g.main.customViews.isNotEmpty()) add("${g.main.customViews.size} custom" to Tone.Secondary)
                if (g.main.views.any { "WebView" in it.tag }) add("WebView" to Tone.Tertiary)
                if (g.main.fragments.isNotEmpty()) add("fragments" to Tone.Neutral)
            }
        },
        searchText = { g ->
            g.name + " " +
                g.main.views.joinToString(" ") { v ->
                    v.tag + " " + v.id + " " +
                        v.attributes.joinToString(" ") { "${it.raw} ${it.resolved}" }
                }
        },
        filters =
            listOf(
                "Custom views" to { g: ResGroup<LayoutRecord> -> g.main.customViews.isNotEmpty() },
                "WebView" to { g -> g.main.views.any { "WebView" in it.tag } },
                "Text input" to
                    { g -> g.main.views.any { "EditText" in it.tag || it.attributes.any { a -> a.name.endsWith("inputType") } } },
                "Passwords" to
                    { g -> g.main.views.any { v -> v.attributes.any { a -> a.name.endsWith("inputType") && "Password" in a.raw } } },
                "Fragments" to { g -> g.main.fragments.isNotEmpty() },
                "Several configs" to { g -> g.configs.size > 1 },
            ),
        refs = { listOf("@layout/${it.name}") },
        detail = { g ->
            val l = g.main
            KeyValueRow("Configurations", g.configs.joinToString())
            KeyValueRow("File", l.path, mono = true)
            KeyValueRow("Root", l.root, mono = true)
            if (l.customViews.isNotEmpty()) KeyValueRow("Custom views", l.customViews.joinToString("\n"), mono = true)
            if (l.fragments.isNotEmpty()) KeyValueRow("Fragments", l.fragments.joinToString("\n"), mono = true)
            if (l.includes.isNotEmpty()) KeyValueRow("Includes", l.includes.joinToString("\n"), mono = true)
            SubsectionLabel("View tree · ${l.viewCount}")
            l.views.take(400).forEach { v ->
                Column(Modifier.padding(start = (v.depth * 14).dp, top = 2.dp, bottom = 2.dp)) {
                    Text(
                        v.tag.substringAfterLast('.') + (
                            v.id?.let {
                                "  #${it.substringAfter('/')}"
                            } ?: ""
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily),
                    )
                    v.attributes
                        .filter {
                            it.name.substringAfter(':') in
                                setOf("text", "hint", "contentDescription", "title", "onClick", "inputType")
                        }.forEach { a ->
                            Text(
                                "${a.name.substringAfter(':')} = " + (a.resolved?.let { "\"$it\"  (${a.raw})" } ?: a.raw),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                }
            }
        },
        source = { ResourceSource.layouts(it.records) },
        webQuery = { g ->
            "android layout ${g.name}" to
                "What does an Android layout named ${g.name} with root ${g.main.root} and views ${g.main.views.take(
                    20,
                ).joinToString { it.tag.substringAfterLast('.') }} likely show?"
        },
    )

private fun menusSpec(groups: List<ResGroup<MenuRecord>>) =
    ResSpec(
        title = "Menus",
        noun = "menus",
        icon = Icons.Outlined.TouchApp,
        items = groups,
        key = { it.name },
        headline = { it.name },
        supporting = { g ->
            g.main.items
                .mapNotNull {
                    it.title?.let { t ->
                        t.resolved ?: t.raw
                    }
                }.joinToString(" · ")
                .ifEmpty { "${g.main.items.size} items" }
        },
        tags = { g -> listOf("${g.main.items.count { it.tag == "item" }} items" to Tone.Neutral) },
        searchText = { g -> g.name + " " + g.main.items.joinToString(" ") { "${it.id} ${it.title?.raw} ${it.title?.resolved}" } },
        filters =
            listOf(
                "Action views" to { g: ResGroup<MenuRecord> -> g.main.items.any { it.actionViewClass != null } },
                "Sub-menus" to { g -> g.main.items.any { it.tag == "menu" } },
            ),
        refs = { listOf("@menu/${it.name}") },
        detail = { g ->
            KeyValueRow("Configurations", g.configs.joinToString())
            KeyValueRow("File", g.main.path, mono = true)
            SubsectionLabel("Items")
            g.main.items.forEach { i ->
                Column(Modifier.padding(start = ((i.depth - 1).coerceAtLeast(0) * 16).dp, top = 4.dp, bottom = 4.dp)) {
                    Text(i.title?.let { it.resolved ?: it.raw } ?: "(${i.tag})", style = MaterialTheme.typography.bodyLarge)
                    MonoLine(
                        listOfNotNull(
                            i.id,
                            i.title?.raw?.takeIf {
                                i.title?.resolved != null
                            },
                            i.showAsAction?.let { "showAsAction=$it" },
                            i.actionViewClass,
                        ).joinToString("  ·  "),
                    )
                }
            }
        },
        source = { ResourceSource.menus(it.records) },
        webQuery = { g ->
            "android menu ${g.name}" to
                "An Android app menu ${g.name} has items: ${g.main.items.mapNotNull {
                    it.title?.resolved
                }.joinToString()}. What screen is it likely on?"
        },
    )

private fun navigationSpec(graphs: List<NavigationRecord>) =
    ResSpec(
        title = "Navigation",
        noun = "navigation graphs",
        icon = Icons.Outlined.TouchApp,
        items = graphs,
        key = { it.name },
        headline = { it.name },
        supporting = { n -> "start ${n.startDestination ?: "?"} · ${n.destinations.size} destinations" },
        tags = { n ->
            listOfNotNull(
                n.destinations
                    .sumOf { it.deepLinks.size }
                    .takeIf { it > 0 }
                    ?.let { "$it deep links" to Tone.Tertiary },
            )
        },
        searchText = { n ->
            n.name + " " +
                n.destinations.joinToString(" ") { "${it.id} ${it.className} ${it.label?.resolved} ${it.deepLinks}" }
        },
        filters = listOf("Deep links" to { n: NavigationRecord -> n.destinations.any { it.deepLinks.isNotEmpty() } }),
        refs = { listOf("@navigation/${it.name}") },
        detail = { n ->
            KeyValueRow("File", n.path, mono = true)
            KeyValueRow("Start", n.startDestination, mono = true)
            SubsectionLabel("Destinations · ${n.destinations.size}")
            n.destinations.forEach { d ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Tag(d.kind, Tone.Secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            d.label?.let { it.resolved ?: it.raw } ?: d.id?.substringAfter('/') ?: "?",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    d.className?.let { MonoLine(it) }
                    d.deepLinks.forEach { MonoLine("deep link  $it", color = MaterialTheme.colorScheme.tertiary) }
                    d.actions.forEach { MonoLine("→ ${it.destination ?: it.popUpTo ?: "?"}  (${it.id?.substringAfter('/')})") }
                    if (d.arguments.isNotEmpty()) MonoLine("args: " + d.arguments.joinToString())
                }
            }
        },
        source = { ResourceSource.navigation(it) },
        webQuery = { n ->
            "android navigation graph ${n.name}" to
                "An Android navigation graph ${n.name} has destinations ${n.destinations.mapNotNull { it.className }.take(
                    20,
                ).joinToString()}. Describe the app flow it implies."
        },
    )

// ---- Styles, drawables, ids, assets, XML configs.

private fun stylesSpec(groups: List<ResGroup<StyleRecord>>) =
    ResSpec(
        title = "Styles & themes",
        noun = "styles",
        icon = Icons.Outlined.TouchApp,
        items = groups,
        key = { it.name },
        headline = { it.name },
        supporting = { g ->
            "parent ${g.main.parent ?: "none"} · ${g.main.items.size} items" +
                if (g.configs.size > 1) " · ${g.configs.joinToString()}" else ""
        },
        tags = { g ->
            listOfNotNull(
                if (g.main.usedBy.isNotEmpty()) "theme in manifest" to Tone.Primary else null,
                if (g.configs.any {
                        "night" in
                            it
                    }
                ) {
                    "night" to Tone.Neutral
                } else {
                    null
                },
            )
        },
        searchText = { g -> g.name + " " + g.main.parent + " " + g.main.items.joinToString(" ") { "${it.name} ${it.raw} ${it.resolved}" } },
        filters =
            listOf(
                "Used as theme" to { g: ResGroup<StyleRecord> -> g.records.any { it.usedBy.isNotEmpty() } },
                "Themes" to { g -> g.name.startsWith("Theme") || ".Theme" in g.name || g.name.contains("Theme.") },
                "App styles" to
                    { g ->
                        !g.name.startsWith("Base.") &&
                            !g.name.startsWith("Widget.") &&
                            !g.name.startsWith("TextAppearance.") &&
                            !g.name.startsWith("Platform.")
                    },
                "Night variant" to { g -> g.configs.any { "night" in it } },
            ),
        refs = { listOf("@style/${it.name}") },
        detail = { g ->
            KeyValueRow("Parent", g.main.parent, mono = true)
            KeyValueRow("Configurations", g.configs.joinToString())
            if (g.main.usedBy.isNotEmpty()) KeyValueRow("Theme of", g.main.usedBy.joinToString("\n"), mono = true)
            g.records.forEach { s ->
                SubsectionLabel("Items · ${s.config.ifEmpty { "default" }}")
                s.items.forEach { ValueRow(it) }
            }
        },
        source = { ResourceSource.styles(it.records) },
        webQuery = { g ->
            "android style ${g.name}" to "What does the Android style or theme ${g.name} (parent ${g.main.parent}) control?"
        },
    )

private fun drawablesSpec(groups: List<ResGroup<DrawableRecord>>) =
    ResSpec(
        title = "Drawables",
        noun = "drawables",
        icon = Icons.Outlined.TouchApp,
        items = groups,
        key = { it.name },
        headline = { it.name.substringAfter('/') },
        supporting = { g ->
            "${g.main.type} · ${g.main.format} · ${g.configs.size} configs · ${formatBytes(g.records.sumOf { it.bytes })}"
        },
        tags = { g ->
            listOf(g.main.format to Tone.Neutral) +
                listOfNotNull(if (g.records.any { it.bytes > 200_000 }) "large" to Tone.Tertiary else null)
        },
        searchText = { g -> g.name + " " + g.records.joinToString(" ") { it.path + " " + it.format } },
        filters =
            listOf(
                "Vector" to { g: ResGroup<DrawableRecord> -> g.main.format == "vector" || g.main.format == "animated-vector" },
                "Bitmaps" to { g -> g.main.format in setOf("png", "webp", "jpg", "gif", "9-patch") },
                "Mipmap" to { g -> g.main.type == "mipmap" },
                "Adaptive icon" to { g -> g.main.format == "adaptive-icon" },
                "Over 200 KB" to { g -> g.records.any { it.bytes > 200_000 } },
            ),
        refs = { g -> listOf("@${g.name}") },
        detail = { g ->
            SubsectionLabel("Files · ${g.records.size}")
            g.records.forEach { d ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    MonoLine(d.config.ifEmpty { "default" }, modifier = Modifier.width(120.dp))
                    MonoLine(d.path, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(
                        listOfNotNull(
                            d.width?.let {
                                "$it×${d.height}"
                            },
                            formatBytes(d.bytes),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        source = { ResourceSource.drawables(it.records) },
        webQuery = { g -> "android drawable ${g.name}" to "What is an Android ${g.main.format} drawable named ${g.name} likely used for?" },
    )

private fun idsSpec(ids: List<ResourceIdRecord>) =
    ResSpec(
        title = "Resource IDs",
        noun = "resource ids",
        icon = Icons.Outlined.TouchApp,
        items = ids,
        key = { it.id },
        headline = { "${it.type}/${it.name}" },
        supporting = { it.id },
        tags = { listOf(it.type to Tone.Neutral) },
        searchText = { "${it.id} ${it.type} ${it.name}" },
        filters =
            ids.groupingBy { it.type }.eachCount().entries.sortedByDescending { it.value }.take(8).map { (t, _) ->
                t to
                    { r: ResourceIdRecord -> r.type == t }
            },
        refs = { listOf("@${it.type}/${it.name}") },
        detail = { r ->
            KeyValueRow("ID", r.id, mono = true, copyable = true)
            KeyValueRow(
                "Decimal",
                r.id
                    .removePrefix("0x")
                    .toLong(16)
                    .toString(),
                mono = true,
                copyable = true,
            )
            KeyValueRow("Type", r.type)
            KeyValueRow("Configurations", r.configs.joinToString { it.ifEmpty { "default" } })
            Text(
                "Code refers to resources by this number; search the code for it (Smali shows it as a const).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        },
        source = { ResourceSource.id(it) },
        webQuery = { r -> "android resource ${r.type} ${r.name}" to "What is the Android resource @${r.type}/${r.name} usually used for?" },
    )

private fun assetsSpec(assets: List<AssetRecord>) =
    ResSpec(
        title = "Assets",
        noun = "assets",
        icon = Icons.Outlined.TouchApp,
        items = assets,
        key = { it.path },
        headline = { it.path.removePrefix("assets/") },
        supporting = { "${it.kind} · ${formatBytes(it.bytes)}" + (it.magic?.let { m -> " · content is $m" } ?: "") },
        tags = { a -> listOfNotNull(if (a.hiddenCode) "hidden code" to Tone.Error else null, a.kind to Tone.Neutral) },
        searchText = { "${it.path} ${it.kind} ${it.magic}" },
        filters =
            listOf(
                "Hidden code" to { a: AssetRecord -> a.hiddenCode },
                "Code & native" to { a -> a.kind in setOf("dex", "native") || a.magic in setOf("dex", "elf") },
                "JavaScript" to { a -> a.kind == "js" },
                "Certificates" to { a -> a.kind == "certificate" },
                "ML models" to { a -> a.kind == "model" },
                "Databases" to { a -> a.kind == "db" },
                "Config & text" to { a -> a.kind in setOf("json", "text") },
                "Over 1 MB" to { a -> a.bytes > 1_000_000 },
            ),
        detail = { a ->
            KeyValueRow("Path", a.path, mono = true, copyable = true)
            KeyValueRow("Kind", a.kind)
            KeyValueRow("Size", formatBytes(a.bytes))
            KeyValueRow("Content", a.magic ?: "not code or an archive")
            if (a.hiddenCode) {
                Text(
                    "Its bytes are ${a.magic} although the name does not say so. Apps load code like this at runtime; static analysis of the DEX files does not cover it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        source = { ResourceSource.asset(it) },
        webQuery = { a ->
            "android asset ${a.path.substringAfterLast('/')}" to
                "An Android app ships the asset ${a.path} (${a.kind}, ${a.bytes} bytes${a.magic?.let {
                    ", content $it"
                } ?: ""}). What is it likely for, and is it a concern?"
        },
    )

private val SECURITY_XML =
    setOf(
        "network-security-config",
        "backup-rules",
        "data-extraction-rules",
        "file-provider-paths",
        "accessibility-service",
        "device-admin",
        "autofill-service",
        "credential-provider",
    )

private fun xmlSpec(groups: List<ResGroup<XmlConfigRecord>>) =
    ResSpec(
        title = "XML configs",
        noun = "XML configs",
        icon = Icons.Outlined.TouchApp,
        items = groups,
        key = { it.name },
        headline = { it.name },
        supporting = { g ->
            g.main.kind + " · " +
                g.main.summary.entries
                    .filter { it.key != "root" }
                    .joinToString(" ") { "${it.key}=${it.value}" }
        },
        tags = { g ->
            listOfNotNull(
                if (g.main.summary["user-certs"] != null) "user CAs" to Tone.Error else null,
                if (g.main.summary["base-cleartext"] == "true" ||
                    g.main.summary["cleartext-domains"] != null
                ) {
                    "cleartext" to Tone.Tertiary
                } else {
                    null
                },
                if (g.main.kind in SECURITY_XML) "security" to Tone.Primary else null,
            )
        },
        searchText = { g -> g.name + " " + g.main.kind + " " + g.main.xml },
        filters =
            listOf("Security-relevant" to { g: ResGroup<XmlConfigRecord> -> g.main.kind in SECURITY_XML }) +
                groups.groupingBy { it.main.kind }.eachCount().entries.sortedByDescending { it.value }.take(6).map { (k, _) ->
                    k to
                        { g: ResGroup<XmlConfigRecord> -> g.main.kind == k }
                },
        refs = { listOf("@xml/${it.name}") },
        detail = { g ->
            KeyValueRow("Kind", g.main.kind)
            KeyValueRow("Configurations", g.configs.joinToString())
            KeyValueRow("File", g.main.path, mono = true)
            if (g.main.summary.isNotEmpty()) {
                SubsectionLabel("Summary")
                g.main.summary.forEach { (k, v) -> KeyValueRow(k, v, mono = true) }
            }
        },
        source = { ResourceSource.xml(it.records) },
        webQuery = { g ->
            "android ${g.main.kind} xml" to
                "Review this Android ${g.main.kind} file from res/xml/${g.name}.xml for security or privacy issues:\n${g.main.xml.take(
                    1500,
                )}"
        },
    )

// ---- SDKs & libraries (same data as `sextant sdks`).

private val REVIEW_CATEGORIES =
    setOf("ads", "analytics", "attribution", "marketing", "crash-reporting", "monitoring", "social", "experimentation")

private fun sdkSpec(ws: WorkspaceState): ResSpec<SdkEntry> {
    val entries =
        ws.analysis.sdks
            ?.entries
            .orEmpty()
    val atStartup =
        ws.analysis.startup
            ?.sdks
            ?.map { it.sdk }
            .orEmpty()
            .toSet()
    return ResSpec(
        title = "SDKs & libraries",
        noun = "SDKs and libraries",
        icon = Icons.Outlined.TouchApp,
        items = entries.sortedWith(compareBy({ it.kind != "sdk" }, { it.category !in REVIEW_CATEGORIES }, { it.category }, { it.name })),
        key = { it.name },
        headline = { it.name },
        supporting = { e ->
            listOfNotNull(
                e.category,
                e.version?.let { "v$it" },
                if (e.classes >
                    0
                ) {
                    "${e.classes} classes"
                } else {
                    null
                },
            ).joinToString(" · ")
        },
        tags = { e ->
            listOfNotNull(
                if (e.category in REVIEW_CATEGORIES) e.category to Tone.Tertiary else e.category to Tone.Neutral,
                if (e.name in atStartup) "starts at launch" to Tone.Primary else null,
            )
        },
        searchText = { e -> "${e.name} ${e.category} ${e.version} ${e.artifacts.joinToString()} ${e.packages.joinToString()}" },
        filters =
            listOf(
                "SDKs" to { e: SdkEntry -> e.kind == "sdk" },
                "Tracking & ads" to { e -> e.category in REVIEW_CATEGORIES },
                "Starts at launch" to { e -> e.name in atStartup },
                "Has version" to { e -> e.version != null },
                "Libraries" to { e -> e.kind == "library" },
            ) +
                entries
                    .filter { it.kind == "sdk" }
                    .groupingBy { it.category }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(
                        8,
                    ).map { (c, _) ->
                        c to
                            { e: SdkEntry -> e.category == c }
                    },
        refs = { emptyList() },
        detail = { e ->
            KeyValueRow("Kind", if (e.kind == "sdk") "SDK (recognized from its code packages)" else "Library (from its version file)")
            KeyValueRow("Role", e.category)
            KeyValueRow("Version", e.version ?: "not proven by any artifact in the APK")
            e.versionEvidence?.let { KeyValueRow("Version evidence", it, mono = true) }
            if (e.classes > 0) KeyValueRow("Classes", e.classes.toString())
            if (e.packages.isNotEmpty()) KeyValueRow("Packages", e.packages.joinToString("\n"), mono = true)
            if (e.artifacts.isNotEmpty()) KeyValueRow("Artifacts", e.artifacts.joinToString("\n"), mono = true)
            if (e.name in atStartup) {
                val s =
                    ws.analysis.startup!!
                        .sdks
                        .first { it.sdk == e.name }
                SubsectionLabel("Starts at launch")
                Text("Reached from ${s.from.substringAfter("->")} via ${s.via}", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { ws.destination = Destination.Application }) { Text("Open startup order") }
            }
        },
        manifestEntry = { e ->
            val declared =
                ws.analysis.components.filter { c ->
                    e.packages.any { p -> c.name.startsWith("$p.") } ||
                        c.name.startsWith(e.name)
                }
            if (declared.isEmpty()) {
                Text(
                    "Declares no components in AndroidManifest.xml.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            declared.take(20).forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(c.type.label, Tone.Secondary)
                    Spacer(Modifier.width(8.dp))
                    MonoLine(c.name, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    TextButton(onClick = { ws.showComponent(c.name) }) { Text("Open") }
                }
                c.facts.manifestXml?.let { CodeBlock(highlighted(it, Syntax.Xml), Modifier.fillMaxWidth().padding(bottom = 8.dp)) }
            }
        },
        webQuery = { e ->
            "${e.name} ${e.version.orEmpty()} android sdk" to
                "The Android app ${ws.analysis.meta.packageName} includes ${e.name}${e.version?.let {
                    " version $it"
                } ?: ""} (${e.category}). " +
                "What data does it collect, what permissions does it need, and are there known vulnerabilities in this version?"
        },
    )
}
