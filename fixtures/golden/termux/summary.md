# com.termux 0.118.3 (1002)
Extracted by sextant <version> · schema 7 · sha256 e6265a57eb5ca363808488e3b01955958bed93bc0c8a0d281849b363b11027ec

## App
- Label: Termux
- min SDK 24 · target SDK 28 · compile SDK 30
- debuggable: not set · allow_backup: no · cleartext traffic: not set
- Launcher: com.termux.app.TermuxActivity
- sharedUserId: com.termux
- Signing: v2, v3 · cert sha256 22:8F:B2:CF:E9:08:31:C1:49:9E:C3:CC:AF:61:E9:6E:8E:1C:E7:07:66:B9:47:46:72:CE:42:73:34:D4:1C:42
- Icon: res/CG.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/uF.xml fg res/fK.xml
- Layout: 1 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 2696 classes · 18613 methods · 9397 fields · 0 Kotlin classes (0%)
- 50563 call edges (66344 call sites) · 4182 distinct string constants
- Biggest packages: androidx.appcompat.widget (170), androidx.fragment.app (127), androidx.recyclerview.widget (119), androidx.preference (87), kotlinx.coroutines (84), com.google.common.collect (75), androidx.transition (74), androidx.appcompat.app (71)

## Insights
- Frameworks: none detected
- SDKs (2): Kotlin Coroutines, Material Components
- Libraries with exact versions: 36 · built with AGP 4.2.2
- Endpoints: 32 URLs on 7 hosts; top: github.com (24), schemas.android.com (2), wiki.termux.com (2), android.googlesource.com (1), f-droid.org (1), termux.dev (1)
- Native: 8 libraries, 24 JNI methods · Locales: 86

## Review findings (static observations to check, not proven issues)
- [low] Runs external commands (2) — e.g. Lcom/termux/shared/shell/TermuxTask;->execute(Landroid/content/Context;Lcom/termux/shared/models/ExecutionCommand;Lcom/termux/shared/shell/TermuxTask$TermuxTaskClient;Lcom/termux/shared/shell/ShellEnvironmentClient;Z)Lcom/termux/shared/shell/TermuxTask;
- [low] Components exported without permission (3) — e.g. com.termux.app.activities.SettingsActivity
- [low] Shares a user ID with other apps (1) — e.g. manifest sharedUserId=com.termux
- [info] Custom task affinity or task reparenting (1) — e.g. com.termux.filepicker.TermuxFileReceiverActivity
- [info] Exported provider grants URI permissions for any path (2) — e.g. com.termux.app.TermuxOpenReceiver$ContentProvider

## Counts
- Permissions: 16 requested (2 dangerous), 1 declared
- Components: 5 activity, 1 activity-alias, 2 service, 2 receiver, 2 provider
- Exported: 7 (0 with deep links)
- Features: 0 required, 2 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 0 intents, 0 providers · Metadata: 5 entries

## Exported components
- activity com.termux.app.TermuxActivity
- activity com.termux.app.activities.SettingsActivity
- activity com.termux.filepicker.TermuxFileReceiverActivity
- activity-alias com.termux.HomeActivity
- service com.termux.app.RunCommandService [requires com.termux.permission.RUN_COMMAND]
- provider com.termux.app.TermuxOpenReceiver$ContentProvider [requires com.termux.permission.RUN_COMMAND]
- provider com.termux.filepicker.TermuxDocumentsProvider [requires android.permission.MANAGE_DOCUMENTS]

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
