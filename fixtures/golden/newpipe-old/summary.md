# org.schabi.newpipe 0.29.0 (1014)
Extracted by sextant <version> · schema 8 · sha256 11c320613aebaf2132897f44dc9fa02bd05100b3edaacad11ceaa250889ff5d4

## App
- Label: NewPipe
- min SDK 23 · target SDK 35 · compile SDK 37
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: org.schabi.newpipe.MainActivity
- Signing: v1, v2, v3 · cert sha256 96:4D:FA:41:8E:66:39:01:10:B0:C0:61:A0:49:7C:BB:D7:39:5B:8B:25:26:BE:E4:DE:CE:95:59:7E:B5:E9:D4
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/as.png bg #cd201f
- Layout: 3 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 10671 classes · 65538 methods · 41554 fields · 0 Kotlin classes (0%)
- 198555 call edges (283950 call sites) · 21501 distinct string constants
- Obfuscation: light · 6% short class names · source files stripped (100%)
- Biggest packages: org.mozilla.javascript (290), j$.util.stream (284), com.google.android.exoplayer2 (254), androidx.compose.material3 (139), androidx.appcompat.widget (135), androidx.compose.ui.platform (129), com.google.android.exoplayer2.source (123), com.google.android.exoplayer2.upstream (122)

## Insights
- Frameworks: Jetpack Compose
- SDKs (13): WorkManager, ACRA, Room, Koin, Coil, ExoPlayer / Media3, OkHttp, Kotlin Coroutines, RxJava, Protocol Buffers, kotlinx.serialization, Jetpack Compose, Material Components
- Libraries with exact versions: 100 · built with AGP 9.2.1
- Endpoints: 566 URLs on 119 hosts; top: www.youtube.com (48), music.youtube.com (31), m.youtube.com (22), youtube.com (22), framatube.org (15), eduvid.org (14)
- Native: 4 libraries, 0 JNI methods · Locales: 124

## Review findings (static observations to check, not proven issues)
- [low] Runs external commands (2) — e.g. Lorg/acra/collector/MemoryInfoCollector;->collectMemInfo()Ljava/lang/String;
- [low] Components exported without permission (5) — e.g. org.schabi.newpipe.PanicResponderActivity
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] WebView JavaScript enabled (1) — e.g. Lorg/schabi/newpipe/error/ReCaptchaActivity;->onCreate(Landroid/os/Bundle;)V

## Counts
- Permissions: 12 requested (3 dangerous), 1 declared
- Components: 13 activity, 0 activity-alias, 11 service, 5 receiver, 4 provider
- Exported: 9 (1 with deep links)
- Features: 0 required, 2 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 1 intents, 0 providers · Metadata: 12 entries

## Exported components
- activity org.schabi.newpipe.MainActivity
- activity org.schabi.newpipe.PanicResponderActivity
- activity org.schabi.newpipe.RouterActivity — http:, http://*.bandcamp.com, http://*.hooktube.com/channel/*
- activity org.schabi.newpipe.util.FilePickerActivityHelper
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- service org.schabi.newpipe.player.PlayerService
- receiver androidx.media.session.MediaButtonReceiver
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
