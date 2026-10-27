# com.duckduckgo.mobile.android 5.297.1 (52971000)
Extracted by sextant <version> · schema 8 · sha256 b1c0cb010b6f188b301c202905333486df180ee321ee802f143de0e4f29bdbd0

## App
- Label: DuckDuckGo
- min SDK 28 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: no · cleartext traffic: not set
- Launcher: com.duckduckgo.app.launch.Launcher
- Signing: v3 · cert sha256 F0:70:7D:02:1C:0B:F3:E6:F8:DC:C1:1B:A3:F5:70:04:25:D5:97:DC:A3:01:A3:1E:21:05:56:93:4E:BB:1B:0A
- Icon: res/6e.xml · @mipmap/ic_launcher_red · adaptive res/6e.xml fg res/7E.xml bg #f05f2b
- Layout: 10 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 77871 classes · 455333 methods · 218138 fields · 49092 Kotlin classes (63%)
- 1109018 call edges (1558111 call sites) · 61441 distinct string constants
- Biggest packages: metro.hints (2607), androidx.compose.material3 (1946), com.duckduckgo.app.browser (1017), com.google.common.collect (1011), androidx.datastore.preferences.protobuf (523), com.duckduckgo.app.di (479), androidx.compose.foundation.layout (472), com.duckduckgo.sync.impl (455)

## Insights
- Frameworks: Jetpack Compose, Kotlin
- SDKs (18): WorkManager, ZXing, Tink, Room, SQLCipher, Dagger / Hilt, Coil, Glide, OkHttp, Retrofit, Kotlin Coroutines, RxJava, Gson, Moshi, kotlinx.serialization, Jetpack Compose, Lottie, Material Components
- Libraries with exact versions: 112 · built with AGP 8.13.2
- Endpoints: 950 URLs on 845 hosts; top: duckduckgo.com (58), www.rfc-editor.org (14), github.com (7), tools.ietf.org (7), www.w3.org (5), help.duckduckgo.com (4)
- Native: 44 libraries, 220 JNI methods · Locales: 88

## Review findings (static observations to check, not proven issues)
- [medium] JavaScript bridge into a WebView (16) — e.g. Lcom/duckduckgo/adblocking/impl/duckplayer/DuckPlayerScriptsJsMessaging;->register(Landroid/webkit/WebView;Lcom/duckduckgo/js/messaging/api/JsMessageCallback;)V
- [low] Runs external commands (1) — e.g. Lcom/duckduckgo/networkprotection/impl/connectionclass/LatencyMeasurerImpl;->measureLatency(Ljava/lang/String;)I
- [low] Components exported without permission (10) — e.g. androidx.compose.ui.tooling.PreviewActivity
- [low] Weak cipher or ECB mode (5) — e.g. Landroidx/biometric/CryptoObjectUtils;->createFakeCryptoObject()Landroidx/biometric/BiometricPrompt$CryptoObject;
- [low] Weak hash algorithm (2) — e.g. Lcom/airbnb/lottie/network/NetworkCache;->getMD5(Ljava/lang/String;I)Ljava/lang/String;
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] WebView JavaScript enabled (16) — e.g. Lcom/duckduckgo/app/browser/BrowserTabFragment;->configureWebView()V

## Counts
- Permissions: 23 requested (8 dangerous), 1 declared
- Components: 100 activity, 9 activity-alias, 20 service, 22 receiver, 2 provider
- Exported: 16 (1 with deep links)
- Features: 0 required, 7 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 2 intents, 0 providers · Metadata: 19 entries

## Exported components
- activity androidx.compose.ui.tooling.PreviewActivity
- activity com.duckduckgo.app.SelectedTextSearchActivity
- activity com.duckduckgo.app.browser.BrowserActivity
- activity com.duckduckgo.app.dispatchers.IntentDispatcherActivity — duck:, http:, https:
- activity com.duckduckgo.app.launch.LaunchBridgeActivity
- activity com.duckduckgo.app.settings.SettingsActivity
- activity com.duckduckgo.app.systemsearch.SystemSearchActivity
- activity com.duckduckgo.newtabpage.impl.settings.NewTabSettingsActivity
- activity com.duckduckgo.widget.DuckAiPinShortcutActivity
- activity-alias com.duckduckgo.app.launch.Launcher
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- service com.duckduckgo.customtabs.impl.service.DuckDuckGoCustomTabService
- service com.duckduckgo.mobile.android.vpn.service.DeviceShieldTileService [requires android.permission.BIND_QUICK_SETTINGS_TILE]
- service com.duckduckgo.networkprotection.impl.quickaccess.VpnTileService [requires android.permission.BIND_QUICK_SETTINGS_TILE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver androidx.work.impl.diagnostics.DiagnosticsReceiver [requires android.permission.DUMP]

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
