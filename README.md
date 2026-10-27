<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/sextant-dark.png">
    <img src="docs/assets/sextant-light.png" width="128" height="128" alt="Sextant app icon">
  </picture>
</p>

<h1 align="center">Sextant</h1>

Sextant is a command-line tool that turns an Android APK into clean, structured data that people, scripts and AI agents can read.

```bash
sextant extract app.apk -o app/
sextant info app/
sextant find app/ "api.example.com"
sextant class app/ LoginRepository
sextant sql app/ "SELECT caller FROM call_edges WHERE callee LIKE 'Ljavax/crypto/Cipher;->getInstance%'"
sextant callers app/ LoginRepository.login --depth 4
sextant source app.apk com.example.LoginRepository.login
sextant insights app/
sextant endpoints app/ --host example.com
sextant diff app-4.2/ app-4.3/
sextant findings app/ --severity medium
sextant xref app/ com.example.Session.token
sextant reach app/ com.example.DeepLinkActivity okhttp3/
sextant smali app.apk com.example.LoginRepository.login
sextant icon app.apk -o icon.png
```

It reads the APK's identity, manifest, permissions, components and signing information (since 0.1.0). Since 0.2.0 it also reads the full code structure from DEX bytecode: classes, methods, fields, call edges and string constants. Everything is written as deterministic JSON and JSONL files, and nothing is ever decompiled or run. Since 0.3.0 it also builds `index.sqlite`, so you can search and query all of it. Since 0.5.0 it also reports what the app is built with (frameworks, SDKs, exact library versions), every URL it contains, its resource values and its native code.

## Install

Sextant needs Java 17 or newer.

Download `sextant-<version>.zip` from the releases page, unzip it, and add `sextant-<version>/bin` to your `PATH`.

The desktop app has its own installers on the same releases page: `.dmg` for macOS (Apple silicon), `.msi` for Windows and `.deb` for Linux. They are not signed yet, so macOS asks you to allow the app once in System Settings → Privacy & Security.

To build from source:

```bash
./gradlew :cli:installShadowDist
cli/build/install/sextant/bin/sextant --help
```

## Usage

```text
sextant extract <app.apk> [-o <dir>] [--force] [--quiet] [--no-code] [--no-index]
sextant info    <dir | app.apk> [--json]
sextant find    <dir> <text> [--kind class|method|field|string|component|permission|deep_link] [--limit N] [--json]
sextant class   <dir> <name> [--json]
sextant sql     <dir> "<query>" [--json | --csv] [--limit N]
sextant sql     <dir> --schema
sextant callers <dir> <method> [--depth N] [--fan-out N] [--json]
sextant callees <dir> <method> [--depth N] [--fan-out N] [--json]
sextant source  <app.apk> <class | method>
sextant insights  <dir> [--json]
sextant endpoints <dir> [--host TEXT] [--limit N] [--json]
sextant diff    <old-dir> <new-dir> [--show N] [--json]
sextant findings <dir> [--severity info|low|medium|high] [--json]
sextant xref    <dir> <class | field> [--limit N] [--json]
sextant reach   <dir> <from> <to> [--depth N] [--paths N] [--json]
sextant smali   <app.apk> <class | method>
sextant icon    <app.apk | dir> [--round] [--layer foreground|background|monochrome] [-o file] [--json]
sextant mcp     [--dir <dir>] [--apk <app.apk>]
sextant --version
```

