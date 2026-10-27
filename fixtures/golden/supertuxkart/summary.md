# org.supertuxkart.stk 1.4 (312)
Extracted by sextant <version> · schema 8 · sha256 b6469d2888970b0c87df0c9eb472ea7d12f331f57f7623c6769986209e830a2f

## App
- Label: SuperTuxKart
- min SDK 16 · target SDK 30 · compile SDK 33
- debuggable: not set · allow_backup: not set · cleartext traffic: not set
- Launcher: org.supertuxkart.stk.SuperTuxKartActivity
- Signing: v1, v2, v3 · cert sha256 E9:14:7D:44:6F:91:33:A2:BE:90:0B:30:D0:A7:FB:EC:5F:70:2D:EF:FF:F3:DE:07:93:3B:6B:A9:0A:2E:C0:F7
- Icon: res/rR.png (256×256) · @drawable/icon · adaptive res/P8.xml fg res/3E.png bg res/X0.png
- Layout: 1 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 246 classes · 1488 methods · 871 fields · 0 Kotlin classes (0%)
- 4118 call edges (5755 call sites) · 664 distinct string constants
- Biggest packages: org.libsdl.app (45), org.minidns.record (35), org.minidns (23), org.minidns.dnssec (21), org.supertuxkart.stk (18), org.minidns.dnslabel (12), org.minidns.iterative (12), org.minidns.util (12)

## Insights
- Frameworks: SDL
- SDKs (0): none detected
- Libraries with exact versions: 0 · built with AGP 7.0.0
- Endpoints: 57 URLs on 52 hosts; top: commons.wikimedia.org (2), freesound.org (2), opengameart.org (2), www.freesound.org (2), www.publicdomainpictures.net (2), ambientcg.com (1)
- Native: 8 libraries, 200 JNI methods · Locales: 58

## Review findings (static observations to check, not proven issues)
- [low] Runs external commands (1) — e.g. Lorg/minidns/dnsserverlookup/AndroidUsingExec;->getDnsServerAddresses()Ljava/util/List;

## Counts
- Permissions: 5 requested (2 dangerous), 0 declared
- Components: 1 activity, 0 activity-alias, 0 service, 0 receiver, 0 provider
- Exported: 1 (0 with deep links)
- Features: 1 required, 7 optional · libraries: none
- Queries (package visibility): 0 packages, 0 intents, 0 providers · Metadata: 0 entries

## Exported components
- activity org.supertuxkart.stk.SuperTuxKartActivity

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
