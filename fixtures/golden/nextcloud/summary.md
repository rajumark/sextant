# com.nextcloud.client 35.0.1 (350000190)
Extracted by sextant <version> · schema 8 · sha256 8fd4d86de9497d3afecc0bad201b177c8f413c0e2e3e152cbc80f48d6e9b481f

## App
- Label: Nextcloud
- min SDK 28 · target SDK 36 · compile SDK 37
- debuggable: not set · allow_backup: no · cleartext traffic: yes
- Launcher: com.nmc.android.ui.LauncherActivity
- Signing: v3 · cert sha256 57:09:57:6F:9D:6C:6A:76:87:DD:5D:56:B2:69:D1:FA:5E:37:97:02:B9:1B:C8:3F:39:FD:7B:7C:59:7C:1B:83
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/E4.xml fg res/Qr.xml bg res/0w.xml
- Layout: 7 dex · ABIs: arm64-v8a, x86_64

## Code
- 54323 classes · 345508 methods · 186079 fields · 18102 Kotlin classes (33%)
- 856166 call edges (1169086 call sites) · 68736 distinct string constants
- Biggest packages: androidx.compose.material3 (1946), com.google.common.collect (1007), com.nextcloud.client.di (869), androidx.media3.session (752), org.bouncycastle.jcajce.provider.symmetric (615), freemarker.core (607), androidx.compose.foundation.layout (476), dynamiccolor (471)

## Insights
- Frameworks: Jetpack Compose, Kotlin
- SDKs (18): WorkManager, ZXing, Bouncy Castle, Conscrypt, Room, Dagger / Hilt, Coil, Glide, osmdroid, ExoPlayer / Media3, Ktor, OkHttp, Kotlin Coroutines, Gson, Jackson, kotlinx.serialization, Jetpack Compose, Material Components
- Libraries with exact versions: 104 · built with AGP 9.3.2
- Endpoints: 164 URLs on 81 hosts; top: mock (10), www.slf4j.org (10), * (9), github.com (9), www.w3.org (8), nextcloud.com (5)
- Native: 12 libraries, 88 JNI methods · Locales: 112

## Review findings (static observations to check, not proven issues)
- [high] TrustManager may accept every certificate (1) — e.g. Lorg/bouncycastle/est/jcajce/JcaJceUtils$1;->checkServerTrusted([Ljava/security/cert/X509Certificate;Ljava/lang/String;)V
- [medium] Cleartext HTTP allowed (1) — e.g. application usesCleartextTraffic=true
- [medium] Content provider exported without permission (1) — e.g. com.owncloud.android.providers.FileContentProvider
- [medium] JavaScript bridge into a WebView (2) — e.g. Lcom/owncloud/android/ui/activity/RichDocumentsEditorWebView;->postOnCreate()V
- [low] Runs external commands (3) — e.g. Lfreemarker/template/utility/Execute;->exec(Ljava/util/List;)Ljava/lang/Object;
- [low] Components exported without permission (12) — e.g. com.nextcloud.ui.composeActivity.ComposeActivity
- [low] SecureRandom with explicit seed (7) — e.g. Lio/ktor/util/NonceKt$nonceGeneratorJob$1;->invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
- [low] Weak cipher or ECB mode (9) — e.g. Landroidx/media3/datasource/cache/CachedContentIndex$LegacyStorage;-><init>(Ljava/io/File;[BZ)V
- [low] Weak hash algorithm (10) — e.g. Lcom/nextcloud/client/utils/HashUtil;->md5Hash(Ljava/lang/String;)Ljava/lang/String;
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] Exported provider grants URI permissions for any path (2) — e.g. com.owncloud.android.providers.DiskLruImageCacheFileProvider
- [info] WebView JavaScript enabled (3) — e.g. Lcom/owncloud/android/authentication/AuthenticatorActivity;->initSimpleSignupLogin()V

## Counts
- Permissions: 33 requested (13 dangerous), 1 declared
- Components: 46 activity, 1 activity-alias, 10 service, 14 receiver, 7 provider
- Exported: 21 (3 with deep links)
- Features: 0 required, 6 optional · libraries: androidx.window.extensions, androidx.window.sidecar, org.apache.http.legacy
- Queries (package visibility): 5 packages, 9 intents, 0 providers · Metadata: 12 entries

## Exported components
- activity androidx.media3.session.BluetoothValidationActivity [requires android.permission.BLUETOOTH_PRIVILEGED] — content://media/internal/audio/media/*
- activity com.nextcloud.ui.composeActivity.ComposeActivity
- activity com.nmc.android.ui.LauncherActivity
- activity com.owncloud.android.authentication.AuthenticatorActivity
- activity com.owncloud.android.authentication.DeepLinkLoginActivity — @string/login_data_own_scheme://login
- activity com.owncloud.android.ui.activity.FileDisplayActivity — http://*/..*/..*/..*/f/..*, http://*/..*/..*/f/..*, http://*/..*/f/..*
- activity com.owncloud.android.ui.activity.ReceiveExternalFilesActivity
- activity com.owncloud.android.ui.activity.SsoGrantPermissionActivity
- activity-alias com.nextcloud.ui.composeActivity.ComposeProcessTextAlias
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- service com.nextcloud.client.player.media3.PlaybackService
- service com.nextcloud.client.widget.DashboardWidgetService [requires android.permission.BIND_REMOTEVIEWS]
- service com.owncloud.android.services.AccountManagerService
- service com.owncloud.android.syncadapter.FileSyncService
- receiver androidx.media3.session.MediaButtonReceiver
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver androidx.work.impl.diagnostics.DiagnosticsReceiver [requires android.permission.DUMP]
- receiver com.owncloud.android.files.BootupBroadcastReceiver
- provider com.owncloud.android.providers.DiskLruImageCacheFileProvider [requires android.permission.MANAGE_DOCUMENTS]
- provider com.owncloud.android.providers.DocumentsStorageProvider [requires android.permission.MANAGE_DOCUMENTS]
- provider com.owncloud.android.providers.FileContentProvider

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
