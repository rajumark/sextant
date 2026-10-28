# org.joinmastodon.android 2.13.4 (191)
Extracted by sextant <version> · schema 8 · sha256 ce1a8222a1f9a59cdde0816a64a39692123c3bc05f64966abae5b0ea2dfe709e

## App
- Label: Mastodon
- min SDK 23 · target SDK 37 · compile SDK 37
- debuggable: not set · allow_backup: yes · cleartext traffic: not set
- Launcher: org.joinmastodon.android.MainActivity
- Signing: v1, v2, v3 · cert sha256 83:CB:2C:D2:BD:D5:10:31:04:4C:D2:76:85:23:24:B2:E9:6F:BE:3D:87:29:8D:F7:B6:79:4C:9D:EA:34:55:04
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/CH.png bg res/6Y.png
- Layout: 2 dex · ABIs: none

## Code
- 2514 classes · 13411 methods · 7981 fields · 0 Kotlin classes (0%)
- 48046 call edges (69299 call sites) · 6400 distinct string constants
- Obfuscation: heavy · 82% short class names · source files stripped (100%)
- Biggest packages: j$.util.stream (274), org.joinmastodon.android.model (169), k1 (110), n2 (110), f (80), j$.util (71), w1 (70), org.joinmastodon.android.api (64)

## Insights
- Frameworks: none detected
- SDKs (4): ZXing, OkHttp, Google Play Services Base, Gson
- Libraries with exact versions: 0 · built with AGP 8.13.2
- Endpoints: 18 URLs on 9 hosts; top: sponsor.joinmastodon.org (4), example.com (2), joinmastodon.org (2), mastodon.online (2), mastodon.social (2), api.joinmastodon.org (1)
- Native: 0 libraries, 0 JNI methods · Locales: 136

## Review findings (static observations to check, not proven issues)
- [low] Components exported without permission (2) — e.g. org.joinmastodon.android.ExternalShareActivity
- [low] Weak cipher or ECB mode (1) — e.g. Lorg/joinmastodon/android/api/n0;->b([B[B[B)Lorg/joinmastodon/android/model/PushNotification;
- [low] Weak hash algorithm (1) — e.g. Lk0/a;-><clinit>()V
- [info] App data can be backed up (1) — e.g. application allowBackup=true
- [info] WebView JavaScript enabled (2) — e.g. Lk1/f3;->i0(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Landroid/os/Bundle;)Landroid/view/View;

## Counts
- Permissions: 10 requested (3 dangerous), 1 declared
- Components: 4 activity, 0 activity-alias, 2 service, 2 receiver, 1 provider
- Exported: 5 (3 with deep links)
- Features: 0 required, 0 optional · libraries: none
- Queries (package visibility): 0 packages, 4 intents, 0 providers · Metadata: 2 entries

## Exported components
- activity org.joinmastodon.android.ExternalShareActivity — mastodon://share
- activity org.joinmastodon.android.MainActivity — https://mastodon.online/@*, https://mastodon.online/collections/*, https://mastodon.social/@*
- activity org.joinmastodon.android.OAuthActivity — org.joinmastodon.android-auth://callback
- receiver org.joinmastodon.android.PushNotificationReceiver [requires com.google.android.c2dm.permission.SEND]
- receiver org.joinmastodon.android.api.PushSubscriptionManager$RegistrationReceiver [requires com.google.android.c2dm.permission.SEND]

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
