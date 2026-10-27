# io.homeassistant.companion.android.minimal 2026.8.4-minimal (24228)
Extracted by sextant <version> · schema 7 · sha256 c3212821efeb998e21c1f0874992f6805a81a6f85ed03519fd3b89ec67690fe4

## App
- Label: Home Assistant
- min SDK 23 · target SDK 37 · compile SDK 37
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: io.homeassistant.companion.android.launch.LaunchActivity
- Signing: v1, v2, v3 · cert sha256 17:48:52:50:A0:3A:0F:2B:3F:29:2A:05:4F:59:5A:9E:79:4B:EE:F8:0C:F9:10:F7:B3:BB:B8:09:8A:BF:6D:50
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/Qr1.xml bg #18bcf2
- Layout: 9 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 60825 classes · 361663 methods · 176930 fields · 32000 Kotlin classes (52%)
- 1013403 call edges (1667048 call sites) · 70283 distinct string constants
- Biggest packages: androidx.compose.material.icons.filled (2132), androidx.compose.material.icons.outlined (2132), androidx.compose.material.icons.rounded (2132), androidx.compose.material.icons.sharp (2132), androidx.compose.material.icons.twotone (2132), androidx.compose.material3 (1951), com.google.common.collect (959), androidx.compose.material (894)

## Insights
- Frameworks: Jetpack Compose, Kotlin
- SDKs (15): WorkManager, ZXing, Room, Dagger / Hilt, Coil, Timber, ExoPlayer / Media3, Cronet, OkHttp, Retrofit, Kotlin Coroutines, Protocol Buffers, kotlinx.serialization, Jetpack Compose, Material Components
- Libraries with exact versions: 123 · built with AGP 9.3.1
- Endpoints: 197 URLs on 94 hosts; top: companion.home-assistant.io (67), github.com (10), www.home-assistant.io (9), www.w3.org (5), my.home-assistant.io (4), developer.android.com (3)
- Native: 12 libraries, 0 JNI methods · Locales: 95

## Review findings (static observations to check, not proven issues)
- [medium] JavaScript bridge into a WebView (1) — e.g. Lio/homeassistant/companion/android/frontend/js/FrontendJsBridge;->registerV1(Landroid/webkit/WebView;)V
- [low] Runs external commands (1) — e.g. Lio/homeassistant/companion/android/util/LogcatReader$readLog$2;->invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
- [low] Components exported without permission (22) — e.g. androidx.compose.ui.tooling.PreviewActivity
- [low] Weak cipher or ECB mode (2) — e.g. Landroidx/biometric/CryptoObjectUtils;->createFakeCryptoObject()Landroidx/biometric/BiometricPrompt$CryptoObject;
- [low] Weak hash algorithm (2) — e.g. Lorg/chromium/net/X509Util;->hashPrincipal(Ljavax/security/auth/x500/X500Principal;)Ljava/lang/String;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Custom task affinity or task reparenting (4) — e.g. androidx.glance.appwidget.action.InvisibleActionTrampolineActivity
- [info] WebView JavaScript enabled (1) — e.g. Lio/homeassistant/companion/android/util/compose/webview/HAWebViewKt;->defaultSettings$lambda$0(Landroid/webkit/WebSettings;)Lkotlin/Unit;

## Counts
- Permissions: 71 requested (40 dangerous), 1 declared
- Components: 27 activity, 5 activity-alias, 57 service, 23 receiver, 2 provider
- Exported: 48 (4 with deep links)
- Features: 0 required, 15 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 0 intents, 1 providers · Metadata: 23 entries

