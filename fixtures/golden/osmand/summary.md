# net.osmand.plus 5.4.10 (541003)
Extracted by sextant <version> · schema 8 · sha256 ca05d8f4ec364f84bde1946738e898abfb392234905eef1dc0ec597d4580b112

## App
- Label: OsmAnd~
- min SDK 24 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: yes · cleartext traffic: yes
- Launcher: net.osmand.plus.activities.MapActivity
- Signing: v2, v3 · cert sha256 38:29:4E:AA:C9:5C:3B:B5:A9:94:69:2C:D7:75:8F:8B:CD:BF:FB:40:81:9A:F6:E8:06:EB:7F:E4:82:32:4D:CD
- Icon: res/hWS.png (192×192 xxxhdpi) · @mipmap/icon · adaptive res/2rq.xml fg res/DKG.png bg res/7Hk.png
- Layout: 5 dex · ABIs: arm64-v8a

## Code
- 28397 classes · 217006 methods · 122237 fields · 8101 Kotlin classes (28%)
- 558702 call edges (743986 call sites) · 54506 distinct string constants
- Biggest packages: net.osmand.core.jni (813), net.osmand.binary (442), androidx.compose.ui.platform (265), com.google.protobuf (252), androidx.compose.runtime (239), net.osmand.plus.search.dialogs (237), net.osmand.plus.routepreparationmenu (228), io.github.cosinekitty.astronomy (216)

## Insights
- Frameworks: Jetpack Compose, Kotlin, Qt
- SDKs (11): Coil, Picasso, Ktor, OkHttp, Kotlin Coroutines, Gson, Jackson, Protocol Buffers, kotlinx.serialization, Jetpack Compose, Material Components
- Libraries with exact versions: 87 · built with AGP 8.10.1
- Endpoints: 1359 URLs on 483 hosts; top: osmand.net (76), docs.osmand.net (36), www.openstreetmap.org (18), openstreetmap.org (11), osm.org (11), here.com (10)
- Native: 7 libraries, 5798 JNI methods · Locales: 119

## Review findings (static observations to check, not proven issues)
- [high] TrustManager may accept every certificate (1) — e.g. Lnet/osmand/plus/utils/PicassoUtils$1;->checkServerTrusted([Ljava/security/cert/X509Certificate;Ljava/lang/String;)V
- [medium] Cleartext HTTP allowed (1) — e.g. application usesCleartextTraffic=true
- [medium] JavaScript bridge into a WebView (2) — e.g. Lnet/osmand/plus/plugins/mapillary/MapillaryImageDialog;->getWebView()Landroid/view/View;
- [low] Runs external commands (2) — e.g. Lnet/osmand/plus/feedback/FeedbackHelper;->readLogcat()[B
- [low] Components exported without permission (7) — e.g. net.osmand.plus.activities.search.GeoIntentActivity
- [low] Weak hash algorithm (2) — e.g. Lnet/osmand/plus/gallery/online/cache/PhotoCacheManager;->hashKey(Ljava/lang/String;)Ljava/lang/String;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] WebView JavaScript enabled (5) — e.g. Lnet/osmand/plus/help/HelpArticleDialogFragment;->setupWebSettings(Landroid/webkit/WebSettings;)V

## Counts
- Permissions: 28 requested (9 dangerous), 2 declared
- Components: 12 activity, 0 activity-alias, 6 service, 6 receiver, 3 provider
- Exported: 11 (3 with deep links)
- Features: 1 required, 14 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 14 packages, 1 intents, 1 providers · Metadata: 17 entries

## Exported components
- activity net.osmand.plus.activities.MapActivity — content://*, content://*/*.kml, content://*/*.sqlite
- activity net.osmand.plus.activities.search.GeoIntentActivity — geo:, http://download.osmand.net/go*, http://download.osmand.netgo*
- activity net.osmand.plus.download.DownloadActivity
- activity net.osmand.plus.wikivoyage.explore.WikivoyageExploreActivity — http://osmand.net/travel*, https://osmand.net/travel*
- service net.osmand.aidl.OsmandAidlService
- service net.osmand.aidl.OsmandAidlServiceV2
- service net.osmand.plus.NavigationService [requires net.osmand.plus.SERVICE_PERMISSION]
- service net.osmand.plus.auto.NavigationCarAppService
- service net.osmand.plus.download.DownloadService [requires net.osmand.plus.SERVICE_PERMISSION]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver net.osmand.plus.plugins.audionotes.MediaRemoteControlReceiver

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
