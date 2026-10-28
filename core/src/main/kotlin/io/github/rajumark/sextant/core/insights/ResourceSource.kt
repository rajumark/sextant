package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.model.AssetRecord
import io.github.rajumark.sextant.core.model.DrawableRecord
import io.github.rajumark.sextant.core.model.LayoutRecord
import io.github.rajumark.sextant.core.model.MenuRecord
import io.github.rajumark.sextant.core.model.NavigationRecord
import io.github.rajumark.sextant.core.model.ResourceIdRecord
import io.github.rajumark.sextant.core.model.StringResource
import io.github.rajumark.sextant.core.model.StyleRecord
import io.github.rajumark.sextant.core.model.XmlConfigRecord
import kotlinx.serialization.Serializable

/**
 * One piece of a resource as the developer wrote it: the source file it lives in (`res/menu/main.xml`,
 * `res/values-fr/strings.xml`), where that is inside the APK (shrunk names like `res/IG.xml`, or `resources.arsc`),
 * and its text. [text] is null for binary files (images, fonts); [note] then says what the file is.
 */
@Serializable
data class SourceBlock(
    val file: String,
    val config: String,
    val inApk: String,
    val text: String?,
    val language: String = "xml",
    val note: String? = null,
)

/** The original source of every resource kind, for "Source" sections and `sextant res … NAME`. */
object ResourceSource {
    fun strings(rows: List<StringResource>): List<SourceBlock> =
        byConfig(rows, { it.config }) { config, rs ->
            val first = rs.first()
            val file =
                when (first.kind) {
                    "plurals" -> "plurals.xml"
                    "array" -> "arrays.xml"
                    else -> "strings.xml"
                }
            val text =
                when (first.kind) {
                    "plurals" ->
                        "<plurals name=\"${first.name}\">\n" +
                            rs.joinToString("") { "    <item quantity=\"${it.item}\">${escape(it.value)}</item>\n" } + "</plurals>"
                    "array" ->
                        "<string-array name=\"${first.name}\">\n" +
                            rs.joinToString("") { "    <item>${escape(it.value)}</item>\n" } + "</string-array>"
                    else -> "<string name=\"${first.name}\">${escape(first.value)}</string>"
                }
            SourceBlock(values(config, file), config, ARSC, text)
        }

    fun styles(records: List<StyleRecord>): List<SourceBlock> =
        byConfig(records, { it.config }) { config, rs ->
            val s = rs.first()
            val text =
                "<style name=\"${s.name}\"" + (s.parent?.let { " parent=\"@style/$it\"" } ?: "") + ">\n" +
                    s.items.joinToString("") { "    <item name=\"${it.name}\">${attr(it.raw)}</item>\n" } + "</style>"
            SourceBlock(values(config, "styles.xml"), config, ARSC, text)
        }

    fun id(r: ResourceIdRecord): List<SourceBlock> =
        listOf(SourceBlock("res/values/public.xml", "", ARSC, "<public type=\"${r.type}\" name=\"${r.name}\" id=\"${r.id}\" />"))

    fun layouts(records: List<LayoutRecord>) = records.map { file("layout", it.name, it.config, it.path, it.xml) }

    fun menus(records: List<MenuRecord>) = records.map { file("menu", it.name, it.config, it.path, it.xml) }

    fun navigation(r: NavigationRecord) = listOf(file("navigation", r.name, "", r.path, r.xml))

    fun xml(records: List<XmlConfigRecord>) = records.map { file("xml", it.name, it.config, it.path, it.xml) }

    fun drawables(records: List<DrawableRecord>) =
        records.map { d ->
            val ext =
                d.path.substringAfterLast('/').let { n ->
                    if (n.endsWith(".9.png")) ".9.png" else "." + n.substringAfterLast('.', "")
                }
            val size = if (d.width != null && d.height != null) ", ${d.width}×${d.height}" else ""
            file(d.type, d.name, d.config, d.path, d.xml, ext, note = "Binary ${d.format} image$size, ${d.bytes} bytes: no text source.")
        }

    fun asset(a: AssetRecord): List<SourceBlock> {
        val language =
            when (a.path.substringAfterLast('.').lowercase()) {
                "json" -> "json"
                "xml", "html", "htm" -> "xml"
                "js", "mjs" -> "js"
                else -> "text"
            }
        val note =
            if (a.text == null) {
                if (a.kind in
                    setOf("text", "json", "html", "js")
                ) {
                    "Too large to show (${a.bytes} bytes)."
                } else {
                    "Binary ${a.kind} file, ${a.bytes} bytes: no text source."
                }
            } else {
                null
            }
        return listOf(SourceBlock(a.path, "", a.path, a.text, language, note))
    }

    private const val ARSC = "resources.arsc"

    private fun file(
        type: String,
        name: String,
        config: String,
        path: String,
        xml: String,
        ext: String = ".xml",
        note: String? = null,
    ) = SourceBlock(
        "res/$type${if (config.isEmpty()) "" else "-$config"}/$name$ext",
        config,
        path,
        xml.ifEmpty { null },
        note = if (xml.isEmpty()) note ?: "Not decoded (file missing or too large)." else null,
    )

    private fun values(
        config: String,
        file: String,
    ) = "res/values${if (config.isEmpty()) "" else "-$config"}/$file"

    /** Default configuration first, then the rest by name. */
    private fun <T> byConfig(
        records: List<T>,
        config: (T) -> String,
        block: (String, List<T>) -> SourceBlock,
    ): List<SourceBlock> =
        records
            .groupBy(config)
            .toSortedMap(compareBy<String> { it.isNotEmpty() }.thenBy { it })
            .map { (c, rs) -> block(c, rs) }

    /** A string value written back the way aapt2 reads it: XML escapes, `\'` `\"` `\n`, quotes around edge spaces. */
    internal fun escape(v: String): String {
        val t =
            v
                .replace("\\", "\\\\")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
        val spaces = t.startsWith(" ") || t.endsWith(" ") || "  " in t
        return if (spaces) "\"$t\"" else t
    }

    private fun attr(v: String) = v.replace("&", "&amp;").replace("<", "&lt;")
}
