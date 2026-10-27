package io.github.rajumark.sextant.desktop.data

import io.github.rajumark.sextant.desktop.model.Analysis
import io.github.rajumark.sextant.desktop.model.ApkLayout
import io.github.rajumark.sextant.desktop.model.AppComponent
import io.github.rajumark.sextant.desktop.model.AppFlags
import io.github.rajumark.sextant.desktop.model.AppMeta
import io.github.rajumark.sextant.desktop.model.Certificate
import io.github.rajumark.sextant.desktop.model.CodeSummary
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.model.DataSpec
import io.github.rajumark.sextant.desktop.model.ExportedSource
import io.github.rajumark.sextant.desktop.model.FileInfo
import io.github.rajumark.sextant.desktop.model.IntentFilterSpec
import io.github.rajumark.sextant.desktop.model.Permission
import io.github.rajumark.sextant.desktop.model.PermissionKind
import io.github.rajumark.sextant.desktop.model.Signing
import kotlin.random.Random

// Fake analyses for UI tests and screenshots. The apps are fictional; numbers are generated from a
// fixed seed so every run (and every screenshot test) shows the same data.

object SampleData {
    /** The dangerous permissions the sample apps use (the real flag comes from core). */
    private val DANGEROUS_SAMPLE =
        setOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.NEARBY_WIFI_DEVICES",
            "android.permission.BODY_SENSORS",
        )

    private data class Spec(
        val fileName: String,
        val label: String,
        val pkg: String,
        val versionName: String,
        val versionCode: Long,
        val minSdk: Int,
        val targetSdk: Int,
        val sizeBytes: Long,
        val seed: Int,
        val permissionCount: Int,
        val extraComponents: Int,
        val domain: String,
        val scheme: String,
        val signer: String,
        val schemes: List<String>,
        val dex: Int,
        val abis: List<String>,
        val flags: AppFlags,
        val ageMinutes: Long,
        val durationMs: Long,
        val warnings: List<String> = emptyList(),
    )

    private val specs =
        listOf(
            Spec(
                fileName = "northwind-notes-4.12.0.apk",
                label = "Northwind Notes",
                pkg = "com.northwind.notes",
                versionName = "4.12.0",
                versionCode = 41200,
                minSdk = 24,
                targetSdk = 35,
                sizeBytes = 48_612_331,
                seed = 7,
                permissionCount = 24,
                extraComponents = 58,
                domain = "notes.northwind.example",
                scheme = "northwind",
                signer = "CN=Northwind Labs, O=Northwind, L=Portland, C=US",
                schemes = listOf("v2", "v3"),
                dex = 4,
                abis = listOf("arm64-v8a", "armeabi-v7a", "x86_64"),
                flags = AppFlags(false, false, null, "@xml/network_security_config", false),
                ageMinutes = 2,
                durationMs = 640,
            ),
            Spec(
                fileName = "orbit-chat-7.0.4.apk",
                label = "Orbit Chat",
                pkg = "app.orbit.chat",
                versionName = "7.0.4",
                versionCode = 70004,
                minSdk = 26,
                targetSdk = 35,
                sizeBytes = 112_804_120,
                seed = 21,
                permissionCount = 33,
                extraComponents = 121,
                domain = "orbit.chat.example",
                scheme = "orbit",
                signer = "CN=Orbit Messaging, O=Orbit, C=DE",
                schemes = listOf("v2", "v3", "v4"),
                dex = 9,
                abis = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"),
                flags = AppFlags(false, false, false, "@xml/network_security_config", true),
                ageMinutes = 26 * 60,
                durationMs = 1_480,
            ),
            Spec(
                fileName = "pebble-weather-2.3.1.apk",
                label = "Pebble Weather",
                pkg = "io.pebble.weather",
                versionName = "2.3.1",
                versionCode = 2031,
                minSdk = 23,
                targetSdk = 34,
                sizeBytes = 21_330_904,
                seed = 3,
                permissionCount = 17,
                extraComponents = 24,
                domain = "pebble.weather.example",
                scheme = "pebble",
                signer = "CN=Pebble Apps, O=Pebble, C=GB",
                schemes = listOf("v1", "v2"),
                dex = 2,
                abis = listOf("arm64-v8a"),
                flags = AppFlags(false, true, true, null, null),
                ageMinutes = 3 * 24 * 60,
                durationMs = 310,
            ),
            Spec(
                fileName = "lumen-camera-1.0.0-beta3.apk",
                label = "Lumen Camera",
                pkg = "dev.lumen.camera",
                versionName = "1.0.0-beta3",
                versionCode = 100,
                minSdk = 29,
                targetSdk = 34,
                sizeBytes = 36_018_447,
                seed = 42,
                permissionCount = 14,
                extraComponents = 9,
                domain = "lumen.camera.example",
                scheme = "lumen",
                signer = "CN=Android Debug, O=Android, C=US",
                schemes = listOf("v2"),
                dex = 1,
                abis = listOf("arm64-v8a", "x86_64"),
                flags = AppFlags(true, true, null, null, false),
                ageMinutes = 8 * 24 * 60,
                durationMs = 270,
                warnings =
                    listOf(
                        "application/@android:label: resource 0x7f1300a2 could not be resolved; kept the raw reference",
                        "activity dev.lumen.camera.debug.Inspector: duplicate attribute android:exported ignored",
                    ),
            ),
        )

    val recent: List<Analysis> by lazy { specs.map(::build) }

    /** The analysis shown when "opening" an APK from the home screen. */
    val sample: Analysis get() = recent.first()

    private const val P = "android.permission."

    private val requestedPool =
        listOf(
            "${P}INTERNET",
            "${P}ACCESS_NETWORK_STATE",
            "${P}POST_NOTIFICATIONS",
            "${P}WAKE_LOCK",
            "${P}RECEIVE_BOOT_COMPLETED",
            "${P}FOREGROUND_SERVICE",
            "${P}CAMERA",
            "${P}VIBRATE",
            "${P}READ_MEDIA_IMAGES",
            "${P}RECORD_AUDIO",
            "${P}USE_BIOMETRIC",
            "${P}ACCESS_WIFI_STATE",
            "com.google.android.c2dm.permission.RECEIVE",
            "${P}READ_CONTACTS",
            "${P}ACCESS_FINE_LOCATION",
            "${P}ACCESS_COARSE_LOCATION",
            "${P}FOREGROUND_SERVICE_DATA_SYNC",
            "${P}READ_MEDIA_VIDEO",
            "${P}USE_FULL_SCREEN_INTENT",
            "${P}BLUETOOTH_CONNECT",
            "${P}SCHEDULE_EXACT_ALARM",
            "${P}MODIFY_AUDIO_SETTINGS",
            "${P}READ_PHONE_STATE",
            "${P}CHANGE_NETWORK_STATE",
            "com.google.android.gms.permission.AD_ID",
            "${P}NFC",
            "${P}CALL_PHONE",
            "${P}WRITE_CONTACTS",
            "${P}READ_CALENDAR",
            "${P}SYSTEM_ALERT_WINDOW",
            "${P}REQUEST_INSTALL_PACKAGES",
            "${P}NEARBY_WIFI_DEVICES",
            "${P}BODY_SENSORS",
        )

    private val features =
        listOf(
            "settings",
            "editor",
            "search",
            "account",
            "onboarding",
            "billing",
            "media",
            "notifications",
            "widgets",
            "backup",
            "sharing",
            "labs",
            "profile",
            "sync",
            "export",
            "reminders",
            "tags",
            "trash",
        )

    private fun build(s: Spec): Analysis {
        val rnd = Random(s.seed)
        val permissions = buildPermissions(s)
        val components = buildComponents(s, rnd)
        val certificates =
            listOf(
                Certificate(
                    sha256 = fingerprint(rnd, 32),
                    sha1 = fingerprint(rnd, 20),
                    subject = s.signer,
                    issuer = s.signer,
                    notBefore = "2019-03-14T09:21:07Z",
                    notAfter = if (s.signer.startsWith("CN=Android Debug")) "2054-06-02T11:40:12Z" else "2049-03-08T09:21:07Z",
                ),
            )
        val meta =
            AppMeta(
                schemaVersion = 1,
                toolVersion = "0.1.0",
                file = FileInfo(s.fileName, s.sizeBytes, hex(rnd, 32)),
                packageName = s.pkg,
                versionName = s.versionName,
                versionCode = s.versionCode,
                minSdk = s.minSdk,
                targetSdk = s.targetSdk,
                compileSdk = s.targetSdk,
                label = s.label,
                applicationClass = "${s.pkg}.${s.label.replace(" ", "")}Application",
                flags = s.flags,
                signing = Signing(verified = true, schemes = s.schemes, certificates = certificates),
                layout = ApkLayout(s.dex, s.abis),
                code =
                    CodeSummary(
                        classes = 1_800 + s.extraComponents * 97,
                        methods = 15_000 + s.extraComponents * 806,
                        fields = 5_000 + s.extraComponents * 300,
                        callEdges = 40_000 + s.extraComponents * 2_400,
                        kotlinClasses = 600 + s.extraComponents * 40,
                    ),
                warnings = s.warnings,
            )
        val manifest = ManifestBuilder.build(meta, permissions, components)
        val outDir = "~/Downloads/" + s.fileName.removeSuffix(".apk") + ".sextant/"
        val partial =
            Analysis(
                meta = meta,
                permissions = permissions.map { it.copy(jsonLine = SampleOutput.permissionLine(it)) },
                components = components.map { it.copy(jsonLine = SampleOutput.componentLine(it)) },
                manifest = manifest,
                outputDir = outDir,
                apkPath = "/samples/${s.fileName}",
                outputFiles = emptyList(),
                analyzedAtMillis = System.currentTimeMillis() - s.ageMinutes * 60_000,
                durationMs = s.durationMs,
            )
        return partial.copy(outputFiles = SampleOutput.files(partial))
    }

    private fun buildPermissions(s: Spec): List<Permission> {
        val requested =
            requestedPool.take(s.permissionCount).map { name ->
                Permission(
                    name = name,
                    kind =
                        if (name.endsWith(
                                "ACCESS_BACKGROUND_LOCATION",
                            )
                        ) {
                            PermissionKind.UsesPermissionSdk23
                        } else {
                            PermissionKind.UsesPermission
                        },
                    maxSdk = if (name.endsWith("READ_EXTERNAL_STORAGE")) 32 else null,
                    dangerous = name in DANGEROUS_SAMPLE,
                    protectionLevel = null,
                    custom = !name.startsWith("android.permission."),
                    impliedBy = null,
                    jsonLine = "",
                )
            } + Permission("${P}READ_EXTERNAL_STORAGE", PermissionKind.UsesPermission, 32, true, null, false, null, "")
        val declared =
            listOf(
                Permission(
                    "${s.pkg}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                    PermissionKind.Declared,
                    null,
                    false,
                    "signature",
                    true,
                    null,
                    "",
                ),
                Permission("${s.pkg}.permission.READ_SUGGESTIONS", PermissionKind.Declared, null, false, "signature", true, null, ""),
                Permission("${s.pkg}.permission.SYNC", PermissionKind.Declared, null, false, "signature|privileged", true, null, ""),
            )
        return (requested + declared).sortedWith(compareBy({ it.kind.ordinal }, { it.name }))
    }

    private fun buildComponents(
        s: Spec,
        rnd: Random,
    ): List<AppComponent> {
        val pkg = s.pkg
        val main = IntentFilterSpec(listOf("android.intent.action.MAIN"), listOf("android.intent.category.LAUNCHER"), emptyList())
        val core =
            listOf(
                component(ComponentType.Activity, "$pkg.ui.LaunchActivity", exported = true, launcher = true, filters = listOf(main)),
                component(ComponentType.Activity, "$pkg.ui.MainActivity", exported = false),
                component(
                    ComponentType.Activity,
                    "$pkg.deeplink.DeepLinkActivity",
                    exported = true,
                    source = ExportedSource.Default,
                    filters =
                        listOf(
                            IntentFilterSpec(
                                listOf("android.intent.action.VIEW"),
                                listOf("android.intent.category.BROWSABLE", "android.intent.category.DEFAULT"),
                                listOf(
                                    DataSpec(scheme = "https", host = s.domain, pathPrefix = "/open"),
                                    DataSpec(scheme = "https", host = s.domain, pathPrefix = "/invite"),
                                ),
                                autoVerify = true,
                            ),
                            IntentFilterSpec(
                                listOf("android.intent.action.VIEW"),
                                listOf("android.intent.category.BROWSABLE", "android.intent.category.DEFAULT"),
                                listOf(DataSpec(scheme = s.scheme, host = "share")),
                            ),
                        ),
                ),
                component(
                    ComponentType.Activity,
                    "$pkg.share.ShareTargetActivity",
                    exported = true,
                    filters =
                        listOf(
                            IntentFilterSpec(
                                listOf("android.intent.action.SEND", "android.intent.action.SEND_MULTIPLE"),
                                listOf("android.intent.category.DEFAULT"),
                                listOf(DataSpec(mimeType = "text/plain"), DataSpec(mimeType = "image/*")),
                            ),
                        ),
                ),
                component(
                    ComponentType.ActivityAlias,
                    "$pkg.LauncherAliasMono",
                    exported = true,
                    enabled = false,
                    launcher = true,
                    target = "$pkg.ui.LaunchActivity",
                    filters = listOf(main),
                ),
                component(
                    ComponentType.Service,
                    "$pkg.sync.SyncService",
                    exported = false,
                    permission = "$pkg.permission.SYNC",
                    process = ":sync",
                ),
                component(
                    ComponentType.Service,
                    "$pkg.push.PushMessagingService",
                    exported = false,
                    filters =
                        listOf(
                            IntentFilterSpec(listOf("com.google.firebase.MESSAGING_EVENT"), emptyList(), emptyList(), priority = -500),
                        ),
                ),
                component(
                    ComponentType.Service,
                    "androidx.work.impl.background.systemjob.SystemJobService",
                    exported = true,
                    permission = "android.permission.BIND_JOB_SERVICE",
                ),
                component(
                    ComponentType.Receiver,
                    "$pkg.boot.BootReceiver",
                    exported = true,
                    filters =
                        listOf(
                            IntentFilterSpec(
                                listOf("android.intent.action.BOOT_COMPLETED", "android.intent.action.MY_PACKAGE_REPLACED"),
                                emptyList(),
                                emptyList(),
                            ),
                        ),
                ),
                component(
                    ComponentType.Receiver,
                    "androidx.profileinstaller.ProfileInstallReceiver",
                    exported = true,
                    permission = "android.permission.DUMP",
                    filters =
                        listOf(
                            IntentFilterSpec(listOf("androidx.profileinstaller.action.INSTALL_PROFILE"), emptyList(), emptyList()),
                        ),
                ),
                component(
                    ComponentType.Receiver,
                    "$pkg.widgets.QuickNoteWidget",
                    exported = true,
                    source = ExportedSource.Default,
                    filters = listOf(IntentFilterSpec(listOf("android.appwidget.action.APPWIDGET_UPDATE"), emptyList(), emptyList())),
                ),
                component(
                    ComponentType.Provider,
                    "$pkg.files.AttachmentProvider",
                    exported = false,
                    authorities = listOf("$pkg.attachments"),
                ),
                component(
                    ComponentType.Provider,
                    "androidx.startup.InitializationProvider",
                    exported = false,
                    authorities = listOf("$pkg.androidx-startup"),
                ),
                component(
                    ComponentType.Provider,
                    "$pkg.search.SuggestionProvider",
                    exported = true,
                    permission = "$pkg.permission.READ_SUGGESTIONS",
                    authorities = listOf("$pkg.suggestions"),
                ),
            )

        val filler =
            (0 until s.extraComponents).map { i ->
                val feature = features[i % features.size]
                val cap = feature.replaceFirstChar { it.uppercase() }
                val round = i / features.size
                val suffix = if (round == 0) "" else (round + 1).toString()
                when (rnd.nextInt(10)) {
                    in 0..4 ->
                        component(
                            ComponentType.Activity,
                            "$pkg.feature.$feature.${cap}Activity$suffix",
                            exported =
                                rnd.nextInt(8) == 0,
                        )
                    in 5..6 -> component(ComponentType.Service, "$pkg.feature.$feature.${cap}Service$suffix", exported = false)
                    in 7..8 ->
                        component(
                            ComponentType.Receiver,
                            "$pkg.feature.$feature.${cap}Receiver$suffix",
                            exported = false,
                            permission = if (rnd.nextBoolean()) "$pkg.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" else null,
                        )
                    else ->
                        component(
                            ComponentType.Provider,
                            "$pkg.feature.$feature.${cap}Provider$suffix",
                            exported = false,
                            authorities = listOf("$pkg.$feature$suffix"),
                        )
                }
            }
        return (core + filler).distinctBy { it.name }.sortedWith(compareBy({ it.type.ordinal }, { it.name }))
    }

    private fun component(
        type: ComponentType,
        name: String,
        exported: Boolean,
        source: ExportedSource = ExportedSource.Explicit,
        enabled: Boolean = true,
        permission: String? = null,
        process: String? = null,
        launcher: Boolean = false,
        target: String? = null,
        authorities: List<String> = emptyList(),
        filters: List<IntentFilterSpec> = emptyList(),
    ) = AppComponent(type, name, exported, source, enabled, permission, process, launcher, target, authorities, filters, "")

    private fun hex(
        rnd: Random,
        bytes: Int,
    ): String = (0 until bytes).joinToString("") { rnd.nextInt(256).toString(16).padStart(2, '0') }

    private fun fingerprint(
        rnd: Random,
        bytes: Int,
    ): String =
        (0 until bytes).joinToString(":") {
            rnd
                .nextInt(256)
                .toString(16)
                .padStart(2, '0')
                .uppercase()
        }
}
