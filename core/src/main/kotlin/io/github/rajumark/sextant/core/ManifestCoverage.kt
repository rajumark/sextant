package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.XmlNode

/**
 * Which manifest attributes Sextant turns into structured fields. Everything is always kept verbatim in manifest.json;
 * this list says what is also typed in meta.json, permissions.jsonl, components.jsonl, features/metadata/queries.jsonl.
 * A corpus test fails when a real manifest uses an attribute that is neither structured nor listed in [RAW_ONLY].
 */
object ManifestCoverage {
    private val COMPONENT_COMMON =
        setOf("name", "exported", "enabled", "permission", "process", "label", "icon", "roundIcon", "directBootAware", "description")
    private val ACTIVITY =
        COMPONENT_COMMON +
            setOf(
                "launchMode", "taskAffinity", "excludeFromRecents", "allowTaskReparenting", "screenOrientation", "theme",
                "configChanges", "windowSoftInputMode", "documentLaunchMode", "lockTaskMode", "parentActivityName", "noHistory",
                "clearTaskOnLaunch", "finishOnTaskLaunch", "alwaysRetainTaskState", "autoRemoveFromRecents", "maxRecents",
                "resizeableActivity", "supportsPictureInPicture", "showWhenLocked", "turnScreenOn", "allowEmbedded",
                "hardwareAccelerated", "stateNotNeeded", "maxAspectRatio", "persistableMode", "relinquishTaskIdentity",
                "showOnLockScreen", "enableOnBackInvokedCallback", "banner", "targetActivity",
            )

    /** `parent>tag` → structured attribute names (without the `android:` prefix). */
    val STRUCTURED: Map<String, Set<String>> =
        mapOf(
            ">manifest" to
                setOf(
                    "package", "versionCode", "versionName", "compileSdkVersion", "compileSdkVersionCodename", "platformBuildVersionCode",
                    "platformBuildVersionName", "installLocation", "sharedUserId", "sharedUserLabel", "sharedUserMaxSdkVersion",
                    "targetSandboxVersion",
                ),
            "manifest>uses-sdk" to setOf("minSdkVersion", "targetSdkVersion", "maxSdkVersion"),
            "manifest>application" to
                setOf(
                    "name", "label", "debuggable", "allowBackup", "usesCleartextTraffic", "networkSecurityConfig", "extractNativeLibs",
                    "testOnly", "fullBackupContent", "dataExtractionRules", "backupAgent", "fullBackupOnly", "hasFragileUserData",
                    "requestLegacyExternalStorage", "preserveLegacyExternalStorage", "largeHeap", "hardwareAccelerated", "supportsRtl",
                    "resizeableActivity", "enableOnBackInvokedCallback", "appComponentFactory", "localeConfig", "appCategory", "isGame",
                    "memtagMode", "usesNonSdkApi", "manageSpaceActivity", "taskAffinity", "process", "theme", "installLocation", "icon",
                    "roundIcon", "banner", "logo", "description", "hasCode", "intentMatchingFlags", "backupInForeground",
                    "restoreAnyVersion", "killAfterRestore", "zygotePreloadName", "windowSoftInputMode", "configChanges",
                ),
            "manifest>uses-permission" to setOf("name", "maxSdkVersion", "minSdkVersion", "usesPermissionFlags", "required"),
            "manifest>uses-permission-sdk-23" to setOf("name", "maxSdkVersion"),
            "manifest>permission" to setOf("name", "protectionLevel", "label", "description", "permissionGroup"),
            "manifest>uses-feature" to setOf("name", "required", "version", "glEsVersion"),
            "manifest>supports-screens" to setOf("*"),
            "manifest>attribution" to setOf("tag", "label"),
            "manifest>queries" to emptySet(),
            "queries>package" to setOf("name"),
            "queries>provider" to setOf("authorities", "name"),
            "queries>intent" to emptySet(),
            "intent>action" to setOf("name"),
            "intent>category" to setOf("name"),
            "intent>data" to setOf("*"),
            "application>activity" to ACTIVITY,
            "application>activity-alias" to ACTIVITY,
            "application>service" to
                COMPONENT_COMMON + setOf("foregroundServiceType", "isolatedProcess", "externalService", "stopWithTask", "useAppZygote", "visibleToInstantApps"),
            "application>receiver" to COMPONENT_COMMON,
            "application>provider" to
                COMPONENT_COMMON +
                    setOf("authorities", "grantUriPermissions", "readPermission", "writePermission", "multiprocess", "syncable", "initOrder", "forceUriPermissions"),
            "application>uses-library" to setOf("name", "required"),
            "application>uses-native-library" to setOf("name", "required"),
            "application>profileable" to setOf("shell", "enabled"),
            "activity>layout" to setOf("*"),
            "provider>path-permission" to setOf("path", "pathPrefix", "pathPattern", "permission", "readPermission", "writePermission"),
            "provider>grant-uri-permission" to setOf("path", "pathPrefix", "pathPattern"),
            "intent-filter>action" to setOf("name"),
            "intent-filter>category" to setOf("name"),
            "intent-filter>data" to
                setOf(
                    "scheme", "host", "port", "path", "pathPrefix", "pathPattern", "pathSuffix", "pathAdvancedPattern", "mimeType", "ssp",
                    "sspPrefix", "sspPattern",
                ),
        ) + listOf("activity", "activity-alias", "service", "receiver", "provider", "application").flatMap { p ->
            listOf("$p>meta-data" to setOf("name", "value", "resource"), "$p>property" to setOf("name", "value", "resource"))
        } + listOf("activity", "activity-alias", "service", "receiver", "provider").map { p ->
            "$p>intent-filter" to setOf("priority", "autoVerify", "label", "order")
        }

    /** Kept only in manifest.json on purpose: cosmetic, tool-generated or not real platform attributes. */
    val RAW_ONLY: Map<String, Set<String>> =
        mapOf(
            "*>intent-filter" to setOf("icon", "roundIcon", "data-generated"),
            "intent-filter>action" to setOf("exported"),
            "activity>meta-data" to setOf("exported"),
            "application>activity" to setOf("fitsSystemWindows"),
            "application>service" to setOf("configChanges"),
            "manifest>uses-permission" to setOf("protectionLevel"),
            "manifest>permission" to setOf("icon"),
            "manifest>attribution" to setOf("minSdkVersion"),
        )

    /** `parent>tag@attribute` for every attribute in [root] that is neither structured nor raw-only. */
    fun unmapped(root: XmlNode): Set<String> {
        val out = sortedSetOf<String>()

        fun walk(
            n: XmlNode,
            parent: String,
        ) {
            val key = "$parent>${n.tag}"
            val structured = STRUCTURED[key]
            val raw = RAW_ONLY[key].orEmpty() + RAW_ONLY["*>${n.tag}"].orEmpty()
            if (structured == null) {
                out += "$key"
            } else if ("*" !in structured) {
                n.attributes.keys
                    .map { it.removePrefix("android:") }
                    .filter { it !in structured && it !in raw }
                    .forEach { out += "$key@$it" }
            }
            n.children.forEach { walk(it, n.tag) }
        }
        walk(root, "")
        return out
    }
}