## Exported components
- activity androidx.compose.ui.tooling.PreviewActivity
- activity androidx.media3.session.BluetoothValidationActivity [requires android.permission.BLUETOOTH_PRIVILEGED] — content://media/internal/audio/media/*
- activity io.homeassistant.companion.android.assist.AssistActivity
- activity io.homeassistant.companion.android.launch.LaunchActivity
- activity io.homeassistant.companion.android.launch.link.LinkActivity — homeassistant://invite, homeassistant://navigate, https://my.home-assistant.io/invite/*
- activity io.homeassistant.companion.android.nfc.TagReaderActivity — https://www.home-assistant.io/tag/*
- activity io.homeassistant.companion.android.qs.TilePreferenceActivity [requires android.permission.BIND_QUICK_SETTINGS_TILE]
- activity io.homeassistant.companion.android.settings.sensor.HealthConnectPermissionActivity
- activity io.homeassistant.companion.android.settings.wear.SettingsWearActivity — homeassistant://wear-phone-signin
- activity io.homeassistant.companion.android.share.ShareActivity
- activity io.homeassistant.companion.android.widgets.assist.AssistShortcutActivity
- activity io.homeassistant.companion.android.widgets.button.ButtonWidgetConfigureActivity
- activity io.homeassistant.companion.android.widgets.camera.CameraWidgetConfigureActivity
- activity io.homeassistant.companion.android.widgets.common.WidgetAuthenticationActivity
- activity io.homeassistant.companion.android.widgets.entity.EntityWidgetConfigureActivity
- activity io.homeassistant.companion.android.widgets.mediaplayer.MediaPlayerControlsWidgetConfigureActivity
- activity io.homeassistant.companion.android.widgets.template.TemplateWidgetConfigureActivity
- activity io.homeassistant.companion.android.widgets.todo.TodoWidgetConfigureActivity
- activity-alias io.homeassistant.companion.android.ViewHealthConnectPermissionUsageActivity [requires android.permission.START_VIEW_PERMISSION_USAGE]
- activity-alias io.homeassistant.companion.android.assist.VoiceCommandIntentActivity
- activity-alias io.homeassistant.companion.android.nfc.TagScannedActivity [requires android.permission.DISPATCH_NFC_MESSAGE]
- service androidx.glance.appwidget.GlanceRemoteViewsService [requires android.permission.BIND_REMOTEVIEWS]
- service androidx.health.platform.client.impl.sdkservice.HealthDataSdkService
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- service io.homeassistant.companion.android.assist.service.AssistRecognitionService [requires android.permission.BIND_VOICE_INTERACTION]
- … 23 more in components.jsonl

## Files
- meta.json — identity, signing, flags, layout, warnings
- manifest.json — full decoded manifest tree
- permissions.jsonl — one permission per line
- components.jsonl — one component per line (grep '"exported":true')
- features.jsonl — uses-feature / uses-library / uses-native-library
- metadata.jsonl — meta-data and property entries (application and components)
- queries.jsonl — package visibility: packages, intents, providers the app can see
- code/classes.jsonl — one class per line (id, super, interfaces, source file, Kotlin)
- code/methods.jsonl — one method per line (id like Lcom/a/B;->m(I)V)
- code/fields.jsonl — one field per line, with constant initial values
- code/calls.jsonl — caller → callee edges with bytecode offsets
- code/strings.jsonl — string constants and the methods that load them
- code/field_refs.jsonl, code/type_refs.jsonl — field reads/writes, type uses (new, cast, instanceof)
- insights/frameworks.jsonl, sdks.jsonl, libraries.jsonl — what the app is built with, with evidence
- insights/endpoints.jsonl — every URL, with the methods/resources it comes from
- insights/findings.jsonl — review findings with evidence (manifest, code, secrets)
- resources/values.jsonl — default strings, bools, integers, colors, dimens
- native/libraries.jsonl, native/symbols.jsonl — .so files and exported symbols (JNI mapped to Java)
- index.sqlite — everything above, queryable:
  - `sextant find <dir> <text>` substring search over names, strings, components, permissions, deep links
  - `sextant class <dir> <name>` one class: hierarchy, fields, methods, callers, strings
  - `sextant xref <dir> <class|field>` where it is used; `sextant reach <dir> <from> <to>` call paths
  - `sextant sql <dir> "<query>"` read-only SQL; `sextant sql <dir> --schema` lists tables and views
