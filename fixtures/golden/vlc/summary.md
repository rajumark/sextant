# org.videolan.vlc 3.7.2 (13070208)
Extracted by sextant <version> · schema 8 · sha256 727693d59c4d678c046386812a103209a25423460c9d22e38e12460f12402ab6

## App
- Label: VLC
- min SDK 17 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: yes · cleartext traffic: yes
- Launcher: org.videolan.vlc.StartActivity
- Signing: v1, v2, v3 · cert sha256 80:53:5B:E6:1E:ED:B9:A0:3B:04:76:A6:F4:93:D4:96:C3:49:87:70:40:43:39:EA:7A:80:00:F5:E6:1D:22:C0
- Icon: res/RJ.png (240×240 xxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/Qr.xml bg res/0w.xml
- Layout: 6 dex · ABIs: x86_64

## Code
- 33388 classes · 224212 methods · 208587 fields · 10213 Kotlin classes (30%)
- 551766 call edges (736471 call sites) · 39314 distinct string constants
- Biggest packages: com.google.common.collect (844), org.bouncycastle.jcajce.provider.symmetric (501), j$.util.stream (495), androidx.paging (461), androidx.leanback.widget (378), org.videolan.vlc (358), org.videolan.vlc.gui (318), com.google.common.util.concurrent (311)

## Insights
- Frameworks: Kotlin
- SDKs (10): ZXing, Bouncy Castle, Room, Ktor, OkHttp, Retrofit, Kotlin Coroutines, Moshi, kotlinx.serialization, Material Components
- Libraries with exact versions: 0 · built with AGP 9.1.1
- Endpoints: 4264 URLs on 401 hosts; top: www.slf4j.org (9), *.*.3g2 (6), *.*.3ga (6), *.*.3gp (6), *.*.3gp2 (6), *.*.3gpp (6)
- Native: 4 libraries, 121 JNI methods · Locales: 112

## Review findings (static observations to check, not proven issues)
- [high] Private key embedded (1) — e.g. Lio/netty/handler/ssl/OpenSsl;-><clinit>()V
- [high] TrustManager may accept every certificate (1) — e.g. Lorg/bouncycastle/est/jcajce/JcaJceUtils$1;->checkServerTrusted([Ljava/security/cert/X509Certificate;Ljava/lang/String;)V
- [medium] Cleartext HTTP allowed (1) — e.g. application usesCleartextTraffic=true
- [medium] Content provider exported without permission (3) — e.g. org.videolan.television.util.TVSearchProvider
- [low] Runs external commands (11) — e.g. Lio/netty/util/internal/NativeLibraryLoader;->tryExec(Ljava/lang/String;)Z
- [low] Components exported without permission (15) — e.g. org.videolan.television.ui.DetailsActivity
- [low] SecureRandom with explicit seed (8) — e.g. Lio/ktor/util/NonceKt$nonceGeneratorJob$1;->invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
- [low] Weak cipher or ECB mode (5) — e.g. Lio/ktor/network/tls/RenderKt;->writeEncryptedPreMasterSecret(Lio/ktor/utils/io/core/BytePacketBuilder;[BLjava/security/PublicKey;Ljava/security/SecureRandom;)V
- [low] Weak hash algorithm (6) — e.g. Lio/ktor/client/plugins/cache/storage/FileCacheStorage;->key(Lio/ktor/http/Url;)Ljava/lang/String;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] Custom task affinity or task reparenting (1) — e.g. org.videolan.television.ui.audioplayer.AudioPlayerActivity

## Counts
- Permissions: 31 requested (8 dangerous), 5 declared
- Components: 40 activity, 0 activity-alias, 7 service, 8 receiver, 5 provider
- Exported: 20 (1 with deep links)
- Features: 0 required, 4 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 1 intents, 1 providers · Metadata: 22 entries

## Exported components
- activity org.videolan.television.ui.DetailsActivity
- activity org.videolan.television.ui.MediaScrapingTvActivity
- activity org.videolan.television.ui.MediaScrapingTvshowDetailsActivity
- activity org.videolan.television.ui.SearchActivity
- activity org.videolan.television.ui.audioplayer.AudioPlayerActivity
- activity org.videolan.television.ui.browser.TVActivity
- activity org.videolan.television.ui.browser.VerticalGridActivity
- activity org.videolan.television.ui.details.MediaListActivity
- activity org.videolan.vlc.StartActivity — :, ://*.*.3G2, ://*.*.3GA
- activity org.videolan.vlc.gui.video.VideoPlayerActivity
- activity org.videolan.vlc.gui.video.benchmark.BenchActivity
- activity org.videolan.vlc.widget.MiniPlayerConfigureActivity
- service org.videolan.vlc.PlaybackService
- receiver androidx.media.session.MediaButtonReceiver
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver org.videolan.vlc.TvReceiver
- receiver org.videolan.vlc.widget.MiniPlayerAppWidgetProvider
- provider org.videolan.television.util.TVSearchProvider
- provider org.videolan.vlc.ArtworkProvider
- provider org.videolan.vlc.FileProvider

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
