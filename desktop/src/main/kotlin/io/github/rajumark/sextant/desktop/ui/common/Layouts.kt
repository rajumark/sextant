package io.github.rajumark.sextant.desktop.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.ResizeHorizontalPointer
import io.github.rajumark.sextant.desktop.theme.codeTextStyle

/**
 * The reusable list-detail layout. Use it wherever a list has something to inspect on the side.
 *
 * - No selection: the list fills the window; the detail panel is hidden.
 * - Wide window (840dp and more): list and detail side by side, with a draggable divider. The detail has a close
 *   button that hides it again.
 * - Narrow window: one panel at a time. Once something is selected the detail replaces the list, with a back button.
 *
 * The layout only adds the back and close buttons. What goes in the list and the detail is up to the caller.
 * [onClose] clears the selection; it is used by both the back and the close buttons.
 */
@Composable
fun ListDetail(
    hasSelection: Boolean,
    onBack: () -> Unit,
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    initialDetailWidth: Dp = 460.dp,
    minListWidth: Dp = 480.dp,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val minDetail = 340.dp
        val twoPane = maxWidth >= 840.dp && maxWidth - minDetail - 16.dp >= minListWidth
        when {
            !hasSelection -> list()
            twoPane -> {
                var detailWidth by remember { mutableStateOf(initialDetailWidth) }
                val density = LocalDensity.current
                val maxDetail = (maxWidth - minListWidth - 16.dp).coerceAtLeast(minDetail)
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight()) { list() }
                    Box(
                        Modifier
                            .width(16.dp)
                            .fillMaxHeight()
                            .pointerHoverIcon(ResizeHorizontalPointer)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state =
                                    rememberDraggableState { delta ->
                                        val d = with(density) { delta.toDp() }
                                        detailWidth = (detailWidth - d).coerceIn(minDetail, maxDetail)
                                    },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(48.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                    DetailPane(
                        showBack = false,
                        onBack = onBack,
                        modifier =
                            Modifier
                                .width(detailWidth.coerceIn(minDetail, maxDetail))
                                .fillMaxHeight()
                                .padding(top = 16.dp, bottom = 16.dp, end = 16.dp),
                        detail = detail,
                    )
                }
            }
            else ->
                DetailPane(
                    showBack = true,
                    onBack = onBack,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    detail = detail,
                )
        }
    }
}

/** The detail panel with its own header: a back button when it is the only panel, a close button otherwise. */
@Composable
private fun DetailPane(
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier,
    detail: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (showBack) {
                    TextButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Back to list")
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.Close, "Close details") }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { detail() }
        }
    }
}

/** Read-only code viewer with a line-number gutter. Lines are virtualized for large files. */
@Composable
fun CodeView(
    lines: List<AnnotatedString>,
    modifier: Modifier = Modifier,
) {
    val style = codeTextStyle()
    val gutter = (lines.size.toString().length * 9 + 16).dp
    val state = rememberLazyListState()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        SelectionContainer {
            LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
                itemsIndexed(lines) { i, line ->
                    Row {
                        Text(
                            (i + 1).toString(),
                            style = style.copy(textAlign = TextAlign.End),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.width(gutter).padding(end = 12.dp),
                        )
                        Text(line, style = style, modifier = Modifier.padding(end = 16.dp))
                    }
                }
            }
        }
    }
}

/** Small inline code block for snippets that fit on screen. */
@Composable
fun CodeBlock(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        SelectionContainer {
            Text(text, style = codeTextStyle(), modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
        }
    }
}

@Composable
fun CodeBlock(
    text: String,
    modifier: Modifier = Modifier,
) = CodeBlock(AnnotatedString(text), modifier)
