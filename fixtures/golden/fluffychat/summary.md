# chat.fluffy.fluffychat 2.11.0 (3570)
Extracted by sextant <version> · schema 7 · sha256 9d6fcc5f9b0a9b0e7dbb2d255c36f9dc2afff4aa83a380809605f30b27b111fa

## App
- Label: FluffyChat
- min SDK 24 · target SDK 36 · compile SDK 37
- debuggable: not set · allow_backup: no · cleartext traffic: not set
- Launcher: chat.fluffy.fluffychat.MainActivity
- Signing: v2, v3 · cert sha256 05:7A:C4:51:64:A2:E6:E3:1F:6D:DE:55:5A:5F:2C:CC:B4:D8:83:4C:B1:85:25:49:68:1B:00:E3:46:68:CA:F3
- Icon: res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml fg res/CH.png bg res/6Y.png
- Layout: 3 dex · ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

## Code
- 9957 classes · 65086 methods · 29986 fields · 0 Kotlin classes (0%)
- 168739 call edges (226842 call sites) · 17349 distinct string constants
- Biggest packages: org.webrtc (446), I0 (201), Z (169), k3 (161), J0 (144), Y0 (134), androidx.datastore.preferences.protobuf (130), q (129)

## Insights
- Frameworks: Flutter
- SDKs (6): WebRTC, ExoPlayer / Media3, OkHttp, UnifiedPush, Jackson, Material Components
- Libraries with exact versions: 64 · built with AGP 8.11.1
- Endpoints: 62 URLs on 41 hosts; top: github.com (7), developer.android.com (5), dashif.org (4), www.w3.org (3), aomedia.org (2), apache.org (2)
- Native: 35 libraries, 964 JNI methods · Locales: 88

## Review findings (static observations to check, not proven issues)
- [low] Components exported without permission (4) — e.g. com.linusu.flutter_web_auth_2.CallbackActivity
- [low] Weak cipher or ECB mode (3) — e.g. Lj4/k;->j()Ljavax/crypto/Cipher;
- [low] Weak hash algorithm (1) — e.g. Landroidx/media3/exoplayer/rtsp/c;->c(Landroidx/media3/exoplayer/rtsp/h$a;Landroid/net/Uri;I)Ljava/lang/String;
- [info] Legacy external storage requested (1) — e.g. application requestLegacyExternalStorage=true
- [info] WebView JavaScript enabled (1) — e.g. Lio/flutter/plugins/urllauncher/WebViewActivity;->onCreate(Landroid/os/Bundle;)V

## Counts
- Permissions: 45 requested (7 dangerous), 2 declared
- Components: 8 activity, 0 activity-alias, 9 service, 8 receiver, 3 provider
- Exported: 10 (2 with deep links)
- Features: 0 required, 7 optional · libraries: androidx.window.extensions, androidx.window.sidecar
- Queries (package visibility): 0 packages, 7 intents, 0 providers · Metadata: 9 entries

## Exported components
- activity chat.fluffy.fluffychat.MainActivity — https://matrix.to, im.fluffychat://chat, matrix:
- activity com.hiennv.flutter_callkit_incoming.CallkitIncomingActivity [requires chat.fluffy.fluffychat.PERMISSION_CALL]
- activity com.linusu.flutter_web_auth_2.CallbackActivity — im.fluffychat.auth:/login
- service com.hiennv.flutter_callkit_incoming.CallkitConnectionService [requires android.permission.BIND_TELECOM_CONNECTION_SERVICE]
- service com.hiennv.flutter_callkit_incoming.CallkitNotificationService [requires chat.fluffy.fluffychat.PERMISSION_CALL]
- service org.unifiedpush.android.connector.internal.RaiseToForegroundService
- receiver androidx.profileinstaller.ProfileInstallReceiver [requires android.permission.DUMP]
- receiver com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver [requires chat.fluffy.fluffychat.PERMISSION_CALL]
- receiver com.pravera.flutter_foreground_task.service.RebootReceiver
- receiver org.unifiedpush.android.connector.internal.MessagingReceiverImpl

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
