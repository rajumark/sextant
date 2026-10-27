package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.output.InfoReports
import io.github.rajumark.sextant.core.output.OutputWriter
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

class ExtractCommand : CoreCliktCommand(name = "extract") {
    private val apk by argument(help = "APK file to analyze").path()
    private val out by option("-o", "--out", help = "Output directory (default: ./<apk-name>.sextant/)").path()
    private val force by option("--force", help = "Overwrite Sextant's files in a non-empty output directory").flag()
    private val quiet by option("--quiet", "-q", help = "No progress output").flag()
    private val noIndex by option("--no-index", help = "Skip index.sqlite (no sql/find/class)").flag()
    private val noCode by option("--no-code", help = "Skip code structure (classes, methods, calls); manifest-level files only").flag()

    override fun help(context: Context) = "Extract structured data from an APK into a folder."

    override fun run() =
        guarded {
            val started = System.nanoTime()
            val dir = out ?: Path.of("${apk.nameWithoutExtension}.sextant")
            // Fail fast on the output directory before spending time on the APK.
            OutputWriter.prepare(dir, force)
            val written = OutputWriter.extractTo(apk, dir, force, includeCode = !noCode, includeIndex = !noIndex)
            val extraction = written.extraction
            val files = written.files
            if (!quiet) {
                val r = InfoReports.of(extraction)
                val m = r.meta
                val elapsed = (System.nanoTime() - started) / 1_000_000_000.0
                progress("Read APK          %.1f MB · sha256 %s".format(m.file.sizeBytes / 1_048_576.0, m.file.sha256.take(12)))
                progress("Decoded manifest  ${m.`package`} ${m.versionName} (${m.versionCode})")
                progress("Permissions       ${r.counts.permissionsRequested} requested · ${r.counts.permissionsDeclared} declared")
                progress("Components        ${extraction.components.size} (${r.counts.exported} exported)")
                progress("Signing           ${m.signing.schemes.joinToString().ifEmpty { "unsigned" }}")
                m.code?.let { code ->
                    progress("Code              ${code.classes} classes · ${code.methods} methods · ${code.callEdges} call edges")
                }
                m.warnings.forEach { System.err.println("! $it") }
                progress("Wrote $dir/     ${files.size} files in %.1fs".format(elapsed))
            }
        }

    private fun progress(line: String) = System.err.println("✓ $line")
}
