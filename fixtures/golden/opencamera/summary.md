# net.sourceforge.opencamera 1.56.2 (96)
Extracted by sextant <version> · schema 8 · sha256 ca5672ca8c7174554ca0b07b5b26e505817a05142f571cf2abe67e790c30249c

## App
- Label: Open Camera
- min SDK 23 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: net.sourceforge.opencamera.MainActivity
- Signing: v1, v2, v3 · cert sha256 43:89:91:C9:68:E8:D3:08:CA:34:07:8D:37:51:1C:90:C5:02:D5:96:BA:F9:F0:D0:C5:FB:D1:C9:BF:67:DF:6D
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/as.png bg res/Gx.png
- Layout: 2 dex · ABIs: none

## Code
- 5349 classes · 38646 methods · 21115 fields · 2244 Kotlin classes (41%)
- 100522 call edges (128332 call sites) · 6735 distinct string constants
- Obfuscation: none · 0% short class names
- Biggest packages: kotlinx.coroutines.flow (290), androidx.appcompat.widget (263), net.sourceforge.opencamera (259), androidx.core.app (215), androidx.core.view (201), kotlinx.coroutines (179), androidx.fragment.app (140), kotlinx.coroutines.channels (136)

## Insights
- Frameworks: Kotlin
- SDKs (1): Kotlin Coroutines
- Libraries with exact versions: 36 · built with AGP 8.13.2
- Endpoints: 14 URLs on 14 hosts; top: design.google.com (1), developer.android.com (1), fonts.google.com (1), fsf.org (1), github.com (1), google.github.io (1)
- Native: 0 libraries, 0 JNI methods · Locales: 85

## Review findings (static observations to check, not proven issues)
- [low] Components exported without permission (1) — e.g. net.sourceforge.opencamera.MyWidgetProviderTakePhoto
- [info] App data can be backed up (1) — e.g. application allowBackup=true

## Counts
- Permissions: 9 requested (8 dangerous), 1 declared
- Components: 3 activity, 0 activity-alias, 4 service, 2 receiver, 1 provider
- Exported: 6 (0 with deep links)
- Features: 2 required, 1 optional · libraries: none
- Queries (package visibility): 0 packages, 1 intents, 0 providers · Metadata: 5 entries

## Exported components
- activity net.sourceforge.opencamera.MainActivity
- service net.sourceforge.opencamera.MyTileService [requires android.permission.BIND_QUICK_SETTINGS_TILE]
- service net.sourceforge.opencamera.MyTileServiceFrontCamera [requires android.permission.BIND_QUICK_SETTINGS_TILE]
- service net.sourceforge.opencamera.MyTileServiceVideo [requires android.permission.BIND_QUICK_SETTINGS_TILE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver net.sourceforge.opencamera.MyWidgetProviderTakePhoto

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
