package io.github.rajumark.sextant.desktop.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.rajumark.sextant.desktop.ui.common.IconBadge
import io.github.rajumark.sextant.desktop.ui.common.MonoLine
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.screens.SearchField
import io.github.rajumark.sextant.desktop.ui.screens.icon
import io.github.rajumark.sextant.desktop.ui.screens.tone

private class PaletteEntry(
    val title: String,
    val detail: String,
    val kind: String,
    val badge: @Composable () -> Unit,
    val go: () -> Unit,
)

/** Scores a candidate: prefix of the simple name beats a word match, which beats a plain substring; -1 = no match. */
private fun score(
    q: String,
    title: String,
    detail: String,
): Int {
    if (q.isEmpty()) return 0
    val t = title.lowercase()
    return when {
        t.startsWith(q) -> 3
        t.split('.', '_', ' ', '$').any { it.startsWith(q) } -> 2
        t.contains(q) || detail.lowercase().contains(q) -> 1
        else -> -1
    }
}

/**
 * Cmd/Ctrl+K: type to jump to any page, component (by class, deep link or authority) or permission.
 * ↑ ↓ move, Enter opens, Esc closes.
 */
@Composable
fun CommandPalette(ws: WorkspaceState) {
    var query by remember { mutableStateOf("") }
    var index by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val all =
        remember(ws.analysis) {
            Destination.entries.map { d ->
                PaletteEntry(d.label, "Page", "page", { IconBadge(d.icon, Tone.Secondary, 32.dp) }) { ws.destination = d }
            } +
                ws.analysis.components.map { c ->
                    PaletteEntry(
                        c.simpleName,
                        listOfNotNull(c.name, c.deepLinks.firstOrNull(), c.authorities.firstOrNull()).joinToString("  ·  "),
                        c.type.label,
                        { IconBadge(c.type.icon(), c.type.tone(), 32.dp) },
                    ) { ws.showComponent(c.name) }
                } +
                ws.analysis.permissions.distinctBy { it.name }.map { p ->
                    PaletteEntry(p.shortName, p.name, "Permission", { IconBadge(Destination.Permissions.icon, Tone.Neutral, 32.dp) }) {
                        ws.showPermission(p.name)
                    }
                }
        }
    val q = query.trim().lowercase()
    val shown =
        remember(all, q) {
            all
                .map { it to score(q, it.title, it.detail) }
                .filter { it.second >= 0 }
                .sortedByDescending { it.second }
                .take(60)
                .map { it.first }
        }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(index) { if (shown.isNotEmpty()) listState.scrollToItem(index.coerceIn(0, shown.lastIndex)) }

    fun close() {
        ws.paletteOpen = false
    }
    Dialog(onDismissRequest = ::close) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(640.dp),
        ) {
            Column(
                Modifier.padding(16.dp).onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionDown -> index = (index + 1).coerceAtMost((shown.size - 1).coerceAtLeast(0))
                        Key.DirectionUp -> index = (index - 1).coerceAtLeast(0)
                        Key.Enter, Key.NumPadEnter ->
                            shown.getOrNull(index)?.let {
                                close()
                                it.go()
                            }
                        Key.Escape -> close()
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
            ) {
                SearchField(
                    query,
                    {
                        query = it
                        index = 0
                    },
                    "Go to a page, component, deep link or permission",
                    Modifier.fillMaxWidth().focusRequester(focus),
                )
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(top = 8.dp), state = listState) {
                    itemsIndexed(shown) { i, e ->
                        ListItem(
                            headlineContent = { Text(e.title, maxLines = 1) },
                            supportingContent = { MonoLine(e.detail) },
                            leadingContent = e.badge,
                            trailingContent = { Text(e.kind, style = MaterialTheme.typography.labelMedium) },
                            colors =
                                ListItemDefaults.colors(
                                    containerColor =
                                        if (i ==
                                            index
                                        ) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                ),
                            modifier =
                                Modifier.clip(MaterialTheme.shapes.large).clickable {
                                    close()
                                    e.go()
                                },
                        )
                    }
                }
                if (shown.isEmpty()) Text("Nothing matches.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
