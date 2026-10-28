package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.AppFlags
import io.github.rajumark.sextant.core.model.Component
import io.github.rajumark.sextant.core.model.FeatureRecord
import io.github.rajumark.sextant.core.model.IntentData
import io.github.rajumark.sextant.core.model.IntentFilter
import io.github.rajumark.sextant.core.model.MetadataRecord
import io.github.rajumark.sextant.core.model.PathPermission
import io.github.rajumark.sextant.core.model.Permission
import io.github.rajumark.sextant.core.model.QueryRecord
import io.github.rajumark.sextant.core.model.XmlNode

/** The manifest facts that end up in meta.json. */
data class ManifestFacts(
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val compileSdk: Int?,
    val label: String?,
    val applicationClass: String?,
    val flags: AppFlags,
)

/** Turns the decoded manifest tree into Sextant's models. Pure: no I/O. */
class ManifestMapper(
    private val manifest: XmlNode,
    private val resources: ResourceResolver,
    private val warnings: MutableList<String>,
) {
    private val packageName = manifest.attr("package")
    private val application = manifest.child("application")
    private val usesSdk = manifest.child("uses-sdk")
    private val minSdk = sdkInt(usesSdk?.android("minSdkVersion"), "minSdkVersion")
    private val targetSdk = sdkInt(usesSdk?.android("targetSdkVersion"), "targetSdkVersion")

    fun facts(): ManifestFacts =
        ManifestFacts(
            packageName = packageName,
            versionName = resources.resolve(manifest.android("versionName")),
            versionCode = resources.resolve(manifest.android("versionCode"))?.toLongOrNull(),
            minSdk = minSdk,
            targetSdk = targetSdk,
            compileSdk = (manifest.android("compileSdkVersion") ?: manifest.attr("platformBuildVersionCode"))?.toIntOrNull(),
            label = resources.resolve(application?.android("label")),
            applicationClass = application?.android("name")?.let(::className),
            flags =
                AppFlags(
                    debuggable = bool(application?.android("debuggable")),
                    allowBackup = bool(application?.android("allowBackup")),
                    usesCleartextTraffic = bool(application?.android("usesCleartextTraffic")),
                    networkSecurityConfig = application?.android("networkSecurityConfig"),
                    extractNativeLibs = bool(application?.android("extractNativeLibs")),
                    testOnly = bool(application?.android("testOnly")),
                    fullBackupContent = application?.android("fullBackupContent"),
                    dataExtractionRules = application?.android("dataExtractionRules"),
                    backupAgent = application?.android("backupAgent")?.let(::className),
                    fullBackupOnly = bool(application?.android("fullBackupOnly")),
                    hasFragileUserData = bool(application?.android("hasFragileUserData")),
                    requestLegacyExternalStorage = bool(application?.android("requestLegacyExternalStorage")),
                    preserveLegacyExternalStorage = bool(application?.android("preserveLegacyExternalStorage")),
                    largeHeap = bool(application?.android("largeHeap")),
                    hardwareAccelerated = bool(application?.android("hardwareAccelerated")),
                    supportsRtl = bool(application?.android("supportsRtl")),
                    resizeableActivity = bool(application?.android("resizeableActivity")),
                    enableOnBackInvokedCallback = bool(application?.android("enableOnBackInvokedCallback")),
                    appComponentFactory = application?.android("appComponentFactory")?.let(::className),
                    localeConfig = application?.android("localeConfig"),
                    appCategory = application?.android("appCategory"),
                    isGame = bool(application?.android("isGame")),
                    memtagMode = application?.android("memtagMode"),
                    usesNonSdkApi = bool(application?.android("usesNonSdkApi")),
                    manageSpaceActivity = application?.android("manageSpaceActivity")?.let(::className),
                    taskAffinity = application?.android("taskAffinity"),
                    process = application?.android("process"),
                    theme = application?.android("theme"),
                    profileableShell = application?.child("profileable")?.let { bool(it.android("shell")) ?: false },
                    installLocation = manifest.android("installLocation"),
                    sharedUserId = manifest.android("sharedUserId"),
                    sharedUserMaxSdk = resources.resolve(manifest.android("sharedUserMaxSdkVersion"))?.toIntOrNull(),
                    targetSandboxVersion = resources.resolve(manifest.android("targetSandboxVersion"))?.toIntOrNull(),
                    supportsScreens =
                        manifest
                            .child("supports-screens")
                            ?.attributes
                            ?.mapKeys { it.key.removePrefix("android:") }
                            ?.toSortedMap()
                            .orEmpty(),
                    icon = application?.android("icon"),
                    roundIcon = application?.android("roundIcon"),
                    banner = application?.android("banner"),
                    logo = application?.android("logo"),
                    description = resources.resolve(application?.android("description")),
                    hasCode = bool(application?.android("hasCode")),
                    intentMatchingFlags = application?.android("intentMatchingFlags"),
                    backupInForeground = bool(application?.android("backupInForeground")),
                    restoreAnyVersion = bool(application?.android("restoreAnyVersion")),
                    killAfterRestore = bool(application?.android("killAfterRestore")),
                    zygotePreloadName = application?.android("zygotePreloadName")?.let(::className),
                    windowSoftInputMode = application?.android("windowSoftInputMode"),
                    configChanges = application?.android("configChanges"),
                    sharedUserLabel = resources.resolve(manifest.android("sharedUserLabel")),
                    compileSdkCodename = manifest.android("compileSdkVersionCodename"),
                    platformBuildVersionName = manifest.attr("platformBuildVersionName"),
                    attributions =
                        manifest
                            .children("attribution")
                            .mapNotNull { a -> a.android("tag")?.let { it to resources.resolve(a.android("label")) } }
                            .toMap()
                            .toSortedMap(),
                ),
        )

    /** `uses-feature`, `uses-library` and `uses-native-library`, sorted by kind then name. */
    fun features(): List<FeatureRecord> {
        val out = mutableListOf<FeatureRecord>()
        for (node in manifest.children("uses-feature")) {
            val name = node.android("name")
            val gl = node.android("glEsVersion")
            when {
                name != null -> out += FeatureRecord("feature", name, bool(node.android("required")), node.android("version"))
                gl != null -> out += FeatureRecord("feature", "glEsVersion", bool(node.android("required")), glVersion(gl))
            }
        }
        for ((tag, kind) in listOf("uses-library" to "library", "uses-native-library" to "native-library")) {
            for (node in application?.children(tag).orEmpty()) {
                val name = node.android("name") ?: continue
                out += FeatureRecord(kind, name, bool(node.android("required")))
            }
        }
        return out.distinct().sortedWith(compareBy({ FEATURE_ORDER.indexOf(it.kind) }, { it.name }, { it.version ?: "" }))
    }

    /** `meta-data` and `property` on the application and on every component, in manifest order per scope. */
    fun metadata(): List<MetadataRecord> {
        val app = application ?: return emptyList()
        val out = mutableListOf<MetadataRecord>()

        fun collect(
            scope: String,
            node: XmlNode,
        ) {
            for (child in node.children) {
                if (child.tag != "meta-data" && child.tag != "property") continue
                val name = child.android("name") ?: continue
                out +=
                    MetadataRecord(scope, child.tag, name, child.android("value")?.let { resources.resolve(it) }, child.android("resource"))
            }
        }
        collect("application", app)
        for (node in app.children.filter { it.tag in TYPE_ORDER }) {
            val name = node.android("name")?.let(::className) ?: continue
            collect(name, node)
        }
        return out.sortedWith(compareBy({ it.scope != "application" }, { it.scope }, { it.name }))
    }

    /** `<queries>`: packages, intents and providers the app needs to see (Android 11 package visibility). */
    fun queries(): List<QueryRecord> {
        val out = mutableListOf<QueryRecord>()
        for (queries in manifest.children("queries")) {
            for (node in queries.children) {
                when (node.tag) {
                    "package" -> node.android("name")?.let { out += QueryRecord("package", it) }
                    "provider" -> node.android("authorities")?.let { out += QueryRecord("provider", it) }
                    "intent" -> {
                        val f = intentFilter(node)
                        val label =
                            listOfNotNull(
                                f.actions.firstOrNull()?.substringAfterLast('.'),
                                f.data.firstOrNull()?.let { d -> d.scheme?.let { "$it:" } ?: d.mimeType },
                            ).joinToString(" ").ifEmpty { "intent" }
                        out += QueryRecord("intent", label, f.actions, f.categories, f.data)
                    }
                }
            }
        }
        return out.distinct().sortedWith(compareBy({ QUERY_ORDER.indexOf(it.kind) }, { it.name }))
    }

    /** `0x00030001` → `3.1`. */
    private fun glVersion(raw: String): String {
        val v = raw.removePrefix("0x").toLongOrNull(16) ?: return raw
        return "${v shr 16}.${v and 0xffff}"
    }

    fun permissions(): List<Permission> {
        val result = mutableListOf<Permission>()
        for (kind in listOf("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")) {
            for (node in manifest.children(kind)) {
                val name = node.android("name") ?: continue
                result +=
                    Permission(
                        name = name,
                        kind = if (kind == "uses-permission-sdk-m") "uses-permission-sdk-23" else kind,
                        maxSdk = resources.resolve(node.android("maxSdkVersion"))?.toIntOrNull(),
                        dangerous = DangerousPermissions.isDangerous(name),
                        protectionLevel = null,
                        custom = !isPlatformPermission(name),
                        impliedBy = null,
                        minSdk = resources.resolve(node.android("minSdkVersion"))?.toIntOrNull(),
                        usesPermissionFlags = node.android("usesPermissionFlags"),
                        required = bool(node.android("required")),
                    )
            }
        }
        for (node in manifest.children("permission")) {
            val name = node.android("name") ?: continue
            val level = node.android("protectionLevel")
            result +=
                Permission(
                    name = name,
                    kind = "declared",
                    maxSdk = null,
                    dangerous = level?.split('|')?.contains("dangerous") == true,
                    protectionLevel = level,
                    custom = true,
                    impliedBy = null,
                    label = resources.resolve(node.android("label")),
                    description = resources.resolve(node.android("description")),
                    permissionGroup = node.android("permissionGroup"),
                )
        }
        result += impliedPermissions(result)
        return result.distinct().sortedWith(compareBy({ KIND_ORDER.indexOf(it.kind) }, { it.name }, { it.maxSdk ?: Int.MAX_VALUE }))
    }

    /**
     * Permissions Android grants without them being listed, same rules as aapt2 `uses-implied-permission`.
     */
    private fun impliedPermissions(requested: List<Permission>): List<Permission> {
        val names = requested.filter { it.kind != "declared" }.associateBy { it.name }
        val target = targetSdk ?: minSdk ?: 1
        val implied = mutableListOf<Permission>()

        fun imply(
            name: String,
            by: String,
            maxSdk: Int?,
        ) {
            if (name in names || implied.any { it.name == name }) return
            implied += Permission(name, "implied", maxSdk, DangerousPermissions.isDangerous(name), null, false, by)
        }
        names[STORAGE_WRITE]?.let { imply(STORAGE_READ, STORAGE_WRITE, it.maxSdk) }
        if (target < 4) {
            imply(STORAGE_WRITE, "targetSdk < 4", null)
            imply(STORAGE_READ, "targetSdk < 4", null)
            imply("android.permission.READ_PHONE_STATE", "targetSdk < 4", null)
        }
        if (target < 16) {
            if ("android.permission.READ_CONTACTS" in
                names
            ) {
                imply("android.permission.READ_CALL_LOG", "android.permission.READ_CONTACTS", null)
            }
            if ("android.permission.WRITE_CONTACTS" in
                names
            ) {
                imply("android.permission.WRITE_CALL_LOG", "android.permission.WRITE_CONTACTS", null)
            }
        }
        return implied
    }

    fun components(): List<Component> {
        val app = application ?: return emptyList()
        return app.children
            .filter { it.tag in TYPE_ORDER }
            .mapNotNull(::component)
            .sortedWith(compareBy({ TYPE_ORDER.indexOf(it.type) }, { it.name }))
    }

    private fun component(node: XmlNode): Component? {
        val name = node.android("name")?.let(::className) ?: return null
        val filters = node.children("intent-filter").map(::intentFilter)
        val exported =
            ExportedRules.resolve(
                type = node.tag,
                explicit = bool(node.android("exported")),
                hasIntentFilters = filters.isNotEmpty(),
                minSdk = minSdk,
                targetSdk = targetSdk,
            )
        val isActivity = node.tag == "activity" || node.tag == "activity-alias"
        return Component(
            type = node.tag,
            name = name,
            exported = exported.exported,
            exportedSource = exported.source,
            enabled = bool(node.android("enabled")) ?: true,
            permission = node.android("permission"),
            process = node.android("process"),
            launcher =
                isActivity &&
                    filters.any {
                        "android.intent.action.MAIN" in it.actions && "android.intent.category.LAUNCHER" in it.categories
                    },
            targetActivity = node.android("targetActivity")?.let(::className),
            // Authorities can be a resource reference (`@string/authority`); resolve before splitting.
            authorities =
                resources
                    .resolve(node.android("authorities"))
                    ?.split(';')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty(),
            intentFilters = filters,
            readPermission = node.android("readPermission"),
            writePermission = node.android("writePermission"),
            grantUriPermissions = bool(node.android("grantUriPermissions")),
            pathPermissions =
                node.children("path-permission").map {
                    PathPermission(
                        path = it.android("path"),
                        pathPrefix = it.android("pathPrefix"),
                        pathPattern = it.android("pathPattern"),
                        permission = it.android("permission"),
                        readPermission = it.android("readPermission"),
                        writePermission = it.android("writePermission"),
                    )
                },
            grantUriPaths =
                node.children("grant-uri-permission").mapNotNull {
                    it.android("path") ?: it.android("pathPrefix")?.let { p -> "$p*" } ?: it.android("pathPattern")
                },
            launchMode = launchMode(resources.resolve(node.android("launchMode"))),
            taskAffinity = node.android("taskAffinity"),
            excludeFromRecents = bool(node.android("excludeFromRecents")),
            allowTaskReparenting = bool(node.android("allowTaskReparenting")),
            screenOrientation = node.android("screenOrientation"),
            theme = node.android("theme"),
            configChanges = node.android("configChanges"),
            windowSoftInputMode = node.android("windowSoftInputMode"),
            documentLaunchMode = node.android("documentLaunchMode"),
            lockTaskMode = node.android("lockTaskMode"),
            parentActivityName = node.android("parentActivityName")?.let(::className),
            noHistory = bool(node.android("noHistory")),
            clearTaskOnLaunch = bool(node.android("clearTaskOnLaunch")),
            finishOnTaskLaunch = bool(node.android("finishOnTaskLaunch")),
            alwaysRetainTaskState = bool(node.android("alwaysRetainTaskState")),
            autoRemoveFromRecents = bool(node.android("autoRemoveFromRecents")),
            maxRecents = resources.resolve(node.android("maxRecents"))?.toIntOrNull(),
            resizeableActivity = bool(node.android("resizeableActivity")),
            supportsPictureInPicture = bool(node.android("supportsPictureInPicture")),
            showWhenLocked = bool(node.android("showWhenLocked")),
            turnScreenOn = bool(node.android("turnScreenOn")),
            allowEmbedded = bool(node.android("allowEmbedded")),
            hardwareAccelerated = bool(node.android("hardwareAccelerated")),
            stateNotNeeded = bool(node.android("stateNotNeeded")),
            maxAspectRatio = node.android("maxAspectRatio"),
            persistableMode = node.android("persistableMode"),
            relinquishTaskIdentity = bool(node.android("relinquishTaskIdentity")),
            showOnLockScreen = bool(node.android("showOnLockScreen")),
            enableOnBackInvokedCallback = bool(node.android("enableOnBackInvokedCallback")),
            banner = node.android("banner"),
            windowLayout =
                node
                    .child("layout")
                    ?.attributes
                    ?.mapKeys { it.key.removePrefix("android:") }
                    ?.toSortedMap()
                    .orEmpty(),
            foregroundServiceType = node.android("foregroundServiceType"),
            isolatedProcess = bool(node.android("isolatedProcess")),
            externalService = bool(node.android("externalService")),
            stopWithTask = bool(node.android("stopWithTask")),
            useAppZygote = bool(node.android("useAppZygote")),
            visibleToInstantApps = bool(node.android("visibleToInstantApps")),
            description = resources.resolve(node.android("description")),
            multiprocess = bool(node.android("multiprocess")),
            syncable = bool(node.android("syncable")),
            initOrder = resources.resolve(node.android("initOrder"))?.toIntOrNull(),
            forceUriPermissions = bool(node.android("forceUriPermissions")),
            directBootAware = bool(node.android("directBootAware")),
            label = resources.resolve(node.android("label")),
            icon = node.android("icon"),
            roundIcon = node.android("roundIcon"),
        )
    }

    /** launchMode as its name; an `@integer/…` resource resolves to the enum's number. */
    private fun launchMode(raw: String?): String? =
        when (raw) {
            "0" -> "standard"
            "1" -> "singleTop"
            "2" -> "singleTask"
            "3" -> "singleInstance"
            "4" -> "singleInstancePerTask"
            else -> raw
        }

    private fun intentFilter(node: XmlNode) =
        IntentFilter(
            actions = node.children("action").mapNotNull { it.android("name") }.sorted(),
            categories = node.children("category").mapNotNull { it.android("name") }.sorted(),
            data =
                node.children("data").map {
                    IntentData(
                        scheme = it.android("scheme"),
                        host = it.android("host"),
                        port = it.android("port"),
                        path = it.android("path"),
                        pathPrefix = it.android("pathPrefix"),
                        pathPattern = it.android("pathPattern"),
                        pathSuffix = it.android("pathSuffix"),
                        pathAdvancedPattern = it.android("pathAdvancedPattern"),
                        mimeType = it.android("mimeType"),
                        ssp = it.android("ssp"),
                        sspPrefix = it.android("sspPrefix"),
                        sspPattern = it.android("sspPattern"),
                    )
                },
            priority = resources.resolve(node.android("priority"))?.toIntOrNull() ?: 0,
            autoVerify = bool(node.android("autoVerify")) ?: false,
            label = resources.resolve(node.android("label")),
            order = resources.resolve(node.android("order"))?.toIntOrNull(),
        )

    /** Android expands `.Foo` and bare `Foo` relative to the package. */
    private fun className(raw: String): String {
        val name = resources.resolve(raw) ?: raw
        if (packageName == null) return name
        return when {
            name.startsWith(".") -> packageName + name
            !name.contains('.') -> "$packageName.$name"
            else -> name
        }
    }

    private fun bool(raw: String?): Boolean? =
        when (resources.resolve(raw)?.lowercase()) {
            null -> null
            "true" -> true
            "false" -> false
            else -> {
                warnings += "unexpected boolean value '$raw'; treated as absent"
                null
            }
        }

    private fun sdkInt(
        raw: String?,
        attribute: String,
    ): Int? {
        val value = resources.resolve(raw) ?: return null
        return value.toIntOrNull() ?: run {
            warnings += "$attribute is '$value', not a number (preview SDK?); reported as null"
            null
        }
    }

    companion object {
        val TYPE_ORDER = listOf("activity", "activity-alias", "service", "receiver", "provider")
        private val FEATURE_ORDER = listOf("feature", "library", "native-library")
        private val QUERY_ORDER = listOf("package", "intent", "provider")
        private val KIND_ORDER = listOf("uses-permission", "uses-permission-sdk-23", "implied", "declared")
        private const val STORAGE_READ = "android.permission.READ_EXTERNAL_STORAGE"
        private const val STORAGE_WRITE = "android.permission.WRITE_EXTERNAL_STORAGE"

        fun isPlatformPermission(name: String) = name.startsWith("android.permission.") || name.startsWith("com.android.")
    }
}
