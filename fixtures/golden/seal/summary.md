# com.junkfood.seal 1.13.1-(F-Droid) (11314)
Extracted by sextant <version> · schema 8 · sha256 095c2275152653155219d0f276f7a8e09be6436b3196cf1d1b56ebb5a39bf488

## App
- Label: Seal
- min SDK 21 · target SDK 34 · compile SDK 34
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: com.junkfood.seal.MainActivity
- Signing: v1, v2, v3 · cert sha256 7D:E6:AE:B9:3F:19:6E:67:7B:08:7B:C8:D0:40:FE:59:8B:61:3A:96:54:99:0D:6F:CA:A4:0D:71:EB:13:CD:49
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/Qr.xml bg #ffffff
- Layout: 1 dex · ABIs: x86_64

## Code
- 6124 classes · 31212 methods · 29952 fields · 0 Kotlin classes (0%)
- 114844 call edges (180290 call sites) · 9933 distinct string constants
- Obfuscation: none · 0% short class names · source files stripped (100%)
- Biggest packages: androidx.compose.material3 (338), androidx.appcompat.widget (140), androidx.compose.ui.platform (124), androidx.compose.foundation (105), androidx.compose.foundation.gestures (105), androidx.compose.runtime (101), com.junkfood.seal.util (98), androidx.compose.foundation.layout (97)

## Insights
- Frameworks: Jetpack Compose
- SDKs (9): Room, Dagger / Hilt, Coil, OkHttp, Kotlin Coroutines, Jackson, kotlinx.serialization, Jetpack Compose, Material Components
- Libraries with exact versions: 91 · built with AGP 8.5.2
- Endpoints: 60 URLs on 23 hosts; top: github.com (26), api.github.com (6), www.w3.org (4), xml.org (3), schemas.android.com (2), xmlpull.org (2)
- Native: 9 libraries, 0 JNI methods · Locales: 91

## Review findings (static observations to check, not proven issues)
- [low] Runs external commands (1) — e.g. Lcom/yausername/youtubedl_android/YoutubeDL;->execute(Lcom/yausername/youtubedl_android/YoutubeDLRequest;Ljava/lang/String;Lkotlin/jvm/functions/Function3;)Lcom/yausername/youtubedl_android/YoutubeDLResponse;
- [low] Components exported without permission (2) — e.g. androidx.compose.ui.tooling.PreviewActivity
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] WebView JavaScript enabled (1) — e.g. Landroidx/navigation/Navigator$navigate$1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;

## Counts
- Permissions: 11 requested (3 dangerous), 1 declared
- Components: 4 activity, 0 activity-alias, 3 service, 2 receiver, 2 provider
- Exported: 4 (2 with deep links)
- Features: 0 required, 0 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 1 intents, 0 providers · Metadata: 5 entries

## Exported components
- activity androidx.compose.ui.tooling.PreviewActivity
- activity com.junkfood.seal.MainActivity — http:, https:
- activity com.junkfood.seal.QuickDownloadActivity — http:, https:
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
