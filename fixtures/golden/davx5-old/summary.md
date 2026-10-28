# at.bitfire.davdroid 4.5.20-ose (405200005)
Extracted by sextant <version> · schema 8 · sha256 df387d8b3ddc3730d88d2bfea939aadb8906cc6cff7de46385a7751d1b536462

## App
- Label: DAVx⁵
- min SDK 24 · target SDK 36 · compile SDK 37
- debuggable: not set · allow_backup: no · cleartext traffic: not set
- Launcher: at.bitfire.davdroid.ui.AccountsActivity
- Signing: v2, v3 · cert sha256 8B:48:E6:76:A6:86:49:67:79:17:83:C1:D8:D0:FC:0D:D6:D2:CE:33:E1:FB:36:78:77:93:EC:D0:86:D4:47:07
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/Qr.xml bg #7cb342
- Layout: 2 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 9227 classes · 44431 methods · 30432 fields · 0 Kotlin classes (0%)
- 154799 call edges (248228 call sites) · 16671 distinct string constants
- Obfuscation: none · 2% short class names · 49 in the default package · source files stripped (100%)
- Biggest packages: org.conscrypt (441), androidx.compose.material3 (301), net.fortuna.ical4j.model.property (165), org.xbill.DNS (157), net.fortuna.ical4j.model (149), androidx.compose.foundation.gestures (126), at.bitfire.davdroid.ui (125), androidx.appcompat.widget (112)

## Insights
- Frameworks: Jetpack Compose
- SDKs (12): AppAuth, WorkManager, Conscrypt, Tink, Room, Dagger / Hilt, Ktor, OkHttp, UnifiedPush, Kotlin Coroutines, kotlinx.serialization, Jetpack Compose
- Libraries with exact versions: 109 · built with AGP 9.4.0
- Endpoints: 65 URLs on 36 hosts; top: www.slf4j.org (9), schema.org (5), javax.xml.xmlconstants (4), json.schemastore.org (4), issuetracker.google.com (3), www.davx5.com (3)
- Native: 8 libraries, 0 JNI methods · Locales: 90

## Review findings (static observations to check, not proven issues)
- [low] Runs external commands (1) — e.g. Lat/bitfire/dav4jvm/ktor/Response$$ExternalSyntheticLambda0;->invoke()Ljava/lang/Object;
- [low] Components exported without permission (16) — e.g. at.bitfire.cert4android.TrustCertificateActivity
- [low] SecureRandom with explicit seed (1) — e.g. Lio/ktor/util/NonceKt$nonceGeneratorJob$1;->invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
- [low] Weak cipher or ECB mode (1) — e.g. Lorg/conscrypt/CryptoUpcalls;->rsaOpWithPrivateKey(Ljava/security/PrivateKey;II[B)[B
- [info] Custom task affinity or task reparenting (1) — e.g. androidx.glance.appwidget.action.InvisibleActionTrampolineActivity
- [info] Exported provider grants URI permissions for any path (1) — e.g. at.bitfire.davdroid.webdav.DavDocumentsProvider

## Counts
- Permissions: 25 requested (8 dangerous), 1 declared
- Components: 23 activity, 0 activity-alias, 14 service, 12 receiver, 3 provider
- Exported: 23 (2 with deep links)
- Features: 0 required, 1 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 5 packages, 8 intents, 2 providers · Metadata: 15 entries

## Exported components
- activity at.bitfire.cert4android.TrustCertificateActivity
- activity at.bitfire.davdroid.ui.AccountsActivity
- activity at.bitfire.davdroid.ui.AppSettingsActivity
- activity at.bitfire.davdroid.ui.account.AccountActivity
- activity at.bitfire.davdroid.ui.setup.LoginActivity — caldav:, caldavs:, carddav:
- activity at.bitfire.davdroid.ui.webdav.WebdavMountsActivity
- activity net.openid.appauth.RedirectUriReceiverActivity — at.bitfire.davdroid:/oauth2/redirect
- service androidx.glance.appwidget.GlanceRemoteViewsService [requires android.permission.BIND_REMOTEVIEWS]
- service androidx.work.impl.background.systemjob.SystemJobService [requires android.permission.BIND_JOB_SERVICE]
- service at.bitfire.davdroid.sync.account.AddressBookAuthenticatorService
- service at.bitfire.davdroid.sync.adapter.CalendarsSyncAdapterService
- service at.bitfire.davdroid.sync.adapter.ContactsSyncAdapterService
- service at.bitfire.davdroid.sync.adapter.JtxSyncAdapterService
- service at.bitfire.davdroid.sync.adapter.OpenTasksSyncAdapterService
- service at.bitfire.davdroid.sync.adapter.TasksOrgSyncAdapterService
- service org.unifiedpush.android.connector.internal.RaiseToForegroundService
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver androidx.work.impl.diagnostics.DiagnosticsReceiver [requires android.permission.DUMP]
- receiver at.bitfire.davdroid.ui.widget.IconSyncButtonWidgetReceiver
- receiver at.bitfire.davdroid.ui.widget.LabeledSyncButtonWidgetReceiver
- receiver org.unifiedpush.android.connector.internal.MessagingReceiverImpl
- receiver org.unifiedpush.android.embedded_fcm_distributor.impl.FirebaseReceiver [requires com.google.android.c2dm.permission.SEND]
- provider at.bitfire.davdroid.webdav.DavDocumentsProvider [requires android.permission.MANAGE_DOCUMENTS]

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
