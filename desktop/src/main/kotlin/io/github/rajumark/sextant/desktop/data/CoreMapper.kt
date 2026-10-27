package io.github.rajumark.sextant.desktop.data

import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.insights.AppIcons
import io.github.rajumark.sextant.core.insights.IconFile
import io.github.rajumark.sextant.core.insights.IconLayer
import io.github.rajumark.sextant.core.output.SextantJson
import io.github.rajumark.sextant.desktop.model.AdaptivePreview
import io.github.rajumark.sextant.desktop.model.Analysis
import io.github.rajumark.sextant.desktop.model.ApkLayout
import io.github.rajumark.sextant.desktop.model.AppComponent
import io.github.rajumark.sextant.desktop.model.ComponentFacts
import io.github.rajumark.sextant.desktop.model.AppFlags
import io.github.rajumark.sextant.desktop.model.AppMeta
import io.github.rajumark.sextant.desktop.model.Certificate
import io.github.rajumark.sextant.desktop.model.CodeSummary
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.model.DataSpec
import io.github.rajumark.sextant.desktop.model.ExportedSource
import io.github.rajumark.sextant.desktop.model.FileInfo
import io.github.rajumark.sextant.desktop.model.IconFileInfo
import io.github.rajumark.sextant.desktop.model.IconGroup
import io.github.rajumark.sextant.desktop.model.IconPick
import io.github.rajumark.sextant.desktop.model.IntentFilterSpec
import io.github.rajumark.sextant.desktop.model.ManifestNode
import io.github.rajumark.sextant.desktop.model.Permission
import io.github.rajumark.sextant.desktop.model.PermissionKind
import io.github.rajumark.sextant.desktop.model.Signing
import java.nio.file.Files
import java.nio.file.Path
import io.github.rajumark.sextant.core.index.IndexReader
import io.github.rajumark.sextant.core.insights.AppStartup
import io.github.rajumark.sextant.core.insights.AppStartups
import io.github.rajumark.sextant.core.insights.ComponentReview
import io.github.rajumark.sextant.core.insights.SdkInventory
import io.github.rajumark.sextant.core.insights.StringCatalog
import io.github.rajumark.sextant.core.output.OutputFiles
import io.github.rajumark.sextant.core.insights.ComponentReviews
import io.github.rajumark.sextant.core.model.Component as CoreComponent
import io.github.rajumark.sextant.core.output.ManifestXml
import io.github.rajumark.sextant.core.model.Permission as CorePermission
import io.github.rajumark.sextant.core.model.XmlNode as CoreXmlNode

