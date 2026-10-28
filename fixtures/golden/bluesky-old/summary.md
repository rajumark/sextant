# xyz.blueskyweb.app 1.135.0 (1635)
Extracted by sextant <version> · schema 8 · sha256 55f8774f2b89046ccc1a7aa3147a4ed9ad3f3eb4c22e11ae8eb17945b1951ae7

## App
- Label: Bluesky
- min SDK 24 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: xyz.blueskyweb.app.MainActivity
- Signing: v2, v3 · cert sha256 40:05:8E:C6:8A:35:55:21:D0:8D:F2:49:98:CF:99:A7:14:91:33:5C:65:D7:58:85:03:9E:80:C0:25:AA:DD:FF
- Icon: res/mipmap-xxxhdpi-v4/ic_launcher.webp (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/mipmap-anydpi-v26/ic_launcher.xml fg res/mipmap-xxxhdpi-v4/ic_launcher_foreground.webp bg #006aff
- Layout: 4 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 38359 classes · 209124 methods · 98390 fields · 1888 Kotlin classes (4%)
- 524901 call edges (746049 call sites) · 44379 distinct string constants
- Obfuscation: heavy · 68% short class names · source files stripped (100%)
- Biggest packages: n1 (1204), sg (739), ja (611), ia (599), fa (498), ea (427), io.sentry (385), ka (361)

## Insights
- Frameworks: Expo, Hermes (JS engine), Jetpack Compose, Kotlin, React Native
- SDKs (17): Google Sign-In, Sentry, Bouncy Castle, Room, Glide, Meta Fresco, Google ML Kit, ExoPlayer / Media3, OkHttp, Firebase Core, Google Play Core / In-App Updates, Google Play Services Base, Firebase Cloud Messaging, Kotlin Coroutines, Gson, Jetpack Compose, Material Components
- Libraries with exact versions: 134 · built with AGP 8.12.0
- Endpoints: 151 URLs on 122 hosts; top: github.com (6), www.w3.org (6), issuetracker.google.com (5), dashif.org (4), developer.android.com (4), docs.sentry.io (3)
- Native: 152 libraries, 372 JNI methods · Locales: 90

## Review findings (static observations to check, not proven issues)
- [medium] WebView file URL access (3) — e.g. Lcom/reactnativecommunity/webview/k;->e(Lcom/facebook/react/uimanager/X;Lcom/reactnativecommunity/webview/d;)Lcom/reactnativecommunity/webview/p;
- [medium] JavaScript bridge into a WebView (2) — e.g. Lcom/reactnativecommunity/webview/d;->d(Lcom/reactnativecommunity/webview/d;)V
- [low] Runs external commands (2) — e.g. Lcom/facebook/react/modules/systeminfo/a;->g()Ljava/lang/String;
- [low] Weak cipher or ECB mode (1) — e.g. Landroidx/media3/datasource/cache/f$b;-><init>(Ljava/io/File;[BZ)V
- [low] Weak hash algorithm (9) — e.g. LAf/a;->b(Ljava/util/UUID;Ljava/lang/String;)Ljava/util/UUID;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Google API key (2) — e.g. @string/google_api_key
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] Custom task affinity or task reparenting (1) — e.g. expo.modules.webbrowser.BrowserProxyActivity
- [info] WebView JavaScript enabled (2) — e.g. Lcom/reactnativecommunity/webview/k;->K(Lcom/reactnativecommunity/webview/p;Z)V

## Counts
- Permissions: 41 requested (14 dangerous), 1 declared
- Components: 11 activity, 11 activity-alias, 10 service, 4 receiver, 12 provider
- Exported: 3 (1 with deep links)
- Features: 0 required, 0 optional · libraries: androidx.camera.extensions.impl, androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 19 intents, 0 providers · Metadata: 49 entries

## Exported components
- activity xyz.blueskyweb.app.MainActivity — bluesky:, exp+bluesky:, https://bsky.app
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver com.google.firebase.iid.FirebaseInstanceIdReceiver [requires com.google.android.c2dm.permission.SEND]

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
