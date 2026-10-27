# io.github.muntashirakon.AppManager 4.1.1 (451)
Extracted by sextant <version> · schema 8 · sha256 e1f6ba10812c74016ad626e11e27a1ae450aec2c0964a11466abb9e7a9c8ef84

## App
- Label: App Manager
- min SDK 21 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: io.github.muntashirakon.AppManager.main.SplashActivity
- Signing: v1, v2, v3 · cert sha256 B6:8E:93:49:10:C0:05:80:14:A0:6A:2E:14:47:29:F9:AA:11:3F:C5:D9:E0:DF:F5:29:EC:BF:1D:1F:61:2D:13
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/Qr.xml bg #090503
- Layout: 6 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 30717 classes · 225186 methods · 109055 fields · 3141 Kotlin classes (10%)
- 562322 call edges (769522 call sites) · 43609 distinct string constants
- Biggest packages: com.google.common.collect (866), com.google.protobuf (675), org.bouncycastle.jcajce.provider.symmetric (559), j$.util.stream (510), com.google.common.util.concurrent (314), kotlinx.coroutines.flow (296), org.bouncycastle.jcajce.provider.digest (268), androidx.appcompat.widget (263)

## Insights
- Frameworks: Kotlin
- SDKs (6): Bouncy Castle, Room, Kotlin Coroutines, Gson, Protocol Buffers, Material Components
- Libraries with exact versions: 0 · built with AGP 8.13.2
- Endpoints: 353 URLs on 260 hosts; top: xmlpull.org (9), github.com (6), www.virustotal.com (5), apache.org (3), play.google.com (3), f-droid.org (2)
- Native: 16 libraries, 644 JNI methods · Locales: 98

## Review findings (static observations to check, not proven issues)
- [high] TrustManager may accept every certificate (2) — e.g. Lio/github/muntashirakon/adb/SslUtils$2;->checkServerTrusted([Ljava/security/cert/X509Certificate;Ljava/lang/String;)V
- [low] Runs external commands (8) — e.g. Lantlr/build/Tool;->system(Ljava/lang/String;)V
- [low] Components exported without permission (26) — e.g. io.github.muntashirakon.AppManager.apk.installer.PackageInstallerActivity
- [low] SecureRandom with explicit seed (7) — e.g. Lorg/bouncycastle/crypto/examples/DESExample;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V
- [low] Weak cipher or ECB mode (8) — e.g. Landroidx/biometric/utils/CryptoObjectUtils;->createFakeCryptoObject()Landroidx/biometric/BiometricPrompt$CryptoObject;
- [low] Weak hash algorithm (12) — e.g. Landroid/sun/security/pkcs12/PKCS12KeyStore;->generateHash([B)[B
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] Custom task affinity or task reparenting (7) — e.g. io.github.muntashirakon.AppManager.DummyActivity

## Counts
- Permissions: 63 requested (4 dangerous), 1 declared
- Components: 39 activity, 5 activity-alias, 11 service, 7 receiver, 2 provider
- Exported: 29 (13 with deep links)
- Features: 0 required, 2 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 0 intents, 0 providers · Metadata: 11 entries

## Exported components
- activity io.github.muntashirakon.AppManager.apk.installer.PackageInstallerActivity — content://*, content://*.*\\..*\\..*\\..*\\..*\\..*\\..*\\.apkm, content://*.*\\..*\\..*\\..*\\..*\\..*\\..*\\.apks
- activity io.github.muntashirakon.AppManager.crypto.auth.AuthFeatureDemultiplexer
- activity io.github.muntashirakon.AppManager.details.manifest.ManifestViewerActivity — content://*, file://*
- activity io.github.muntashirakon.AppManager.fm.FmActivity — :, content:, directory:
- activity io.github.muntashirakon.AppManager.history.ops.OpHistoryActivity
- activity io.github.muntashirakon.AppManager.intercept.ActivityInterceptor — about:, alipays:, android:
- activity io.github.muntashirakon.AppManager.logcat.LogViewerActivity — content://*.*\\..*\\..*\\..*\\..*\\..*\\..*\\.am.log, content://*.*\\..*\\..*\\..*\\..*\\..*\\.am.log, content://*.*\\..*\\..*\\..*\\..*\\.am.log
- activity io.github.muntashirakon.AppManager.logcat.RecordLogDialogActivity
- activity io.github.muntashirakon.AppManager.main.MainActivity — market://search
- activity io.github.muntashirakon.AppManager.main.SplashActivity
- activity io.github.muntashirakon.AppManager.misc.HelpActivity
- activity io.github.muntashirakon.AppManager.misc.LabsActivity
- activity io.github.muntashirakon.AppManager.profiles.ProfilesActivity
- activity io.github.muntashirakon.AppManager.runningapps.RunningAppsActivity
- activity io.github.muntashirakon.AppManager.scanner.ScannerActivity — content://*, file://*
- activity io.github.muntashirakon.AppManager.settings.SettingsActivity — app-manager://settings
- activity io.github.muntashirakon.AppManager.usage.AppUsageActivity
- activity io.github.muntashirakon.AppManager.viewer.ExplorerActivity — content://*, file://*
- activity io.github.muntashirakon.AppManager.viewer.audio.AudioPlayerActivity — content:, file:
- activity-alias io.github.muntashirakon.AppManager.apk.installer.AppInfoActivity
- activity-alias io.github.muntashirakon.AppManager.details.AppInfoActivity — app-manager://details, content://*, content://*.*\\..*\\..*\\..*\\..*\\..*\\..*\\.apkm
- activity-alias io.github.muntashirakon.AppManager.editor.EditorActivity — content:, content:/.*..*.BAT, content:/.*..*.CSS
- activity-alias io.github.muntashirakon.AppManager.intercept.BrowserInterceptor — http:, https:
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver io.github.muntashirakon.AppManager.logcat.RecordingWidgetProvider
- … 4 more in components.jsonl

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
