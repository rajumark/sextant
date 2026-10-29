package io.github.rajumark.sextant.core.index

/** One saved query: a SQL file bundled with Sextant. The first comment lines describe it; `:name` marks a parameter. */
class SavedQuery(
    val name: String,
    val description: String,
    val sql: String,
) {
    /** Parameter names in the order SQLite numbers them (first appearance). */
    val params: List<String> =
        run {
            val code = sql.lines().filterNot { it.trim().startsWith("--") }.joinToString("\n")
            PARAM
                .findAll(code.replace(LITERAL, "''"))
                .map { it.groupValues[1] }
                .distinct()
                .toList()
        }

    private companion object {
        val PARAM = Regex(""":([a-z_]+)""")

        // Parameters never appear inside string literals ('android:inputType'), so those are skipped.
        val LITERAL = Regex("""'(?:[^']|'')*'""")
    }
}

/** The saved queries behind `sextant q`, in `resources/…/sextant/queries/<name>.sql`. */
object SavedQueries {
    private val NAMES =
        listOf(
            "unused-strings",
            "untranslated-strings",
            "string-users",
            "password-fields",
            "activity-layouts",
            "exported-components",
            "dangerous-permissions",
            "signing",
            "webviews",
            "hosts",
            "jni-methods",
            "native-libraries",
            "native-sensitive-imports",
            "service-providers",
            "kotlin-classes",
            "annotated",
            "broad-catches",
            "fonts",
            "largest-images",
            "literal-users",
            "opcode-stats",
            "biggest-packages",
            "undecoded-files",
        )

    val ALL: List<SavedQuery> by lazy { NAMES.map(::load) }

    fun find(name: String): SavedQuery? = ALL.firstOrNull { it.name == name }

    private fun load(name: String): SavedQuery {
        val sql =
            SavedQueries::class.java.getResourceAsStream("/io/github/rajumark/sextant/queries/$name.sql")?.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
                ?: error("saved query $name is missing from the jar")
        val description =
            sql
                .lines()
                .takeWhile { it.trim().startsWith("--") }
                .joinToString(" ") { it.trim().removePrefix("--").trim() }
        return SavedQuery(name, description, sql.trim())
    }
}
