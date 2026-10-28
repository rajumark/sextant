package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.ExitCode
import io.github.rajumark.sextant.core.Extractor
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.insights.StringCatalog
import io.github.rajumark.sextant.core.model.ResourceHarvest
import io.github.rajumark.sextant.core.model.ResourceRef
import io.github.rajumark.sextant.core.output.OutputReader
import io.github.rajumark.sextant.core.output.SextantJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.nio.file.Files

/** One kind of harvested resource: its records, a name for each, one table row, and the `@type/name` refs it answers to. */
private class Kind<T>(
    val serializer: KSerializer<T>,
    val records: (ResourceHarvest) -> List<T>,
    val key: (T) -> String,
    val columns: List<String>,
    val row: (T) -> List<String>,
    val refTypes: List<String>,
    val text: (T) -> String,
)

private inline fun <reified T> kind(
    noinline records: (ResourceHarvest) -> List<T>,
    noinline key: (T) -> String,
    columns: List<String>,
    noinline row: (T) -> List<String>,
    refTypes: List<String> = emptyList(),
    noinline text: (T) -> String = { "" },
) = Kind(serializer(), records, key, columns, row, refTypes, text)

private val KINDS: Map<String, Kind<*>> =
    linkedMapOf(
        "strings" to
            kind(
                { it.strings },
                { it.name },
                listOf("name", "kind", "config", "item", "value"),
                {
                    listOf(
                        it.name,
                        it.kind,
                        it.config.ifEmpty {
                            "default"
                        },
                        it.item.orEmpty(),
                        it.value,
                    )
                },
                listOf("string", "plurals", "array"),
                { it.value },
            ),
        "string-keys" to
            kind(
                { StringCatalog.of(it) },
                { it.name },
                listOf("name", "kind", "locales", "used by", "flags", "value"),
                { listOf(it.name, it.kind, it.locales.size.toString(), it.usedBy.size.toString(), it.flags.joinToString(","), it.value) },
                listOf("string", "plurals", "array"),
                { it.values.values.joinToString(" ") + " " + it.flags.joinToString(" ") },
            ),
        "ids" to
            kind(
                { it.ids },
                { it.name },
                listOf("id", "type", "name", "configs"),
                { listOf(it.id, it.type, it.name, it.configs.size.toString()) },
            ),
        "styles" to
            kind(
                { it.styles },
                { it.name },
                listOf("name", "config", "parent", "items", "used by"),
                {
                    listOf(
                        it.name,
                        it.config.ifEmpty {
                            "default"
                        },
                        it.parent.orEmpty(),
                        it.items.size.toString(),
                        it.usedBy.joinToString(),
                    )
                },
                listOf("style"),
                { it.items.joinToString(" ") { i -> "${i.raw} ${i.resolved.orEmpty()}" } },
            ),
        "drawables" to
            kind(
                { it.drawables },
                { it.name },
                listOf("type", "name", "config", "format", "bytes", "size"),
                {
                    listOf(
                        it.type,
                        it.name,
                        it.config.ifEmpty { "default" },
                        it.format,
                        it.bytes.toString(),
                        if (it.width !=
                            null
                        ) {
                            "${it.width}x${it.height}"
                        } else {
                            ""
                        },
                    )
                },
                listOf("drawable", "mipmap"),
            ),
        "layouts" to
            kind(
                { it.layouts },
                { it.name },
                listOf("name", "config", "root", "views", "custom views", "fragments"),
                {
                    listOf(
                        it.name,
                        it.config.ifEmpty {
                            "default"
                        },
                        it.root.orEmpty(),
                        it.viewCount.toString(),
                        it.customViews.size.toString(),
                        it.fragments.joinToString(),
                    )
                },
                listOf("layout"),
                { l -> l.views.joinToString(" ") { v -> v.attributes.joinToString(" ") { "${it.raw} ${it.resolved.orEmpty()}" } } },
            ),
        "menus" to
            kind(
                { it.menus },
                { it.name },
                listOf("name", "config", "items", "titles"),
                { m ->
                    listOf(
                        m.name,
                        m.config.ifEmpty { "default" },
                        m.items.size.toString(),
                        m.items
                            .mapNotNull {
                                it.title?.let { t ->
                                    t.resolved
                                        ?: t.raw
                                }
                            }.joinToString(" · "),
                    )
                },
                listOf("menu"),
                { m -> m.items.joinToString(" ") { "${it.title?.raw} ${it.title?.resolved}" } },
            ),
        "navigation" to
            kind(
                { it.navigation },
                { it.name },
                listOf("name", "start", "destinations", "deep links"),
                { n ->
                    listOf(
                        n.name,
                        n.startDestination.orEmpty(),
                        n.destinations.size.toString(),
                        n.destinations.flatMap { it.deepLinks }.joinToString(),
                    )
                },
                listOf("navigation"),
                { n -> n.destinations.joinToString(" ") { "${it.className} ${it.label?.resolved}" } },
            ),
        "assets" to
            kind(
                { it.assets },
                { it.path },
                listOf("path", "kind", "bytes", "magic", "hidden code"),
                { listOf(it.path, it.kind, it.bytes.toString(), it.magic.orEmpty(), if (it.hiddenCode) "yes" else "") },
            ),
        "xml" to
            kind(
                { it.xml },
                { it.name },
                listOf("name", "config", "kind", "summary"),
                { x ->
                    listOf(x.name, x.config.ifEmpty { "default" }, x.kind, x.summary.entries.joinToString(" ") { "${it.key}=${it.value}" })
                },
                listOf("xml"),
                { it.xml },
            ),
        "refs" to
            kind(
                { it.refs },
                { it.ref },
                listOf("ref", "from kind", "from", "attribute"),
                { listOf(it.ref, it.fromKind, it.from, it.attribute) },
            ),
    )