| Command | What it does |
|---|---|
| `extract` | Parses the APK and writes the output folder (default `./<apk-name>.sextant/`). Refuses a non-empty folder unless `--force` is given. Even with `--force`, only Sextant's own files are replaced. `--no-code` skips the `code/` files for a fast manifest-only run. |
| `info` | Prints a short summary of an extracted folder, or of an APK directly (in memory, nothing written). `--json` prints one JSON object to stdout. |
| `find` | Substring search over class, method and field names, string constants, components, permissions and deep links. For strings it shows the methods that load them. |
| `class` | Shows one class: access, superclass, interfaces, subclasses, fields with constant values, methods with call counts, callers from outside the class, and its strings. It accepts `com.app.Login`, `Lcom/app/Login;` or just `Login`. |
| `callers` / `callees` | Shows who calls a method, or what it calls, as a tree. The method can be `Lcom/a/B;->m(I)V`, `Lcom/a/B;->m`, `com.a.B.m`, `B.m` or just `m`, and overloads are shown together. External methods (framework, libraries) are marked, and so are cycles. `--depth` (default 3) and `--fan-out` (default 15) keep trees readable. |
| `source` | Prints decompiled Java (jadx) for one class or method, on demand. Names stay as in the bytecode, so they match the index. This takes a few seconds on large apps. |
| `insights` | Shows frameworks, SDKs by category, top hosts, exact library versions and native libraries in one view. |
| `endpoints` | Lists every URL with its sources (method, resource, deep link, manifest value, asset or native file). `--host` filters by host. |
| `diff` | Compares two extracted versions: app facts, permissions, components, deep links, frameworks, SDKs, library versions, hosts, URLs, native libraries, JNI methods, packages, classes, string constants and resources. Totals are exact; `--show` sets how many items are listed. |
| `findings` | Review points with evidence: debuggable, cleartext traffic, backups, exported components and providers without permission, WebView JavaScript, JS bridges and file access, dynamic code loading, external commands, weak hashes and ciphers, trust-all TLS, accept-all hostnames, seeded SecureRandom, and embedded secrets (masked). These are static observations to check, not proven vulnerabilities. |
| `xref` | Shows where a class or field is used: instantiations, casts, `instanceof`, `const-class`, calls into its methods, and field reads and writes. |
| `reach` | Shows call paths from an entry point (component, class or method) to targets (`Cipher.getInstance`, `okhttp3/`, a descriptor prefix). Virtual and interface calls dispatch into subclass overrides (class-hierarchy analysis); signatures with more than 25 implementations, such as `Runnable.run`, are not expanded. You get one shortest path per target method. |
| `smali` | Prints the bytecode assembly (baksmali) of a class or method. It works where decompiling fails. `#@54` offsets are hex code units, matching the decimal `offsets` in the code files. |
| `icon` | Finds the app icon. It lists every file behind `android:icon` (or `--round` for `android:roundIcon`) per density, with format, pixel size and byte size, decodes adaptive icons into their foreground, background and monochrome layers, marks the best file, and `-o` saves it. It works on an APK directly (fast: manifest and resources only) or on an extracted folder. |
| `sql` | Runs read-only SQL over `index.sqlite`. It prints a table by default (`--json` gives one object per line, `--csv` gives CSV) and stops at 1,000 rows unless you pass `--limit`. `--schema` prints every table and view. |

Progress and errors go to **stderr**, data goes to **stdout**, so `--json` output is always safe to pipe.

### Exit codes

| Code | Meaning |
|---|---|
| 0 | Success |
| 1 | Usage error |
| 2 | Input error (missing file, not an APK, undecodable manifest) |
| 3 | Output error (non-empty folder without `--force`, not writable) |
| 4 | Internal error (a bug, so please report it) |

## Output

