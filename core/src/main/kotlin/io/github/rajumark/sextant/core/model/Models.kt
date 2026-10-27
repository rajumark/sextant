package io.github.rajumark.sextant.core.model

import io.github.rajumark.sextant.core.insights.AppIcons
import kotlinx.serialization.Serializable

// Property order here is the key order in the output files. Keep it stable.
// Property names are written in snake_case (see SextantJson).

@Serializable
data class AppMeta(
    val schemaVersion: Int,
    val tool: ToolInfo,
    val file: FileInfo,
    val `package`: String?,
    val versionName: String?,
    val versionCode: Long?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val compileSdk: Int?,
    val label: String?,
    val applicationClass: String?,
    val flags: AppFlags,
    val signing: Signing,
    val layout: Layout,
    val code: CodeStats?,
    val insights: InsightStats?,
    /** Launcher icons resolved to files (schema 6). */
    val icons: AppIcons? = null,
    val warnings: List<String>,
)

@Serializable
data class ToolInfo(
    val name: String,
    val version: String,
)

@Serializable
data class FileInfo(
    val name: String,
    val sizeBytes: Long,
    val sha256: String,
)

/** `null` means the attribute is absent from the manifest; we never guess. */
@Serializable
data class AppFlags(
    val debuggable: Boolean?,
    val allowBackup: Boolean?,
    val usesCleartextTraffic: Boolean?,
    val networkSecurityConfig: String?,
    val extractNativeLibs: Boolean?,
    // Schema 7: the rest of <application> plus identity attributes of <manifest>. null = not set in the manifest.
    val testOnly: Boolean? = null,
    val fullBackupContent: String? = null,
    val dataExtractionRules: String? = null,
    val backupAgent: String? = null,
    val fullBackupOnly: Boolean? = null,
    val hasFragileUserData: Boolean? = null,
    val requestLegacyExternalStorage: Boolean? = null,
    val preserveLegacyExternalStorage: Boolean? = null,
    val largeHeap: Boolean? = null,
    val hardwareAccelerated: Boolean? = null,
    val supportsRtl: Boolean? = null,
    val resizeableActivity: Boolean? = null,
    val enableOnBackInvokedCallback: Boolean? = null,
    val appComponentFactory: String? = null,
    val localeConfig: String? = null,
    val appCategory: String? = null,
    val isGame: Boolean? = null,
    val memtagMode: String? = null,
    val usesNonSdkApi: Boolean? = null,
    val manageSpaceActivity: String? = null,
    val taskAffinity: String? = null,
    val process: String? = null,
    val theme: String? = null,
    /** `<profileable android:shell="true">`: lets `adb shell` profile a release build. */
    val profileableShell: Boolean? = null,
    val installLocation: String? = null,
    /** Shares a Linux user ID (process and data) with other apps signed by the same key. Deprecated. */
    val sharedUserId: String? = null,
    val sharedUserMaxSdk: Int? = null,
    val targetSandboxVersion: Int? = null,
    /** `<supports-screens>` attributes as written (smallScreens, largeScreens, anyDensity, …). */
    val supportsScreens: Map<String, String> = emptyMap(),
    // Schema 8: the remaining <application> and <manifest> attributes.
    val icon: String? = null,
    val roundIcon: String? = null,
    val banner: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val hasCode: Boolean? = null,
    val intentMatchingFlags: String? = null,
    val backupInForeground: Boolean? = null,
    val restoreAnyVersion: Boolean? = null,
    val killAfterRestore: Boolean? = null,
    val zygotePreloadName: String? = null,
    val windowSoftInputMode: String? = null,
    val configChanges: String? = null,
    val sharedUserLabel: String? = null,
    val compileSdkCodename: String? = null,
    val platformBuildVersionName: String? = null,
    /** `<attribution android:tag>`: data-access audit tags the app declares (Android 11+), with their labels. */
    val attributions: Map<String, String?> = emptyMap(),
)

@Serializable
data class Signing(
    val verified: Boolean,
    val schemes: List<String>,
    val certificates: List<Certificate>,
)

@Serializable
data class Certificate(
    val sha256: String,
    val sha1: String,
    val subject: String,
    val issuer: String,
    val notBefore: String,
    val notAfter: String,
)

@Serializable
data class Layout(
    val dexFiles: Int,
    val nativeAbis: List<String>,
)

@Serializable
data class Permission(
    val name: String,
    val kind: String,
    val maxSdk: Int?,
    val dangerous: Boolean,
    val protectionLevel: String?,
    val custom: Boolean,
    val impliedBy: String?,
    // Schema 8. uses-permission: lowest SDK it applies to, flags (neverForLocation), required.
    val minSdk: Int? = null,
    val usesPermissionFlags: String? = null,
    val required: Boolean? = null,
    // Schema 8. Declared permissions.
    val label: String? = null,
    val description: String? = null,
    val permissionGroup: String? = null,
)

