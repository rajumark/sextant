# Review findings

`sextant findings` (and `insights/findings.jsonl`) report **static observations to review**, not proven vulnerabilities. Each finding lists the evidence: a manifest entry, a method, a string or a resource. At most 25 evidence items are kept per finding; `count` is exact.

| Severity | Meaning |
|---|---|
| `high` | Usually a real problem in a release build; check first |
| `medium` | Worth checking; often fine with the right context |
| `low` | Common patterns that deserve a look in a focused review |
| `info` | Context, not a problem by itself |

## Manifest

| ID | Severity | Fires when |
|---|---|---|
| `debuggable` | high | `android:debuggable="true"` |
| `test-only` | medium | `android:testOnly="true"` (debug/test build; only installable with `adb install -t`) |
| `cleartext` | medium | `android:usesCleartextTraffic="true"` |
| `cleartext-default` | low | Cleartext is allowed by default: targetSdk < 28, no network security config, attribute not set |
| `exported-provider` | medium | An exported, enabled provider with no `permission`, `readPermission` or `writePermission` |
| `exported-provider-partial` | low | An exported provider with only one of `readPermission` / `writePermission` (the other direction is open) |
| `exported-component` | low | Exported, enabled activities/services/receivers without a permission (launcher activities excluded) |
| `uri-grants-any-path` | info | An exported provider with `grantUriPermissions="true"` and no `<grant-uri-permission>` elements |
| `task-affinity` | info | Activities with a `taskAffinity` other than the package, or `allowTaskReparenting="true"` |
| `shared-user-id` | low | `android:sharedUserId` is set (deprecated; shares process and data with other apps) |
| `profileable-shell` | low | `<profileable android:shell="true">` |
| `allow-backup` | info | `android:allowBackup="true"` |
| `legacy-storage` | info | `requestLegacyExternalStorage="true"` |

## Code

| ID | Severity | Fires when |
|---|---|---|
| `trust-all-certificates` | high | An `X509TrustManager.checkServerTrusted` implementation has an empty body |
| `accept-all-hostnames` | medium | A `HostnameVerifier.verify` implementation has at most two instructions (likely `return true`) |
| `webview-js-bridge` | medium | `WebView.addJavascriptInterface` is called |
| `webview-file-access` | medium | `WebSettings.setAllowFileAccessFromFileURLs` is called |
| `dynamic-code` | medium | `DexClassLoader` / `InMemoryDexClassLoader` is constructed |
| `weak-cipher` | low | A method loads `DES`, `RC4`, `Blowfish`, an `…/ECB/…` transformation or bare `"AES"`, and calls `Cipher.getInstance` |
| `weak-hash` | low | A method loads `MD5`, `MD2`, `MD4` or `SHA-1` and calls `MessageDigest.getInstance` |
| `exec` | low | `Runtime.exec` or `ProcessBuilder.start` is called |
| `seeded-random` | low | `SecureRandom.setSeed` is called |
| `webview-javascript` | info | `WebSettings.setJavaScriptEnabled` is called |

## Secrets

Found in code string constants and string resources. Values are masked (first 6 and last 4 characters).

| ID | Severity | Pattern |
|---|---|---|
| `secret-private-key` | high | A PEM private-key header followed by base64 key material (a bare header, as used by parsers, is ignored) |
| `secret-aws-key` | high | `AKIA` + 16 characters |
| `secret-stripe-live` | high | `sk_live_…` |
| `secret-slack-token` | high | `xox[baprs]-…` |
| `secret-github-token` | high | `gh[pousr]_` + 36 characters |
| `jwt` | medium | Three base64url segments starting with `eyJ` |
| `google-api-key` | info | `AIza…` (Google keys in apps are usually public by design; check they are restricted) |

## Output

```json
{"id":"weak-hash","severity":"low","title":"Weak hash algorithm",
 "detail":"MD5/SHA-1 are not collision resistant; fine for checksums, not for security decisions.",
 "evidence":[{"kind":"method","where":"Lde/…/DecodeActivity;->C(Ljava/lang/String;[B)V","value":"SHA-1"}],"count":3}
```

In the index: tables `findings` and `finding_evidence`, plus the `finding` search kind.
