package io.github.rajumark.sextant.desktop.data

// Tiny JSON writer used to show what the CLI writes to disk. Values are Map (key order
// kept), List, String, Number, Boolean or null — the same shapes kotlinx.serialization
// produces in the CLI.

internal object MiniJson {
    fun compact(value: Any?): String = buildString { write(value, null, 0) }

    fun pretty(value: Any?): String = buildString { write(value, "  ", 0) }

    private fun StringBuilder.write(
        value: Any?,
        indent: String?,
        depth: Int,
    ) {
        when (value) {
            null -> append("null")
            is String -> quote(value)
            is Boolean, is Number -> append(value.toString())
            is Map<*, *> ->
                container('{', '}', value.entries.toList(), indent, depth) { (k, v) ->
                    quote(k.toString())
                    append(if (indent == null) ":" else ": ")
                    write(v, indent, depth + 1)
                }
            is List<*> -> container('[', ']', value, indent, depth) { write(it, indent, depth + 1) }
            else -> quote(value.toString())
        }
    }

    private fun <T> StringBuilder.container(
        open: Char,
        close: Char,
        items: List<T>,
        indent: String?,
        depth: Int,
        item: StringBuilder.(T) -> Unit,
    ) {
        append(open)
        if (items.isEmpty()) {
            append(close)
            return
        }
        // Short scalar lists stay on one line even in pretty mode, like the CLI's examples.
        val inline = indent == null || items.all { it !is Map<*, *> && it !is List<*> && it !is Map.Entry<*, *> }
        items.forEachIndexed { i, v ->
            if (i > 0) append(if (inline && indent != null) ", " else ",")
            if (!inline) newline(indent, depth + 1)
            item(v)
        }
        if (!inline) newline(indent, depth)
        append(close)
    }

    private fun StringBuilder.newline(
        indent: String,
        depth: Int,
    ) {
        append('\n')
        repeat(depth) { append(indent) }
    }

    private fun StringBuilder.quote(s: String) {
        append('"')
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
        append('"')
    }
}
