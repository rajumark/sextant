# Security review

`sextant findings` lists **review points**: static observations a reviewer should check, each with the evidence that triggered it. They are deliberately not called vulnerabilities, because a check may exist elsewhere and some patterns are fine in context.

```bash
sextant findings app.sqlite
sextant findings app.sqlite --severity medium   # medium and high only
sextant findings app.sqlite --json              # one JSON object per finding, with evidence
```

```text
[medium] Cleartext HTTP allowed  (1)  · cleartext
    android:usesCleartextTraffic=true allows unencrypted HTTP.
    - manifest application usesCleartextTraffic=true
[low] Components exported without permission  (6)  · exported-component
    Other apps can start these activities, services or receivers; check how they handle incoming intents.
    - manifest de.markusfisch.android.binaryeye.activity.CameraActivity  = activity
    ...
[low] Weak hash algorithm  (3)  · weak-hash
    MD5/SHA-1 are not collision resistant; fine for checksums, not for security decisions.
    - method Lde/markusfisch/android/binaryeye/activity/DecodeActivity;->C(Ljava/lang/String;[B)V  = SHA-1
```

The full list of rules, with what each one means, is in [Review findings](../reference/findings.md).

## A review workflow

1. **Attack surface.** Exported components without permission, deep links, providers and their read/write permissions:

    ```bash
    sextant sql app.sqlite "SELECT type, name, permission, read_permission, write_permission FROM components WHERE exported = 1 AND enabled = 1"
    sextant sql app.sqlite "SELECT url, component FROM deep_links"
    ```

2. **What those entry points can reach.** Use `reach` from an exported component to sensitive APIs:

    ```bash
    sextant reach app.sqlite com.example.DeepLinkActivity 'WebView.loadUrl'
    sextant reach app.sqlite com.example.ShareReceiver okhttp3/
    ```

3. **Network.** Cleartext settings, the network security config, every host the app mentions:

    ```bash
    sextant endpoints app.sqlite
    sextant sql app.sqlite "SELECT value FROM meta WHERE key = 'meta_json'" --json   # flags.uses_cleartext_traffic, network_security_config
    ```

4. **Crypto and TLS.** `weak-cipher`, `weak-hash`, `trust-all-certificates` and `accept-all-hostnames` point at the methods; read them with `source` or `smali`.

5. **Secrets.** Embedded keys and tokens are masked in the output (only the first 6 and last 4 characters are shown). Search for more with `find`:

    ```bash
    sextant find app.sqlite "api_key" --kind resource
    sextant find app.sqlite "BEGIN"  --kind string
    ```

6. **Native code.** JNI methods map back to the Java side:

    ```bash
    sextant sql app.sqlite "SELECT jni_class, jni_method, library FROM native_symbols WHERE jni_class IS NOT NULL"
    ```

## Signing

Every signer of every scheme, every certificate field and the verifier's own warnings are rows:

```bash
sextant q signing app.apk                       # scheme, SDK range, subject, key, expiry, fingerprint per signer
sextant sql app.apk "SELECT level, scope, issue FROM signing_issues"
sextant sql app.apk "SELECT seq, certificate FROM signing_lineage"   # key rotation, oldest first
```

A v3 signer with `min_sdk` above the app's own minimum means older devices verify a different (older) key; an empty `signing_lineage` means no key rotation.

## How the code rules work

- **Correlated, not keyword-only.** `weak-cipher` fires only when a method both loads a cipher string (`"AES/ECB/…"`, `"DES"`, bare `"AES"`, which defaults to ECB) and calls `Cipher.getInstance`. The same applies to `weak-hash` with `MessageDigest.getInstance`.
- **Body size, not names.** `trust-all-certificates` fires when an `X509TrustManager.checkServerTrusted` implementation is empty; `accept-all-hostnames` when a `HostnameVerifier.verify` has at most two instructions.
- **Manifest semantics.** A provider counts as protected by `android:permission`, or by `readPermission` and `writePermission` together; a provider with only one of them gets `exported-provider-partial`. Launcher activities are not reported as exported components.

!!! example "On real apps"
    Across the 46-APK test corpus, every high-severity finding was checked by hand. Examples: Logseq's `SslSkipPlugin` and an `InsecureTrustManager` bundled into Standard Notes accept every certificate; AppManager's wireless-ADB pairing does so on purpose (self-signed certificates); VLC ships a test key inside Netty's `OpenSsl` class.

## In CI: fail the build and upload to code scanning

`--fail-on <severity>` makes `findings` exit with code **5** when any finding is at or above that severity, so a release pipeline can stop on, say, a debuggable build or a trust-all TrustManager. The normal output is still printed.

```bash
sextant extract -q app-release.apk -o build/app.sqlite
sextant findings build/app.sqlite --fail-on high
```

`--sarif` prints the findings as [SARIF 2.1.0](https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html), the format GitHub code scanning, GitLab and most security dashboards import. Each finding is one result with its severity mapped to a SARIF level (`high` → error, `medium` → warning, `low`/`info` → note) and to GitHub's `security-severity`. An APK has no source files, so each evidence item points at the part of the APK it lives in (`AndroidManifest.xml`, `resources.arsc`, or the class as `smali/com/example/Foo.smali`), with the exact component, method or resource as a logical location.

```yaml title=".github/workflows/apk-review.yml"
name: APK review
on: [push]
permissions:
  contents: read
  security-events: write
jobs:
  sextant:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - run: ./gradlew assembleRelease
      - name: Install Sextant
        run: |
          curl -fsSL -o sextant.zip https://github.com/rajumark/sextant/releases/latest/download/sextant-1.0.0.zip
          unzip -q sextant.zip && echo "$PWD/sextant-1.0.0/bin" >> "$GITHUB_PATH"
      - run: sextant extract -q app/build/outputs/apk/release/app-release.apk -o build/app.sqlite
      - run: sextant findings build/app.sqlite --sarif > sextant.sarif
      - uses: github/codeql-action/upload-sarif@v3
        with: { sarif_file: sextant.sarif, category: sextant }
      - run: sextant findings build/app.sqlite --fail-on high
```

Combine with `--severity` to keep low-severity notes out of the dashboard: `sextant findings build/app.sqlite --severity medium --sarif`.
