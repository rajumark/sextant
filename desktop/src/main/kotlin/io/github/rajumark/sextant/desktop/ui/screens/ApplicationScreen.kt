package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.core.insights.AppStartup
import io.github.rajumark.sextant.core.insights.StartupStep
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
import io.github.rajumark.sextant.desktop.ui.common.highlighted
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState

private fun descriptorOf(className: String) = "L" + className.replace('.', '/') + ";"

/** The startup methods (from core) that belong to a step, e.g. every in-app attachBaseContext along the class chain. */
private fun methodsFor(
    s: AppStartup,
    step: StartupStep,
) = when {
    step.what.startsWith("Application.") -> {
        val name = step.what.substringAfter("Application.")
        s.methods.filter { m -> m.method.substringAfter("->").startsWith("$name(") && s.superclasses.any { m.method.startsWith(descriptorOf(it)) } }
    }
    else -> s.methods.filter { it.method.startsWith(descriptorOf(step.target) + "->") }
}

/**
 * The Application class and what runs when the process starts, in order (same data as `sextant app`): a timeline of
 * steps on the left; the selected step's class, calls and manifest entry on the right.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApplicationScreen(ws: WorkspaceState) {
    val s = ws.analysis.startup
    if (s == null) {
        EmptyState(Icons.Outlined.RocketLaunch, "No startup data", "Open the APK again to see its Application class and startup order.")
        return
    }
    val selected = s.steps.firstOrNull { it.order == ws.selectedStartupStep }
    ListDetail(
        hasSelection = selected != null,
        onBack = { ws.selectedStartupStep = null },
        initialDetailWidth = 520.dp,
        list = {
            Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
                ListHeader(
                    "Application",
                    s.applicationClass?.let { "Custom Application class · ${s.steps.size} startup steps" } ?: "No custom Application class",
                )
                Spacer(Modifier.height(12.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MonoLine(s.applicationClass ?: "android.app.Application", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            s.applicationClass?.let { CopyButton(it, "class name") }
                        }
                        if (s.superclasses.size > 1) KeyValueRow("Extends", s.superclasses.drop(1).joinToString(" → "), mono = true, keyWidth = 120.dp)
                        s.appComponentFactory?.let { KeyValueRow("Component factory", it, mono = true, keyWidth = 120.dp) }
                        s.zygotePreload?.let { KeyValueRow("Zygote preload", it, mono = true, keyWidth = 120.dp) }
                        s.backupAgent?.let { KeyValueRow("Backup agent", it, mono = true, keyWidth = 120.dp) }
                        s.process?.let { KeyValueRow("Main process", it, mono = true, keyWidth = 120.dp) }
                        if (s.sdks.isNotEmpty()) {
                            Text(
                                "SDKs started at launch",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                s.sdks.forEach { Tag("${it.sdk} · ${it.category}", Tone.Secondary) }
                            }
                        } else if (!s.codeIndexed) {
                            Text(
                                "Open an extracted folder with code to follow startup calls and see which SDKs start.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
                SubsectionLabel("Startup order")
                LazyColumn(Modifier.fillMaxSize()) {
                    items(s.steps, key = { it.order }) { step ->
                        val calls = methodsFor(s, step).sumOf { it.calls.size }
                        SelectableRow(
                            selected = step.order == ws.selectedStartupStep,
                            onClick = { ws.selectedStartupStep = step.order },
                            headline = { Text("${step.order}. ${step.what.trim()}", maxLines = 1) },
                            supporting = { MonoLine(step.target) },
                            leading = {
                                IconBadge(
                                    when {
                                        step.what.contains("ContentProvider") -> Icons.Outlined.Storage
                                        step.what.contains("Initializer") -> Icons.Outlined.Extension
                                        step.what.startsWith("Application") -> Icons.Outlined.PlayArrow
                                        else -> Icons.Outlined.Build
                                    },
                                    if (step.what.startsWith("Application")) Tone.Primary else Tone.Neutral,
                                )
                            },
                            trailing = { if (calls > 0) Tag("$calls calls") },
                            inset = if (step.what.startsWith("  ")) 36.dp else 12.dp,
                        )
                    }
                }
            }
        },
        detail = {
            if (selected == null) {
                EmptyState(Icons.Outlined.TouchApp, "Select a startup step", "Its class, the calls it makes and its manifest entry appear here.")
            } else {
                StepDetail(ws, s, selected)
            }
        },
    )
}

@Composable
private fun StepDetail(
    ws: WorkspaceState,
    s: AppStartup,
    step: StartupStep,
) {
    val component = ws.analysis.components.firstOrNull { it.name == step.target }
    val provider = s.providers.firstOrNull { it.name == step.target }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        DetailHeader(
            step.target.substringAfterLast('.'),
            step.what.trim(),
            leading = { IconBadge(Icons.Outlined.RocketLaunch, Tone.Primary, 48.dp) },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (component != null) TextButton(onClick = { ws.showComponent(component.name) }) { Text("Open provider") }
                    CopyButton(step.target, "class name")
                }
            },
        )
        MonoLine(step.target, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        Text(explain(step), style = MaterialTheme.typography.bodyMedium)

        provider?.let { p ->
            SubsectionLabel("Provider")
            KeyValueRow("Authorities", p.authorities.joinToString("\n").ifEmpty { null }, mono = true)
            KeyValueRow("initOrder", p.initOrder?.toString())
            KeyValueRow("Exported", if (p.exported) "yes" else "no")
            KeyValueRow("SDK", p.sdk)
            if (p.initializers.isNotEmpty()) {
                SubsectionLabel("androidx.startup initializers · ${p.initializers.size}")
                p.initializers.forEach { MonoLine(it, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 2.dp)) }
            }
        }

        val methods = methodsFor(s, step)
        methods.forEach { m ->
            SubsectionLabel(m.method.substringAfter("->").substringBefore("(") + " · ${m.calls.size} calls")
            MonoLine(m.method)
            Spacer(Modifier.height(6.dp))
            m.calls.forEach { call ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    MonoLine(call, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    val sdk = s.sdks.firstOrNull { it.via == call }
                    if (sdk != null) Tag(sdk.sdk, Tone.Secondary)
                }
            }
            if (m.more > 0) Text("… ${m.more} more", style = MaterialTheme.typography.bodySmall)
        }
        if (methods.isEmpty() && s.codeIndexed && step.what.trim().let { it.startsWith("Application.") || it.contains("onCreate") || it.contains("create") }) {
            SubsectionLabel("Calls")
            Text("Not overridden in app code: the framework default runs.", style = MaterialTheme.typography.bodyMedium)
        }

        val reached = s.sdks.filter { sdk -> methods.any { it.method == sdk.from } }
        if (reached.isNotEmpty()) {
            SubsectionLabel("SDKs reached from here")
            reached.forEach { KeyValueRow(it.sdk, "via ${it.via}", mono = true, keyWidth = 140.dp) }
        }

        SubsectionLabel("Manifest entry")
        val xml = component?.facts?.manifestXml ?: ws.analysis.applicationXml
        if (xml != null) {
            Row(verticalAlignment = Alignment.Top) {
                CodeBlock(highlighted(xml, Syntax.Xml), Modifier.weight(1f))
                CopyButton(xml, "manifest entry")
            }
        } else {
            Text("Not declared in the manifest (comes from code or the framework).", style = MaterialTheme.typography.bodyMedium)
        }
        WebActions(
            searchQuery = "${step.target} Android",
            question =
                "In the Android app ${ws.analysis.meta.packageName}, ${step.target} runs at startup (${step.what.trim()}). " +
                    "What does it usually do, and what should a security or privacy reviewer check about it?",
        )
    }
}

private fun explain(step: StartupStep): String {
    val w = step.what.trim()
    return when {
        w.startsWith("Zygote") -> "Runs once in the app zygote before any app process forks from it; used to preload code for isolated services."
        w.startsWith("AppComponentFactory") -> "Android asks this factory to create the Application, activities, services, receivers and providers."
        w == "Application.attachBaseContext" -> "First app code to run in the process, before any content provider; often installs multidex, crash handlers or locale wrappers."
        w.startsWith("ContentProvider") -> "Every provider of the main process is created before Application.onCreate, in initOrder order; many SDKs start themselves this way."
        w.contains("Initializer") -> "Run by androidx.startup's InitializationProvider during its onCreate."
        w == "Application.onCreate" -> "Runs after all providers; apps usually start SDKs, logging, dependency injection and background work here."
        else -> ""
    }
}
