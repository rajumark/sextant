# com.logseq.app 2.0.2 (89)
Extracted by sextant <version> · schema 7 · sha256 e011983cdea2946711dd6ff35b00cff3321cd4f8937fee3f42e718f3aa6226e0

## App
- Label: Logseq
- min SDK 24 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: com.logseq.app.MainActivity
- Signing: v2, v3 · cert sha256 E2:EA:95:DF:B0:1D:FD:B8:CD:DC:8A:63:B4:1D:97:66:E5:3D:68:B2:CE:85:1E:C7:A7:E5:FD:F3:D1:66:D2:35
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/as.png bg res/Gx.png
- Layout: 3 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 29869 classes · 151676 methods · 75716 fields · 22503 Kotlin classes (75%)
- 545097 call edges (1036668 call sites) · 30373 distinct string constants
- Biggest packages: androidx.compose.material.icons.filled (2132), androidx.compose.material.icons.outlined (2132), androidx.compose.material.icons.rounded (2132), androidx.compose.material.icons.sharp (2132), androidx.compose.material.icons.twotone (2132), androidx.compose.material3 (1426), androidx.compose.foundation.layout (389), androidx.compose.ui.platform (329)

## Insights
- Frameworks: Capacitor, Cordova, Jetpack Compose, Kotlin
- SDKs (5): Kotlin Coroutines, Gson, kotlinx.serialization, Jetpack Compose, Material Components
- Libraries with exact versions: 76 · built with AGP 8.13.0
- Endpoints: 69 URLs on 63 hosts; top: api.logseq.io (2), capacitorjs.com (2), clojure.org (2), issuetracker.google.com (2), schemas.android.com (2), www.w3.org (2)
- Native: 12 libraries, 28 JNI methods · Locales: 86

## Review findings (static observations to check, not proven issues)
- [high] TrustManager may accept every certificate (1) — e.g. Lcom/jcesarmobile/sslskip/SslSkipPlugin$1;->checkServerTrusted([Ljava/security/cert/X509Certificate;Ljava/lang/String;)V
- [medium] HostnameVerifier may accept every host (1) — e.g. Lcom/jcesarmobile/sslskip/SslSkipPlugin$2;->verify(Ljava/lang/String;Ljavax/net/ssl/SSLSession;)Z
- [medium] JavaScript bridge into a WebView (5) — e.g. Lcom/getcapacitor/MessageHandler;-><init>(Lcom/getcapacitor/Bridge;Landroid/webkit/WebView;Lorg/apache/cordova/PluginManager;)V
- [low] Runs external commands (1) — e.g. Lorg/apache/commons/io/FileSystemUtils;->openProcess([Ljava/lang/String;)Ljava/lang/Process;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] WebView JavaScript enabled (2) — e.g. Lcom/getcapacitor/Bridge;->initWebView()V

## Counts
- Permissions: 12 requested (6 dangerous), 1 declared
- Components: 4 activity, 0 activity-alias, 2 service, 1 receiver, 3 provider
- Exported: 2 (1 with deep links)
- Features: 0 required, 0 optional · libraries: none
- Queries (package visibility): 0 packages, 3 intents, 0 providers · Metadata: 6 entries

## Exported components
- activity com.logseq.app.MainActivity — logseq:
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]

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