/** The single place that knows core's model shapes. A core schema change is fixed here, not in the screens. */
object CoreMapper {
    fun map(
        extraction: Extraction,
        outputDir: Path,
        apkPath: Path?,
        analyzedAtMillis: Long,
        durationMs: Long?,
    ): Analysis {
        val m = extraction.meta
        val pkg = m.`package` ?: "(no package)"
        val meta =
            AppMeta(
                schemaVersion = m.schemaVersion,
                toolVersion = m.tool.version,
                file = FileInfo(m.file.name, m.file.sizeBytes, m.file.sha256),
                packageName = pkg,
                versionName = m.versionName,
                versionCode = m.versionCode,
                minSdk = m.minSdk,
                targetSdk = m.targetSdk,
                compileSdk = m.compileSdk,
                // A label that is still a raw resource reference is less useful than the package's last segment.
                label =
                    m.label?.takeUnless { it.startsWith("@") || it.isBlank() }
                        ?: pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                applicationClass = m.applicationClass,
                flags =
                    AppFlags(
                        debuggable = m.flags.debuggable,
                        allowBackup = m.flags.allowBackup,
                        usesCleartextTraffic = m.flags.usesCleartextTraffic,
                        networkSecurityConfig = m.flags.networkSecurityConfig,
                        extractNativeLibs = m.flags.extractNativeLibs,
                    ),
                signing =
                    Signing(
                        verified = m.signing.verified,
                        schemes = m.signing.schemes,
                        certificates =
                            m.signing.certificates.map {
                                Certificate(
                                    it.sha256,
                                    it.sha1,
                                    it.subject,
                                    it.issuer,
                                    it.notBefore,
                                    it.notAfter,
                                )
                            },
                    ),
                layout = ApkLayout(m.layout.dexFiles, m.layout.nativeAbis),
                code = m.code?.let { CodeSummary(it.classes, it.methods, it.fields, it.callEdges, it.kotlinClasses) },
                warnings = m.warnings,
                icon = iconPick(m.icons),
                iconGroups = iconGroups(m.icons),
                adaptive = adaptivePreview(m.icons),
            )
        return Analysis(
            meta = meta,
            permissions = extraction.permissions.map { permission(it, extraction.manifest) },
            components =
                extraction.components.zip(ComponentReviews.of(extraction.components, extraction.meta.`package`)).map { (c, r) ->
                    component(c, r, extraction.manifest)
                },
            manifest = node(extraction.manifest),
            outputDir = outputDir.toString(),
            apkPath = apkPath?.toString(),
            outputFiles = OutputFolder.list(outputDir),
            analyzedAtMillis = analyzedAtMillis,
            durationMs = durationMs,
            startup = startup(extraction, outputDir),
            resources = extraction.resources,
            sdks =
                runCatching {
                    extraction.insights?.let { SdkInventory.of(outputDir.fileName.toString(), m.`package`, m.versionName, it.sdks, it.libraries) }
                        ?: SdkInventory.fromFolder(outputDir)
                }.getOrNull(),
            stringKeys = StringCatalog.of(extraction.resources),
            applicationXml =
                extraction.manifest.children
                    .firstOrNull { it.tag == "application" }
                    ?.let { ManifestXml.render(CoreXmlNode(it.tag, it.attributes, emptyList())).trimEnd() },
        )
    }

    /** Same as `sextant app`: follows startup calls when the folder has a code index. */
    private fun startup(
        extraction: Extraction,
        outputDir: Path,
    ): AppStartup? =
        runCatching {
            if (Files.exists(outputDir.resolve(OutputFiles.INDEX))) {
                IndexReader.open(outputDir).use { AppStartups.build(extraction, it) }
            } else {
                AppStartups.build(extraction, null)
            }
        }.getOrNull()

    private fun adaptivePreview(icons: AppIcons?): AdaptivePreview? {
        val adaptive = (icons?.icon ?: icons?.roundIcon)?.adaptive ?: return null
        return AdaptivePreview(
            backgroundColor = adaptive.background?.color,
            background = layerPick(adaptive.background),
            foreground = layerPick(adaptive.foreground),
            monochrome = layerPick(adaptive.monochrome),
        )
    }

    private fun layerPick(layer: IconLayer?): IconPick? {
        val path = layer?.best ?: return null
        return IconPick(path, layer.files.firstOrNull { it.path == path }?.format ?: "")
    }

    private fun iconGroups(icons: AppIcons?): List<IconGroup> {
        if (icons == null) return emptyList()

        fun file(f: IconFile) = IconFileInfo(f.path, f.config, f.format, f.width, f.height, f.sizeBytes)
        val groups = mutableListOf<IconGroup>()
        icons.icon?.let { groups += IconGroup("Launcher icon", it.resource, it.files.map(::file)) }
        icons.roundIcon?.let { groups += IconGroup("Round icon", it.resource, it.files.map(::file)) }
        icons.icon?.adaptive?.let { adaptive ->
            adaptive.background?.let { groups += IconGroup("Adaptive background", it.resource, it.files.map(::file)) }
            adaptive.foreground?.let { groups += IconGroup("Adaptive foreground", it.resource, it.files.map(::file)) }
            adaptive.monochrome?.let { groups += IconGroup("Monochrome", it.resource, it.files.map(::file)) }
        }
        return groups.filter { it.files.isNotEmpty() }
    }

