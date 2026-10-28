package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.core.insights.MenuEntry
import io.github.rajumark.sextant.core.insights.MenuPreview
import io.github.rajumark.sextant.core.insights.MenuPreviews
import io.github.rajumark.sextant.core.model.LayoutRecord
import io.github.rajumark.sextant.core.model.MenuRecord
import io.github.rajumark.sextant.desktop.ui.common.MonoLine
import io.github.rajumark.sextant.desktop.ui.common.SubsectionLabel

/**
 * How the menu looks in the app, laid out by core ([MenuPreviews]): an app bar or toolbar with its action buttons and
 * the ⋮ list opened, bottom-navigation tabs, or drawer rows. Icons are placeholders (the item's initial) until drawables
 * can be drawn; the item list below names each real drawable.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuPreviewCard(
    records: List<MenuRecord>,
    layouts: List<LayoutRecord>,
) {
    if (records.isEmpty()) return
    var config by remember(records) { mutableStateOf(0) }
    var tablet by remember { mutableStateOf(false) }
    val menu = records[config.coerceIn(records.indices)]
    val p = remember(menu, layouts, tablet) { MenuPreviews.of(menu, layouts, if (tablet) 5 else MenuPreviews.PHONE_SLOTS) }
    val bar = p.host == "app-bar" || p.host == "toolbar"
    SubsectionLabel(
        "Preview · " +
            when (p.host) {
                "bottom-nav" -> "bottom navigation"
                "drawer" -> "navigation drawer"
                "toolbar" -> "toolbar"
                else -> "app bar"
            },
    )
    if (p.hostViews.isEmpty()) {
        Text(
            "No layout shows this menu, so the app inflates it from code (options, popup or context menu). Shown as an app bar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    p.hostViews.forEach { MonoLine("shown by $it") }
    if (records.size > 1 || bar) {
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (records.size > 1) {
                records.forEachIndexed { i, r ->
                    FilterChip(selected = i == config, onClick = { config = i }, label = { Text(r.config.ifEmpty { "default" }) })
                }
            }
            if (bar) {
                FilterChip(selected = !tablet, onClick = { tablet = false }, label = { Text("Phone · ${MenuPreviews.PHONE_SLOTS} slots") })
                FilterChip(selected = tablet, onClick = { tablet = true }, label = { Text("Tablet · 5 slots") })
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
    ) {
        when (p.host) {
            "bottom-nav" -> BottomNav(p)
            "drawer" -> Drawer(p)
            else -> AppBar(p, menu.name)
        }
    }
    if (p.hidden.isNotEmpty()) {
        SubsectionLabel("Hidden until code shows them · ${p.hidden.size}")
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth().alpha(0.7f),
        ) { Column(Modifier.padding(vertical = 4.dp)) { Rows(p.hidden, 0) } }
    }
    if (p.untitled > 0) {
        Text(
            "${p.untitled} item${if (p.untitled == 1) " has" else "s have"} no title in XML: the app sets it in code (shown by id).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AppBar(
    p: MenuPreview,
    name: String,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            p.toolbar.forEach { e ->
                Row(
                    Modifier.padding(horizontal = 4.dp).alpha(if (e.enabled) 1f else 0.38f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Glyph(e)
                    if (e.withText && e.title != null) {
                        Spacer(Modifier.width(4.dp))
                        Text(e.title!!, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
            if (p.overflow.isNotEmpty()) Icon(Icons.Outlined.MoreVert, "More options", Modifier.padding(start = 4.dp).size(22.dp))
        }
        if (p.overflow.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 3.dp,
                    modifier = Modifier.widthIn(min = 200.dp, max = 280.dp),
                ) { Column(Modifier.padding(vertical = 8.dp)) { Rows(p.overflow, 0) } }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(24.dp))
        }
    }
}

@Composable
private fun BottomNav(p: MenuPreview) {
    Column {
        Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
            Text("screen content", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            p.toolbar.forEachIndexed { i, e ->
                Column(Modifier.weight(1f).alpha(if (e.enabled) 1f else 0.38f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .background(
                                if (i == 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                RoundedCornerShape(16.dp),
                            ).padding(horizontal = 16.dp, vertical = 4.dp),
                    ) { Glyph(e) }
                    Spacer(Modifier.height(4.dp))
                    Title(e, MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun Drawer(p: MenuPreview) {
    Column(Modifier.padding(12.dp).widthIn(max = 320.dp)) {
        Text(
            "Navigation drawer",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        )
        Rows(p.overflow, 0, drawer = true)
    }
}

/** List rows (overflow, drawer, hidden): group dividers, check boxes and radios, sub-menus that open on click. */
@Composable
private fun Rows(
    list: List<MenuEntry>,
    indent: Int,
    drawer: Boolean = false,
) {
    var group: Int? = list.firstOrNull()?.group
    list.forEachIndexed { i, e ->
        if (i > 0 && e.group != group) HorizontalDivider(Modifier.padding(vertical = 4.dp))
        group = e.group
        var open by remember(e) { mutableStateOf(false) }
        val selected = drawer && e.checked
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (drawer) 48.dp else 40.dp)
                .then(
                    if (selected) {
                        Modifier.background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(24.dp),
                        )
                    } else {
                        Modifier
                    },
                ).then(if (e.children.isNotEmpty()) Modifier.clickable { open = !open } else Modifier)
                .padding(start = (16 + indent * 16).dp, end = 12.dp)
                .alpha(if (e.enabled) 1f else 0.38f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (drawer) {
                Glyph(e)
                Spacer(Modifier.width(12.dp))
            }
            Title(e, MaterialTheme.typography.bodyLarge, Modifier.weight(1f))
            when (e.mark) {
                "check" -> Icon(if (e.checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, null, Modifier.size(20.dp))
                "radio" ->
                    if (!drawer) {
                        Icon(
                            if (e.checked) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                            null,
                            Modifier.size(20.dp),
                        )
                    }
            }
            if (e.children.isNotEmpty()) {
                Icon(
                    if (open) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
                    "Sub-menu",
                    Modifier.size(20.dp),
                )
            }
        }
        if (open) Rows(e.children, indent + 1, drawer)
    }
}

@Composable
private fun Title(
    e: MenuEntry,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    val title = e.title
    if (title != null) {
        Text(title, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
    } else {
        Text(
            e.id?.substringAfter('/') ?: "untitled",
            style = style.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}

/** A search icon for search action views, else the item's initial in a circle standing in for its drawable. */
@Composable
private fun Glyph(e: MenuEntry) {
    if (e.actionView?.contains("SearchView") == true) {
        Icon(Icons.Outlined.Search, e.title, Modifier.size(24.dp))
        return
    }
    Box(
        Modifier.size(24.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (e.title ?: e.id?.substringAfter('/') ?: "?").first().uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
