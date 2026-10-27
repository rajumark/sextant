# org.ppsspp.ppsspp 1.19.3 (119030000)
Extracted by sextant <version> · schema 8 · sha256 44e1ecb9268702fb65adcee14cffb6cdc3607e87abcf409004c9bdb6b2543f6c

## App
- Label: PPSSPP
- min SDK 9 · target SDK 35 · compile SDK 35
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: org.ppsspp.ppsspp.PpssppActivity
- Signing: v1, v2, v3 · cert sha256 19:ED:8A:FE:05:20:D5:12:BD:1D:9A:78:01:31:D4:86:ED:B7:E3:84:61:4B:5E:43:AF:9A:D8:C6:88:B2:E1:00
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/as.png bg #2d4553
- Layout: 1 dex · ABIs: arm64-v8a, armeabi-v7a, x86_64

## Code
- 1401 classes · 12394 methods · 8211 fields · 0 Kotlin classes (0%)
- 28533 call edges (35042 call sites) · 2250 distinct string constants
- Biggest packages: androidx.appcompat.widget (201), androidx.core.app (102), androidx.core.view (89), androidx.appcompat.app (86), androidx.fragment.app (76), androidx.annotation (59), androidx.lifecycle (55), org.ppsspp.ppsspp (50)

## Insights
- Frameworks: none detected
- SDKs (0): none detected
- Libraries with exact versions: 21 · built with AGP 8.11.1
- Endpoints: 96 URLs on 37 hosts; top: forums.ppsspp.org (2), media.retroachievements.org (2), metadata.ppsspp.org (2), retroachievements.org (2), store.ppsspp.org (2), twitter.com (2)
- Native: 7 libraries, 123 JNI methods · Locales: 85

## Review findings (static observations to check, not proven issues)
- [low] Components exported without permission (1) — e.g. org.ppsspp.ppsspp.ShortcutActivity
- [low] Profileable from the shell (1) — e.g. application/profileable shell=true
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true

## Counts
- Permissions: 10 requested (6 dangerous), 0 declared
- Components: 2 activity, 0 activity-alias, 0 service, 0 receiver, 0 provider
- Exported: 2 (1 with deep links)
- Features: 1 required, 9 optional · libraries: none
- Queries (package visibility): 0 packages, 0 intents, 0 providers · Metadata: 3 entries

## Exported components
- activity org.ppsspp.ppsspp.PpssppActivity — content://*.*\\..*\\..*\\.CHD, content://*.*\\..*\\..*\\.CSO, content://*.*\\..*\\..*\\.ELF
- activity org.ppsspp.ppsspp.ShortcutActivity

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