```text
app/
  summary.md          ← read this first (short, written for agents)
  meta.json           ← identity, SDK levels, flags, signing, layout, warnings
  manifest.json       ← the full decoded manifest as a tree
  permissions.jsonl   ← one permission per line
  components.jsonl    ← one component per line
  features.jsonl      ← uses-feature / uses-library / uses-native-library
  metadata.jsonl      ← meta-data and property (application and components)
  queries.jsonl       ← <queries>: packages, intents, providers the app can see
  code/
    classes.jsonl     ← one class per line
    methods.jsonl     ← one method per line
    fields.jsonl      ← one field per line, with constant initial values
    calls.jsonl       ← caller → callee edges with bytecode offsets
    strings.jsonl     ← string constants and the methods that load them
    field_refs.jsonl  ← field reads/writes per method
    type_refs.jsonl   ← new / const-class / check-cast / instance-of / arrays per method
  insights/
    frameworks.jsonl  ← Flutter, React Native/Hermes, Expo, .NET MAUI, Capacitor, Unity, Godot, libGDX, SDL, Compose, …
    sdks.jsonl        ← known SDKs by category, with class and manifest evidence
    libraries.jsonl   ← exact Maven coordinates + versions from META-INF/*.version
    endpoints.jsonl   ← every URL and where it was found
    findings.jsonl    ← review points with evidence
  resources/
    values.jsonl      ← default strings, bools, integers, colors, dimens
  native/
    libraries.jsonl   ← .so files per ABI: size, sha256, exported symbols, JNI methods
    symbols.jsonl     ← exported functions; JNI symbols mapped back to Java class + method
  index.sqlite        ← all of the above in SQLite, with full-text search
```

Rules for the output:

- **Deterministic.** The same APK always gives byte-identical files: sorted records, fixed key order, no timestamps, `\n` line endings.
- **Truthful.** A missing manifest attribute is `null`, never a guess. Derived values say so. For example, `exported_source` is `explicit` or `default`, and implied permissions carry `implied_by`.
- **Self-contained lines.** Every JSONL line has full names, so a single `grep` hit can be understood without joins.

`components.jsonl`:

```json
{"type":"activity","name":"org.example.DeepLinkActivity","exported":true,"exported_source":"default","enabled":true,"permission":null,"process":null,"launcher":false,"target_activity":null,"authorities":[],"intent_filters":[{"actions":["android.intent.action.VIEW"],"categories":["android.intent.category.BROWSABLE","android.intent.category.DEFAULT"],"data":[{"scheme":"https","host":"example.org","port":null,"path":null,"path_prefix":"/join","path_pattern":null,"path_suffix":null,"path_advanced_pattern":null,"mime_type":null}],"priority":0,"auto_verify":true}]}
```

`permissions.jsonl`. The `kind` field is `uses-permission`, `uses-permission-sdk-23`, `implied` (granted by Android without being listed, the same rules as aapt2) or `declared` (defined by the app):

```json
{"name":"android.permission.CAMERA","kind":"uses-permission","max_sdk":null,"dangerous":true,"protection_level":null,"custom":false,"implied_by":null}
{"name":"android.permission.READ_EXTERNAL_STORAGE","kind":"implied","max_sdk":28,"dangerous":true,"protection_level":null,"custom":false,"implied_by":"android.permission.WRITE_EXTERNAL_STORAGE"}
```

### Manifest coverage

Every manifest element is kept in `manifest.json` (the full tree), and the parts people query are also structured:

| Area | Where |
|---|---|
| Identity: package, versions, SDKs, `installLocation`, `sharedUserId`, `targetSandboxVersion` | `meta.json` (`flags` holds the identity extras) |
| `<application>` flags: backup (`allowBackup`, `fullBackupContent`, `dataExtractionRules`, `backupAgent`), `debuggable`, `testOnly`, cleartext and network config, legacy storage, `largeHeap`, `memtagMode`, `localeConfig`, `appCategory`, `profileable`, … | `meta.json → flags` |
| Permissions (requested, sdk-23, implied, declared) | `permissions.jsonl` |
| Components, including provider `readPermission`/`writePermission`, `grantUriPermissions`, `path-permission`, plus `launchMode`, `taskAffinity`, `foregroundServiceType`, `directBootAware` and `isolatedProcess` | `components.jsonl` |
| Intent filters, including `ssp*` patterns, `label` and `order` | inside `components.jsonl` |
| `uses-feature` (with the OpenGL ES version), `uses-library`, `uses-native-library` | `features.jsonl` |
| `meta-data` and `property` | `metadata.jsonl` |
| `<queries>` | `queries.jsonl` |
| `supports-screens` | `meta.json → flags.supports_screens` |

