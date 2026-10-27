package io.github.rajumark.sextant.desktop.model

// What the screens show about one analyzed APK. Filled from the core library's Extraction by
// CoreMapper (and from fixtures in tests). Kept separate from core's serialized models so a
// schema change in core touches the mapper only.

data class Analysis(
    val meta: AppMeta,
    val permissions: List<Permission>,
    val components: List<AppComponent>,
    val manifest: ManifestNode,
    /** The output folder `sextant extract` wrote (or the folder that was opened). */
    val outputDir: String,
    /** The APK this came from, when known; needed to analyze again. */
    val apkPath: String?,
    val outputFiles: List<OutputFile>,
    val analyzedAtMillis: Long,
    /** Wall time of the extraction; null when an existing folder was opened. */
    val durationMs: Long?,
) {
    val dangerousCount: Int get() = permissions.count { it.dangerous }
    val declaredCount: Int get() = permissions.count { it.kind == PermissionKind.Declared }
    val requestedCount: Int get() = permissions.size - declaredCount
    val exportedCount: Int get() = components.count { it.exported }
    val deepLinkComponents: List<AppComponent> get() = components.filter { it.deepLinks.isNotEmpty() }
    val launcher: AppComponent? get() = components.firstOrNull { it.launcher && it.enabled } ?: components.firstOrNull { it.launcher }

    fun countOf(type: ComponentType): Int = components.count { it.type == type }
}

data class AppMeta(
    val schemaVersion: Int,
    val toolVersion: String,
    val file: FileInfo,
    val packageName: String,
    val versionName: String?,
    val versionCode: Long?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val compileSdk: Int?,
    val label: String,
    val applicationClass: String?,
    val flags: AppFlags,
    val signing: Signing,
    val layout: ApkLayout,
    val code: CodeSummary?,
    val warnings: List<String>,
    /** The launcher icon file to show, or null when the APK has none that can be drawn. */
    val icon: IconPick? = null,
    /** Every icon file the APK declares, grouped by role (launcher, round, adaptive layers). */
    val iconGroups: List<IconGroup> = emptyList(),
    /** The adaptive icon's layers, when the launcher icon is adaptive. Used for the launcher preview. */
    val adaptive: AdaptivePreview? = null,
) {
    /** "4.12.0 (41200)", tolerating either part being absent. */
    val versionText: String
        get() = listOfNotNull(versionName, versionCode?.let { "($it)" }).joinToString(" ").ifEmpty { "no version" }
}

data class FileInfo(
    val name: String,
    val sizeBytes: Long,
    val sha256: String,
)

data class AppFlags(
    val debuggable: Boolean?,
    val allowBackup: Boolean?,
    val usesCleartextTraffic: Boolean?,
    val networkSecurityConfig: String?,
    val extractNativeLibs: Boolean?,
)

data class Signing(
    val verified: Boolean,
    val schemes: List<String>,
    val certificates: List<Certificate>,
)

data class Certificate(
    val sha256: String,
    val sha1: String,
    val subject: String,
    val issuer: String,
    val notBefore: String,
    val notAfter: String,
)

data class ApkLayout(
    val dexFiles: Int,
    val nativeAbis: List<String>,
)

/** One icon role, for example "Launcher icon", with each density file the APK ships for it. */
data class IconGroup(
    val title: String,
    val resource: String,
    val files: List<IconFileInfo>,
)

data class IconFileInfo(
    val path: String,
    val config: String,
    val format: String,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long,
) {
    /** Only raster formats can be drawn; compiled vector XML cannot. */
    val drawable: Boolean get() = format in setOf("png", "webp", "jpg")
}

/** The layers of an adaptive icon: a background (color or picture) and a foreground, each as a file in the APK. */
data class AdaptivePreview(
    val backgroundColor: String?,
    val background: IconPick?,
    val foreground: IconPick?,
    val monochrome: IconPick?,
)

/** A file inside the APK zip that holds the app icon, and its format (png, webp, jpg, gif or xml). */
data class IconPick(
    val path: String,
    val format: String,
) {
    /** Only raster formats can be drawn; compiled vector XML cannot. */
    val drawable: Boolean get() = format in setOf("png", "webp", "jpg")
}

