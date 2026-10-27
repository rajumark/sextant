# dev.imranr.obtainium.fdroid 1.7.1 (23593)
Extracted by sextant <version> · schema 7 · sha256 78f330b9e23c94bd3d5958bd707606821af9ad332face5d90e9d9e5d1fe0cd07

## App
- Label: Obtainium
- min SDK 26 · target SDK 36 · compile SDK 37
- debuggable: not set · allow_backup: not set · cleartext traffic: yes
- Launcher: dev.imranr.obtainium.MainActivity
- Signing: v2, v3 · cert sha256 B3:53:60:1F:6A:1D:5F:D6:60:3A:E2:F5:0B:E8:0C:F3:01:36:7B:86:B6:AB:8B:1F:66:24:3D:A9:6C:D5:73:62
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/Qr.xml bg #ffffff
- Layout: 1 dex · ABIs: arm64-v8a

## Code
- 2029 classes · 8673 methods · 6032 fields · 0 Kotlin classes (0%)
- 34129 call edges (59210 call sites) · 5928 distinct string constants
- Biggest packages: (default) (1759), org.chromium.support_lib_boundary (58), dev.re7gog.shizuku_apk_installer (19), com.dexterous.flutterlocalnotifications.models (17), androidx.preference (14), android.support.v4.media (11), com.github.dart_lang.jni (10), androidx.media (9)

## Insights
- Frameworks: Flutter
- SDKs (3): WorkManager, Room, Material Components
- Libraries with exact versions: 67 · built with AGP 9.2.1
- Endpoints: 36 URLs on 28 hosts; top: github.com (4), developer.android.com (3), docs.flutter.dev (2), issuetracker.google.com (2), www.w3.org (2), android.googlesource.com (1)
- Native: 4 libraries, 10 JNI methods · Locales: 85

## Review findings (static observations to check, not proven issues)
- [medium] Cleartext HTTP allowed (1) — e.g. application usesCleartextTraffic=true
- [medium] JavaScript bridge into a WebView (1) — e.g. Lx40;->g(Ljava/lang/Object;Lx0;)V
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] WebView JavaScript enabled (2) — e.g. Lio/flutter/plugins/urllauncher/WebViewActivity;->onCreate(Landroid/os/Bundle;)V

## Counts
- Permissions: 18 requested (3 dangerous), 1 declared
- Components: 2 activity, 0 activity-alias, 3 service, 6 receiver, 4 provider
- Exported: 5 (1 with deep links)
- Features: 0 required, 3 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 3 intents, 1 providers · Metadata: 11 entries

## Exported components
- activity dev.imranr.obtainium.MainActivity — obtainium:
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver androidx.work.impl.diagnostics.DiagnosticsReceiver [requires android.permission.DUMP]
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
