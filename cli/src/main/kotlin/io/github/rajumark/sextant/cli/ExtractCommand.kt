package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.ExitCode
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.index.IndexBuilder
import io.github.rajumark.sextant.core.output.InfoReports
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

/** `sextant extract <app.apk>`: APK in, one SQLite file out. */
class ExtractCommand : CoreCliktCommand(name = "extract") {
    private val apk by argument(help = "APK or split bundle to analyze").path()
    private val out by option("-o", "--out", help = "Output file (default: ./<apk-name>.sqlite)").path()
    private val force by option("--force", help = "Overwrite the output file if it exists").flag()
    private val quiet by option("--quiet", "-q", help = "No progress output").flag()
    private val noCode by option("--no-code", help = "Skip code structure (classes, methods, calls) for a fast manifest-level file").flag()

    override fun help(context: Context) =
        "Extract everything Sextant reads from an APK into one SQLite file. Every other command takes that file " +
            "(or the APK itself)."

    override fun run() =
        guarded {
            val started = System.nanoTime()
            val target = out ?: Path.of("${apk.nameWithoutExtension}.sqlite")
            // Fail fast on the output before spending time on the APK.
            val folder = Files.isDirectory(target)
            if (folder) throw SextantException(ExitCode.OUTPUT, "$target is a folder", "pass a file name such as app.sqlite")
            val exists = Files.exists(target)
            if (exists && !force) throw SextantException(ExitCode.OUTPUT, "$target already exists", "pass --force to overwrite it")
            if (!Files.isRegularFile(apk)) throw SextantException.input("file not found: $apk")
            val extraction = IndexBuilder.build(apk, target, includeCode = !noCode)
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
                progress(
                    "Files             ${extraction.files.size} in the APK, ${extraction.files.count { it.decodedBy != null }} decoded",
                )
                m.warnings.forEach { System.err.println("! $it") }
                progress("Wrote $target  ${size(Files.size(target))} in %.1fs".format(elapsed))
            }
        }

    private fun progress(line: String) = System.err.println("✓ $line")
}
