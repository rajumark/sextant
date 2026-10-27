# com.x8bit.bitwarden 2024.10.0 (11270)
Extracted by sextant <version> · schema 8 · sha256 a6616c0c1c2c111b340071175b0bfe22c9df00718c902a688f66573ec920c788

## App
- Label: Bitwarden
- min SDK 21 · target SDK 34 · compile SDK 34
- debuggable: no · allow_backup: no · cleartext traffic: not set
- Launcher: com.x8bit.bitwarden.MainActivity
- Signing: v1, v2, v3 · cert sha256 24:E0:6C:04:C2:08:04:8F:19:F1:C9:93:B4:DD:A4:43:0E:A8:B0:6D:B8:37:5E:A0:E3:7B:83:46:96:B9:AC:3A
- Icon: res/mipmap-xxxhdpi-v4/ic_launcher.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/mipmap-anydpi-v26/ic_launcher.xml fg res/drawable/ic_launcher_foreground.xml bg #175ddc
- Layout: 2 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 13941 classes · 94728 methods · 66702 fields · 23 Kotlin classes (0%)
- 218582 call edges (276542 call sites) · 14688 distinct string constants
- Biggest packages: com.google.crypto.tink.proto (443), com.google.crypto.tink.shaded.protobuf (365), kotlinx.coroutines.flow (296), androidx.core.view (279), androidx.appcompat.widget (257), androidx.core.app (233), com.google.android.material.color.utilities (220), androidx.recyclerview.widget (203)

## Insights
- Frameworks: .NET (Xamarin / MAUI), Kotlin
- SDKs (11): Firebase Analytics, Microsoft App Center, Google Sign-In, Tink, Glide, Firebase Core, Google Play Services Base, Firebase Cloud Messaging, Kotlin Coroutines, Gson, Material Components
- Libraries with exact versions: 70
- Endpoints: 32 URLs on 29 hosts; top: ns.adobe.com (2), schemas.android.com (2), www.googleapis.com (2), android.googlesource.com (1), bitwarden-c2b35.firebaseio.com (1), cloud.google.com (1)
- Native: 644 libraries, 40 JNI methods · Locales: 86

## Review findings (static observations to check, not proven issues)
- [low] Components exported without permission (3) — e.g. crc641b016c64ce400e42.WebAuthCallbackActivity
- [low] Weak cipher or ECB mode (3) — e.g. Landroidx/biometric/CryptoObjectUtils;->createFakeCryptoObject()Landroidx/biometric/BiometricPrompt$CryptoObject;
- [low] Weak hash algorithm (2) — e.g. Lcom/google/firebase/installations/local/IidStore;->getIdFromPublicKey(Ljava/security/PublicKey;)Ljava/lang/String;
- [info] Google API key (2) — e.g. @string/google_api_key

## Counts
- Permissions: 15 requested (4 dangerous), 1 declared
- Components: 8 activity, 0 activity-alias, 11 service, 9 receiver, 5 provider
- Exported: 12 (1 with deep links)
- Features: 0 required, 2 optional · libraries: androidx.window.extensions, androidx.window.sidecar, org.apache.http.legacy
- Queries (package visibility): 0 packages, 3 intents, 0 providers · Metadata: 19 entries

## Exported components
- activity com.x8bit.bitwarden.MainActivity
- activity crc641b016c64ce400e42.WebAuthCallbackActivity — bitwarden:
- activity crc6468b6408a11370c2f.WebAuthenticatorIntermediateActivity
- activity crc64ba438d8f48cf7e75.IntermediateActivity
- service com.x8bit.bitwarden.Accessibility.AccessibilityService [requires android.permission.BIND_ACCESSIBILITY_SERVICE]
- service com.x8bit.bitwarden.Autofill.AutofillService [requires android.permission.BIND_AUTOFILL_SERVICE]
- service com.x8bit.bitwarden.Autofill.CredentialProviderService [requires android.permission.BIND_CREDENTIAL_PROVIDER_SERVICE]
- service com.x8bit.bitwarden.AutofillTileService [requires android.permission.BIND_QUICK_SETTINGS_TILE]
- service com.x8bit.bitwarden.GeneratorTileService [requires android.permission.BIND_QUICK_SETTINGS_TILE]
- service com.x8bit.bitwarden.MyVaultTileService [requires android.permission.BIND_QUICK_SETTINGS_TILE]
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver com.google.firebase.iid.FirebaseInstanceIdReceiver [requires com.google.android.c2dm.permission.SEND]

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
