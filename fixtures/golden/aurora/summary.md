# com.aurora.store 4.8.4 (76)
Extracted by sextant <version> · schema 8 · sha256 fd9c75d90d0f4a7c132b9b4a5a2cf1992a45e03b8d8ff988b7dcfbc0db2c4d11

## App
- Label: Aurora Store
- min SDK 23 · target SDK 37 · compile SDK 37
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: com.aurora.store.ComposeActivity
- Signing: v1, v2, v3 · cert sha256 5C:83:C7:67:2B:92:99:55:DC:0A:1D:B8:9A:5E:6A:E4:38:9E:2E:AE:7E:C9:39:95:60:41:69:4E:58:15:F5:32
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/CH.png bg #6c63ff
- Layout: 2 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 16451 classes · 99878 methods · 48275 fields · 0 Kotlin classes (0%)
- 234974 call edges (319480 call sites) · 13754 distinct string constants
- Biggest packages: (default) (12255), com.aurora.gplayapi (1260), com.google.protobuf (534), com.aurora.store.compose.navigation (192), androidx.recyclerview.widget (108), com.google.gson.internal.bind (98), com.google.android.material.internal (91), com.aurora.gplayapi.data.models (77)

## Insights
- Frameworks: none detected
- SDKs (8): WorkManager, Room, OkHttp, Kotlin Coroutines, Gson, Protocol Buffers, Jetpack Compose, Material Components
- Libraries with exact versions: 108 · built with AGP 9.3.0
- Endpoints: 75 URLs on 21 hosts; top: android.clients.google.com (27), gitlab.com (7), play.google.com (6), auroraoss.com (4), reports.exodus-privacy.eu.org (4), www.googleapis.com (4)
- Native: 4 libraries, 0 JNI methods · Locales: 91

## Review findings (static observations to check, not proven issues)
- [low] Runs external commands (1) — e.g. Lhq0;->c([Ljava/lang/String;)Lac7;
- [low] Components exported without permission (1) — e.g. com.aurora.store.DeepLinkConfirmActivity
- [low] Weak cipher or ECB mode (1) — e.g. Lzy1;->a()Lkh0$c;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] WebView JavaScript enabled (1) — e.g. Lvf3;->g(Ljava/lang/Object;)Ljava/lang/Object;

## Counts
- Permissions: 21 requested (3 dangerous), 1 declared
- Components: 5 activity, 0 activity-alias, 4 service, 10 receiver, 3 provider
- Exported: 7 (1 with deep links)
- Features: 0 required, 4 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 0 intents, 0 providers · Metadata: 7 entries

## Exported components
- activity com.aurora.store.ComposeActivity
- activity com.aurora.store.DeepLinkConfirmActivity — http://market.android.com, http://play.google.com, https://market.android.com
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver androidx.work.impl.diagnostics.DiagnosticsReceiver [requires android.permission.DUMP]
- receiver com.aurora.store.data.receiver.DeviceOwnerReceiver [requires android.permission.BIND_DEVICE_ADMIN]
- provider rikka.shizuku.ShizukuProvider [requires android.permission.INTERACT_ACROSS_USERS_FULL]

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
