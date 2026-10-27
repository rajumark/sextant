# org.mozilla.fennec_fdroid 158.0.2 (1580220)
Extracted by sextant <version> · schema 7 · sha256 dc2b6d74e575cf4ca2b1b21f7c97e16704284931a179c3c9314dcd6ae0829200

## App
- Label: Fennec
- min SDK 26 · target SDK 37 · compile SDK 37
- debuggable: not set · allow_backup: no · cleartext traffic: yes
- Launcher: org.mozilla.fennec_fdroid.App
- sharedUserId: org.mozilla.fennec_fdroid.sharedID
- Signing: v2, v3 · cert sha256 06:66:53:58:EF:D8:BA:05:BE:23:6A:47:A1:2C:B0:95:8D:7D:75:DD:93:9D:77:C2:B3:1F:53:98:53:7E:BD:C5
- Icon: res/sK.webp (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/5c.webp bg #ff20123a
- Layout: 3 dex · ABIs: arm64-v8a

## Code
- 39888 classes · 149542 methods · 107094 fields · 0 Kotlin classes (0%)
- 429740 call edges (600943 call sites) · 33110 distinct string constants
- Biggest packages: org.mozilla.fenix.GleanMetrics (1079), org.mozilla.geckoview (787), org.mozilla.fenix.nimbus (579), androidx.compose.material3 (565), androidx.media3.session (476), org.mozilla.fenix.bookmarks (471), mozilla.telemetry.glean.internal (437), org.mozilla.fenix.browser (363)

## Insights
- Frameworks: Jetpack Compose
- SDKs (20): Google Mobile Ads (AdMob), Mozilla Glean, Google Sign-In, WorkManager, ZXing, Mozilla GeckoView, WebRTC, Sentry, Tink, Room, ExoPlayer / Media3, Google Play Core / In-App Updates, Google Play Services Base, UnifiedPush, Kotlin Coroutines, Protocol Buffers, kotlinx.serialization, Jetpack Compose, Lottie, Material Components
- Libraries with exact versions: 118 · built with AGP 9.4.0
- Endpoints: 2856 URLs on 2756 hosts; top: www.mozilla.org (8), www.w3.org (8), addons.mozilla.org (5), identity.mozilla.com (5), safebrowsing.googleapis.com (5), www.google.com (5)
- Native: 15 libraries, 84 JNI methods · Locales: 155

## Review findings (static observations to check, not proven issues)
- [medium] Cleartext HTTP allowed (1) — e.g. application usesCleartextTraffic=true
- [low] Runs external commands (2) — e.g. Lcom/sun/jna/NativeLibrary;->getLinuxLdPaths()Ljava/util/ArrayList;
- [low] Components exported without permission (12) — e.g. mozilla.components.feature.pwa.WebAppLauncherActivity
- [low] Profileable from the shell (1) — e.g. application/profileable shell=true
- [low] Shares a user ID with other apps (1) — e.g. manifest sharedUserId=org.mozilla.fennec_fdroid.sharedID
- [low] Weak cipher or ECB mode (2) — e.g. Landroidx/biometric/BiometricPrompt;->authenticate(Landroidx/biometric/BiometricPrompt$PromptInfo;)V
- [low] Weak hash algorithm (4) — e.g. Lcom/airbnb/lottie/network/NetworkCache;->filenameForUrl(Ljava/lang/String;Lcom/airbnb/lottie/network/FileExtension;Z)Ljava/lang/String;

## Counts
- Permissions: 33 requested (9 dangerous), 3 declared
- Components: 21 activity, 26 activity-alias, 106 service, 14 receiver, 8 provider
- Exported: 20 (3 with deep links)
- Features: 1 required, 8 optional · libraries: android.car, androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 5 intents, 0 providers · Metadata: 77 entries

## Exported components
- activity androidx.media3.session.BluetoothValidationActivity [requires android.permission.BLUETOOTH_PRIVILEGED] — content://media/internal/audio/media/*
- activity mozilla.components.feature.pwa.WebAppLauncherActivity
- activity mozilla.telemetry.glean.debug.GleanDebugActivity
- activity org.mozilla.fenix.HomeActivity — fenix://enable_private_browsing, fenix://home, fenix://home_collections
- activity org.mozilla.fenix.IntentReceiverActivity — content:, fenix://settings_wallpapers, http:
- activity-alias org.mozilla.fennec_fdroid.App
- activity-alias org.mozilla.gecko.BrowserApp
- activity-alias org.mozilla.gecko.LauncherActivity
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- service org.mozilla.fenix.autofill.AutofillService [requires android.permission.BIND_AUTOFILL_SERVICE]
- service org.mozilla.fenix.customtabs.CustomTabsService
- service org.unifiedpush.android.connector.internal.RaiseToForegroundService
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver androidx.work.impl.diagnostics.DiagnosticsReceiver [requires android.permission.DUMP]
- receiver com.adjust.sdk.AdjustPreinstallReferrerReceiver
- receiver org.ironfoxoss.unifiedpush.PushReceiver
- receiver org.mozilla.fenix.experiments.QANimbusToolingReceiver [requires android.permission.DUMP]
- receiver org.mozilla.fenix.messaging.QAMessageNotificationWorkerReceiver [requires android.permission.DUMP]
- receiver org.mozilla.gecko.search.SearchWidgetProvider
- receiver org.unifiedpush.android.connector.internal.MessagingReceiverImpl

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
