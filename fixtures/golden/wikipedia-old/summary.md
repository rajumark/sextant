# org.wikipedia r/50610-r-2026-10-06 (50610)
Extracted by sextant <version> · schema 8 · sha256 4602977099bff12bf24afbbe5a516c0979cb6e5122a4fbbf21569c3fe46a7040

## App
- Label: Wikipedia
- min SDK 24 · target SDK 37 · compile SDK 37
- debuggable: not set · allow_backup: yes · cleartext traffic: no
- Launcher: org.wikipedia.DefaultIcon
- Signing: v2, v3 · cert sha256 1C:3D:DB:0E:8F:32:58:B0:FF:3B:7B:6A:60:8D:19:E7:CD:EE:D3:8A:58:97:BB:DA:23:BA:C1:55:92:3B:12:67
- Icon: res/M6.png (192×192 xxxhdpi) · @mipmap/launcher · adaptive res/dT.xml fg res/z21.xml bg res/DW.xml
- Layout: 2 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 11545 classes · 55073 methods · 42282 fields · 0 Kotlin classes (0%)
- 222930 call edges (366563 call sites) · 22541 distinct string constants
- Obfuscation: none · 1% short class names · source files stripped (100%)
- Biggest packages: androidx.compose.material3 (225), org.wikipedia.feed (184), org.wikipedia.dataclient.mwapi (147), org.wikipedia.readinglist (142), androidx.appcompat.widget (131), org.wikipedia.views (125), androidx.paging (123), org.jsoup.parser (117)

## Insights
- Frameworks: Jetpack Compose
- SDKs (16): WorkManager, Room, LeakCanary, Coil, Timber, MapLibre, ExoPlayer / Media3, OkHttp, Retrofit, Kotlin Coroutines, Gson, Jackson, kotlinx.serialization, Jetpack Compose, Lottie, Material Components
- Libraries with exact versions: 110 · built with AGP 9.4.1
- Endpoints: 144 URLs on 51 hosts; top: www.mediawiki.org (30), thumb.wikimedia.org (10), donate.wikimedia.org (7), en.wikipedia.org (7), *.wikipedia.org (6), www.w3.org (6)
- Native: 8 libraries, 0 JNI methods · Locales: 197

## Review findings (static observations to check, not proven issues)
- [medium] JavaScript bridge into a WebView (2) — e.g. Lokio/internal/DefaultSocket;-><init>(Landroid/os/Handler;Landroid/content/Context;Lcom/hcaptcha/sdk/HCaptchaConfig;Lcom/hcaptcha/sdk/HCaptchaInternalConfig;Lcom/hcaptcha/sdk/IHCaptchaVerifier;Lcom/hcaptcha/sdk/HCaptchaWebView;)V
- [low] Runs external commands (1) — e.g. Lcom/hcaptcha/sdk/HCaptchaDebugInfo;->roBuildProps()Ljava/util/HashMap;
- [low] Components exported without permission (7) — e.g. org.wikipedia.main.MainActivity
- [low] Weak hash algorithm (1) — e.g. Lcom/hcaptcha/sdk/HCaptchaDebugInfo;->debugInfo(Ljava/lang/String;Ljava/lang/String;)Ljava/util/ArrayList;
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Custom task affinity or task reparenting (1) — e.g. androidx.glance.appwidget.action.InvisibleActionTrampolineActivity
- [info] WebView JavaScript enabled (3) — e.g. Lokio/internal/DefaultSocket;-><init>(Landroid/os/Handler;Landroid/content/Context;Lcom/hcaptcha/sdk/HCaptchaConfig;Lcom/hcaptcha/sdk/HCaptchaInternalConfig;Lcom/hcaptcha/sdk/IHCaptchaVerifier;Lcom/hcaptcha/sdk/HCaptchaWebView;)V

## Counts
- Permissions: 17 requested (7 dangerous), 1 declared
- Components: 75 activity, 2 activity-alias, 6 service, 12 receiver, 3 provider
- Exported: 12 (1 with deep links)
- Features: 1 required, 4 optional · libraries: androidx.window.extensions, androidx.window.sidecar, com.sec.android.app.multiwindow
- Queries (package visibility): 0 packages, 2 intents, 0 providers · Metadata: 14 entries

## Exported components
- activity org.wikipedia.main.MainActivity
- activity org.wikipedia.page.PageActivity — http://*.wikipedia.org/sr.*, http://*.wikipedia.org/wiki/*, http://*.wikipedia.org/zh.*
- activity org.wikipedia.search.SearchActivity
- activity-alias org.wikipedia.DefaultIcon
- service androidx.glance.appwidget.GlanceRemoteViewsService [requires android.permission.BIND_REMOTEVIEWS]
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver androidx.work.impl.diagnostics.DiagnosticsReceiver [requires android.permission.DUMP]
- receiver org.wikipedia.notifications.NotificationPollBroadcastReceiver
- receiver org.wikipedia.widgets.WidgetProviderFeaturedPage
- receiver org.wikipedia.widgets.WidgetProviderSearch
- receiver org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetReceiver

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
