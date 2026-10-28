package io.github.rajumark.sextant.desktop.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.copyToClipboard
import io.github.rajumark.sextant.desktop.openInBrowser
import io.github.rajumark.sextant.desktop.urlQuery

/**
 * The two actions at the bottom of every detail pane: look the item up on the web, or ask an AI assistant about it.
 * Links open in the user's browser only when clicked; Sextant itself never goes online.
 */
@Composable
fun WebActions(
    searchQuery: String,
    question: String,
    modifier: Modifier = Modifier,
) {
    val notify = LocalNotifier.current
    var menuOpen by remember { mutableStateOf(false) }

    fun open(url: String) {
        if (!openInBrowser(url)) notify("Could not open a browser")
    }
    Row(modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { open("https://duckduckgo.com/?q=${urlQuery(searchQuery)}") }) {
            Icon(Icons.Outlined.Search, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Search on web")
        }
        Box {
            Button(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.Lightbulb, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ask AI")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("ChatGPT") }, onClick = {
                    menuOpen = false
                    open("https://chatgpt.com/?q=${urlQuery(question)}")
                })
                DropdownMenuItem(text = { Text("Claude") }, onClick = {
                    menuOpen = false
                    open("https://claude.ai/new?q=${urlQuery(question)}")
                })
                DropdownMenuItem(text = { Text("Gemini") }, onClick = {
                    menuOpen = false
                    // Gemini cannot take a question in its address, so the question is copied for pasting.
                    copyToClipboard(question)
                    if (openInBrowser(
                            "https://gemini.google.com/app",
                        )
                    ) {
                        notify("Question copied. Paste it into Gemini.")
                    } else {
                        notify("Could not open a browser")
                    }
                })
            }
        }
    }
}
