package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.Bundles
import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.Extractor
import io.github.rajumark.sextant.core.model.obfuscationText
import io.github.rajumark.sextant.core.output.IconText
import io.github.rajumark.sextant.core.output.InfoReport
import io.github.rajumark.sextant.core.output.InfoReports
import io.github.rajumark.sextant.core.output.OutputReader
import io.github.rajumark.sextant.core.output.SextantJson
import java.nio.file.Files

class InfoCommand : CoreCliktCommand(name = "info") {
    private val target by argument(help = "An extracted folder, or an APK (analyzed in memory, nothing written)").path()
    private val json by option("--json", help = "Machine-readable JSON on stdout").flag()

    override fun help(context: Context) = "Show a short summary of an APK or an extracted folder."

    override fun run() =
        guarded {
            val extraction = if (Files.isDirectory(target)) OutputReader.read(target) else Extractor.extract(target)
            val report = InfoReports.of(extraction)
            if (json) println(SextantJson.compact.encodeToString(InfoReport.serializer(), report)) else print(human(report, extraction))
        }

    private fun human(
        r: InfoReport,
        extraction: Extraction,
    ): String {
        val m = r.meta
        val c = r.counts
        val rows =
            listOf(
                "Package" to (m.`package` ?: "?"),
                "Label" to (m.label ?: "?"),
                "Version" to "${m.versionName ?: "?"} (${m.versionCode ?: "?"})",
                "SDK" to "min ${m.minSdk ?: "?"} · target ${m.targetSdk ?: "?"}",
                "Signing" to
                    (
                        m.signing.schemes
                            .joinToString()
                            .ifEmpty { "unsigned" }
                    ) +
                    (
                        m.signing.certificates
                            .firstOrNull()
                            ?.let { " · sha256 ${it.sha256.take(11)}…" } ?: ""
                    ),
                "Permissions" to
                    "${c.permissionsRequested} requested (${c.permissionsDangerous} dangerous) · ${c.permissionsDeclared} declared",
                "Components" to c.components.entries.joinToString(" · ") { "${it.value} ${it.key}" },
                "Exported" to "${c.exported} components · ${c.withDeepLinks} with deep links",
                "Launcher" to r.launcher.joinToString().ifEmpty { "none" },
                "DEX / ABIs" to "${m.layout.dexFiles} dex · ${m.layout.nativeAbis.joinToString().ifEmpty { "no native code" }}",
                *listOfNotNull(m.bundle?.let { "Bundle" to Bundles.describe(it) }).toTypedArray(),
                "Icon" to (m.icons?.icon?.let { IconText.describe(it) } ?: "not set"),
                "Manifest" to
                    (
                        "${extraction.features.count { it.kind == "feature" }} features · " +
                            "${extraction.features.count { it.kind != "feature" }} libraries · ${extraction.metadata.size} metadata · " +
                            "${extraction.queries.size} queries" + (m.flags.sharedUserId?.let { " · sharedUserId $it" } ?: "")
                    ),
                "Frameworks" to (
                    m.insights
                        ?.frameworks
                        ?.joinToString()
                        ?.ifEmpty { "none detected" } ?: "?"
                ),
                "Insights" to
                    (
                        m.insights?.let {
                            "${it.sdks} SDKs · ${it.libraries} libraries · ${it.endpoints} URLs on ${it.hosts} hosts · " +
                                "${it.nativeLibraries} native libs · ${it.locales.size} locales"
                        } ?: "?"
                    ),
                "Code" to
                    (
                        m.code?.let {
                            "${it.classes} classes · ${it.methods} methods · ${it.callEdges} call edges · ${it.kotlinClasses} Kotlin classes"
                        }
                            ?: "not extracted (--no-code)"
                    ),
                *listOfNotNull(m.code?.obfuscationText()?.let { "Obfuscation" to it }).toTypedArray(),
            )
        return buildString {
            rows.forEach { (k, v) -> appendLine("%-12s %s".format(k, v)) }
            m.warnings.forEach { appendLine("! $it") }
        }
    }
}