@Serializable
data class Component(
    val type: String,
    val name: String,
    val exported: Boolean,
    val exportedSource: String,
    val enabled: Boolean,
    val permission: String?,
    val process: String?,
    val launcher: Boolean,
    val targetActivity: String?,
    val authorities: List<String>,
    val intentFilters: List<IntentFilter>,
    // Schema 7. Providers: separate read/write permissions, URI grants, per-path permissions.
    val readPermission: String? = null,
    val writePermission: String? = null,
    val grantUriPermissions: Boolean? = null,
    val pathPermissions: List<PathPermission> = emptyList(),
    val grantUriPaths: List<String> = emptyList(),
    // Activities.
    val launchMode: String? = null,
    val taskAffinity: String? = null,
    val excludeFromRecents: Boolean? = null,
    val allowTaskReparenting: Boolean? = null,
    val screenOrientation: String? = null,
    // Schema 8: the rest of <activity>. null = not set in the manifest.
    val theme: String? = null,
    val configChanges: String? = null,
    val windowSoftInputMode: String? = null,
    val documentLaunchMode: String? = null,
    val lockTaskMode: String? = null,
    val parentActivityName: String? = null,
    val noHistory: Boolean? = null,
    val clearTaskOnLaunch: Boolean? = null,
    val finishOnTaskLaunch: Boolean? = null,
    val alwaysRetainTaskState: Boolean? = null,
    val autoRemoveFromRecents: Boolean? = null,
    val maxRecents: Int? = null,
    val resizeableActivity: Boolean? = null,
    val supportsPictureInPicture: Boolean? = null,
    val showWhenLocked: Boolean? = null,
    val turnScreenOn: Boolean? = null,
    /** Can be embedded in another app's task (activity embedding, bubbles). */
    val allowEmbedded: Boolean? = null,
    val hardwareAccelerated: Boolean? = null,
    val stateNotNeeded: Boolean? = null,
    val maxAspectRatio: String? = null,
    val persistableMode: String? = null,
    val relinquishTaskIdentity: Boolean? = null,
    /** Deprecated (API 27) form of showWhenLocked. */
    val showOnLockScreen: Boolean? = null,
    val enableOnBackInvokedCallback: Boolean? = null,
    val banner: String? = null,
    /** `<layout>` child: default and minimum window size and gravity for freeform/multi-window. */
    val windowLayout: Map<String, String> = emptyMap(),
    // Services.
    val foregroundServiceType: String? = null,
    val isolatedProcess: Boolean? = null,
    // Schema 8. Services: runs in the caller's isolated process; stops when the user removes the task.
    val externalService: Boolean? = null,
    val stopWithTask: Boolean? = null,
    val useAppZygote: Boolean? = null,
    val visibleToInstantApps: Boolean? = null,
    val description: String? = null,
    // Schema 8. Providers.
    val multiprocess: Boolean? = null,
    val syncable: Boolean? = null,
    val initOrder: Int? = null,
    val forceUriPermissions: Boolean? = null,
    // Any component.
    val directBootAware: Boolean? = null,
    val label: String? = null,
    val icon: String? = null,
    val roundIcon: String? = null,
)

/** `<path-permission>` inside a provider: permissions for paths matching the pattern. */
@Serializable
data class PathPermission(
    val path: String?,
    val pathPrefix: String?,
    val pathPattern: String?,
    val permission: String?,
    val readPermission: String?,
    val writePermission: String?,
)

@Serializable
data class IntentFilter(
    val actions: List<String>,
    val categories: List<String>,
    val data: List<IntentData>,
    val priority: Int,
    val autoVerify: Boolean,
    val label: String? = null,
    val order: Int? = null,
)

@Serializable
data class IntentData(
    val scheme: String?,
    val host: String?,
    val port: String?,
    val path: String?,
    val pathPrefix: String?,
    val pathPattern: String?,
    val pathSuffix: String?,
    val pathAdvancedPattern: String?,
    val mimeType: String?,
    val ssp: String? = null,
    val sspPrefix: String? = null,
    val sspPattern: String? = null,
)

/** `uses-feature`, `uses-library` or `uses-native-library`. */
@Serializable
data class FeatureRecord(
    /** feature | library | native-library */
    val kind: String,
    /** Feature or library name; for an OpenGL ES requirement, `glEsVersion`. */
    val name: String,
    /** null when the manifest does not say (features and libraries default to required). */
    val required: Boolean?,
    /** OpenGL ES version (`0x00030000` → `3.0`) or feature version. */
    val version: String? = null,
)

/** `meta-data` or `property`, on the application or on one component. */
@Serializable
data class MetadataRecord(
    /** application, or the component's full class name */
    val scope: String,
    /** meta-data | property */
    val kind: String,
    val name: String,
    /** android:value, with resource references resolved when possible */
    val value: String?,
    /** android:resource (an XML or other resource reference) */
    val resource: String?,
)

/** One entry of `<queries>` (Android 11 package visibility). */
@Serializable
data class QueryRecord(
    /** package | intent | provider */
    val kind: String,
    /** package name, provider authority, or a short description of the intent */
    val name: String,
    val actions: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val data: List<IntentData> = emptyList(),
)

/** Generic decoded XML element, used for manifest.json. */
@Serializable
data class XmlNode(
    val tag: String,
    val attributes: Map<String, String>,
    val children: List<XmlNode>,
)
