package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.index.TeeSink
import io.github.rajumark.sextant.core.insights.AppIcons
import io.github.rajumark.sextant.core.insights.CodeResourceRefs
import io.github.rajumark.sextant.core.insights.Findings
import io.github.rajumark.sextant.core.insights.FindingsCollector
import io.github.rajumark.sextant.core.insights.Icons
import io.github.rajumark.sextant.core.insights.Insights
import io.github.rajumark.sextant.core.insights.InsightsBuilder
import io.github.rajumark.sextant.core.insights.InsightsCollector
import io.github.rajumark.sextant.core.insights.ResourceHarvester
import io.github.rajumark.sextant.core.insights.ResourceUseCollector
import io.github.rajumark.sextant.core.model.ApkFile
import io.github.rajumark.sextant.core.model.AppMeta
import io.github.rajumark.sextant.core.model.Component
import io.github.rajumark.sextant.core.model.FeatureRecord
import io.github.rajumark.sextant.core.model.MetadataRecord
import io.github.rajumark.sextant.core.model.Permission
import io.github.rajumark.sextant.core.model.QueryRecord
import io.github.rajumark.sextant.core.model.ResourceHarvest
import io.github.rajumark.sextant.core.model.ToolInfo
import io.github.rajumark.sextant.core.model.XmlNode
import java.nio.file.Path
import java.util.zip.ZipFile

/** Everything Sextant keeps in memory about one APK. Code records are streamed to a [CodeSink] instead. */
data class Extraction(
    val meta: AppMeta,
    val manifest: XmlNode,
    val permissions: List<Permission>,
    val components: List<Component>,
    /** Null when read back from an output folder (`info`): the counts are in [AppMeta.insights]. */
    val insights: Insights? = null,
    val features: List<FeatureRecord> = emptyList(),
    val metadata: List<MetadataRecord> = emptyList(),
    val queries: List<QueryRecord> = emptyList(),
    /** res/ and assets/ (schema 8); empty when read from an older folder. */
    val resources: ResourceHarvest = ResourceHarvest(),
    /** Every zip entry (index only); empty unless [Extractor.extract] was asked to list files. */
    val files: List<ApkFile> = emptyList(),
)

object Extractor {
    /** Just the launcher icons of an APK: decodes the manifest and resources only (fast). */
    fun icons(input: Path): AppIcons {
        val apk = Bundles.open(input).apk
        val contents = ApkReader.read(apk)
        val decoded = ManifestDecoder.decode(contents.manifest, contents.resourceTable, mutableListOf())
        return ZipFile(apk.toFile()).use { Icons.resolve(it, decoded.root, decoded.resources, decoded.decodeXml) }
    }

    /** A sink that only lets [CodeExtractor] count, for in-memory summaries. */
    val COUNT_ONLY = object : CodeSink {}

    /**
     * @param code where to stream code records; `null` skips code extraction entirely (`meta.code` is then null).
     * @param listFiles also list and hash every zip entry ([Extraction.files]), for the index.
     */
    fun extract(
        input: Path,
        code: CodeSink? = COUNT_ONLY,
        listFiles: Boolean = false,
    ): Extraction {
        val warnings = mutableListOf<String>()
        // A split bundle is merged into one APK first; the signature is checked on its base APK as shipped.
        val opened = Bundles.open(input)
        val apk = opened.apk
        val contents = ApkReader.read(apk)
        val decoded = ManifestDecoder.decode(contents.manifest, contents.resourceTable, warnings)
        val mapper = ManifestMapper(decoded.root, decoded.resources, warnings)
        val facts = mapper.facts()
        val permissions = mapper.permissions()
        val components = mapper.components()
        val signing = SigningReader.read(opened.signed, warnings)
        val collector = InsightsCollector()
        val findingsCollector = FindingsCollector()
        val resourceUses = ResourceUseCollector()
        val codeStats = code?.let { CodeExtractor.extract(apk, TeeSink(it, collector, findingsCollector, resourceUses), warnings) }
        val insightsStart = System.nanoTime()
        val built = InsightsBuilder.build(apk, decoded.root, components, decoded.resources, collector.takeIf { code != null }, warnings)
        val findings =
            Findings.build(
                facts.flags,
                facts.targetSdk,
                components,
                built.resources,
                findingsCollector.takeIf {
                    code != null
                },
                facts.packageName,
            )
        val insights =
            built.copy(
                findings = findings,
                stats = built.stats.copy(findings = findings.groupingBy { it.severity }.eachCount().toSortedMap()),
            )
        if (System.getProperty("sextant.profile") == "true") {
            System.err.println("[profile] insights                   %6d ms".format((System.nanoTime() - insightsStart) / 1_000_000))
        }
        val icons =
            try {
                ZipFile(apk.toFile()).use { Icons.resolve(it, decoded.root, decoded.resources, decoded.decodeXml) }
            } catch (e: Exception) {
                warnings += "app icon could not be resolved (${e.javaClass.simpleName}: ${e.message})"
                null
            }
        if (facts.packageName == null) warnings += "manifest has no package attribute"
        val harvested =
            try {
                ZipFile(apk.toFile()).use { ResourceHarvester.harvest(it, decoded) }
            } catch (e: Exception) {
                warnings += "resources could not be harvested (${e.javaClass.simpleName}: ${e.message})"
                ResourceHarvest()
            }
        // Code is a referrer too: which methods load which resources.
        val codeRefs = CodeResourceRefs.of(resourceUses.uses, decoded.table)
        val resources = if (codeRefs.isEmpty()) harvested else harvested.copy(refs = harvested.refs + codeRefs)
        val files =
            if (!listFiles) {
                emptyList()
            } else {
                try {
                    val decoders = ApkFiles.decoders(codeStats, decoded.table.isNotEmpty(), signing, resources, insights)
                    ApkFiles.markDecoded(ApkFiles.list(apk, decoded.table, warnings), decoders, signing)
                } catch (e: Exception) {
                    warnings += "zip entries could not be listed (${e.javaClass.simpleName}: ${e.message})"
                    emptyList()
                }
            }

        val meta =
            AppMeta(
                schemaVersion = Sextant.SCHEMA_VERSION,
                tool = ToolInfo(Sextant.NAME, Sextant.version),
                file = if (opened.bundle != null) ApkReader.fileInfo(input) else contents.file,
                `package` = facts.packageName,
                versionName = facts.versionName,
                versionCode = facts.versionCode,
                minSdk = facts.minSdk,
                targetSdk = facts.targetSdk,
                compileSdk = facts.compileSdk,
                label = facts.label,
                applicationClass = facts.applicationClass,
                flags = facts.flags,
                signing = signing,
                layout = contents.layout,
                bundle = opened.bundle,
                code = codeStats,
                insights = insights.stats,
                icons = icons,
                warnings = warnings.distinct().sorted(),
            )
        return Extraction(
            meta,
            decoded.root,
            permissions,
            components,
            insights,
            mapper.features(),
            mapper.metadata(),
            mapper.queries(),
            resources,
            files,
        )
    }
}
