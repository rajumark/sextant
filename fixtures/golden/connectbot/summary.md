# org.connectbot 1.11.1-oss (11101000)
Extracted by sextant <version> · schema 8 · sha256 96cf501a83e89284ff3ec93ac06e7a6a6aa8a35c0c02486b865945fe59ac8223

## App
- Label: ConnectBot
- min SDK 24 · target SDK 36 · compile SDK 37
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: org.connectbot.ui.MainActivity
- Signing: v2, v3 · cert sha256 08:78:9B:AD:18:CE:8E:C7:B6:63:7B:5E:70:24:5A:76:3A:E5:02:4F:4D:49:AC:85:32:40:47:FB:06:B5:DA:8A
- Icon: res/hW.png (192×192 xxxhdpi) · @mipmap/icon · adaptive res/2r.xml fg res/Qr.xml bg res/0w.xml
- Layout: 1 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 6697 classes · 30361 methods · 23127 fields · 0 Kotlin classes (0%)
- 105815 call edges (169371 call sites) · 9906 distinct string constants
- Biggest packages: org.conscrypt (442), androidx.compose.material3 (312), org.connectbot.terminal (293), org.connectbot.service (155), org.connectbot.ui.components (127), org.connectbot.ui.screens.settings (118), androidx.compose.foundation.gestures (115), androidx.appcompat.widget (114)

## Insights
- Frameworks: Jetpack Compose
- SDKs (8): Conscrypt, Tink, Room, Dagger / Hilt, Timber, Kotlin Coroutines, Jetpack Compose, Material Components
- Libraries with exact versions: 97 · built with AGP 9.4.1
- Endpoints: 13 URLs on 9 hosts; top: github.com (4), connectbot.org (2), android.googlesource.com (1), buymeacoffee.com (1), gnu.org (1), goo.gle (1)
- Native: 24 libraries, 88 JNI methods · Locales: 89

## Review findings (static observations to check, not proven issues)
- [low] SecureRandom with explicit seed (1) — e.g. Lorg/connectbot/ui/screens/generatepubkey/GeneratePubkeyViewModel$startKeyGeneration$2$keyPair$1;->invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
- [low] Weak cipher or ECB mode (7) — e.g. Lcom/trilead/ssh2/crypto/CommonDecoder;->decryptData([B[B[BILjava/lang/String;)[B
- [low] Weak hash algorithm (4) — e.g. Lcom/trilead/ssh2/KnownHosts;->hostnameMatches(Ljava/lang/String;[Ljava/lang/String;)Z
- [info] App data can be backed up (1) — e.g. application allowBackup=true

## Counts
- Permissions: 13 requested (4 dangerous), 1 declared
- Components: 1 activity, 0 activity-alias, 3 service, 2 receiver, 1 provider
- Exported: 2 (1 with deep links)
- Features: 0 required, 1 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 0 intents, 0 providers · Metadata: 5 entries

## Exported components
- activity org.connectbot.ui.MainActivity — local:, mosh:, ssh:
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
