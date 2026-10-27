package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.Extractor
import io.github.rajumark.sextant.core.index.IndexReader
import io.github.rajumark.sextant.core.insights.AppStartup
import io.github.rajumark.sextant.core.insights.AppStartups
import io.github.rajumark.sextant.core.output.OutputFiles
import io.github.rajumark.sextant.core.output.OutputReader
import io.github.rajumark.sextant.core.output.SextantJson
import java.nio.file.Files

/** `sextant app <dir|apk>`: the Application class and everything that runs when the app process starts. */
class AppCommand : CoreCliktCommand(name = "app") {
    private val target by argument(help = "An extracted folder (follows startup calls with its index), or an APK (manifest only)").path()
    private val json by option("--json", help = "One JSON object").flag()

    override fun help(context: Context) =
        "Show the Application class and the startup order: AppComponentFactory, attachBaseContext, content providers " +
            "(androidx.startup initializers), onCreate; with an extracted folder, also their calls and the SDKs they start."

    override fun run() =
        guarded {
            val dir = Files.isDirectory(target)
            val extraction = if (dir) OutputReader.read(target) else Extractor.extract(target, code = null)
            val hasIndex = dir && Files.exists(target.resolve(OutputFiles.INDEX))
            val startup =
                if (hasIndex) IndexReader.open(target).use { AppStartups.build(extraction, it) } else AppStartups.build(extraction, null)
            if (json) println(SextantJson.compact.encodeToString(AppStartup.serializer(), startup)) else print(human(startup))
        }

    private fun human(s: AppStartup) =
        buildString {
            appendLine("Application class: ${s.applicationClass ?: "none (android.app.Application)"}")
            if (s.superclasses.size > 1) appendLine("Extends: " + s.superclasses.drop(1).joinToString(" → "))
            s.appComponentFactory?.let { appendLine("AppComponentFactory: $it") }
            s.zygotePreload?.let { appendLine("Zygote preload: $it") }
            s.backupAgent?.let { appendLine("Backup agent: $it") }
            s.manageSpaceActivity?.let { appendLine("Manage space activity: $it") }
            s.process?.let { appendLine("Main process: $it") }
            appendLine()
            appendLine("## Startup order")
            s.steps.forEach { appendLine("  ${it.order.toString().padStart(2)}. ${it.what}: ${it.target}") }
            if (s.providers.isNotEmpty()) {
                appendLine()
                appendLine("## Content providers started with the process (${s.providers.size})")
                s.providers.forEach { p ->
                    val extra = listOfNotNull(p.initOrder?.let { "initOrder $it" }, p.sdk, if (p.exported) "exported" else null)
                    appendLine("  ${p.name}" + if (extra.isEmpty()) "" else "  (${extra.joinToString(", ")})")
                    p.initializers.forEach { appendLine("      initializer $it") }
                }
            }
            if (!s.codeIndexed) {
                appendLine()
                appendLine("(No code index: run `sextant extract` and pass the folder to see startup calls and SDKs.)")
                return@buildString
            }
            if (s.sdks.isNotEmpty()) {
                appendLine()
                appendLine("## SDKs reached at startup (${s.sdks.size})")
                s.sdks.forEach { appendLine("  ${it.sdk} (${it.category})  via ${it.via}") }
            }
            s.methods.forEach { m ->
                appendLine()
                appendLine("## ${m.method}")
                if (m.calls.isEmpty()) appendLine("  (no calls)")
                m.calls.forEach { appendLine("  → $it") }
                if (m.more > 0) appendLine("  … ${m.more} more")
            }
        }
}
