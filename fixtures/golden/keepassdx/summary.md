# com.kunzisoft.keepass.libre 4.5.6 (45600)
Extracted by sextant <version> · schema 7 · sha256 0c52db962ec434eacae760ba8a9538c56e192005f358aac3e60d776fcb05c1ee

## App
- Label: KeePassDX
- min SDK 19 · target SDK 36 · compile SDK 36
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: com.kunzisoft.keepass.activities.FileDatabaseSelectActivity
- Signing: v1, v2, v3 · cert sha256 C8:10:AA:B7:EF:A4:D5:AD:3F:C2:94:1F:DF:5F:99:B8:8A:3B:73:F8:62:19:71:56:27:ED:B4:DA:BA:C5:41:A4
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW1.xml fg res/qx.png bg res/M0.png
- Layout: 3 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 18277 classes · 120999 methods · 84485 fields · 5020 Kotlin classes (27%)
- 305687 call edges (412519 call sites) · 21991 distinct string constants
- Biggest packages: org.bouncycastle.jcajce.provider.symmetric (563), com.kunzisoft.keepass.activities (328), kotlinx.coroutines.flow (296), com.kunzisoft.keepass.viewmodels (295), org.bouncycastle.jcajce.provider.digest (268), androidx.appcompat.widget (246), androidx.core.app (221), com.kunzisoft.keepass.activities.fragments (219)

## Insights
- Frameworks: Kotlin
- SDKs (4): Bouncy Castle, Room, Kotlin Coroutines, Material Components
- Libraries with exact versions: 51 · built with AGP 8.13.2
- Endpoints: 87 URLs on 11 hosts; top: github.com (10), www.keepassdx.com (3), schemas.android.com (2), android.googlesource.com (1), f-droid.org (1), gitlab.com (1)
- Native: 8 libraries, 28 JNI methods · Locales: 94

## Review findings (static observations to check, not proven issues)
- [high] TrustManager may accept every certificate (1) — e.g. Lorg/bouncycastle/est/jcajce/JcaJceUtils$1;->checkServerTrusted([Ljava/security/cert/X509Certificate;Ljava/lang/String;)V
- [low] Components exported without permission (4) — e.g. com.kunzisoft.keepass.activities.MainCredentialActivity
- [low] SecureRandom with explicit seed (7) — e.g. Lorg/bouncycastle/crypto/examples/DESExample;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V
- [low] Weak cipher or ECB mode (7) — e.g. Landroidx/biometric/CryptoObjectUtils;->createFakeCryptoObject()Landroidx/biometric/BiometricPrompt$CryptoObject;
- [low] Weak hash algorithm (3) — e.g. Lcom/kunzisoft/keepass/database/element/binary/BinaryData$BinaryCountingOutputStream;-><init>(Lcom/kunzisoft/keepass/database/element/binary/BinaryData;Ljava/io/OutputStream;)V
- [info] App data can be backed up (1) — e.g. application allowBackup=true

## Counts
- Permissions: 9 requested (1 dangerous), 1 declared
- Components: 20 activity, 0 activity-alias, 9 service, 2 receiver, 1 provider
- Exported: 9 (2 with deep links)
- Features: 0 required, 0 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 1 packages, 6 intents, 0 providers · Metadata: 10 entries

## Exported components
- activity com.kunzisoft.keepass.activities.FileDatabaseSelectActivity
- activity com.kunzisoft.keepass.activities.MainCredentialActivity — content://*.*, content://*.*\\.*, content://*.*\\..*\\.*
- activity com.kunzisoft.keepass.credentialprovider.activity.EntrySelectionLauncherActivity — otpauth://hotp, otpauth://totp
- activity com.kunzisoft.keepass.settings.MagikeyboardSettingsActivity
- service com.kunzisoft.keepass.credentialprovider.autofill.KeeAutofillService [requires android.permission.BIND_AUTOFILL_SERVICE]
- service com.kunzisoft.keepass.credentialprovider.magikeyboard.MagikeyboardService [requires android.permission.BIND_INPUT_METHOD]
- service com.kunzisoft.keepass.credentialprovider.passkey.PasskeyProviderService [requires android.permission.BIND_CREDENTIAL_PROVIDER_SERVICE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver com.kunzisoft.keepass.receivers.DexModeReceiver

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
