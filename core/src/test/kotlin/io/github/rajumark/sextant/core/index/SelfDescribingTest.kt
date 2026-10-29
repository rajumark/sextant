package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.CorpusApks
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files

/** The index explains itself (schema_docs) and every saved query runs against a real one. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelfDescribingTest {
    private val dir = Files.createTempDirectory("sextant-docs")
    private val file = dir.resolve("binaryeye.sqlite").also { IndexBuilder.build(CorpusApks.require("binaryeye"), it) }

    @AfterAll
    fun cleanup() {
        Files.deleteIfExists(file)
        Files.deleteIfExists(dir)
    }

    @Test
    fun `every table and view is described, and schema_docs lists every real table`() {
        SchemaDocs.ALL
            .filter { it.column == null && it.description == null }
            .map { it.table }
            .shouldBeEmpty()
        IndexReader.open(file).use { index ->
            val tables =
                index
                    .query(
                        "SELECT name FROM sqlite_master WHERE type IN ('table', 'view') AND name NOT LIKE 'search_%' AND name NOT LIKE 'sqlite_%'",
                    ).rows
                    .map { it[0].toString() }
            val documented =
                index
                    .query("SELECT DISTINCT \"table\" FROM schema_docs")
                    .rows
                    .map { it[0].toString() }
                    .toSet()
            tables.filter { it !in documented }.shouldBeEmpty()
            index.query("SELECT description FROM schema_docs WHERE \"table\" = 'links' AND \"column\" IS NULL").rows[0][0] shouldNotBe null
        }
    }

    @Test
    fun `every saved query runs`() {
        IndexReader.open(file).use { index ->
            for (q in SavedQueries.ALL) {
                withClue(q.name) {
                    q.description.isNotBlank() shouldBe true
                    index.query(q.sql, 5, q.params.map { "app_name" })
                }
            }
        }
    }
}
