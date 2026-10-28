# com.standardnotes 3.202.10 (3005235)
Extracted by sextant <version> · schema 8 · sha256 782f6e65ee3496e269b18cca2564de95c916f31a498d5160425dd9233e63bd8d

## App
- Label: Standard Notes
- min SDK 28 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: com.standardnotes.MainActivity
- Signing: v3 · cert sha256 F5:DE:E5:E6:53:81:21:14:6A:40:50:BD:4F:4C:F6:EE:2D:DC:4D:69:0A:DD:B9:45:44:E9:A3:B2:0C:4C:B5:A7
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/as.png bg res/Gx.png
- Layout: 2 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 14433 classes · 99002 methods · 40632 fields · 4679 Kotlin classes (32%)
- 228786 call edges (285327 call sites) · 17820 distinct string constants
- Obfuscation: none · 0% short class names
- Biggest packages: com.google.common.collect (950), androidx.datastore.preferences.protobuf (360), com.google.common.util.concurrent (313), kotlinx.coroutines.flow (296), androidx.appcompat.widget (247), androidx.core.app (214), androidx.room (210), androidx.core.view (200)

## Insights
- Frameworks: Hermes (JS engine), Kotlin, React Native
- SDKs (5): WorkManager, Room, Meta Fresco, OkHttp, Kotlin Coroutines
- Libraries with exact versions: 49 · built with AGP 8.8.0
- Endpoints: 85 URLs on 71 hosts; top: www.slf4j.org (9), issuetracker.google.com (3), app.standardnotes.com (2), fb.me (2), play.google.com (2), www.facebook.com (2)
- Native: 52 libraries, 16 JNI methods · Locales: 86

## Review findings (static observations to check, not proven issues)
- [high] TrustManager may accept every certificate (1) — e.g. Lde/undercouch/gradle/tasks/download/internal/InsecureTrustManager;->checkServerTrusted([Ljava/security/cert/X509Certificate;Ljava/lang/String;)V
- [medium] HostnameVerifier may accept every host (2) — e.g. Lde/undercouch/gradle/tasks/download/internal/InsecureHostnameVerifier;->verify(Ljava/lang/String;Ljavax/net/ssl/SSLSession;)Z
- [medium] WebView file URL access (2) — e.g. Lcom/reactnativecommunity/webview/RNCWebViewManagerImpl;->createViewInstance(Lcom/facebook/react/uimanager/ThemedReactContext;Lcom/reactnativecommunity/webview/RNCWebView;)Lcom/reactnativecommunity/webview/RNCWebViewWrapper;
- [low] Runs external commands (1) — e.g. Lcom/facebook/react/modules/systeminfo/AndroidInfoHelpers;->getMetroHostPropValue()Ljava/lang/String;
- [low] Components exported without permission (2) — e.g. app.notifee.core.NotificationReceiverActivity
- [low] Weak cipher or ECB mode (6) — e.g. Landroidx/biometric/CryptoObjectUtils;->createFakeCryptoObject()Landroidx/biometric/BiometricPrompt$CryptoObject;
- [low] Weak hash algorithm (6) — e.g. Lcom/facebook/common/util/SecureHashUtil;->makeSHA1HashBase64([B)Ljava/lang/String;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] WebView JavaScript enabled (1) — e.g. Lcom/reactnativecommunity/webview/RNCWebViewManagerImpl;->setJavaScriptEnabled(Lcom/reactnativecommunity/webview/RNCWebViewWrapper;Z)V

## Counts
- Permissions: 19 requested (4 dangerous), 1 declared
- Components: 2 activity, 0 activity-alias, 6 service, 13 receiver, 5 provider
- Exported: 6 (0 with deep links)
- Features: 0 required, 1 optional · libraries: none
- Queries (package visibility): 0 packages, 3 intents, 0 providers · Metadata: 9 entries

## Exported components
- activity app.notifee.core.NotificationReceiverActivity
- activity com.standardnotes.MainActivity
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver androidx.work.impl.diagnostics.DiagnosticsReceiver [requires android.permission.DUMP]
- receiver app.notifee.core.AlarmPermissionBroadcastReceiver

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
