package io.github.rajumark.sextant.desktop.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.copyToClipboard
import io.github.rajumark.sextant.desktop.theme.MonoFamily
import kotlin.math.abs

/** Shows a short message in the app's snackbar. */
val LocalNotifier = compositionLocalOf<(String) -> Unit> { {} }

enum class Tone { Neutral, Primary, Secondary, Tertiary, Error }

data class ToneColors(
    val container: Color,
    val content: Color,
)

@Composable
fun Tone.colors(): ToneColors {
    val c = MaterialTheme.colorScheme
    return when (this) {
        Tone.Neutral -> ToneColors(c.surfaceContainerHighest, c.onSurfaceVariant)
        Tone.Primary -> ToneColors(c.primaryContainer, c.onPrimaryContainer)
        Tone.Secondary -> ToneColors(c.secondaryContainer, c.onSecondaryContainer)
        Tone.Tertiary -> ToneColors(c.tertiaryContainer, c.onTertiaryContainer)
        Tone.Error -> ToneColors(c.errorContainer, c.onErrorContainer)
    }
}

/** Small read-only label, e.g. "exported" or "signature". */
@Composable
fun Tag(
    text: String,
    tone: Tone = Tone.Neutral,
    mono: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = tone.colors()
    Surface(color = colors.container, contentColor = colors.content, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.let { if (mono) it.copy(fontFamily = MonoFamily) else it },
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Tonal circle holding an icon; used as list leading content. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tone: Tone,
    size: Dp = 40.dp,
) {
    val colors = tone.colors()
    Box(
        Modifier.size(size).clip(CircleShape).background(colors.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.content, modifier = Modifier.size(size * 0.5f))
    }
}

/** Letter avatar for an app, colored by its package name. */
@Composable
fun AppAvatar(
    label: String,
    key: String,
    size: Dp = 48.dp,
    onContainer: Boolean = false,
) {
    val tones = listOf(Tone.Primary, Tone.Tertiary, Tone.Secondary)
    val colors =
        if (onContainer) {
            // On a tonal container the tonal avatar would vanish; use the lowest surface instead.
            ToneColors(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.colorScheme.primary)
        } else {
            tones[abs(key.hashCode()) % tones.size].colors()
        }
    Box(
        Modifier.size(size).clip(MaterialTheme.shapes.large).background(colors.container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.take(1).uppercase(),
            color = colors.content,
            style = if (size >= 56.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(32.dp)) {
                if (icon != null) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                action?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    supporting: String,
    icon: ImageVector,
    tone: Tone,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val body: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon, tone, 36.dp)
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            Text(value, style = MaterialTheme.typography.displaySmall)
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, colors = colors, content = body)
    } else {
        Card(modifier = modifier, shape = MaterialTheme.shapes.large, colors = colors, content = body)
    }
}

/** A label/value row. `null` values render as "not set", matching the CLI's truthful nulls. */
@Composable
fun KeyValueRow(
    key: String,
    value: String?,
    mono: Boolean = false,
    copyable: Boolean = false,
    keyWidth: Dp = 168.dp,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            key,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(keyWidth),
        )
        if (value == null) {
            Text(
                "not set",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f),
            )
        } else {
            SelectionContainer(Modifier.weight(1f)) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium.let { if (mono) it.copy(fontFamily = MonoFamily) else it },
                )
            }
            if (copyable) CopyButton(value, key)
        }
    }
}

@Composable
fun CopyButton(
    text: String,
    what: String,
) {
    val notify = LocalNotifier.current
    IconButton(onClick = {
        copyToClipboard(text)
        notify("Copied $what")
    }, modifier = Modifier.size(32.dp)) {
        Icon(Icons.Outlined.ContentCopy, "Copy $what", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadge(icon, Tone.Secondary, 64.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 360.dp),
        )
    }
}

/** Single-line mono text that ellipsizes, for package names in lists. */
@Composable
fun MonoLine(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFamily),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun SubsectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

fun formatBytes(bytes: Long): String =
    when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }

fun shortHash(hash: String): String = if (hash.length > 12) hash.take(6) + "…" + hash.takeLast(4) else hash

/** "just now", "5 min ago", "yesterday", "3 days ago", or a date for older entries. */
fun relativeTime(
    millis: Long,
    now: Long = System.currentTimeMillis(),
): String {
    val minutes = (now - millis).coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        minutes < 48 * 60 -> "yesterday"
        minutes < 14 * 24 * 60 -> "${minutes / (24 * 60)} days ago"
        else ->
            java.time.Instant
                .ofEpochMilli(millis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .toString()
    }
}

/**
 * Icon on a Material expressive shape instead of a plain circle. Use it where one action needs emphasis (the drop area),
 * not on text-heavy containers.
 */
@Composable
fun ShapeBadge(
    icon: ImageVector,
    tone: Tone,
    shape: Shape,
    size: Dp,
) {
    val colors = tone.colors()
    Box(Modifier.size(size).clip(shape).background(colors.container), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = colors.content, modifier = Modifier.size(size * 0.45f))
    }
}
