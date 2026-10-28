# org.tuxpaint 0.9.35 (9350)
Extracted by sextant <version> · schema 8 · sha256 0834a371444b62be6d7c776a43994f7ab603367eb7d5b55d2847b2de7896ce3f

## App
- Label: Tux Paint
- min SDK 21 · target SDK 34 · compile SDK 34
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: org.tuxpaint.ConfigActivity, org.tuxpaint.tuxpaintActivity
- Signing: v1, v2, v3 · cert sha256 94:0A:39:91:9D:74:3B:56:56:0E:3B:38:04:6D:F2:2F:24:8F:2D:3D:FA:F3:95:35:87:60:A2:36:8C:65:FC:04
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/as.png bg #ffffff
- Layout: 1 dex · ABIs: arm64-v8a

## Code
- 4216 classes · 31026 methods · 12608 fields · 1941 Kotlin classes (46%)
- 80084 call edges (97770 call sites) · 4382 distinct string constants
- Obfuscation: none · 0% short class names
- Biggest packages: kotlinx.coroutines.flow (290), androidx.appcompat.widget (246), androidx.core.view (198), kotlinx.coroutines (179), androidx.core.app (152), androidx.fragment.app (142), kotlinx.coroutines.channels (136), kotlin.collections (134)

## Insights
- Frameworks: Kotlin, SDL
- SDKs (1): Kotlin Coroutines
- Libraries with exact versions: 31 · built with AGP 8.2.1
- Endpoints: 27 URLs on 27 hosts; top: android.googlesource.com (1), culmus.sourceforge.net (1), freedesktop.org (1), ftp.berlios.de (1), ftp.gnu.org (1), ftp.io.com (1)
- Native: 98 libraries, 54 JNI methods · Locales: 89

## Review findings (static observations to check, not proven issues)
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true

## Counts
- Permissions: 6 requested (4 dangerous), 1 declared
- Components: 5 activity, 0 activity-alias, 0 service, 0 receiver, 1 provider
- Exported: 2 (0 with deep links)
- Features: 1 required, 0 optional · libraries: none
- Queries (package visibility): 0 packages, 0 intents, 0 providers · Metadata: 2 entries

## Exported components
- activity org.tuxpaint.ConfigActivity
- activity org.tuxpaint.tuxpaintActivity

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
