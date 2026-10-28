# com.termux.api 0.53.0 (1002)
Extracted by sextant <version> · schema 8 · sha256 4497dbbf81906df52e59ed387a5223d225aa0de3aca817cc557a621e4dadda44

## App
- Label: Termux:API
- min SDK 24 · target SDK 28 · compile SDK 35
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: com.termux.api.activities.TermuxAPILauncherActivity
- sharedUserId: com.termux
- Signing: v2, v3 · cert sha256 22:8F:B2:CF:E9:08:31:C1:49:9E:C3:CC:AF:61:E9:6E:8E:1C:E7:07:66:B9:47:46:72:CE:42:73:34:D4:1C:42
- Icon: res/r2.xml · @drawable/ic_launcher
- Layout: 2 dex · ABIs: none

## Code
- 3327 classes · 20075 methods · 9729 fields · 0 Kotlin classes (0%)
- 52326 call edges (67750 call sites) · 4585 distinct string constants
- Obfuscation: none · 0% short class names · source files stripped (100%)
- Biggest packages: com.termux.api.apis (238), androidx.appcompat.widget (205), androidx.core.view (170), androidx.fragment.app (123), androidx.recyclerview.widget (108), androidx.lifecycle (92), androidx.appcompat.app (84), androidx.preference (84)

## Insights
- Frameworks: none detected
- SDKs (2): Kotlin Coroutines, Material Components
- Libraries with exact versions: 43 · built with AGP 8.7.3
- Endpoints: 28 URLs on 7 hosts; top: github.com (19), developer.android.com (3), schemas.android.com (2), dontkillmyapp.com (1), termux.dev (1), wiki.termux.com (1)
- Native: 0 libraries, 0 JNI methods · Locales: 86

## Review findings (static observations to check, not proven issues)
- [low] Runs external commands (2) — e.g. Lcom/termux/shared/shell/command/runner/app/AppShell;->execute(Landroid/content/Context;Lcom/termux/shared/shell/command/ExecutionCommand;Lcom/termux/shared/shell/command/runner/app/AppShell$AppShellClient;Lcom/termux/shared/shell/command/environment/IShellEnvironment;Ljava/util/HashMap;Z)Lcom/termux/shared/shell/command/runner/app/AppShell;
- [low] Components exported without permission (2) — e.g. com.termux.api.activities.TermuxAPIMainActivity
- [low] Shares a user ID with other apps (1) — e.g. manifest sharedUserId=com.termux
- [low] Weak cipher or ECB mode (1) — e.g. Landroidx/biometric/CryptoObjectUtils;->createFakeCryptoObject()Landroidx/biometric/BiometricPrompt$CryptoObject;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Exported provider grants URI permissions for any path (1) — e.g. com.termux.api.apis.ShareAPI$ContentProvider

## Counts
- Permissions: 35 requested (14 dangerous), 2 declared
- Components: 9 activity, 1 activity-alias, 10 service, 3 receiver, 2 provider
- Exported: 6 (0 with deep links)
- Features: 0 required, 10 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 0 intents, 0 providers · Metadata: 4 entries

## Exported components
- activity com.termux.api.activities.TermuxAPIMainActivity
- activity com.termux.api.settings.activities.TermuxAPISettingsActivity
- activity-alias com.termux.api.activities.TermuxAPILauncherActivity
- service com.termux.api.apis.NotificationListAPI$NotificationService [requires android.permission.BIND_NOTIFICATION_LISTENER_SERVICE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- provider com.termux.api.apis.ShareAPI$ContentProvider [requires com.termux.sharedfiles.READ_WRITE]

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
