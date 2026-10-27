# org.jitsi.meet 26.0.0 (26000004)
Extracted by sextant <version> · schema 8 · sha256 72aab4912b08431a2340fc1fb1f181be3a07e8b7ac607e0bb9e4c3814c2e4248

## App
- Label: Jitsi Meet
- min SDK 26 · target SDK 35 · compile SDK 35
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: org.jitsi.meet.MainActivity
- Signing: v2, v3 · cert sha256 41:60:53:93:B4:A0:87:FE:B2:5D:13:E9:1F:42:1C:7D:1A:1A:91:46:25:1F:C2:BE:99:2F:C1:B3:0B:31:81:F6
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/as.png bg #66a8dd
- Layout: 2 dex · ABIs: x86_64

## Code
- 9686 classes · 61470 methods · 28190 fields · 0 Kotlin classes (0%)
- 151318 call edges (198921 call sites) · 15733 distinct string constants
- Biggest packages: org.webrtc (430), androidx.appcompat.widget (201), com.facebook.react.uimanager (201), androidx.core.view (191), com.facebook.react.bridge (175), com.horcrux.svg (164), com.facebook.react.devsupport (155), com.facebook.imagepipeline.producers (138)

## Insights
- Frameworks: Hermes (JS engine), React Native
- SDKs (9): WebRTC, Meta Fresco, Timber, ExoPlayer / Media3, OkHttp, Kotlin Coroutines, Gson, Jackson, Material Components
- Libraries with exact versions: 44 · built with AGP 8.6.0
- Endpoints: 82 URLs on 72 hosts; top: dashif.org (4), github.com (3), www.w3.org (3), developer.android.com (2), jitsi.org (2), schemas.android.com (2)
- Native: 13 libraries, 194 JNI methods · Locales: 87

## Review findings (static observations to check, not proven issues)
- [medium] WebView file URL access (2) — e.g. Lcom/reactnativecommunity/webview/l;->e(Lcom/facebook/react/uimanager/A0;Lcom/reactnativecommunity/webview/f;)Lcom/reactnativecommunity/webview/q;
- [medium] JavaScript bridge into a WebView (1) — e.g. Lcom/reactnativecommunity/webview/f;->d(Lcom/reactnativecommunity/webview/f;)V
- [low] Runs external commands (1) — e.g. Lcom/facebook/react/modules/systeminfo/a;->f()Ljava/lang/String;
- [low] Weak cipher or ECB mode (1) — e.g. Landroidx/media3/datasource/cache/f$b;-><init>(Ljava/io/File;[BZ)V
- [low] Weak hash algorithm (2) — e.g. Lcom/facebook/soloader/x;->b(Ljava/lang/String;)Ljava/lang/String;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] WebView JavaScript enabled (1) — e.g. Lcom/reactnativecommunity/webview/l;->L(Lcom/reactnativecommunity/webview/q;Z)V

## Counts
- Permissions: 17 requested (5 dangerous), 1 declared
- Components: 2 activity, 0 activity-alias, 3 service, 1 receiver, 3 provider
- Exported: 3 (1 with deep links)
- Features: 1 required, 2 optional · libraries: none
- Queries (package visibility): 0 packages, 0 intents, 0 providers · Metadata: 8 entries

## Exported components
- activity org.jitsi.meet.MainActivity — https://alpha.jitsi.net, https://beta.meet.jit.si, https://meet.jit.si
- service org.jitsi.meet.sdk.ConnectionService [requires android.permission.BIND_TELECOM_CONNECTION_SERVICE]
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
