package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.rajumark.sextant.core.index.CacheEntry
import io.github.rajumark.sextant.core.index.IndexCache
import io.github.rajumark.sextant.core.output.SextantJson

/** `sextant cache`: the indexes built for APKs given directly to a command. */
class CacheCommand : CoreCliktCommand(name = "cache") {
    private val clear by option("--clear", help = "Delete every cached index").flag()
    private val json by option("--json", help = "One JSON object per cached index").flag()

    override fun help(context: Context) =
        "List the indexes Sextant keeps for APKs given directly to a command (one per APK, keyed by its SHA-256), " +
            "or delete them with --clear. Set SEXTANT_CACHE to move the folder."

    override fun run() =
        guarded {
            if (clear) {
                val freed = IndexCache.clear()
                println("Cleared ${IndexCache.root()} (${size(freed)} freed)")
                return@guarded
            }
            val entries = IndexCache.entries()
            if (json) {
                entries.forEach { println(SextantJson.compact.encodeToString(CacheEntry.serializer(), it)) }
                return@guarded
            }
            println("Cache: ${IndexCache.root()}")
            if (entries.isEmpty()) {
                println("No cached indexes yet. Any command given an APK builds one, e.g. sextant info app.apk")
                return@guarded
            }
            println()
            print(
                Table.render(
                    listOf("app", "version", "package", "size", "sha256", "state"),
                    entries.map {
                        listOf(
                            it.label ?: "?",
                            it.versionName ?: "?",
                            it.`package` ?: "?",
                            size(it.sizeBytes),
                            it.sha256.take(12),
                            if (it.current) "ready" else "rebuilt on next use",
                        )
                    },
                ),
            )
            println()
            println("${entries.size} index${if (entries.size == 1) "" else "es"}, ${size(entries.sumOf { it.sizeBytes })}")
        }
}
