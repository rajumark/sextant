# im.vector.app 1.6.62 (40106624)
Extracted by sextant <version> · schema 8 · sha256 6ce8ee6c3a5a76ab2928f3f3b6e3934c352a3fd6bf76b7ff3dc291c6cbd928de

## App
- Label: Element Classic
- min SDK 21 · target SDK 35 · compile SDK 35
- debuggable: not set · allow_backup: no · cleartext traffic: not set
- Launcher: im.vector.application.features.Alias
- Signing: v1, v2, v3 · cert sha256 B1:5D:9B:67:3B:55:B9:FE:65:61:90:79:4D:87:2E:59:EC:5B:85:B3:BF:B8:A7:6B:94:42:F1:7E:AE:28:87:C9
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/CH.png bg res/6Y.png
- Layout: 8 dex · ABIs: x86_64

## Code
- 66011 classes · 447460 methods · 258973 fields · 0 Kotlin classes (0%)
- 960687 call edges (1242811 call sites) · 69933 distinct string constants
- Obfuscation: none · 0% short class names · source files stripped (100%)
- Biggest packages: com.dropbox.core.v2.teamlog (2178), org.checkerframework.com.google.common.collect (1226), com.google.common.collect (972), com.dropbox.core.v2.team (679), androidx.media3.session (635), com.dropbox.core.v2.files (559), com.dropbox.core.v2.sharing (501), org.matrix.rustcomponents.sdk.crypto (496)

## Insights
- Frameworks: Hermes (JS engine), Jetpack Compose, React Native
- SDKs (26): PostHog, WorkManager, ZXing, Matrix SDK, WebRTC, Sentry, Realm, Room, Dagger / Hilt, Glide, Meta Fresco, Timber, MapLibre, ExoPlayer / Media3, OkHttp, Retrofit, UnifiedPush, Kotlin Coroutines, RxJava, Gson, Jackson, Moshi, Protocol Buffers, kotlinx.serialization, Jetpack Compose, Material Components
- Libraries with exact versions: 78 · built with AGP 8.11.0
- Endpoints: 203 URLs on 135 hosts; top: github.com (14), element.io (10), www.w3.org (7), dashif.org (4), localhost (4), tools.ietf.org (4)
- Native: 21 libraries, 854 JNI methods · Locales: 107

## Review findings (static observations to check, not proven issues)
- [medium] WebView file URL access (4) — e.g. Lcom/reactnativecommunity/webview/RNCWebViewManagerImpl;->createViewInstance(Lcom/facebook/react/uimanager/ThemedReactContext;Lcom/reactnativecommunity/webview/RNCWebView;)Lcom/reactnativecommunity/webview/RNCWebViewWrapper;
- [medium] JavaScript bridge into a WebView (2) — e.g. Lcom/reactnativecommunity/webview/RNCWebView;->createRNCWebViewBridge(Lcom/reactnativecommunity/webview/RNCWebView;)V
- [low] Runs external commands (9) — e.g. Lcom/facebook/react/modules/systeminfo/AndroidInfoHelpers;->getMetroHostPropValue()Ljava/lang/String;
- [low] Components exported without permission (6) — e.g. im.vector.app.features.link.LinkHandlerActivity
- [low] Weak cipher or ECB mode (16) — e.g. Landroidx/biometric/CryptoObjectUtils;->createFakeCryptoObject()Landroidx/biometric/BiometricPrompt$CryptoObject;
- [low] Weak hash algorithm (5) — e.g. Lcom/facebook/common/util/SecureHashUtil;->makeSHA1HashBase64([B)Ljava/lang/String;
- [info] Custom task affinity or task reparenting (1) — e.g. im.vector.app.features.call.VectorCallActivity
- [info] WebView JavaScript enabled (7) — e.g. Lcom/reactnativecommunity/webview/RNCWebViewManagerImpl;->setJavaScriptEnabled(Lcom/reactnativecommunity/webview/RNCWebViewWrapper;Z)V

## Counts
- Permissions: 34 requested (11 dangerous), 1 declared
- Components: 63 activity, 2 activity-alias, 17 service, 17 receiver, 9 provider
- Exported: 11 (3 with deep links)
- Features: 1 required, 3 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 4 intents, 0 providers · Metadata: 18 entries

## Exported components
- activity im.vector.app.features.link.LinkHandlerActivity — https://app.element.io, https://develop.element.io, https://mobile.element.io/
- activity im.vector.app.features.login.SSORedirectRouterActivity — element://connect
- activity im.vector.app.features.share.IncomingShareActivity
- activity-alias im.vector.app.features.permalink.PermalinkHandlerActivity — element://matrix.to, element://room, element://user
- activity-alias im.vector.application.features.Alias
- service androidx.sharetarget.ChooserTargetServiceCompat [requires android.permission.BIND_CHOOSER_TARGET_SERVICE]
- service im.vector.app.features.importer.ImporterService
- service org.jitsi.meet.sdk.ConnectionService [requires android.permission.BIND_TELECOM_CONNECTION_SERVICE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver androidx.work.impl.diagnostics.DiagnosticsReceiver [requires android.permission.DUMP]
- receiver im.vector.app.core.pushers.VectorUnifiedPushMessagingReceiver

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
