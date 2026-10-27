# org.thunderdog.challegram 0.28.11.1800 (1800202)
Extracted by sextant <version> · schema 8 · sha256 5a6f13f790ce889b2984f0303ee6f605f7ae7106fed55879dc7b6c2bf9104ede

## App
- Label: Telegram X
- min SDK 23 · target SDK 37 · compile SDK 37
- debuggable: not set · allow_backup: no · cleartext traffic: not set
- Launcher: org.thunderdog.challegram.MainActivity
- Signing: v1, v2, v3 · cert sha256 A1:98:45:0B:69:30:63:05:CB:B7:1D:2E:7D:82:5F:7F:A6:A6:7C:A7:C5:60:42:19:6E:05:C9:58:B9:AD:4B:F6
- Icon: res/c7.png (192×192 xxxhdpi) · @mipmap/app_launcher · adaptive res/Vt.xml fg res/M1.webp bg res/Qs.webp
- Layout: 3 dex · ABIs: arm64-v8a

## Code
- 12141 classes · 66756 methods · 50619 fields · 0 Kotlin classes (0%)
- 228925 call edges (346486 call sites) · 29481 distinct string constants
- Biggest packages: (default) (6735), org.drinkless.tdlib (3600), org.webrtc (355), j$.util.stream (263), j$.util (91), com.coremedia.iso.boxes (64), org.osmdroid.views.overlay (55), androidx.camera.camera2.compat.quirk (53)

## Insights
- Frameworks: none detected
- SDKs (6): WorkManager, WebRTC, Room, osmdroid, ExoPlayer / Media3, OkHttp
- Libraries with exact versions: 53 · built with AGP 9.3.1
- Endpoints: 133 URLs on 75 hosts; top: telegra.ph (18), github.com (9), telegram.org (8), t.me (7), developer.android.com (3), overlay.openstreetmap.nl (3)
- Native: 8 libraries, 371 JNI methods · Locales: 91

## Review findings (static observations to check, not proven issues)
- [medium] JavaScript bridge into a WebView (2) — e.g. Lwf3;->n0()V
- [low] Runs external commands (1) — e.g. Lzo5;->A(Ljava/lang/String;)Ljava/util/ArrayList;
- [low] Components exported without permission (2) — e.g. org.thunderdog.challegram.ManageSpaceActivity
- [low] Weak cipher or ECB mode (2) — e.g. Ld4;->g(Lou0;)J
- [low] Weak hash algorithm (3) — e.g. Lh42;-><init>([BZ)V
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] Custom task affinity or task reparenting (1) — e.g. org.thunderdog.challegram.voip.gui.VoIPFeedbackActivity
- [info] WebView JavaScript enabled (3) — e.g. Lwf3;->n0()V

## Counts
- Permissions: 55 requested (14 dangerous), 1 declared
- Components: 3 activity, 0 activity-alias, 12 service, 14 receiver, 3 provider
- Exported: 6 (1 with deep links)
- Features: 1 required, 11 optional · libraries: com.sec.android.app.multiwindow, org.apache.http.legacy
- Queries (package visibility): 5 packages, 2 intents, 0 providers · Metadata: 14 entries

## Exported components
- activity org.thunderdog.challegram.MainActivity — http://t.me, http://telegram.dog, http://telegram.me
- activity org.thunderdog.challegram.ManageSpaceActivity
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- service org.thunderdog.challegram.sync.SyncService
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver androidx.work.impl.diagnostics.DiagnosticsReceiver [requires android.permission.DUMP]

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
