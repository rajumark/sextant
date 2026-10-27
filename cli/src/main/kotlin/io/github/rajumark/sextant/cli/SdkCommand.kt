package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.Extractor
import io.github.rajumark.sextant.core.insights.SdkEntry
import io.github.rajumark.sextant.core.insights.SdkInventory
import io.github.rajumark.sextant.core.insights.SdkProfile
import io.github.rajumark.sextant.core.output.SextantJson
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * `sextant sdks <apk|dir>...`: SDKs and libraries with their roles and versions, for one app or compared across many
 * ("which apps ship OkHttp below 4.9?", "which apps have both ads and analytics?").
 */
class SdkCommand : CoreCliktCommand(name = "sdks") {
    private val targets by argument(help = "Extracted folders and/or APKs (APKs are analyzed in memory)").path().multiple(required = true)
    private val categories by option("--category", help = "Only apps that have an SDK in this category; repeat to require several (ads, analytics, …)").multiple()
    private val sdks by option("--sdk", help = "Only apps that contain this SDK or library (name or group:artifact, case-insensitive substring); repeat to require several").multiple()
    private val below by option("--below", help = "With one --sdk: only apps whose version of it is lower than this (e.g. 4.9.0)")
    private val json by option("--json", help = "One JSON object per app (its SDK profile)").flag()

    override fun help(context: Context) =
        "Show SDKs and libraries by role (analytics, ads, crash reporting, auth, database, UI, …) with versions when an artifact " +
            "proves them. With several apps, show which SDKs appear where and filter apps by SDK, category or version."

    override fun run() =
        guarded {
            if (below != null && sdks.size != 1) throw UsageError("--below needs exactly one --sdk")
            val profiles = targets.map(::profile)
            val shown =
                profiles.filter { p ->
                    p.categories.containsAll(categories) &&
                        sdks.all { q -> matching(p, q).isNotEmpty() } &&
                        (below == null || matching(p, sdks.single()).any { e -> e.version?.let { v -> SdkInventory.compareVersions(v, below!!)?.let { it < 0 } } == true })
                }
            when {
                json -> shown.forEach { println(SextantJson.compact.encodeToString(SdkProfile.serializer(), it)) }
                profiles.size == 1 -> print(single(profiles.single()))
                else -> print(across(shown))
            }
            if (profiles.size > 1) System.err.println("${shown.size} of ${profiles.size} apps match")
        }

    private fun matching(
        p: SdkProfile,
        q: String,
    ): List<SdkEntry> = p.entries.filter { it.name.contains(q, ignoreCase = true) || it.artifacts.any { a -> a.contains(q, ignoreCase = true) } }

    private fun profile(target: Path): SdkProfile {
        if (!Files.isDirectory(target)) {
            val e = Extractor.extract(target)
            val i = e.insights!!
            return SdkInventory.of(target.name, e.meta.`package`, e.meta.versionName, i.sdks, i.libraries)
        }
        return SdkInventory.fromFolder(target)
    }

    private fun single(p: SdkProfile) =
        buildString {
            appendLine("${p.packageName ?: p.app} ${p.versionName.orEmpty()}")
            appendLine("Roles: " + p.categories.joinToString().ifEmpty { "none recognized" })
            appendLine()
            val sdkRows = p.entries.filter { it.kind == "sdk" }
            appendLine("## SDKs (${sdkRows.size})")
            if (sdkRows.isNotEmpty()) {
                append(Table.render(listOf("category", "sdk", "version", "classes", "evidence"), sdkRows.map { listOf(it.category, it.name, it.version ?: "?", it.classes.toString(), it.versionEvidence.orEmpty()) }))
            }
            val libs = p.entries.filter { it.kind == "library" }
            appendLine()
            appendLine("## Libraries with versions (${libs.size})")
            if (libs.isNotEmpty()) append(Table.render(listOf("category", "library", "version", "evidence"), libs.map { listOf(it.category, it.name, it.version.orEmpty(), it.versionEvidence.orEmpty()) }))
        }

    private fun across(profiles: List<SdkProfile>) =
        buildString {
            val rows =
                profiles
                    .flatMap { p -> p.entries.filter { it.kind == "sdk" || sdks.isNotEmpty() }.map { p to it } }
                    .filter { (_, e) -> sdks.isEmpty() || sdks.any { q -> e.name.contains(q, true) || e.artifacts.any { a -> a.contains(q, true) } } }
                    .groupBy { it.second.name }
                    .entries
                    .sortedWith(compareByDescending<Map.Entry<String, List<Pair<SdkProfile, SdkEntry>>>> { it.value.size }.thenBy { it.key })
            append(
                Table.render(
                    listOf("sdk", "category", "apps", "versions", "in"),
                    rows.map { (name, uses) ->
                        listOf(
                            name,
                            uses.first().second.category,
                            uses.size.toString(),
                            uses.mapNotNull { it.second.version }.distinct().joinToString(),
                            uses.joinToString { it.first.packageName ?: it.first.app },
                        )
                    },
                ),
            )
        }
}
