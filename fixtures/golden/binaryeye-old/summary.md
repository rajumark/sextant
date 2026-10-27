# de.markusfisch.android.binaryeye 1.75.3 (178)
Extracted by sextant <version> · schema 7 · sha256 f89d1ceeb5b5ff7cdeca1fa09225075eab31c50067c58ca0ba19606393581daf

## App
- Label: Binary Eye
- min SDK 21 · target SDK 37 · compile SDK 37
- debuggable: not set · allow_backup: yes · cleartext traffic: yes
- Launcher: de.markusfisch.android.binaryeye.activity.SplashActivity
- Signing: v1, v2, v3 · cert sha256 9E:CD:BF:06:77:DD:50:60:9C:76:C2:BA:05:DB:E3:62:23:23:27:B1:0A:1C:37:CD:58:72:29:93:D9:FE:5D:57
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/Qr.xml bg #b6d46f
- Layout: 1 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 2945 classes · 16974 methods · 9481 fields · 0 Kotlin classes (0%)
- 56160 call edges (90377 call sites) · 7500 distinct string constants
- Biggest packages: (default) (2614), androidx.camera.camera2.internal.compat.quirk (48), de.tsenger.vdstools (25), de.tsenger.vdstools.generic (24), androidx.appcompat.widget (19), de.tsenger.vdstools.idb (17), androidx.preference (15), androidx.lifecycle (14)

## Insights
- Frameworks: none detected
- SDKs (1): Material Components
- Libraries with exact versions: 46 · built with AGP 9.3.2
- Endpoints: 24 URLs on 9 hosts; top: markusfisch.de (8), www.w3.org (4), example.com (3), android.googlesource.com (1), github.com (1), issuetracker.google.com (1)
- Native: 12 libraries, 56 JNI methods · Locales: 87

## Review findings (static observations to check, not proven issues)
- [medium] Cleartext HTTP allowed (1) — e.g. application usesCleartextTraffic=true
- [low] Components exported without permission (6) — e.g. de.markusfisch.android.binaryeye.activity.CameraActivity
- [low] Weak hash algorithm (3) — e.g. Lde/markusfisch/android/binaryeye/activity/DecodeActivity;->B(Ljava/lang/String;[B)V
- [info] App data can be backed up (1) — e.g. application allowBackup=true

## Counts
- Permissions: 12 requested (5 dangerous), 1 declared
- Components: 13 activity, 0 activity-alias, 2 service, 1 receiver, 2 provider
- Exported: 9 (2 with deep links)
- Features: 1 required, 1 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 2 intents, 0 providers · Metadata: 7 entries

## Exported components
- activity de.markusfisch.android.binaryeye.activity.CameraActivity — binaryeye://scan, http://markusfisch.de/BinaryEye*, https://markusfisch.de/BinaryEye*
- activity de.markusfisch.android.binaryeye.activity.EncodeActivity — binaryeye://encode, http://markusfisch.de/encode*, https://markusfisch.de/encode*
- activity de.markusfisch.android.binaryeye.activity.HistoryActivity
- activity de.markusfisch.android.binaryeye.activity.PickActivity
- activity de.markusfisch.android.binaryeye.activity.PreferencesActivity
- activity de.markusfisch.android.binaryeye.activity.SplashActivity
- activity de.markusfisch.android.binaryeye.activity.WelcomeActivity
- service de.markusfisch.android.binaryeye.service.ScanTileService [requires android.permission.BIND_QUICK_SETTINGS_TILE]
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
