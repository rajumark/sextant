# com.shatteredpixel.shatteredpixeldungeon 4.0.2 (922)
Extracted by sextant <version> · schema 8 · sha256 ceef4cdd4beddcb3d85379ac004cb4ab5d918b89dc7366b4ce9c4a3585a5e78e

## App
- Label: Shattered Pixel Dungeon
- min SDK 21 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: com.shatteredpixel.shatteredpixeldungeon.android.AndroidLauncher
- Signing: v1, v2, v3 · cert sha256 D4:5F:76:0B:89:60:8B:4B:25:3A:3B:16:0D:C2:70:7F:1F:F4:E8:DD:8C:A4:D2:E8:E1:B8:1C:2C:72:D8:17:53
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/as.png bg res/Gx.png
- Layout: 1 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 3215 classes · 17014 methods · 8956 fields · 0 Kotlin classes (0%)
- 56311 call edges (85229 call sites) · 6201 distinct string constants
- Obfuscation: light · 10% short class names · 324 in the default package · source files stripped (100%)
- Biggest packages: (default) (324), com.shatteredpixel.shatteredpixeldungeon.windows (281), com.shatteredpixel.shatteredpixeldungeon.actors.mobs (194), com.shatteredpixel.shatteredpixeldungeon.sprites (157), com.shatteredpixel.shatteredpixeldungeon.ui (149), com.shatteredpixel.shatteredpixeldungeon.scenes (140), com.shatteredpixel.shatteredpixeldungeon.actors.buffs (130), com.shatteredpixel.shatteredpixeldungeon.levels (87)

## Insights
- Frameworks: libGDX
- SDKs (0): none detected
- Libraries with exact versions: 9 · built with AGP 9.2.0
- Endpoints: 15 URLs on 13 hosts; top: shatteredpixel.com (3), android.googlesource.com (1), api.github.com (1), celesti-whispers.itch.io (1), explore.transifex.com (1), github.com (1)
- Native: 8 libraries, 508 JNI methods · Locales: 0

## Review findings (static observations to check, not proven issues)
- [info] App data can be backed up (1) — e.g. application allowBackup=true

## Counts
- Permissions: 3 requested (0 dangerous), 1 declared
- Components: 2 activity, 0 activity-alias, 0 service, 1 receiver, 1 provider
- Exported: 2 (0 with deep links)
- Features: 1 required, 0 optional · libraries: none
- Queries (package visibility): 0 packages, 0 intents, 0 providers · Metadata: 1 entries

## Exported components
- activity com.shatteredpixel.shatteredpixeldungeon.android.AndroidLauncher
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
