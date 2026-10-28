package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.core.model.ResourceRef

// Shared tools for the resource pages (Strings, Layouts, …, SDKs): sorting, reference counts and copying the list.

/** How a resource list can be ordered. [ResSort.Default] is the order core gives. */
enum class ResSort(
    val label: String,
) {
    Default("Default order"),
    Name("Name A–Z"),
    MostReferenced("Most referenced"),
    Largest("Largest first"),
}

/** How many times each `@type/name` is referenced from other resources and the manifest (code is not counted). */
fun referenceCounts(refs: List<ResourceRef>): Map<String, Int> = refs.groupingBy { it.ref }.eachCount()

/** The filter label for entries no resource or manifest entry points at. Code may still use them. */
const val NOT_REFERENCED = "Not referenced in res/manifest"

/** Orders [items] by [sort]; [refCount] and [size] are only used by the sorts that need them. */
fun <T> sortResources(
    items: List<T>,
    sort: ResSort,
    name: (T) -> String,
    refCount: (T) -> Int,
    size: ((T) -> Long)?,
): List<T> =
    when (sort) {
        ResSort.Default -> items
        ResSort.Name -> items.sortedBy { name(it).lowercase() }
        ResSort.MostReferenced -> items.sortedByDescending(refCount)
        ResSort.Largest -> if (size != null) items.sortedByDescending(size) else items
    }

/** The shown list as plain text, one entry per line: name, then its summary line, tab separated. */
fun <T> resourceListText(
    items: List<T>,
    name: (T) -> String,
    supporting: (T) -> String,
): String = items.joinToString("\n") { "${name(it)}\t${supporting(it)}" }

@Composable
fun ResSortMenu(
    options: List<ResSort>,
    current: ResSort,
    onPick: (ResSort) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) {
        Icon(Icons.AutoMirrored.Outlined.Sort, null)
        Spacer(Modifier.width(6.dp))
        Text(current.label)
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        options.forEach { s ->
            DropdownMenuItem(text = { Text(s.label) }, onClick = {
                onPick(s)
                open = false
            })
        }
    }
}
