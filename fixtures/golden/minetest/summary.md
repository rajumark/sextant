# net.minetest.minetest 5.17.0 (5170000)
Extracted by sextant <version> · schema 8 · sha256 87a136d408ed560d3d56e55ca2ac780deb7a8941635b16960c88831fb33f9233

## App
- Label: Luanti
- min SDK 21 · target SDK 35 · compile SDK 34
- debuggable: not set · allow_backup: no · cleartext traffic: not set
- Launcher: net.minetest.minetest.MainActivity
- Signing: v1, v2, v3 · cert sha256 E7:D6:04:7C:1D:75:71:98:11:B3:17:96:4A:4A:B1:C0:AD:3B:00:3B:C3:B8:B3:D7:29:25:B4:D6:05:CA:45:EE
- Icon: res/GX.png (96×96) · @mipmap/ic_launcher
- Layout: 1 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 4193 classes · 30998 methods · 12358 fields · 1941 Kotlin classes (46%)
- 79887 call edges (97127 call sites) · 4378 distinct string constants
- Obfuscation: none · 0% short class names
- Biggest packages: kotlinx.coroutines.flow (290), androidx.appcompat.widget (246), androidx.core.view (198), kotlinx.coroutines (179), androidx.core.app (152), androidx.fragment.app (142), kotlinx.coroutines.channels (136), kotlin.collections (134)

## Insights
- Frameworks: Kotlin, SDL
- SDKs (1): Kotlin Coroutines
- Libraries with exact versions: 30 · built with AGP 8.13.2
- Endpoints: 7 URLs on 7 hosts; top: android.googlesource.com (1), bford.info (1), content.luanti.org (1), curl.se (1), docs.luanti.org (1), schemas.android.com (1)
- Native: 8 libraries, 212 JNI methods · Locales: 91

## Review findings (static observations to check, not proven issues)
- [low] Components exported without permission (1) — e.g. net.minetest.minetest.GameActivity
- [info] Exported provider grants URI permissions for any path (1) — e.g. net.minetest.minetest.LuantiDocumentsProvider

## Counts
- Permissions: 3 requested (1 dangerous), 1 declared
- Components: 2 activity, 0 activity-alias, 1 service, 0 receiver, 3 provider
- Exported: 3 (0 with deep links)
- Features: 1 required, 0 optional · libraries: none
- Queries (package visibility): 0 packages, 0 intents, 0 providers · Metadata: 4 entries

## Exported components
- activity net.minetest.minetest.GameActivity
- activity net.minetest.minetest.MainActivity
- provider net.minetest.minetest.LuantiDocumentsProvider [requires android.permission.MANAGE_DOCUMENTS]

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
