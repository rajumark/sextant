# org.billthefarmer.tuner 1.58 (158)
Extracted by sextant <version> · schema 7 · sha256 7fbfaee87f3402f7c4b269ac42a36a2ff2c91b382655647efad23d631caff944

## App
- Label: Tuner
- min SDK 23 · target SDK 35 · compile SDK 35
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: org.billthefarmer.tuner.Tuner
- Signing: v1, v2, v3 · cert sha256 F5:51:94:7F:71:F1:27:69:FE:27:DB:76:D3:9F:91:9A:B3:E9:34:77:CF:EE:4A:0A:AC:80:D0:FD:9E:60:6F:22
- Icon: res/5p.png (192×192 xxxhdpi) · @drawable/ic_launcher
- Layout: 1 dex · ABIs: none

## Code
- 53 classes · 192 methods · 518 fields · 0 Kotlin classes (0%)
- 851 call edges (1525 call sites) · 76 distinct string constants
- Biggest packages: org.billthefarmer.tuner (53)

## Insights
- Frameworks: none detected
- SDKs (0): none detected
- Libraries with exact versions: 0 · built with AGP 8.13.2
- Endpoints: 0 URLs on 0 hosts; top: 
- Native: 0 libraries, 0 JNI methods · Locales: 9

## Review findings (static observations to check, not proven issues)
- [info] App data can be backed up (1) — e.g. application allowBackup=true

## Counts
- Permissions: 2 requested (2 dangerous), 0 declared
- Components: 3 activity, 0 activity-alias, 0 service, 0 receiver, 0 provider
- Exported: 1 (0 with deep links)

## Exported components
- activity org.billthefarmer.tuner.Tuner

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
