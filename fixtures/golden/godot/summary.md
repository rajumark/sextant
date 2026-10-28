# org.godotengine.editor.v4 4.7.2.stable (407025003)
Extracted by sextant <version> · schema 8 · sha256 1458a57a3fae0ed94e9d756024ed14edc560a00235aab1ad8400454ba9f53f9b

## App
- Label: Godot Engine 4 (release)
- min SDK 24 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: no · cleartext traffic: not set
- Launcher: org.godotengine.editor.ProjectManager
- Signing: v2, v3 · cert sha256 A8:80:B9:17:E0:6F:45:42:7D:72:A1:16:00:80:6C:46:A2:B2:DC:30:E8:C3:B2:FE:10:66:8D:62:31:DB:4C:8E
- Icon: res/mipmap-anydpi-v26/themed_icon.xml · @mipmap/themed_icon · adaptive res/mipmap-anydpi-v26/themed_icon.xml fg res/mipmap-xxxhdpi-v4/icon_foreground.webp bg res/mipmap-xxxhdpi-v4/icon_background.webp
- Layout: 2 dex · ABIs: arm64-v8a

## Code
- 9761 classes · 64740 methods · 32550 fields · 2604 Kotlin classes (26%)
- 174057 call edges (249850 call sites) · 15836 distinct string constants
- Obfuscation: none · 0% short class names
- Biggest packages: org.bouncycastle.jcajce.provider.symmetric (501), kotlinx.coroutines.flow (296), org.bouncycastle.jcajce.provider.digest (253), androidx.appcompat.widget (199), androidx.core.view (193), kotlinx.coroutines (183), androidx.fragment.app (156), androidx.core.app (146)

## Insights
- Frameworks: Godot, Kotlin
- SDKs (2): Bouncy Castle, Kotlin Coroutines
- Libraries with exact versions: 27 · built with AGP 8.6.1
- Endpoints: 108 URLs on 102 hosts; top: godotengine.org (2), schemas.android.com (2), www.iryoku.com (2), www.unicode.org (2), adriaan.games (1), allenwp.com (1)
- Native: 4 libraries, 68 JNI methods · Locales: 85

## Review findings (static observations to check, not proven issues)
- [low] Profileable from the shell (1) — e.g. application/profileable shell=true
- [low] SecureRandom with explicit seed (7) — e.g. Lorg/bouncycastle/crypto/examples/DESExample;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V
- [low] Weak hash algorithm (1) — e.g. Lorg/godotengine/godot/utils/Crypt;->md5(Ljava/lang/String;)Ljava/lang/String;
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] Custom task affinity or task reparenting (3) — e.g. org.godotengine.editor.GodotGame

## Counts
- Permissions: 12 requested (4 dangerous), 0 declared
- Components: 5 activity, 1 activity-alias, 0 service, 1 receiver, 2 provider
- Exported: 2 (1 with deep links)
- Features: 1 required, 8 optional · libraries: androidx.window.extensions, androidx.window.sidecar, libopenxr.google.so
- Queries (package visibility): 1 packages, 2 intents, 1 providers · Metadata: 6 entries

## Exported components
- activity-alias org.godotengine.editor.ProjectManager — content://*.*\\.godot, file://*.*\\.godot
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
