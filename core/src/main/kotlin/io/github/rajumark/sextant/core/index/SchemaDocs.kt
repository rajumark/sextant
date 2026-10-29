package io.github.rajumark.sextant.core.index

import kotlinx.serialization.Serializable

/** One row of `schema_docs`: a table or view (column = null) or one of its columns. */
@Serializable
data class SchemaDoc(
    val table: String,
    val column: String?,
    /** table | view | virtual table for table rows; the SQL type for columns. */
    val type: String,
    val description: String?,
)

/**
 * Documentation of every table, view and column, read from the comments in [IndexSchema.DDL]: the comment lines
 * right above a CREATE statement describe the table, a comment after a column describes the column (continued on the
 * next comment line). Stored in each index as `schema_docs`, so the file explains itself to people and agents.
 */
object SchemaDocs {
    private val CREATE = Regex("""^CREATE (TABLE|VIEW|VIRTUAL TABLE) (\w+)""")
    private val COLUMN = Regex("""^"?(\w+)"?\s+(TEXT|INTEGER|REAL|BLOB)\b[^-]*(?:--\s*(.*))?$""")

    val ALL: List<SchemaDoc> by lazy { parse(IndexSchema.DDL) }

    fun parse(ddl: String): List<SchemaDoc> {
        val docs = mutableListOf<SchemaDoc>()
        val comment = mutableListOf<String>()
        var table: String? = null
        for (raw in ddl.lines()) {
            val line = raw.trim()
            if (table == null) {
                when {
                    line.startsWith("--") -> comment += line.removePrefix("--").trim()
                    CREATE.containsMatchIn(line) -> {
                        val m = CREATE.find(line)!!
                        val kind = m.groupValues[1].lowercase()
                        docs += SchemaDoc(m.groupValues[2], null, kind, comment.joinToString(" ").ifBlank { null })
                        comment.clear()
                        // Tables list columns until ");"; views and one-line statements end here.
                        if (kind == "table" && !line.trimEnd().endsWith(";")) table = m.groupValues[2]
                    }
                    line.isEmpty() -> comment.clear()
                }
                continue
            }
            if (line.startsWith(")")) {
                table = null
                continue
            }
            if (line.startsWith("--")) {
                // A comment line inside the column list continues the previous column's description.
                val last = docs.last()
                if (last.column != null) {
                    docs[docs.lastIndex] =
                        last.copy(description = listOfNotNull(last.description, line.removePrefix("--").trim()).joinToString(" "))
                }
                continue
            }
            COLUMN.find(line)?.let { m ->
                docs += SchemaDoc(table, m.groupValues[1], m.groupValues[2], m.groupValues[3].trim().ifEmpty { null })
            }
        }
        return docs
    }
}
