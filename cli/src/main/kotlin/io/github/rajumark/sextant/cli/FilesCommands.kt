package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.ExitCode
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.index.CoverageRow
import io.github.rajumark.sextant.core.index.TextLine
import io.github.rajumark.sextant.core.model.ApkFile
import io.github.rajumark.sextant.core.output.SextantJson
import java.util.Locale

private val FILE_TYPES =
    listOf(
        "manifest",
        "resource-table",
        "dex",
        "resource",
        "native-library",
        "asset",
        "signature",
        "library-version",
        "kotlin-metadata",
        "service-loader",
        "meta-inf",
        "other",
    )

/** `sextant files <app>`: every zip entry of the APK, from its SQLite file. */
class FilesCommand : CoreCliktCommand(name = "files") {
    private val dir by argument(help = INDEX_INPUT).path()
    private val type by option("--type", help = "Only this file type").choice(*FILE_TYPES.toTypedArray())
    private val status by option("--status", help = "decoded: read by a Sextant decoder; listed: listed only").choice("decoded", "listed")
    private val search by option("--search", help = "Only paths or resource names containing this text")
    private val bySize by option("--by-size", help = "Largest files first").flag()
    private val limit by option("--limit", help = "Maximum rows (default 500)").int().default(500)
    private val json by option("--json", help = "One JSON object per file").flag()
    private val grep by option("--grep", help = "Lines containing this text in the APK's text files (assets, configs, licenses, …)")
    private val save by option("--save", help = "Write this file of the APK (its path, e.g. res/o-.png) to --out instead of listing")
    private val out by option("-o", "--out", help = "Where --save writes the file").path()

    override fun help(context: Context) =
        "List every file inside the APK: type, size, content, the resource it is (shrunk apps rename res/ files) and " +
            "which decoder read it. --grep searches the text files; --save writes one file out from the stored bytes."

    override fun run() =
        guarded {
            openIndex(dir).use { index ->
                grep?.let { text ->
                    val lines = index.textGrep(text, limit)
                    for (l in lines) {
                        if (json) {
                            println(SextantJson.compact.encodeToString(TextLine.serializer(), l))
                        } else {
                            println("${l.path}:${l.line}: ${if (l.text.length > 200) l.text.take(197) + "…" else l.text}")
                        }
                    }
                    System.err.println(
                        "${lines.size} matching line${if (lines.size == 1) "" else "s"}" +
                            if (lines.size == limit) " (stopped at --limit $limit)" else "",
                    )
                    return@use
                }
                save?.let { path ->
                    val dest =
                        out
                            ?: throw SextantException(
                                ExitCode.USAGE,
                                "--save needs --out",
                                "e.g. --save $path -o ${path.substringAfterLast('/')}",
                            )
                    val bytes =
                        index.fileContent(path)
                            ?: throw SextantException.input(
                                "$path is not stored in $dir",
                                "list the files with: sextant files $dir --search ${path.substringAfterLast('/')}",
                            )
                    java.nio.file.Files
                        .write(dest, bytes)
                    System.err.println("Saved $path → $dest (${size(bytes.size.toLong())})")
                    return@use
                }
                val (files, total) = index.files(type, status?.let { it == "decoded" }, search, bySize, limit)
                if (json) {
                    files.forEach { println(SextantJson.compact.encodeToString(ApkFile.serializer(), it)) }
                } else if (files.isNotEmpty()) {
                    print(
                        Table.render(
                            listOf("path", "type", "size", "content", "resource", "decoded by"),
                            files.map { f ->
                                val resource = f.resource?.let { r -> if (f.config.isNullOrEmpty()) r else "$r (${f.config})" }
                                listOf(f.path, f.type, size(f.sizeBytes), f.magic ?: "-", resource ?: "-", f.decodedBy ?: "-")
                            },
                        ),
                    )
                }
                System.err.println(
                    "${files.size} of $total file${if (total == 1) "" else "s"}" +
                        if (total > files.size) " (stopped at --limit $limit)" else "",
                )
            }
        }
}

/** `sextant coverage <dir>`: how much of the APK Sextant decoded, per file type. */
class CoverageCommand : CoreCliktCommand(name = "coverage") {
    private val dir by argument(help = INDEX_INPUT).path()
    private val json by option("--json", help = "One JSON object per file type").flag()

    override fun help(context: Context) =
        "Show how much of the APK was decoded into rows: zip entries and bytes per file type, and examples of files " +
            "that are only listed. Every file is always listed with its size and hash."

    override fun run() =
        guarded {
            openIndex(dir).use { index ->
                val rows = index.coverage()
                if (json) {
                    rows.forEach { println(SextantJson.compact.encodeToString(CoverageRow.serializer(), it)) }
                    return@use
                }
                val files = rows.sumOf { it.files }
                val decoded = rows.sumOf { it.decoded }
                val bytes = rows.sumOf { it.bytes }
                val decodedBytes = rows.sumOf { it.decodedBytes }
                val name = listOfNotNull(index.meta("label"), index.meta("version_name")).joinToString(" ")
                println("${name.ifEmpty { "APK" }}: $files files, ${size(bytes)} uncompressed")
                println(
                    "decoded: $decoded of $files files (${pct(
                        decoded.toLong(),
                        files.toLong(),
                    )}), ${size(decodedBytes)} of ${size(bytes)} (${pct(decodedBytes, bytes)})",
                )
                println()
                print(
                    Table.render(
                        listOf("type", "files", "decoded", "size", "decoded size"),
                        rows.map { r ->
                            listOf(
                                r.type,
                                r.files.toString(),
                                "${r.decoded} (${pct(r.decoded.toLong(), r.files.toLong())})",
                                size(r.bytes),
                                "${size(r.decodedBytes)} (${pct(r.decodedBytes, r.bytes)})",
                            )
                        },
                    ),
                )
                val listed = rows.filter { it.listedOnly.isNotEmpty() }
                if (listed.isNotEmpty()) {
                    println()
                    println("Listed only (size and hash, no decoder yet), largest first:")
                    for (r in listed) {
                        val more = r.files - r.decoded - r.listedOnly.size
                        println("  %-16s %s%s".format(r.type, r.listedOnly.joinToString(", "), if (more > 0) ", … $more more" else ""))
                    }
                    println("  (sextant files <dir> --status listed shows them all)")
                }
            }
        }

    private fun pct(
        part: Long,
        whole: Long,
    ): String =
        when {
            whole == 0L -> "-"
            part == whole -> "100%"
            else -> "%.1f%%".format(Locale.ROOT, part * 100.0 / whole).let { if (it == "100.0%") "99.9%" else it }
        }
}

internal fun size(bytes: Long): String =
    when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(Locale.ROOT, bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(Locale.ROOT, bytes / 1024.0 / 1024)
        else -> "%.2f GB".format(Locale.ROOT, bytes / 1024.0 / 1024 / 1024)
    }