/** `sextant res <apk|dir> <kind> [name]`: harvested resources, searchable, with who references each one. */
class ResourceCommand : CoreCliktCommand(name = "res") {
    private val target by argument(help = "An extracted folder, or an APK (analyzed in memory, nothing written)").path()
    private val kindName by argument(help = "What to list: ${KINDS.keys.joinToString()}").choice(*KINDS.keys.toTypedArray())
    private val name by argument(help = "Show one entry in full (all configurations) and what references it").optional()
    private val search by option("--search", help = "Only entries whose name or value contains this text")
    private val flag by option(
        "--flag",
        help = "string-keys only: ${StringCatalog.FLAGS.joinToString()}",
    ).choice(*StringCatalog.FLAGS.toTypedArray())
    private val config by option("--config", help = "Only this configuration, e.g. fr, night, default")
    private val limit by option("--limit", help = "Maximum rows (default 500)").int().default(500)
    private val json by option("--json", help = "One JSON object per record").flag()

    override fun help(context: Context) =
        "List strings (every locale, plurals, arrays), resource ids, styles and themes, drawables, layouts, menus, navigation " +
            "graphs, assets and XML configs. References are shown as written and resolved; NAME shows one entry and who uses it."

    override fun run() =
        guarded {
            val extraction = if (Files.isDirectory(target)) OutputReader.read(target) else Extractor.extract(target, code = null)
            @Suppress("UNCHECKED_CAST")
            print(render(KINDS.getValue(kindName) as Kind<Any?>, extraction.resources))
        }

    private fun render(
        k: Kind<Any?>,
        h: ResourceHarvest,
    ): String {
        var records = k.records(h)
        config?.let { c -> records = records.filter { r -> (rowOf(k, r).getOrNull(k.columns.indexOf("config")) ?: c) == c } }
        if (name != null) return detail(k, h, records.filter { k.key(it) == name })
        flag?.let { f ->
            records =
                records.filter { (it as? io.github.rajumark.sextant.core.insights.StringKey)?.flags?.contains(f) ?: true }
        }
        search?.let { q ->
            records =
                records.filter { k.key(it).contains(q, true) || k.row(it).any { c -> c.contains(q, true) } || k.text(it).contains(q, true) }
        }
        val shown = records.take(limit)
        val out =
            if (json) {
                shown.joinToString("") { SextantJson.compact.encodeToString(k.serializer, it) + "\n" }
            } else if (shown.isEmpty()) {
                ""
            } else {
                Table.render(k.columns, shown.map { rowOf(k, it) })
            }
        System.err.println(
            "${shown.size} of ${k.records(h).size} $kindName" + if (records.size > limit) " (stopped at --limit $limit)" else "",
        )
        return out
    }

    private fun rowOf(
        k: Kind<Any?>,
        r: Any?,
    ) = k.row(r)

    private fun detail(
        k: Kind<Any?>,
        h: ResourceHarvest,
        matches: List<Any?>,
    ): String {
        if (matches.isEmpty()) {
            throw SextantException(
                ExitCode.USAGE,
                "no $kindName entry named $name",
                "list them with: sextant res $target $kindName",
            )
        }
        val refs: List<ResourceRef> = h.refs.filter { r -> k.refTypes.any { t -> r.ref == "@$t/$name" } }
        if (json) {
            return matches.joinToString("") { SextantJson.compact.encodeToString(k.serializer, it) + "\n" } +
                refs.joinToString("") { SextantJson.compact.encodeToString(ResourceRef.serializer(), it) + "\n" }
        }
        return buildString {
            appendLine("$name  ($kindName, ${matches.size} configuration${if (matches.size == 1) "" else "s"})")
            appendLine()
            append(Table.render(k.columns, matches.map { rowOf(k, it) }))
            if (kindName in setOf("styles", "layouts", "menus", "navigation", "xml")) {
                matches.forEach { m ->
                    appendLine()
                    appendLine(SextantJson.pretty.encodeToString(k.serializer, m))
                }
            }
            appendLine()
            appendLine("## Used by (${refs.size})")
            if (refs.isEmpty()) appendLine("  nothing in resources or the manifest (code may still use it by id)")
            refs.forEach { appendLine("  ${it.fromKind} ${it.from}  ${it.attribute}") }
        }
    }
}
