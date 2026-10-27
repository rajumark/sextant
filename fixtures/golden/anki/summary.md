# com.ichi2.anki 2.25.3 (22503300)
Extracted by sextant <version> · schema 8 · sha256 bd0ac8cb9ddff42e1c316e37ee1e5f2cbe94a30f81485fa317d53664574c2b3a

## App
- Label: AnkiDroid
- min SDK 24 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: yes · cleartext traffic: yes
- Launcher: com.ichi2.anki.IntentHandler
- Signing: v2, v3 · cert sha256 24:A3:D4:75:6C:62:DD:69:C8:6D:F6:9A:A7:FA:32:7B:2A:6B:F5:5F:55:C4:A1:74:9F:99:BD:1D:A7:59:06:BB
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/Qr.xml bg res/0w.xml
- Layout: 3 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 11204 classes · 69251 methods · 40925 fields · 492 Kotlin classes (4%)
- 221437 call edges (331605 call sites) · 24050 distinct string constants
- Biggest packages: (default) (9550), com.google.protobuf (120), anki.scheduler (74), org.chromium.support_lib_boundary (65), anki.stats (54), com.ichi2.anki.dialogs (40), j$.nio.file (39), com.ichi2.anki (38)

## Insights
- Frameworks: Jetpack Compose, Kotlin
- SDKs (11): WorkManager, ACRA, Room, LeakCanary, ExoPlayer / Media3, OkHttp, Kotlin Coroutines, Protocol Buffers, kotlinx.serialization, Jetpack Compose, Material Components
- Libraries with exact versions: 91 · built with AGP 9.4.0
- Endpoints: 255 URLs on 75 hosts; top: github.com (43), issuetracker.google.com (15), ankiweb.net (13), docs.ankidroid.org (13), docs.ankiweb.net (10), code.google.com (9)
- Native: 8 libraries, 12 JNI methods · Locales: 109

## Review findings (static observations to check, not proven issues)
- [medium] Cleartext HTTP allowed (1) — e.g. application usesCleartextTraffic=true
- [medium] Content provider exported without permission (1) — e.g. com.ichi2.anki.provider.CardContentProvider
- [medium] JavaScript bridge into a WebView (2) — e.g. Lcom/ichi2/anki/pages/PageFragment;->setupBridgeCommand(Lnw5;)V
- [low] Runs external commands (4) — e.g. Lgs1;->onCreateDialog(Landroid/os/Bundle;)Landroid/app/Dialog;
- [low] Components exported without permission (11) — e.g. com.canhub.cropper.CropImageActivity
- [low] Weak hash algorithm (1) — e.g. Lnq8;->a(Ljava/lang/String;)Ljava/lang/String;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] Custom task affinity or task reparenting (2) — e.g. leakcanary.internal.RequestPermissionActivity
- [info] WebView JavaScript enabled (6) — e.g. Lcom/ichi2/anki/Info;->onCreate(Landroid/os/Bundle;)V

## Counts
- Permissions: 17 requested (4 dangerous), 2 declared
- Components: 31 activity, 4 activity-alias, 7 service, 16 receiver, 8 provider
- Exported: 17 (3 with deep links)
- Features: 0 required, 4 optional · libraries: androidx.window.extensions, androidx.window.sidecar, com.sec.android.app.multiwindow
- Queries (package visibility): 0 packages, 7 intents, 0 providers · Metadata: 18 entries

## Exported components
- activity com.canhub.cropper.CropImageActivity
- activity com.ichi2.anki.IntentHandler — content://*.*\\.apkg, content://*.*\\.colpkg, content://*.*\\.csv
- activity com.ichi2.anki.IntentHandler2
- activity com.ichi2.anki.Reviewer
- activity com.ichi2.anki.instantnoteeditor.InstantNoteEditorActivity
- activity com.ichi2.anki.ui.windows.managespace.ManageSpaceActivity
- activity com.ichi2.anki.ui.windows.permissions.AllPermissionsExplanationActivity [requires android.permission.START_VIEW_PERMISSION_USAGE]
- activity leakcanary.internal.activity.LeakActivity — content://*.*\\..*\\..*\\..*\\..*\\..*\\..*\\.hprof, content://*.*\\..*\\..*\\..*\\..*\\..*\\.hprof, content://*.*\\..*\\..*\\..*\\..*\\.hprof
- activity-alias com.ichi2.anki.AnkiCardContextMenuAction
- activity-alias com.ichi2.anki.CardBrowserDeepLink — anki://x-callback-url/browser
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver androidx.work.impl.diagnostics.DiagnosticsReceiver [requires android.permission.DUMP]
- receiver com.ichi2.anki.receiver.SdCardReceiver
- receiver com.ichi2.widget.AddNoteWidget
- receiver com.ichi2.widget.AnkiDroidWidgetSmall
- provider com.ichi2.anki.provider.CardContentProvider

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
