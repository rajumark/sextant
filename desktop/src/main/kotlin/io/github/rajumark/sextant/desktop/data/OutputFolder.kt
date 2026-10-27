package io.github.rajumark.sextant.desktop.data

import io.github.rajumark.sextant.core.output.OutputFiles
import io.github.rajumark.sextant.desktop.model.OutputFile
import io.github.rajumark.sextant.desktop.model.OutputFormat
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/** Lists the files of an output folder for the Output screen, reading content only when previewed. */
object OutputFolder {
    /** Previews stop here; code JSONL files of large apps run to hundreds of MB. */
    const val PREVIEW_LINES = 2_000

    private val descriptions =
        mapOf(
            OutputFiles.SUMMARY to "Overview for people and agents. Read this first.",
            OutputFiles.META to "Identity, signing, flags and APK layout.",
            OutputFiles.MANIFEST to "The full decoded manifest tree.",
            OutputFiles.PERMISSIONS to "One permission per line.",
            OutputFiles.COMPONENTS to "One component per line.",
            OutputFiles.INDEX to "SQLite index for sextant sql, find and class.",
        )

    fun list(dir: Path): List<OutputFile> {
        if (!Files.isDirectory(dir)) return emptyList()
        val files =
            Files.walk(dir, 3).use { s ->
                s.filter { it.isRegularFile() && !it.name.startsWith(".") }.toList()
            }
        val order = OutputFiles.ALL + OutputFiles.INDEX
        return files
            .map { dir.relativize(it).toString().replace('\\', '/') to it }
            .sortedWith(compareBy({ order.indexOf(it.first).let { i -> if (i < 0) order.size else i } }, { it.first }))
            .map { (rel, path) -> file(rel, path) }
    }

    private fun file(
        rel: String,
        path: Path,
    ): OutputFile {
        val format =
            when (path.extension) {
                "md" -> OutputFormat.Markdown
                "json" -> OutputFormat.Json
                "jsonl" -> OutputFormat.JsonLines
                else -> OutputFormat.Binary
            }
        val description =
            descriptions[rel] ?: when {
                rel.startsWith("code/") -> "Code structure, one record per line."
                rel.startsWith("insights/") -> "Detected libraries, SDKs and endpoints."
                rel.startsWith("native/") -> "Native libraries and their symbols."
                rel.startsWith("resources/") -> "Resolved resource values."
                else -> "Written by sextant extract."
            }
        return OutputFile(rel, description, format, runCatching { path.fileSize() }.getOrDefault(0L)) { preview(path, format) }
    }

    private fun preview(
        path: Path,
        format: OutputFormat,
    ): String {
        if (format == OutputFormat.Binary) {
            return "Binary file. Query it with:\n  sextant sql ${path.parent} \"SELECT name FROM sqlite_master\""
        }
        val out = StringBuilder()
        var lines = 0
        Files.newBufferedReader(path).use { r ->
            while (lines < PREVIEW_LINES) {
                val line = r.readLine() ?: return out.toString()
                out.append(line).append('\n')
                lines++
            }
            if (r.readLine() != null) out.append("… preview stops at $PREVIEW_LINES lines; open the file for the rest\n")
        }
        return out.toString()
    }
}
