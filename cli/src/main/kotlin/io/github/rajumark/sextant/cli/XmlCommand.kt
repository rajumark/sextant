package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.ExitCode
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.index.XmlHit
import io.github.rajumark.sextant.core.model.XmlNode
import io.github.rajumark.sextant.core.output.SextantJson

/** `sextant xml <app> [file]`: any binary XML file of the APK, rebuilt from the xml_nodes and xml_attrs tables. */
class XmlCommand : CoreCliktCommand(name = "xml") {
    private val target by argument(help = INDEX_INPUT).path()
    private val file by argument(help = "A file inside the APK (res/BW.xml, AndroidManifest.xml) or a resource (@layout/main)").optional()
    private val tag by option("--tag", help = "Find elements whose tag is or ends with this, e.g. EditText also finds TextInputEditText")
    private val attr by option("--attr", help = "Find elements with this attribute, e.g. android:inputType")
    private val value by option("--value", help = "Only attributes whose value contains this text")
    private val limit by option("--limit", help = "Maximum rows (default 200)").int().default(200)
    private val json by option("--json", help = "One JSON object per hit, or the tree as JSON").flag()

    override fun help(context: Context) =
        "Print any XML file of the APK (manifest, layouts, menus, drawables, colors, animations, …) as stored in SQLite, " +
            "or find elements across all of them with --tag, --attr and --value."

    override fun run() =
        guarded {
            openIndex(target).use { index ->
                val wanted = file
                if (wanted != null) {
                    val paths = index.xmlPaths(wanted)
                    if (paths.isEmpty()) {
                        throw SextantException.input(
                            "no XML file matches '$wanted'",
                            "list them with: sextant files $target --search ${wanted.substringAfterLast('/')}",
                        )
                    }
                    for (path in paths) {
                        val tree = index.xmlTree(path) ?: continue
                        if (json) {
                            println(SextantJson.compact.encodeToString(XmlNode.serializer(), tree))
                        } else {
                            if (paths.size > 1) println("<!-- $path -->")
                            print(render(tree, ""))
                        }
                    }
                    return@use
                }
                if (tag == null && attr == null && value == null) {
                    throw SextantException(
                        ExitCode.USAGE,
                        "pass a file or resource, or --tag / --attr / --value",
                        "e.g. sextant xml $target --tag EditText --attr android:inputType",
                    )
                }
                val (hits, total) = index.xmlFind(tag, attr, value, limit)
                if (json) {
                    hits.forEach { println(SextantJson.compact.encodeToString(XmlHit.serializer(), it)) }
                } else if (hits.isNotEmpty()) {
                    val withAttr = attr != null || value != null
                    print(
                        Table.render(
                            listOf("path", "resource", "tag") + if (withAttr) listOf("attribute", "value") else emptyList(),
                            hits.map { h ->
                                listOf(h.path, h.resource ?: "-", h.tag) +
                                    if (withAttr) listOf(h.attribute.orEmpty(), h.value.orEmpty()) else emptyList()
                            },
                        ),
                    )
                }
                System.err.println(
                    "${hits.size} of $total match${if (total == 1) "" else "es"}" +
                        if (total > hits.size) " (stopped at --limit $limit)" else "",
                )
            }
        }

    /** Indented XML: attributes on their own lines when there are several, element text inline. */
    private fun render(
        n: XmlNode,
        indent: String,
    ): String =
        buildString {
            val attrs = n.attributes.filterKeys { it != "#text" }
            val text = n.attributes["#text"]
            append(indent).append('<').append(n.tag)
            when {
                attrs.size == 1 ->
                    attrs.forEach { (k, v) ->
                        append(' ')
                            .append(k)
                            .append("=\"")
                            .append(escape(v))
                            .append('"')
                    }
                attrs.size > 1 ->
                    attrs.forEach { (k, v) ->
                        append('\n')
                            .append(indent)
                            .append("    ")
                            .append(k)
                            .append("=\"")
                            .append(escape(v))
                            .append('"')
                    }
            }
            when {
                n.children.isNotEmpty() -> {
                    append(">\n")
                    n.children.forEach { append(render(it, "$indent    ")) }
                    append(indent).append("</").append(n.tag).append(">\n")
                }
                text != null ->
                    append('>')
                        .append(escape(text))
                        .append("</")
                        .append(n.tag)
                        .append(">\n")
                else -> append(" />\n")
            }
        }

    private fun escape(s: String) =
        s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
