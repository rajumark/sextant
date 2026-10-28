package io.github.rajumark.sextant.core.output

import io.github.rajumark.sextant.core.model.XmlNode

/** Readable XML for the decoded manifest, and the exact element that declares one entry ("Manifest entry" views). */
object ManifestXml {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private val COMPONENT_TAGS = setOf("activity", "activity-alias", "service", "receiver", "provider")

    /** [node] and its children as indented XML; the root also gets the android namespace declaration. */
    fun render(
        node: XmlNode,
        declareNamespace: Boolean = false,
    ): String = StringBuilder().also { write(node, 0, declareNamespace, it) }.toString()

    /** The `<activity>`, `<service>`, … element declaring the component [name], as written in the manifest. */
    fun component(
        manifest: XmlNode,
        name: String,
    ): XmlNode? {
        val pkg = manifest.attributes["package"]
        val app = manifest.children.firstOrNull { it.tag == "application" } ?: return null
        return app.children.firstOrNull { it.tag in COMPONENT_TAGS && fullName(it.attributes["android:name"], pkg) == name }
    }

    /** The `<uses-permission>`, `<permission>`, … elements naming [name] (a permission can be requested and declared). */
    fun permission(
        manifest: XmlNode,
        name: String,
    ): List<XmlNode> = manifest.children.filter { it.tag.contains("permission") && it.attributes["android:name"] == name }

    private fun fullName(
        raw: String?,
        pkg: String?,
    ): String? =
        when {
            raw == null || pkg == null -> raw
            raw.startsWith(".") -> pkg + raw
            !raw.contains('.') -> "$pkg.$raw"
            else -> raw
        }

    private fun write(
        n: XmlNode,
        depth: Int,
        declareNamespace: Boolean,
        out: StringBuilder,
    ) {
        val pad = "    ".repeat(depth)
        val text = n.attributes["#text"]
        val attrs =
            (if (declareNamespace) listOf("xmlns:android" to ANDROID_NS) else emptyList()) +
                n.attributes.toList().filter { it.first != "#text" }
        out.append(pad).append('<').append(n.tag)
        if (attrs.size <= 1) {
            attrs.forEach { (k, v) ->
                out
                    .append(' ')
                    .append(k)
                    .append("=\"")
                    .append(escape(v))
                    .append('"')
            }
        } else {
            attrs.forEach { (k, v) ->
                out
                    .append('\n')
                    .append(pad)
                    .append("    ")
                    .append(k)
                    .append("=\"")
                    .append(escape(v))
                    .append('"')
            }
        }
        if (text != null) {
            out
                .append('>')
                .append(escape(text))
                .append("</")
                .append(n.tag)
                .append(">\n")
        } else if (n.children.isEmpty()) {
            out.append(" />\n")
        } else {
            out.append(">\n")
            n.children.forEach { write(it, depth + 1, false, out) }
            out
                .append(pad)
                .append("</")
                .append(n.tag)
                .append(">\n")
        }
    }

    private fun escape(v: String) = v.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;")
}
