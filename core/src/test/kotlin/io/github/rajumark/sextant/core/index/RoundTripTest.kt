package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.CorpusApks
import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.insights.Insights
import io.github.rajumark.sextant.core.model.ApkFile
import io.github.rajumark.sextant.core.model.AppMeta
import io.github.rajumark.sextant.core.model.Component
import io.github.rajumark.sextant.core.model.FeatureRecord
import io.github.rajumark.sextant.core.model.MetadataRecord
import io.github.rajumark.sextant.core.model.Permission
import io.github.rajumark.sextant.core.model.QueryRecord
import io.github.rajumark.sextant.core.model.ResourceHarvest
import io.github.rajumark.sextant.core.model.XmlNode
import io.github.rajumark.sextant.core.output.SextantJson
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files

/** APK in, SQLite out: everything the extractor built must come back unchanged from the .sqlite file alone. */
class RoundTripTest {
    @ParameterizedTest
    @ValueSource(strings = ["binaryeye", "newpipe", "connectbot", "aurora"])
    fun `an extraction read back from SQLite equals the one written`(id: String) {
        val dir = Files.createTempDirectory("sextant-roundtrip")
        val file = dir.resolve("$id.sqlite")
        try {
            val written = IndexBuilder.build(CorpusApks.require(id), file)
            IndexReader.open(file).use { index ->
                val read = index.extraction()
                for ((part, json) in PARTS) withClue("$id $part") { json(read) shouldBe json(written) }
                // The raw layer: every binary XML file rebuilt from xml_nodes / xml_attrs equals the decoded file.
                written.raw.xml.size shouldBeGreaterThan 1
                for (doc in written.raw.xml) {
                    withClue("$id ${doc.path}") {
                        SextantJson.compact.encodeToString(XmlNode.serializer().nullable, index.xmlTree(doc.path)) shouldBe
                            SextantJson.compact.encodeToString(XmlNode.serializer(), doc.root)
                    }
                }
                index.query("SELECT count(*) FROM resource_values").rows[0][0] shouldBe written.raw.resourceTable.size
            }
        } finally {
            Files.deleteIfExists(file)
            Files.deleteIfExists(dir)
        }
    }

    private companion object {
        val PARTS: List<Pair<String, (Extraction) -> String>> =
            listOf(
                "meta" to { e -> SextantJson.compact.encodeToString(AppMeta.serializer(), e.meta) },
                "manifest" to { e -> SextantJson.compact.encodeToString(XmlNode.serializer(), e.manifest) },
                "permissions" to { e -> SextantJson.compact.encodeToString(ListSerializer(Permission.serializer()), e.permissions) },
                "components" to { e -> SextantJson.compact.encodeToString(ListSerializer(Component.serializer()), e.components) },
                "features" to { e -> SextantJson.compact.encodeToString(ListSerializer(FeatureRecord.serializer()), e.features) },
                "metadata" to { e -> SextantJson.compact.encodeToString(ListSerializer(MetadataRecord.serializer()), e.metadata) },
                "queries" to { e -> SextantJson.compact.encodeToString(ListSerializer(QueryRecord.serializer()), e.queries) },
                "resources" to { e -> SextantJson.compact.encodeToString(ResourceHarvest.serializer(), e.resources) },
                "insights" to { e -> SextantJson.compact.encodeToString(Insights.serializer().nullable, e.insights) },
                "files" to { e -> SextantJson.compact.encodeToString(ListSerializer(ApkFile.serializer()), e.files) },
            )
    }
}
