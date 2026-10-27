package io.github.rajumark.sextant.desktop.data

import io.github.rajumark.sextant.desktop.model.ManifestNode
import io.github.rajumark.sextant.desktop.model.Permission
import io.github.rajumark.sextant.desktop.model.PermissionKind

/** Text views of the decoded manifest: the manifest.json shape, readable XML, and per-permission snippets. */
object ManifestFormat {
    fun json(n: ManifestNode): String = MiniJson.pretty(jsonTree(n))

    private fun jsonTree(n: ManifestNode): Map<String, Any?> =
        linkedMapOf(
            "tag" to n.tag,
            "attributes" to linkedMapOf(*n.attributes.toTypedArray()),
            "children" to n.children.map(::jsonTree),
        )

    fun xml(root: ManifestNode): String = StringBuilder().also { xml(root, 0, it) }.toString()

    private fun xml(
        n: ManifestNode,
        depth: Int,
        out: StringBuilder,
    ) {
        val pad = "    ".repeat(depth)
        out.append(pad).append('<').append(n.tag)
        val ns = if (depth == 0) listOf("xmlns:android" to "http://schemas.android.com/apk/res/android") else emptyList()
        val attrs = ns + n.attributes
        if (attrs.size <= 1) {
            attrs.forEach { (k, v) ->
                out
                    .append(' ')
                    .append(k)
                    .append("=\"")
                    .append(v)
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
                    .append(v)
                    .append('"')
            }
        }
        if (n.children.isEmpty()) {
            out.append(" />\n")
        } else {
            out.append(">\n")
            n.children.forEach { xml(it, depth + 1, out) }
            out
                .append(pad)
                .append("</")
                .append(n.tag)
                .append(">\n")
        }
    }

    fun permissionSnippet(p: Permission): String =
        when (p.kind) {
            PermissionKind.Declared ->
                "<permission\n    android:name=\"${p.name}\"\n    android:protectionLevel=\"${p.protectionLevel ?: "normal"}\" />"
            else -> "<${p.kind.wire} android:name=\"${p.name}\"" + (p.maxSdk?.let { "\n    android:maxSdkVersion=\"$it\"" } ?: "") + " />"
        }
}
