package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Outlined.Close, "Clear search") }
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        modifier = modifier,
    )
}

/** Selectable list row on the M3 list-item tokens. */
@Composable
fun SelectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    headline: @Composable () -> Unit,
    supporting: @Composable () -> Unit,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    inset: Dp = 12.dp,
) {
    ListItem(
        headlineContent = headline,
        supportingContent = supporting,
        leadingContent = leading,
        trailingContent = trailing,
        colors =
            ListItemDefaults.colors(
                containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            ),
        modifier =
            Modifier
                .padding(horizontal = inset, vertical = 2.dp)
                .clip(MaterialTheme.shapes.large)
                .clickable(onClick = onClick),
    )
}

@Composable
fun ListHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
fun DetailHeader(
    title: String,
    subtitle: String,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        leading()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
    Spacer(Modifier.height(8.dp))
}

/**
 * Lazy-list keys must be unique, but real manifests repeat entries (a permission both declared and requested,
 * or a component listed twice). Later repeats get an index suffix instead of crashing the list.
 */
fun <T> withUniqueKeys(
    items: List<T>,
    key: (T) -> String,
): List<Pair<String, T>> {
    val seen = HashMap<String, Int>()
    return items.map { item ->
        val base = key(item)
        val n = seen.merge(base, 1, Int::plus)!!
        (if (n == 1) base else "$base#$n") to item
    }
}
