package io.github.rajumark.sextant.desktop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.core.Sextant
import io.github.rajumark.sextant.desktop.AppState
import io.github.rajumark.sextant.desktop.ExtractionJob
import io.github.rajumark.sextant.desktop.Problem
import io.github.rajumark.sextant.desktop.model.Analysis
import io.github.rajumark.sextant.desktop.theme.SeedPresets
import io.github.rajumark.sextant.desktop.theme.ThemeMode
import io.github.rajumark.sextant.desktop.ui.common.CodeBlock
import io.github.rajumark.sextant.desktop.ui.common.SubsectionLabel
import io.github.rajumark.sextant.desktop.ui.common.Tag
import io.github.rajumark.sextant.desktop.ui.common.Tone
import kotlinx.coroutines.delay
import kotlin.io.path.name

/**
 * Minimal analysis dialog: one expressive indicator and one status line. Core reports no progress yet, so the
 * indicator is indeterminate; a percentage replaces it once core can report one.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExtractionDialog(
    app: AppState,
    job: ExtractionJob,
) {
    val result = job.result
    LaunchedEffect(result) {
        if (result != null) {
            delay(700)
            app.finishExtraction()
        }
    }
    val failure = job.failure
    AlertDialog(
        onDismissRequest = {},
        title = { Text(if (failure != null) "Could not analyze ${job.apk.name}" else "Analyzing ${job.apk.name}") },
        text = {
            Column(Modifier.width(360.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    failure != null -> {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(failure.message ?: "", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        failure.hint?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    result == null -> {
                        LoadingIndicator(Modifier.size(72.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Analyzing…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> {
                        Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Done", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            when {
                failure != null -> Button(onClick = app::dismissFailure) { Text("Close") }
                else -> Button(onClick = app::finishExtraction, enabled = result != null) { Text("Open") }
            }
        },
        dismissButton = {
            if (failure == null && result == null) TextButton(onClick = app::cancelExtraction) { Text("Cancel") }
        },
    )
}

@Composable
fun ProblemDialog(
    problem: Problem,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.ErrorOutline, null) },
        title = { Text(problem.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(problem.message, style = MaterialTheme.typography.bodyLarge)
                problem.hint?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
fun SettingsDialog(app: AppState) {
    AlertDialog(
        onDismissRequest = { app.showSettings = false },
        title = { Text("Settings") },
        text = {
            // The dialog scrolls inside itself when the window is short; the window never scrolls.
            Column(
                Modifier
                    .width(480.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SubsectionLabel("Appearance")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = app.themeMode == mode,
                            onClick = { app.themeMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                            label = { Text(mode.name) },
                        )
                    }
                }
                SubsectionLabel("Color")
                Text(
                    "The seed color builds every role of the Material 3 scheme, light and dark.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SeedPresets.forEach { (name, color) ->
                        val selected = app.seed == color
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (selected) 3.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape,
                                ).clickable { app.changeSeed(color) }
                                .semantics {
                                    contentDescription = "$name seed${if (selected) ", selected" else ""}"
                                },
                        )
                    }
                }
                SubsectionLabel("About")
                Text("Sextant Desktop ${Sextant.version}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Apache-2.0 · runs locally, nothing leaves your machine",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { app.showSettings = false }) { Text("Done") } },
    )
}

/** Adds and removes short tags on one analysis. Tags are a personal reminder; they are saved per output folder. */
@Composable
fun TagDialog(
    app: AppState,
    analysis: Analysis,
) {
    val dir = analysis.outputDir
    val current = app.tags[dir].orEmpty()
    var draft by remember { mutableStateOf("") }

    fun add() {
        if (draft.isBlank()) return
        app.setTags(dir, current + draft)
        draft = ""
    }
    AlertDialog(
        onDismissRequest = { app.tagging = null },
        title = { Text("Tags for ${analysis.meta.label}") },
        text = {
            Column(Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(32) },
                        singleLine = true,
                        label = { Text("New tag") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { add() }),
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = ::add, enabled = draft.isNotBlank()) { Text("Add") }
                }
                if (current.isEmpty()) {
                    Text(
                        "No tags yet. Use tags to remember why an app matters, for example \"check exported services\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        current.forEach { tag ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Tag(tag, Tone.Primary)
                                IconButton(onClick = { app.setTags(dir, current - tag) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Outlined.Close, "Remove tag $tag", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { app.tagging = null }) { Text("Done") } },
    )
}

/** Confirms before Sextant's output folder is deleted. The APK is not touched. */
@Composable
fun DeleteDialog(
    app: AppState,
    analysis: Analysis,
) {
    AlertDialog(
        onDismissRequest = { app.deleting = null },
        icon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete ${analysis.meta.label} analysis?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Sextant's files in this folder are deleted and the analysis is removed from recent. Its tags are removed too. Your APK is not touched.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                CodeBlock(analysis.outputDir, Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { app.deleteAnalysis(analysis) },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = { app.deleting = null }) { Text("Cancel") } },
    )
}
