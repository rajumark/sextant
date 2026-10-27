package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.ResourceValue
import io.github.rajumark.sextant.core.model.XmlNode
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.core.dex.nodes.RootNode
import jadx.core.xmlgen.BinaryXMLParser
import jadx.core.xmlgen.ResTableBinaryParser
import jadx.core.xmlgen.entry.ValuesParser
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/** Decoded manifest tree plus a way to resolve `@type/name` references against resources.arsc. */
class DecodedManifest(
    val root: XmlNode,
    val resources: ResourceResolver,
    /** Decodes another binary XML file from the APK (e.g. an adaptive icon) with the same resource table; null if invalid. */
    val decodeXml: (ByteArray) -> XmlNode?,
    /** Every entry of resources.arsc in every configuration (empty when there is no table). */
    val table: List<RawResource> = emptyList(),
) {
    /** BCP-47 locale of a configuration qualifier (`-pt-rBR` → `pt-BR`), or null. */
    fun locale(config: String): String? = ManifestDecoder.locale(config)
}

/** One resources.arsc entry in one configuration: a simple value, or a bag (style, plurals, array) of items. */
class RawResource(
    val id: Int,
    val type: String,
    val name: String,
    /** Qualifier without the leading dash; `""` for the default configuration. */
    val config: String,
    val value: String?,
    /** Bags: parent style as `@style/Name`. */
    val parent: String?,
    /** Bags: (attribute or item key, value). Plural keys are the raw quantity ids. */
    val items: List<Pair<String, String>>,
)

/** One file behind a file resource, for one configuration (density qualifier etc.). */
data class ResourceFile(
    val config: String,
    val path: String,
)

/** Looks up resource values (strings, bools, integers, colors, dimens) and file resources from resources.arsc. */
class ResourceResolver(
    private val values: Map<String, String>,
    /** Every simple value (string, bool, integer, color, dimen) in its chosen configuration, sorted by type then name. */
    val entries: List<ResourceValue> = emptyList(),
    /** Locales the app has string translations for (`fr`, `pt-BR`, `sr-Latn`), sorted. */
    val locales: List<String> = emptyList(),
    /** File resources (`@mipmap/ic_launcher`) → one file per configuration (`xxxhdpi` → `res/o-.png`), sorted by config. */
    val files: Map<String, List<ResourceFile>> = emptyMap(),
) {
    /**
     * Returns the resolved value for `@type/name`, or the input unchanged when it is not a resolvable reference.
     * References to references (`@string/app_name` → `@string/app_name_prod` → "Wikipedia") are followed.
     */
    fun resolve(value: String?): String? {
        var current = value ?: return null
        repeat(MAX_HOPS) {
            if (!current.startsWith("@")) return current
            current = values[current] ?: return current
        }
        return current
    }

    /** Files of a file resource, following `@mipmap/a` → `@drawable/b` aliases. */
    fun files(ref: String?): List<ResourceFile> {
        var current = ref ?: return emptyList()
        repeat(MAX_HOPS) {
            files[current]?.let { return it }
            current = values[current]?.takeIf { it.startsWith("@") } ?: return emptyList()
        }
        return emptyList()
    }

    companion object {
        private const val MAX_HOPS = 10
        val EMPTY = ResourceResolver(emptyMap())
    }
}

/**
 * Decodes binary AndroidManifest.xml with jadx-core's resource decoders only; no DEX code is loaded.
 * The decoded XML text is re-parsed into a plain [XmlNode] tree.
 */
object ManifestDecoder {
    private val SIMPLE_TYPES = setOf("string", "bool", "integer", "color", "dimen")
    private val FILE_TYPES = setOf("mipmap", "drawable")
    private val LOCALE = Regex("""^-?([a-z]{2,3})(?:-r([A-Z]{2}))?(?:-|$)""")
    private val BCP47 = Regex("""^-?b\+([A-Za-z0-9+]+)""")

    fun decode(
        manifest: ByteArray,
        resourceTable: ByteArray?,
        warnings: MutableList<String>,
    ): DecodedManifest {
        val root = RootNode(JadxDecompiler(JadxArgs()))
        var resolver = ResourceResolver.EMPTY
        var table = emptyList<RawResource>()
        if (resourceTable != null) {
            try {
                val parser = ResTableBinaryParser(root)
                parser.decode(resourceTable.inputStream())
                root.processResources(parser.resStorage)
                resolver = buildResolver(parser)
                table = rawTable(parser)
            } catch (e: Exception) {
                warnings += "resources.arsc could not be decoded (${e.javaClass.simpleName}); resource references are left unresolved"
            }
        }
        val xml =
            try {
                BinaryXMLParser(root).parse(manifest.inputStream()).codeStr
            } catch (e: Exception) {
                throw SextantException.input("AndroidManifest.xml could not be decoded: ${e.message}", cause = e)
            }
        val tree =
            try {
                parseXml(xml)
            } catch (e: Exception) {
                throw SextantException.input("AndroidManifest.xml could not be decoded: not valid binary XML (${e.message})", cause = e)
            }
        val decodeOther: (ByteArray) -> XmlNode? = { bytes ->
            runCatching { parseXml(BinaryXMLParser(root).parse(bytes.inputStream()).codeStr) }.getOrNull()
        }
        return DecodedManifest(tree, resolver, decodeOther, table)
    }