Each of these also has a table in `index.sqlite`. Optional features and required/optional libraries are cross-checked against aapt2 on every corpus APK.

### Code files

Code is read straight from `classesN.dex` with dexlib2, so it works even where Java decompilation fails (obfuscated or packed apps). IDs are smali descriptors, the format every Android tool understands:

| Kind | Example ID |
|---|---|
| class | `Lcom/app/Login;` |
| method | `Lcom/app/Login;->auth(Ljava/lang/String;)Z` |
| field | `Lcom/app/Login;->token:Ljava/lang/String;` |

```json
{"caller":"Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V","callee":"Landroid/app/Activity;->getIntent()Landroid/content/Intent;","kind":"virtual","count":1,"offsets":[84]}
{"value":"https://markusfisch.de/BinaryEye","method":"Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V","count":1,"offsets":[124]}
```

- `calls.jsonl` has one line per distinct (caller, callee, kind). `kind` is `virtual`, `static`, `direct`, `super`, `interface` or `polymorphic`. `offsets` are in 16-bit code units, as in `dexdump`, and at most 32 are kept (`count` is always exact).
- When a class is defined in several dex files, the first one wins, as on Android. The count is in `meta.json → code.duplicate_classes`.
- Unpaired UTF-16 surrogates in DEX strings are written as `\uXXXX` escapes, so the files stay valid UTF-8.
- The files are large for big apps (Signal: 41k classes, 778k call edges, about 470 MB), because every line is self-contained. Version 3's SQLite index is the compact form.

### Insights

Every insight carries the evidence it is based on:

- **Frameworks** come from fingerprints in files, classes and the manifest. For example, Bitwarden is detected as `.NET (Xamarin / MAUI)` from `libmonodroid.so`, `libmonosgen-2.0.so`, the assembly blobs and 306 `mono.android` classes.
- **SDKs** come from an editable rules file (`core/src/main/resources/io/github/rajumark/sextant/sdks.json`, about 110 SDKs). A rule matches by package prefix, which counts classes, and by manifest entries such as component names and meta-data, which survive obfuscation.
- **Libraries** come from the `META-INF/<group>_<artifact>.version` files that AndroidX and many libraries ship. These give exact coordinates (Signal: 141 libraries, built with AGP 9.4.0).
- **Endpoints** come from code strings (with the method that uses each one), string resources, deep links, manifest meta-data, text assets (JS bundles) and native libraries. Binary files pack strings back to back, so URLs found there are reported only as `scheme://host`.
- **Native symbols** come from parsing ELF `.dynsym` tables. Exported `Java_…` functions are decoded back to the Java class and method they implement (Signal: 4,496 JNI methods).

### App icons

