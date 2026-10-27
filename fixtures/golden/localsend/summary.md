# org.localsend.localsend_app 1.18.2 (643)
Extracted by sextant <version> · schema 7 · sha256 82ec3568fba2aa5295b9aae8b76f701d7a4703d86b9f8bad749472038fbaeab3

## App
- Label: LocalSend
- min SDK 24 · target SDK 37 · compile SDK 36
- debuggable: not set · allow_backup: not set · cleartext traffic: yes
- Launcher: org.localsend.localsend_app.MainActivity
- Signing: v2 · cert sha256 32:20:C3:53:A7:3C:FB:D0:C2:F3:05:24:71:C4:45:32:4C:F4:52:BC:BA:26:DE:1C:47:3A:52:FE:5C:44:E1:D6
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/as.png bg #ffffff
- Layout: 1 dex · ABIs: arm64-v8a

## Code
- 3022 classes · 16233 methods · 12095 fields · 0 Kotlin classes (0%)
- 61557 call edges (98209 call sites) · 8846 distinct string constants
- Biggest packages: k (107), B0 (79), androidx.datastore.preferences.protobuf (69), W2 (67), K (64), l0 (64), k4 (62), e0 (54)

## Insights
- Frameworks: Flutter
- SDKs (3): Glide, ExoPlayer / Media3, Material Components
- Libraries with exact versions: 62 · built with AGP 8.12.1
- Endpoints: 35 URLs on 21 hosts; top: developer.android.com (5), dashif.org (4), github.com (3), www.w3.org (3), docs.flutter.dev (2), issuetracker.google.com (2)
- Native: 5 libraries, 10 JNI methods · Locales: 86

## Review findings (static observations to check, not proven issues)
- [medium] Cleartext HTTP allowed (1) — e.g. application usesCleartextTraffic=true
- [low] Components exported without permission (1) — e.g. com.pravera.flutter_foreground_task.service.RebootReceiver
- [low] Weak cipher or ECB mode (1) — e.g. Lr0/a;->s(Lj0/k;)J
- [low] Weak hash algorithm (1) — e.g. Ll0/d0;->a(Lp0/t;Landroid/net/Uri;I)Ljava/lang/String;
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] WebView JavaScript enabled (1) — e.g. Lio/flutter/plugins/urllauncher/WebViewActivity;->onCreate(Landroid/os/Bundle;)V

## Counts
- Permissions: 19 requested (7 dangerous), 1 declared
- Components: 2 activity, 0 activity-alias, 4 service, 3 receiver, 3 provider
- Exported: 5 (0 with deep links)
- Features: 0 required, 2 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 1 intents, 0 providers · Metadata: 8 entries

## Exported components
- activity org.localsend.localsend_app.MainActivity
- service androidx.sharetarget.ChooserTargetServiceCompat [requires android.permission.BIND_CHOOSER_TARGET_SERVICE]
- service org.localsend.localsend_app.QuickTileService [requires android.permission.BIND_QUICK_SETTINGS_TILE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver com.pravera.flutter_foreground_task.service.RebootReceiver

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
