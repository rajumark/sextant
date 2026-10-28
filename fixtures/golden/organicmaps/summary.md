# app.organicmaps 2026.09.29-32-FDroid (26092932)
Extracted by sextant <version> · schema 8 · sha256 654a736d43317084f13560281fe7013f869800610e9d6036b6789d9c9e07f8bc

## App
- Label: Organic Maps
- min SDK 21 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: app.organicmaps.DownloadResourcesActivity
- Signing: v1, v2, v3 · cert sha256 E2:16:FC:38:6C:32:7E:C8:8D:05:60:26:F1:DE:A2:7B:E2:BC:3F:9D:7E:E9:56:E4:36:1C:4B:64:EC:45:9F:E9
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/as.png bg #006c35
- Layout: 2 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 7143 classes · 43764 methods · 20931 fields · 0 Kotlin classes (0%)
- 108501 call edges (138561 call sites) · 8564 distinct string constants
- Obfuscation: light · 6% short class names
- Biggest packages: j$.util.stream (268), androidx.appcompat.widget (185), androidx.car.app.model (170), androidx.core.view (156), androidx.recyclerview.widget (151), androidx.fragment.app (140), app.organicmaps.bookmarks (122), androidx.car.app (113)

## Insights
- Frameworks: none detected
- SDKs (7): Google Sign-In, WorkManager, Room, OkHttp, Google Play Services Base, Kotlin Coroutines, Material Components
- Libraries with exact versions: 68 · built with AGP 9.2.1
- Endpoints: 343 URLs on 80 hosts; top: www.openstreetmap.org (7), omaps.app (3), organicmaps.app (3), * (2), ge0.me (2), github.com (2)
- Native: 4 libraries, 1768 JNI methods · Locales: 57

## Review findings (static observations to check, not proven issues)
- [low] Runs external commands (1) — e.g. Lapp/organicmaps/sdk/util/log/ZipLogsTask;->saveSystemLogcat(Ljava/lang/String;)V
- [low] Components exported without permission (3) — e.g. app.organicmaps.SplashActivity
- [low] Weak hash algorithm (1) — e.g. Lorg/microg/gms/common/MultiConnectionKeeper;->sha1sum([B)Ljava/lang/String;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] WebView JavaScript enabled (3) — e.g. Lapp/organicmaps/WebContainerDelegate;->initWebView(Ljava/lang/String;)V

## Counts
- Permissions: 16 requested (4 dangerous), 1 declared
- Components: 19 activity, 2 activity-alias, 9 service, 10 receiver, 2 provider
- Exported: 6 (1 with deep links)
- Features: 1 required, 4 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 3 intents, 1 providers · Metadata: 13 entries

## Exported components
- activity app.organicmaps.SplashActivity — content://*, content://*/.*\\..*\\..*\\..*\\..*\\..*\\..*\\..*\\.GPX, content://*/.*\\..*\\..*\\..*\\..*\\..*\\..*\\..*\\.KMB
- activity-alias app.organicmaps.DownloadResourcesActivity
- activity-alias app.organicmaps.ManageSpaceActivity
- service app.organicmaps.car.AndroidAutoService
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