Icons are resolved from the manifest through `resources.arsc` to the real files, so they are found even when R8 resource shrinking renames them (Signal's launcher icon is `res/o-.png`, not `res/mipmap-xxxhdpi/ic_launcher.png`):

```text
$ sextant icon signal.apk
icon  @mipmap/ic_launcher
  anydpi-v26     xml   -               548 B  res/BW.xml
  mdpi           png   48×48          2141 B  res/9w.png
  …
  xxxhdpi        png   192×192       11631 B  res/o-.png  ← best
adaptive icon  res/BW.xml
  background  @drawable/ic_launcher_background  res/0w1.xml
  foreground  @drawable/ic_launcher_foreground  res/Qr.xml
  monochrome  @drawable/ic_launcher_monochrome  res/Vy.xml
```

- The best file is the largest bitmap by pixel area. When the icon is only a vector drawable, the vector is reported instead (vectors are compiled binary XML, so `-o` refuses to save them as an image; try `--layer foreground`).
- Layers wrapped in `<inset>` or `<bitmap>` are followed, and solid-color layers report their color (Bitwarden: `#175ddc`).
- The data is also in `meta.json → icons`, in an `icons` table in the index, in `summary.md`, in `info`, and in an MCP `icon` tool.
- Every per-density icon file aapt2 picks (`application-icon-*`) is checked against these results for every APK in the corpus.

### Review findings

Findings are deliberately worded as things to check:

- Calls are matched together with the strings loaded in the same method. `weak-cipher` fires when a method loads `"AES/ECB/…"` (or `DES`, `RC4`, or bare `"AES"`, which defaults to ECB) and also calls `Cipher.getInstance`.
- `trust-all-certificates` fires when a `X509TrustManager.checkServerTrusted` implementation has an empty body, and `accept-all-hostnames` fires when a `HostnameVerifier.verify` has at most two instructions.
- Secrets (AWS keys, PEM private keys with key material, Stripe/Slack/GitHub tokens, JWTs, Google API keys) are masked in the output: only the first 6 and last 4 characters are shown.

On the corpus these point at real patterns. For example, AppManager's wireless-ADB pairing uses a trust-all TrustManager on purpose (self-signed certificates), and Signal uses `AES/ECB/NoPadding` for single-block operations. Context decides.

### Version diff

`sextant diff old/ new/` compares two extractions by stable names. Example, Signal 8.31.2 → 8.32.3 (both real releases):

```text
## components  (+3 −3 ~0)
  + activity org.thoughtcrime.securesms.mediaviewer.MediaViewerActivity = exported=0(explicit) …
## packages (classes)  (+11 −4 ~168)
  + org.signal.mediaviewer = 48
## resources  (+53 −17 ~0)
  + string/AssistantScreenSecurityBottomSheet__title = Screen security is enabled
```

R8-obfuscated class names can change between builds, so renamed classes show up as removed plus added. Package counts and resources are the more stable signals.

### index.sqlite

The index is a normalized SQLite database with the same facts, plus full-text search. Run `sextant sql <dir> --schema` for the full schema.

| Table / view | Contents |
|---|---|
| `meta` | package, versions, SDKs, sha256, and the whole `meta.json` as `meta_json` |
| `permissions`, `components`, `deep_links` | manifest-level facts |
| `classes`, `fields` | definitions from the APK |
| `methods` | **every** method reference: `defined = 1` for methods in the APK, 0 for external ones (framework, other libraries) |
| `calls`, `string_refs` | edges by integer id (compact) |
| `call_edges`, `string_uses` | the same edges with readable descriptors (views) |
| `libraries`, `frameworks`, `sdks` | what the app is built with |
| `endpoints`, `endpoint_sources` | URLs and where each was found |
| `resources` | default resource values |
| `native_libraries`, `native_symbols` | native code, with JNI symbols mapped to Java |
| `search` | FTS5 trigram index over names, strings, resources, endpoints, SDKs, libraries and native symbols, where any 3+ character fragment matches |

Example queries:

```sql
-- Who calls Cipher.getInstance?
SELECT caller, count FROM call_edges WHERE callee LIKE 'Ljavax/crypto/Cipher;->getInstance%';

-- Exported components that need no permission
SELECT type, name FROM components WHERE exported = 1 AND enabled = 1 AND permission IS NULL;

-- Methods that load a URL
SELECT value, method FROM string_uses WHERE value LIKE 'https://%';

-- Biggest packages
SELECT package, count(*) AS n FROM classes GROUP BY package ORDER BY n DESC LIMIT 10;

-- Which Java methods are implemented in native code?
SELECT jni_class, jni_method, library FROM native_symbols WHERE jni_class IS NOT NULL;

-- Which hosts does the code (not just resources) refer to?
SELECT e.host, count(*) FROM endpoints e JOIN endpoint_sources s ON s.endpoint = e.id WHERE s.kind = 'code' GROUP BY e.host;
```

On size and speed: for Signal (41k classes, 778k call edges) the index is 355 MB, smaller than the 468 MB of JSONL. Typical apps extract in 1–5 s. The largest corpus apps (Element: 66k classes, 856k search rows) take 15–20 s, mostly spent building the full-text index. `--no-index` skips the index entirely.

`meta.json` carries `schema_version`. It changes only when the format of the output files changes, independently of the tool version.

## For AI agents

### MCP server

`sextant mcp` runs a [Model Context Protocol](https://modelcontextprotocol.io) server on stdin/stdout, so an agent can analyze APKs itself. Tools: `extract`, `summary`, `find`, `class`, `callers`, `callees`, `sql`, `source`, `insights` and `endpoints`. After `extract`, the other tools work on that app without paths.

```bash
# Claude Code
claude mcp add sextant -- sextant mcp
```

```json
// Any MCP client (e.g. a JSON config with "mcpServers")
{ "mcpServers": { "sextant": { "command": "sextant", "args": ["mcp"] } } }
```

The agent can then be asked questions such as "extract ~/Downloads/app.apk and tell me which exported components reach network code".

### Files and commands

1. Read `summary.md` (under 60 lines) for an overview.
2. `grep` the JSONL files for details: `grep '"exported":true' components.jsonl`.
3. Follow code: `grep 'api.example.com' code/strings.jsonl` gives the method; `grep '"caller":"<method id>"' code/calls.jsonl` gives what it calls.
4. Or ask the index: `sextant find <dir> <text> --json`, `sextant class <dir> <name> --json`, `sextant sql <dir> "<query>" --json`.
5. For an overview without extracting: `sextant info app.apk --json`.

## How it is tested

Every test APK is a real app downloaded from F-Droid, GitHub releases or the developer's own site. None are built by us. The 39-app corpus (2.7 GB) covers Java (tiny to huge), Kotlin and Compose, Flutter, React Native and Expo, .NET MAUI, Capacitor web wrappers, libGDX, Godot, SDL and C++ game engines, emulators, R8-obfuscated official builds, autofill and sync-adapter apps, and sizes from 0.2 MB to 189 MB. Each APK is pinned by URL and sha256 in `fixtures/external.lock`.

```bash
scripts/fetch-apks.sh        # download + verify the corpus (1.2 GB, not committed)
./gradlew build              # unit, golden-file, determinism, bad-input and CLI end-to-end tests
scripts/sweep.sh             # run the CLI over every APK with timings (one at a time, trims each output; --keep keeps all)
scripts/compare-aapt2.sh     # cross-check package, versions, SDKs, label, permissions against aapt2
scripts/compare-dexdump.sh   # cross-check class and member counts against dexdump
./gradlew updateGolden       # regenerate fixtures/golden after an intended output change
```

Corpus runs are built to fit in little disk space. APKs are processed one at a time, each output is deleted (or trimmed, in the sweep) as soon as it has been checked, and golden tests hash code records in memory instead of writing them. A full run needs about one large APK's worth of space (≤ 2 GB). Both the sweep and the tests stop with a clear message when free space drops below `SEXTANT_MIN_FREE_GB` (5 GB for the sweep, 3 GB for tests).

## Limitations

- Input is a single `.apk`. Split bundles (`.apks`, `.xapk`, `.aab`) are not supported yet.
- `exported` follows Android's static rules. Runtime behaviour (dynamic receivers, permission checks in code) is not visible here.
- v4 signatures live in a separate `.idsig` file and are not read.
- Only root `classesN.dex` files are read. Dex files hidden in assets or loaded at runtime are not.
- Call edges are static. A call through an interface or superclass is recorded against that base method, so to find callers of an override, also ask about the base method.

Only analyze apps you own, apps with open-source licenses, or apps you have permission to examine.

## License

Apache-2.0