data class CodeSummary(
    val classes: Int,
    val methods: Int,
    val fields: Int,
    val callEdges: Int,
    val kotlinClasses: Int,
)

enum class PermissionKind(
    val wire: String,
) {
    UsesPermission("uses-permission"),
    UsesPermissionSdk23("uses-permission-sdk-23"),
    Declared("declared"),
    ;

    companion object {
        fun fromWire(wire: String): PermissionKind = entries.firstOrNull { it.wire == wire } ?: UsesPermission
    }
}

data class Permission(
    val name: String,
    val kind: PermissionKind,
    val maxSdk: Int?,
    val dangerous: Boolean,
    val protectionLevel: String?,
    val custom: Boolean,
    val impliedBy: String?,
    /** The exact line in permissions.jsonl. */
    val jsonLine: String,
) {
    val shortName: String get() = name.substringAfterLast('.')
}

enum class ComponentType(
    val wire: String,
    val label: String,
    val plural: String,
) {
    Activity("activity", "Activity", "Activities"),
    ActivityAlias("activity-alias", "Activity alias", "Aliases"),
    Service("service", "Service", "Services"),
    Receiver("receiver", "Receiver", "Receivers"),
    Provider("provider", "Provider", "Providers"),
    ;

    companion object {
        fun fromWire(wire: String): ComponentType = entries.firstOrNull { it.wire == wire } ?: Activity
    }
}

enum class ExportedSource(
    val wire: String,
) {
    Explicit("explicit"),
    Default("default"),
    ;

    companion object {
        fun fromWire(wire: String): ExportedSource = if (wire == Default.wire) Default else Explicit
    }
}

data class IntentFilterSpec(
    val actions: List<String>,
    val categories: List<String>,
    val data: List<DataSpec>,
    val priority: Int = 0,
    val autoVerify: Boolean = false,
)

data class DataSpec(
    val scheme: String? = null,
    val host: String? = null,
    val port: String? = null,
    val path: String? = null,
    val pathPrefix: String? = null,
    val pathPattern: String? = null,
    val pathSuffix: String? = null,
    val pathAdvancedPattern: String? = null,
    val mimeType: String? = null,
) {
    /** A readable URI for scheme/host data, e.g. `https://example.org/join*`. */
    val uri: String?
        get() {
            val s = scheme ?: return null
            val h = host ?: return "$s:"
            val p =
                path
                    ?: pathPrefix?.let { "$it*" }
                    ?: pathSuffix?.let { "*$it" }
                    ?: pathPattern
                    ?: pathAdvancedPattern
                    ?: ""
            val portPart = port?.let { ":$it" } ?: ""
            return "$s://$h$portPart$p"
        }
}

data class AppComponent(
    val type: ComponentType,
    val name: String,
    val exported: Boolean,
    val exportedSource: ExportedSource,
    val enabled: Boolean,
    val permission: String?,
    val process: String?,
    val launcher: Boolean,
    val targetActivity: String?,
    val authorities: List<String>,
    val intentFilters: List<IntentFilterSpec>,
    /** The exact line in components.jsonl. */
    val jsonLine: String,
) {
    val simpleName: String get() = name.substringAfterLast('.')
    val packagePath: String get() = name.substringBeforeLast('.', "")

    val deepLinks: List<String>
        get() =
            intentFilters
                .filter { f -> "android.intent.category.BROWSABLE" in f.categories }
                .flatMap { f -> f.data.mapNotNull { it.uri } }
                .distinct()
}

data class ManifestNode(
    val tag: String,
    val attributes: List<Pair<String, String>> = emptyList(),
    val children: List<ManifestNode> = emptyList(),
) {
    fun attr(name: String): String? = attributes.firstOrNull { it.first == name }?.second
}

enum class OutputFormat { Markdown, Json, JsonLines, Binary }

/** One file in the output folder. Content is read on demand: code files can be hundreds of MB. */
class OutputFile(
    /** Path relative to the output folder, e.g. `code/classes.jsonl`. */
    val name: String,
    val description: String,
    val format: OutputFormat,
    val sizeBytes: Long,
    private val loader: () -> String,
) {
    fun load(): String = loader()
}