    /**
     * The icon to draw: the launcher icon's best bitmap; for adaptive icons (vector-only or no bitmap) the foreground
     * layer's bitmap. Null when no raster file exists.
     */
    private fun iconPick(icons: AppIcons?): IconPick? {
        val ref = icons?.icon ?: icons?.roundIcon ?: return null
        val direct = ref.best?.let { path -> IconPick(path, ref.files.firstOrNull { it.path == path }?.format ?: "") }
        if (direct != null && direct.drawable) return direct
        val foreground = ref.adaptive?.foreground ?: return null
        val layer = foreground.best ?: return null
        return IconPick(layer, foreground.files.firstOrNull { it.path == layer }?.format ?: "").takeIf { it.drawable }
    }

    private fun permission(
        p: CorePermission,
        manifest: CoreXmlNode,
    ) = Permission(
            name = p.name,
            kind = PermissionKind.fromWire(p.kind),
            maxSdk = p.maxSdk,
            dangerous = p.dangerous,
            protectionLevel = p.protectionLevel,
            custom = p.custom,
            impliedBy = p.impliedBy,
            jsonLine = SextantJson.compact.encodeToString(CorePermission.serializer(), p),
            manifestXml =
                ManifestXml
                    .permission(manifest, p.name)
                    .filter { it.tag == (if (p.kind == "declared") "permission" else p.kind) }
                    .joinToString("") { ManifestXml.render(it) }
                    .trimEnd()
                    .ifEmpty { null },
        )

    private fun component(
        c: CoreComponent,
        r: ComponentReview,
        manifest: CoreXmlNode,
    ) = AppComponent(
            type = ComponentType.fromWire(c.type),
            name = c.name,
            exported = c.exported,
            exportedSource = ExportedSource.fromWire(c.exportedSource),
            enabled = c.enabled,
            permission = c.permission,
            process = c.process,
            launcher = c.launcher,
            targetActivity = c.targetActivity,
            authorities = c.authorities,
            intentFilters =
                c.intentFilters.map { f ->
                    IntentFilterSpec(
                        actions = f.actions,
                        categories = f.categories,
                        data =
                            f.data.map {
                                DataSpec(
                                    scheme = it.scheme,
                                    host = it.host,
                                    port = it.port,
                                    path = it.path,
                                    pathPrefix = it.pathPrefix,
                                    pathPattern = it.pathPattern,
                                    pathSuffix = it.pathSuffix,
                                    pathAdvancedPattern = it.pathAdvancedPattern,
                                    mimeType = it.mimeType,
                                )
                            },
                        priority = f.priority,
                        autoVerify = f.autoVerify,
                    )
                },
            jsonLine = SextantJson.compact.encodeToString(CoreComponent.serializer(), c),
            readPermission = c.readPermission,
            writePermission = c.writePermission,
            grantUriPermissions = c.grantUriPermissions,
            facts =
                ComponentFacts(
                    tags = r.tags,
                    reasons = r.reasons,
                    entryPoints = r.entryPoints,
                    deepLinks = r.deepLinks,
                    appLinkHosts = r.appLinkHosts,
                    aliases = r.aliases,
                    adb = r.adb.map { it.label to it.command },
                    attributes = ComponentReviews.attributes(c),
                    manifestXml = ManifestXml.component(manifest, c.name)?.let { ManifestXml.render(it).trimEnd() },
                    label = c.label,
                    pathPermissions =
                        c.pathPermissions.map { p ->
                            (p.path ?: p.pathPrefix?.let { "$it*" } ?: p.pathPattern ?: "?") + "  " +
                                listOfNotNull(
                                    p.permission?.let { "permission=$it" },
                                    p.readPermission?.let { "read=$it" },
                                    p.writePermission?.let { "write=$it" },
                                ).joinToString(" ")
                        },
                    grantUriPaths = c.grantUriPaths,
                ),
        )

    private fun node(n: CoreXmlNode): ManifestNode = ManifestNode(n.tag, n.attributes.toList(), n.children.map(::node))
}