    private fun buildResolver(parser: ResTableBinaryParser): ResourceResolver {
        val valuesParser = ValuesParser(parser.strings, parser.resStorage.resourcesNames)
        // Default configuration wins; otherwise the first configuration in sorted order, so the result is deterministic.
        val candidates = sortedMapOf<String, MutableList<Pair<String, String>>>()
        val locales = sortedSetOf<String>()
        val files = sortedMapOf<String, MutableList<ResourceFile>>()
        for (entry in parser.resStorage.resources) {
            if (entry.typeName in FILE_TYPES && entry.simpleValue != null) {
                val value = runCatching { valuesParser.getSimpleValueString(entry) }.getOrNull()
                if (value != null && value.startsWith("res/")) {
                    files.getOrPut("@${entry.typeName}/${entry.keyName}") { mutableListOf() } +=
                        ResourceFile(entry.config.orEmpty().removePrefix("-"), value)
                }
            }
            if (entry.typeName == "string" && !entry.config.isNullOrEmpty()) locale(entry.config)?.let(locales::add)
            if (entry.typeName !in SIMPLE_TYPES || entry.simpleValue == null) continue
            val value = runCatching { valuesParser.getSimpleValueString(entry) }.getOrNull() ?: continue
            candidates.getOrPut("@${entry.typeName}/${entry.keyName}") { mutableListOf() } += entry.config.orEmpty() to value
        }
        val values =
            candidates.mapValues { (_, options) ->
                options.firstOrNull { it.first.isEmpty() }?.second ?: options.minBy { it.first }.second
            }
        val entries =
            values
                .map { (ref, value) -> ResourceValue(ref.substringAfter('@').substringBefore('/'), ref.substringAfter('/'), value) }
                .sortedWith(compareBy({ it.type }, { it.name }))
        return ResourceResolver(values, entries, locales.toList(), files.mapValues { (_, v) -> v.sortedBy { it.config } })
    }

    private fun rawTable(parser: ResTableBinaryParser): List<RawResource> {
        val names = parser.resStorage.resourcesNames
        val values = ValuesParser(parser.strings, names)
        return parser.resStorage.resources.map { e ->
            val simple = e.simpleValue?.let { runCatching { values.getSimpleValueString(e) }.getOrNull() }
            val items =
                e.namedValues.orEmpty().map { nv ->
                    val key =
                        if (e.typeName == "plurals") {
                            nv.nameRef.toString()
                        } else {
                            runCatching { values.decodeNameRef(nv.nameRef) }.getOrNull() ?: "0x%08x".format(nv.nameRef)
                        }
                    key to (runCatching { values.decodeValue(nv.rawValue) }.getOrNull() ?: "")
                }
            val parent = e.parentRef.takeIf { it != 0 }?.let { ref -> names[ref]?.let { "@$it" } ?: "0x%08x".format(ref) }
            RawResource(e.id, e.typeName, e.keyName, e.config.orEmpty().removePrefix("-"), simple, parent, items)
        }
    }

    /** `-fr` → `fr`, `-pt-rBR` → `pt-BR`, `-b+sr+Latn` → `sr-Latn`; other qualifiers → null. */
    internal fun locale(config: String): String? {
        BCP47.find(config)?.let { return it.groupValues[1].replace('+', '-') }
        val m = LOCALE.find(config) ?: return null
        return if (m.groupValues[2].isEmpty()) m.groupValues[1] else "${m.groupValues[1]}-${m.groupValues[2]}"
    }

    private fun parseXml(xml: String): XmlNode {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        return toNode(doc.documentElement)
    }

    private fun toNode(element: Element): XmlNode {
        val attrs = sortedMapOf<String, String>()
        val map = element.attributes
        for (i in 0 until map.length) {
            val attr = map.item(i)
            if (attr.nodeName.startsWith("xmlns")) continue
            attrs[attr.nodeName] = attr.nodeValue
        }
        val children = mutableListOf<XmlNode>()
        val nodes = element.childNodes
        val text = StringBuilder()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node is Element) children += toNode(node) else if (node.nodeType == org.w3c.dom.Node.TEXT_NODE) text.append(node.nodeValue)
        }
        // Element text matters in res/xml configs (<domain>example.org</domain>); manifests have none.
        if (children.isEmpty() && text.isNotBlank()) attrs["#text"] = text.toString().trim()
        return XmlNode(element.tagName, attrs, children)
    }
}

// Small helpers for reading the tree.
internal fun XmlNode.attr(name: String): String? = attributes[name]

internal fun XmlNode.android(name: String): String? = attributes["android:$name"]

internal fun XmlNode.children(tag: String): List<XmlNode> = children.filter { it.tag == tag }

internal fun XmlNode.child(tag: String): XmlNode? = children.firstOrNull { it.tag == tag }
