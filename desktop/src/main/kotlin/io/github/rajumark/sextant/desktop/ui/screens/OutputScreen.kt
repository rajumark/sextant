package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.isMac
import io.github.rajumark.sextant.desktop.model.OutputFile
import io.github.rajumark.sextant.desktop.model.OutputFormat
import io.github.rajumark.sextant.desktop.ui.common.CodeBlock
import io.github.rajumark.sextant.desktop.ui.common.CodeView
import io.github.rajumark.sextant.desktop.ui.common.CopyButton
import io.github.rajumark.sextant.desktop.ui.common.IconBadge
import io.github.rajumark.sextant.desktop.ui.common.MonoLine
import io.github.rajumark.sextant.desktop.ui.common.SectionCard
import io.github.rajumark.sextant.desktop.ui.common.Syntax
import io.github.rajumark.sextant.desktop.ui.common.Tag
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.common.formatBytes
import io.github.rajumark.sextant.desktop.ui.common.highlighted
import io.github.rajumark.sextant.desktop.ui.common.rememberHighlighted
import io.github.rajumark.sextant.desktop.ui.common.syntax
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun OutputFormat.icon() =
    when (this) {
        OutputFormat.Markdown -> Icons.AutoMirrored.Outlined.Article
        OutputFormat.Json -> Icons.Outlined.DataObject
        OutputFormat.JsonLines -> Icons.AutoMirrored.Outlined.ViewList
        OutputFormat.Binary -> Icons.Outlined.Storage
    }

@Composable
fun OutputScreen(
    ws: WorkspaceState,
    onReveal: () -> Unit,
) {
    val a = ws.analysis
    val files = a.outputFiles
    val selected = files.firstOrNull { it.name == ws.selectedFile } ?: files.firstOrNull()
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(400.dp)
                .fillMaxHeight()
                .padding(24.dp),
        ) {
            Text("Output folder", style = MaterialTheme.typography.headlineSmall)
            Text(
                "What sextant extract writes. Same APK, byte-identical files.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoLine(a.outputDir, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                CopyButton(a.outputDir, "folder path")
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onReveal) {
                Icon(Icons.Outlined.FolderOpen, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isMac) "Reveal in Finder" else "Open folder")
            }
            Spacer(Modifier.height(16.dp))
            // Only the file list and the terminal card scroll, inside this column.
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                files.forEach { f ->
                    SelectableRow(
                        selected = f.name == selected?.name,
                        onClick = { ws.selectedFile = f.name },
                        headline = { Text(f.name, style = MaterialTheme.typography.bodyLarge) },
                        supporting = { Text(f.description, style = MaterialTheme.typography.bodySmall) },
                        leading = { IconBadge(f.format.icon(), if (f.format == OutputFormat.Markdown) Tone.Primary else Tone.Secondary) },
                        trailing = {
                            Text(formatBytes(f.sizeBytes), style = MaterialTheme.typography.labelMedium)
                        },
                        inset = 0.dp,
                    )
                }
                Spacer(Modifier.height(16.dp))
                TerminalCard(ws)
            }
        }
        if (selected != null) {
            FilePreview(selected, Modifier.weight(1f).fillMaxHeight().padding(top = 24.dp, end = 24.dp, bottom = 24.dp))
        }
    }
}

@Composable
private fun FilePreview(
    f: OutputFile,
    modifier: Modifier,
) {
    // Files are read off the UI thread; previews are capped (see OutputFolder.PREVIEW_LINES).
    val content by produceState<String?>(null, f) {
        value = withContext(Dispatchers.IO) { runCatching { f.load() }.getOrElse { "Could not read ${f.name}: ${it.message}" } }
    }
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(40.dp)) {
            Text(f.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Tag(
                when (f.format) {
                    OutputFormat.Markdown -> "Markdown"
                    OutputFormat.Json -> "JSON"
                    OutputFormat.JsonLines -> "JSON Lines"
                    OutputFormat.Binary -> "SQLite"
                },
                Tone.Secondary,
            )
            Spacer(Modifier.weight(1f))
            Text(formatBytes(f.sizeBytes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content?.let { CopyButton(it, f.name) }
        }
        Spacer(Modifier.height(8.dp))
        val text = content
        if (text == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            CodeView(rememberHighlighted(text, f.format.syntax()), Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun TerminalCard(ws: WorkspaceState) {
    val text =
        buildString {
            appendLine("cd ${ws.analysis.outputDir}")
            appendLine("sextant info . --json")
            appendLine("grep '\"exported\":true' components.jsonl")
            append("jq -r 'select(.dangerous).name' permissions.jsonl")
        }
    SectionCard("Use it from the terminal", icon = Icons.Outlined.Terminal) {
        CodeBlock(highlighted(text, Syntax.Plain), Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
            CopyButton(text, "commands")
        }
    }
}
