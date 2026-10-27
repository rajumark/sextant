package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.model.ResourceHarvest
import io.github.rajumark.sextant.core.model.ResourceRef
import kotlinx.serialization.Serializable

/** One string resource key across all its configurations, with who uses it and what its text looks like. */
@Serializable
data class StringKey(
    val name: String,
    /** string | plurals | array */
    val kind: String,
    /** Default-configuration text; plurals show `other`, arrays their items joined with " · ". */
    val value: String,
    /** config → text (same joining as [value]), default first. */
    val values: Map<String, String>,
    val locales: List<String>,
    /** App locales this key has no translation for. */
    val missingLocales: List<String>,
    /** url | email | secret-like | format | html | empty — see [StringCatalog.FLAGS]. */
    val flags: List<String>,
    val usedBy: List<ResourceRef>,
)

object StringCatalog {
    /** Every flag, in display order. */
    val FLAGS = listOf("used", "unused", "url", "email", "secret-like", "format", "html", "untranslated", "empty")

    private val URL = Regex("""\b(https?|wss?|ftp)://[^\s"'<>]+""", RegexOption.IGNORE_CASE)
    private val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val FORMAT = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[sdfxXoceEgGb%]""")
    private val HTML = Regex("""<(/?)(b|i|u|a|br|font|p|strong|em|span|big|small|sup|sub)\b""", RegexOption.IGNORE_CASE)

    fun of(h: ResourceHarvest): List<StringKey> {
        // Libraries (AppCompat, Material) bring far more locales than the app translates. The app's own locale set is the
        // locales most translated keys have, so library-only locales do not mark every app string as untranslated.
        val keyLocales = h.strings.filter { it.locale != null }.groupBy { it.name }.mapValues { (_, rs) -> rs.mapNotNull { it.locale }.toSet() }
        val appLocales =
            keyLocales.values
                .flatten()
                .groupingBy { it }
                .eachCount()
                .filter { it.value * 2 >= keyLocales.size }
                .keys
                .sorted()
        val refs = h.refs.groupBy { it.ref }
        return h.strings.groupBy { it.name }.map { (name, rows) ->
            val kind = rows.first().kind
            val byConfig =
                rows.groupBy { it.config }.mapValues { (_, items) ->
                    when (kind) {
                        "plurals" -> (items.firstOrNull { it.item == "other" } ?: items.first()).value
                        "array" -> items.joinToString(" · ") { it.value }
                        else -> items.first().value
                    }
                }.toSortedMap(compareBy<String> { it.isNotEmpty() }.thenBy { it })
            val value = byConfig[""] ?: byConfig.values.first()
            val locales = rows.mapNotNull { it.locale }.distinct().sorted()
            val usedBy = (refs["@string/$name"].orEmpty() + refs["@plurals/$name"].orEmpty() + refs["@array/$name"].orEmpty())
            val all = byConfig.values.joinToString("\n")
            val flags =
                buildList {
                    add(if (usedBy.isEmpty()) "unused" else "used")
                    if (URL.containsMatchIn(all)) add("url")
                    if (EMAIL.containsMatchIn(all)) add("email")
                    if (Findings.secretRule(value) != null) add("secret-like")
                    if (FORMAT.containsMatchIn(value)) add("format")
                    if (HTML.containsMatchIn(value)) add("html")
                    // No translation at all usually means translatable="false" (URLs, keys); only partial ones are gaps.
                    if (locales.isNotEmpty() && !locales.containsAll(appLocales)) add("untranslated")
                    if (value.isBlank()) add("empty")
                }
            StringKey(name, kind, value, byConfig, locales, appLocales - locales.toSet(), flags, usedBy)
        }.sortedBy { it.name }
    }
}
