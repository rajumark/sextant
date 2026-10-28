package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.SextantException
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
import kotlinx.serialization.KSerializer
import java.sql.Connection

/**
 * The `records` table: every manifest, resource and insight record exactly as the extractor built it, one JSON row
 * each, so the whole extraction can be rebuilt from index.sqlite alone (APK in, SQLite out). The typed tables are
 * the queryable view of the same facts. Writer and reader share [parts], so they cannot drift apart.
 */
internal object Records {
    private class Part<T>(
        val kind: String,
        val serializer: KSerializer<T>,
        val get: (Extraction) -> List<T>,
    )

    private val parts: List<Part<*>> =
        listOf(
            Part("manifest", XmlNode.serializer()) { listOf(it.manifest) },
            Part("permission", Permission.serializer()) { it.permissions },
            Part("component", Component.serializer()) { it.components },
            Part("feature", FeatureRecord.serializer()) { it.features },
            Part("metadata", MetadataRecord.serializer()) { it.metadata },
            Part("query", QueryRecord.serializer()) { it.queries },
            Part("resources", ResourceHarvest.serializer()) { listOf(it.resources) },
            Part("insights", Insights.serializer()) { listOfNotNull(it.insights) },
        )

    fun write(
        conn: Connection,
        extraction: Extraction,
    ) {
        conn.prepareStatement("INSERT INTO records VALUES (?,?,?)").use { st ->
            for (part in parts) {
                @Suppress("UNCHECKED_CAST")
                val p = part as Part<Any?>
                p.get(extraction).forEachIndexed { i, record ->
                    st.setString(1, p.kind)
                    st.setInt(2, i)
                    st.setString(3, SextantJson.compact.encodeToString(p.serializer, record))
                    st.addBatch()
                }
            }
            st.executeBatch()
        }
    }

    /** The extraction stored in [conn], with [meta] and [files] read by the caller. */
    fun read(
        conn: Connection,
        meta: AppMeta,
        files: List<ApkFile>,
    ): Extraction {
        val hasTable =
            conn.createStatement().use { st ->
                st.executeQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'records'").use { it.next() }
            }
        if (!hasTable) {
            throw SextantException.input(
                "this index has no full records (built by an older Sextant)",
                "run `sextant extract <app.apk> --force` again, or pass the APK itself",
            )
        }
        val rows = HashMap<String, MutableList<String>>()
        conn.createStatement().use { st ->
            st.executeQuery("SELECT kind, json FROM records ORDER BY kind, seq").use { rs ->
                while (rs.next()) rows.getOrPut(rs.getString(1)) { mutableListOf() } += rs.getString(2)
            }
        }

        fun <T> list(
            kind: String,
            serializer: KSerializer<T>,
        ): List<T> = rows[kind].orEmpty().map { SextantJson.reader.decodeFromString(serializer, it) }
        return Extraction(
            meta = meta,
            manifest = list("manifest", XmlNode.serializer()).firstOrNull() ?: XmlNode("manifest", emptyMap(), emptyList()),
            permissions = list("permission", Permission.serializer()),
            components = list("component", Component.serializer()),
            insights = list("insights", Insights.serializer()).firstOrNull(),
            features = list("feature", FeatureRecord.serializer()),
            metadata = list("metadata", MetadataRecord.serializer()),
            queries = list("query", QueryRecord.serializer()),
            resources = list("resources", ResourceHarvest.serializer()).firstOrNull() ?: ResourceHarvest(),
            files = files,
        )
    }
}
