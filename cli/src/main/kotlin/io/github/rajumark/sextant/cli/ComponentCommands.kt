package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.ExitCode
import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.insights.ComponentReview
import io.github.rajumark.sextant.core.insights.ComponentReviews
import io.github.rajumark.sextant.core.model.Component
import io.github.rajumark.sextant.core.output.InfoReports
import io.github.rajumark.sextant.core.output.ManifestXml
import io.github.rajumark.sextant.core.output.SextantJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import java.nio.file.Path

/** From an APK (through the index cache) or a .sqlite file. */
private fun load(target: Path): Extraction = loadExtraction(target)

/** The component, its review, and the `<activity>` attributes that are set, as one JSON object. */
private fun json(
    c: Component,
    r: ComponentReview,
): String =
    JsonObject(
        mapOf(
            "component" to SextantJson.compact.encodeToJsonElement(c),
            "review" to SextantJson.compact.encodeToJsonElement(r),
            "attributes" to JsonObject(ComponentReviews.attributes(c).associate { (k, v) -> k to JsonPrimitive(v) }),
        ),
    ).toString()

/** `sextant components <apk|dir>`: every manifest component with its verdict tags, filterable. */
class ComponentsCommand : CoreCliktCommand(name = "components") {
    private val target by argument(help = INDEX_INPUT).path()
    private val types by option(
        "--type",
        help = "Only this type; repeat for several (activity includes aliases with --type activity-alias)",
    ).choice(*InfoReports.TYPES.toTypedArray())
        .multiple()
    private val tags by option("--tag", help = "Only components with this verdict; repeat to require several")
        .choice(*ComponentReview.TAGS.toTypedArray())
        .multiple()
    private val exported by option("--exported", help = "Only exported components").flag()
    private val search by option("--search", help = "Only names, deep links, authorities, permissions or processes containing this text")
    private val json by option("--json", help = "One JSON object per component: component, review, attributes").flag()

    override fun help(context: Context) =
        "List activities, aliases, services, receivers and providers with verdicts: launcher, app-link, deep-link, " +
            "hidden-entry, task-hijack, lock-screen, exported-by-default, guarded, disabled."

    override fun run() =
        guarded {
            val extraction = load(target)
            val reviews = ComponentReviews.of(extraction.components, extraction.meta.`package`)
            val q = search?.trim().orEmpty()
            val shown =
                extraction.components.zip(reviews).filter { (c, r) ->
                    (types.isEmpty() || c.type in types) &&
                        r.tags.containsAll(tags) &&
                        (!exported || c.exported) &&
                        (q.isEmpty() || matches(c, r, q))
                }
            if (json) {
                shown.forEach { (c, r) -> println(json(c, r)) }
            } else if (shown.isNotEmpty()) {
                print(
                    Table.render(
                        listOf("type", "name", "exported", "tags", "entry"),
                        shown.map { (c, r) ->
                            listOf(
                                c.type,
                                c.name,
                                if (!c.exported) {
                                    "no"
                                } else if (c.exportedSource == "default") {
                                    "default"
                                } else {
                                    "yes"
                                },
                                r.tags.joinToString(","),
                                r.deepLinks.firstOrNull() ?: c.authorities.firstOrNull() ?: r.entryPoints.firstOrNull() ?: "",
                            )
                        },
                    ),
                )
            }
            System.err.println("${shown.size} of ${extraction.components.size} components")
        }

    private fun matches(
        c: Component,
        r: ComponentReview,
        q: String,
    ) = c.name.contains(q, true) ||
        r.deepLinks.any { it.contains(q, true) } ||
        c.authorities.any { it.contains(q, true) } ||
        listOfNotNull(c.permission, c.readPermission, c.writePermission, c.process).any { it.contains(q, true) }
}

/** `sextant component <apk|dir> <name>`: one component in layers: verdicts and why, attributes, entry points, adb, raw. */
class ComponentCommand : CoreCliktCommand(name = "component") {
    private val target by argument(help = INDEX_INPUT).path()
    private val name by argument(help = "Class name, or a unique suffix such as .MainActivity")
    private val json by option("--json", help = "One JSON object: component, review, attributes").flag()

    override fun help(context: Context) =
        "Explain one component: verdicts with reasons, activity attributes, intent filters in plain words, deep links, " +
            "aliases and ready-to-run adb commands."

    override fun run() =
        guarded {
            val extraction = load(target)
            val all = extraction.components
            val matches =
                all.filter { it.name == name }.ifEmpty {
                    all.filter {
                        it.name.endsWith(
                            if (name.startsWith('.')) name else ".$name",
                        )
                    }
                }
            val c =
                when (matches.size) {
                    1 -> matches.single()
                    0 -> throw SextantException(ExitCode.USAGE, "no component named $name", "list them with: sextant components $target")
                    else ->
                        throw SextantException(
                            ExitCode.USAGE,
                            "$name matches ${matches.size} components",
                            "use the full name: " + matches.take(5).joinToString { it.name },
                        )
                }
            val pkg = extraction.meta.`package`
            val r = ComponentReviews.of(all, pkg)[all.indexOf(c)]
            if (json) {
                println(json(c, r))
                return@guarded
            }
            println("${c.name}  (${c.type})")
            val exported =
                if (!c.exported) {
                    "no"
                } else if (c.exportedSource == "default") {
                    "yes, by default"
                } else {
                    "yes"
                }
            println("Exported: $exported · enabled: ${if (c.enabled) "yes" else "no"}")
            section("Verdicts", r.tags.map { "$it: ${r.reasons.getValue(it)}" }.ifEmpty { listOf("none") })
            val props =
                listOfNotNull(
                    c.permission?.let { "permission" to it },
                    c.readPermission?.let { "readPermission" to it },
                    c.writePermission?.let { "writePermission" to it },
                    c.process?.let { "process" to it },
                    c.targetActivity?.let { "targetActivity" to it },
                    c.label?.let { "label" to it },
                ) + ComponentReviews.attributes(c)
            if (props.isNotEmpty()) {
                val width = props.maxOf { it.first.length }
                section("Attributes", props.map { (k, v) -> k.padEnd(width) + "  " + v })
            }
            if (r.aliases.isNotEmpty()) section("Aliases pointing here", r.aliases)
            section(
                "Entry points",
                r.entryPoints.ifEmpty {
                    listOf(
                        "No intent filters: only explicit intents" + if (c.exported) " (from any app)." else " from this app.",
                    )
                },
            )
            if (r.deepLinks.isNotEmpty()) section("Deep links", r.deepLinks)
            if (r.appLinkHosts.isNotEmpty()) section("App Link hosts (need /.well-known/assetlinks.json)", r.appLinkHosts)
            if (r.adb.isNotEmpty()) section("adb", r.adb.map { "# ${it.label}\n  ${it.command}" })
            val element = ManifestXml.component(extraction.manifest, c.name)
            if (element != null) section("Manifest entry", ManifestXml.render(element).trimEnd().lines())
        }

    private fun section(
        title: String,
        lines: List<String>,
    ) {
        println()
        println("## $title")
        lines.forEach { println("  $it") }
    }
}
