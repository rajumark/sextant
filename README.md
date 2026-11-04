<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/sextant-dark.png">
    <img src="docs/assets/sextant-light.png" width="128" height="128" alt="Sextant app icon">
  </picture>
</p>

<h1 align="center">Sextant</h1>

Sextant is a command-line tool that turns an Android APK into one SQLite file of clean, structured data that people, scripts and AI agents can query. APK in, SQLite out.

**[Documentation](https://rajumark.github.io/sextant/cli/)** · [Install](https://rajumark.github.io/sextant/cli/getting-started/installation/) · [Commands](https://rajumark.github.io/sextant/cli/reference/commands/) · [Releases](https://github.com/rajumark/sextant/releases)

```bash
sextant extract app.apk                 # writes app.sqlite
sextant info app.sqlite
sextant find app.sqlite "api.example.com"
sextant class app.sqlite LoginRepository
sextant sql app.sqlite "SELECT caller FROM call_edges WHERE callee LIKE 'Ljavax/crypto/Cipher;->getInstance%'"
sextant callers app.sqlite LoginRepository.login --depth 4
sextant source app.apk com.example.LoginRepository.login
sextant insights app.sqlite
sextant endpoints app.sqlite --host example.com
sextant diff app-4.2.apk app-4.3.apk
sextant findings app.sqlite --severity medium
sextant findings app.sqlite --sarif > sextant.sarif   # GitHub code scanning; --fail-on high for CI
sextant xref app.sqlite com.example.Session.token
sextant reach app.sqlite com.example.DeepLinkActivity okhttp3/
sextant smali app.apk com.example.LoginRepository.login
sextant icon app.apk -o icon.png
sextant coverage app.sqlite
sextant files app.sqlite --status listed --by-size
```

Every command also takes the APK itself: it is indexed once into a cache and reused (`sextant info app.apk`).

It reads the APK's identity, manifest, permissions, components and signing information (since 0.1.0). Since 0.2.0 it also reads the full code structure from DEX bytecode: classes, methods, fields, call edges and string constants. `extract` never decompiles or runs anything (`source` and `grep` decompile on demand). Since 0.3.0 everything is searchable and queryable in SQLite, and now one SQLite file is the whole output. Since 0.5.0 it also reports what the app is built with (frameworks, SDKs, exact library versions), every URL it contains, its resource values and its native code.

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
sextant extract <app.apk> [-o <app.sqlite>] [--force] [--quiet] [--no-code] [--no-content] [--no-instructions]
sextant info    <app.apk | app.sqlite> [--json]
sextant find    <app.apk | app.sqlite> <text> [--kind class|method|field|string|component|permission|deep_link|resource|endpoint|sdk|library|native_symbol|finding|string_resource|native_string] [--limit N] [--json]
sextant class   <app.apk | app.sqlite> <name> [--json]
sextant sql     <app.apk | app.sqlite> "<query>" [--json | --csv] [--limit N]
sextant sql     <app.apk | app.sqlite> --schema
sextant callers <app.apk | app.sqlite> <method> [--depth N] [--fan-out N] [--json]
sextant callees <app.apk | app.sqlite> <method> [--depth N] [--fan-out N] [--json]
sextant source  <app.apk | app.sqlite> <class | method> [--mapping mapping.txt]
sextant grep    <app.apk | app.sqlite> <text> [--regex] [--case] [--package P] [--limit N] [--mapping mapping.txt] [--json] [--no-cache]
sextant insights  <app.apk | app.sqlite> [--json]
sextant endpoints <app.apk | app.sqlite> [--host TEXT] [--limit N] [--json]
sextant diff    <old.apk | old.sqlite> <new.apk | new.sqlite> [--show N] [--json]
sextant findings <app.apk | app.sqlite> [--severity info|low|medium|high] [--json | --sarif] [--fail-on SEVERITY]
sextant xref    <app.apk | app.sqlite> <class | field> [--limit N] [--json]
sextant reach   <app.apk | app.sqlite> <from> <to> [--depth N] [--paths N] [--json]
sextant smali   <app.apk | app.sqlite> <class | method>
sextant components <app.apk | app.sqlite> [--type T] [--tag TAG] [--exported] [--search TEXT] [--json]
sextant component  <app.apk | app.sqlite> <name> [--json]
sextant app     <app.apk | app.sqlite> [--json]
sextant res     <app.apk | app.sqlite> <kind> [<name>] [--search TEXT] [--flag F] [--config C] [--limit N] [--json]
sextant sdks    <app.apk | app.sqlite>... [--category C] [--sdk S] [--below VERSION] [--json]
sextant deobf   <mapping.txt> <name> [--limit N]
sextant icon    <app.apk | app.sqlite> [--round] [--layer foreground|background|monochrome] [-o file] [--json]
sextant xml     <app.apk | app.sqlite> [<file | @resource>] [--tag TAG] [--attr NAME] [--value TEXT] [--limit N] [--json]
sextant links   <app.apk | app.sqlite> <thing> [--depth N] [--rel REL] [--limit N] [--json]
sextant q       [<query> [<app.apk | app.sqlite>]] [--arg NAME=VALUE] [--json | --csv] [--limit N]
sextant files   <app.apk | app.sqlite> [--type TYPE] [--status decoded|listed] [--search TEXT] [--by-size] [--limit N] [--json] [--grep TEXT] [--save PATH -o FILE]
sextant coverage <app.apk | app.sqlite> [--json]
sextant cache   [--clear] [--json]
sextant mcp     [--dir <app.sqlite | app.apk>] [--apk <app.apk>]
sextant --version
```

Every command takes either the `.sqlite` file from `extract` or the APK itself. Given an APK, Sextant indexes it once into a cache keyed by the APK's SHA-256 and every later command on the same APK reads that index at once. `sextant cache` lists the cached indexes and `--clear` deletes them; `SEXTANT_CACHE` moves the folder. A new Sextant version rebuilds an index on next use.

| Command | What it does |
|---|---|
| `extract` | Parses the APK in one pass and writes one SQLite file (default `./<apk-name>.sqlite`). Refuses to overwrite an existing file unless `--force` is given. `--no-code` skips the code tables (classes, methods, calls, instructions) for a faster, smaller file; `--no-content` stores the bytes of the dex files only instead of every file; `--no-instructions` leaves out the per-instruction rows (about a quarter of a large app's file). Split bundles (`.apks`, `.xapk`, `.apkm`) work like a plain APK. |
| `info` | Prints a short summary of an app (APK or `.sqlite`). `--json` prints one JSON object to stdout. |
| `find` | Substring search over class, method and field names, string constants, components, permissions, deep links, resources, endpoints, SDKs, libraries, native symbols and strings, and findings. For strings it shows the methods that load them. |
| `class` | Shows one class: access, superclass, interfaces, subclasses, fields with constant values, methods with call counts, callers from outside the class, and its strings. It accepts `com.app.Login`, `Lcom/app/Login;` or just `Login`. |
| `callers` / `callees` | Shows who calls a method, or what it calls, as a tree. The method can be `Lcom/a/B;->m(I)V`, `Lcom/a/B;->m`, `com.a.B.m`, `B.m` or just `m`, and overloads are shown together. External methods (framework, libraries) are marked, and so are cycles. `--depth` (default 3) and `--fan-out` (default 15) keep trees readable. |
| `source` | Prints decompiled Java (jadx) for one class or method, on demand. Names stay as in the bytecode, so they match the index. This takes a few seconds on large apps. |
| `insights` | Shows frameworks, SDKs by category, top hosts, exact library versions and native libraries in one view. |
| `endpoints` | Lists every URL with its sources (method, resource, deep link, manifest value, asset or native file). `--host` filters by host. |
| `diff` | Compares two versions (APKs or `.sqlite` files): app facts, permissions, components, deep links, frameworks, SDKs, library versions, hosts, URLs, native libraries, JNI methods, packages, classes, string constants and resources. Totals are exact; `--show` sets how many items are listed. |
| `findings` | Review points with evidence: debuggable, cleartext traffic, backups, exported components and providers without permission, WebView JavaScript, JS bridges and file access, dynamic code loading, external commands, weak hashes and ciphers, trust-all TLS, accept-all hostnames, seeded SecureRandom, and embedded secrets (masked). These are static observations to check, not proven vulnerabilities. |
| `xref` | Shows where a class or field is used: instantiations, casts, `instanceof`, `const-class`, calls into its methods, and field reads and writes. |
| `reach` | Shows call paths from an entry point (component, class or method) to targets (`Cipher.getInstance`, `okhttp3/`, a descriptor prefix). Virtual and interface calls dispatch into subclass overrides (class-hierarchy analysis); signatures with more than 25 implementations, such as `Runnable.run`, are not expanded. You get one shortest path per target method. |
| `smali` | Prints the bytecode assembly (baksmali) of a class or method. It works where decompiling fails. `#@54` offsets are hex code units, matching the decimal `calls.offsets` and `instructions.offset`. |
| `grep` | Searches decompiled Java for text or a regular expression and prints each matching line with its class, method and line number. The first search decompiles the app once and caches the code locally; `--package` limits it to one package on big apps and `--mapping` shows original names. |
| `components` / `component` | Lists activities, aliases, services, receivers and providers with verdicts (launcher, app-link, deep-link, hidden-entry, unguarded, …), filterable by type, tag and exported; `component` explains one: verdicts with reasons, attributes, intent filters in plain words, deep links and ready-to-run adb commands. |
| `app` | Shows the Application class and the startup order: AppComponentFactory, attachBaseContext, content providers (androidx.startup initializers), onCreate, with the calls of each step and the SDKs they start. |
| `res` | Lists strings (every locale, plurals, arrays), string keys with flags (used, unused, untranslated, url, …), resource ids, styles, drawables, layouts, menus, navigation graphs, assets, XML configs and references; a name shows one entry with what uses it. |
| `sdks` | Shows SDKs and libraries by role (analytics, ads, crash reporting, …) with versions; across several apps, which SDKs appear where, filtered by `--category`, `--sdk` and `--below`. |
| `deobf` | Looks up names in a ProGuard/R8 `mapping.txt`, both ways. |
| `icon` | Finds the app icon. It lists every file behind `android:icon` (or `--round` for `android:roundIcon`) per density, with format, pixel size and byte size, decodes adaptive icons into their foreground, background and monochrome layers, marks the best file, and `-o` saves it. It works on an APK directly (from the cache when indexed, else manifest and resources only) or on a `.sqlite` file. |
| `xml` | Prints any XML file of the APK (manifest, layouts, menus, drawables, colors, animations, …) rebuilt from the `xml_nodes` and `xml_attrs` tables, by path (`res/BW.xml`) or resource (`@layout/main`, every configuration). With `--tag`, `--attr` and `--value` it finds elements across all files instead: `--tag EditText --attr android:inputType --value Password` lists every password field. `--tag` also matches class names that end in it (`TextInputEditText`). |
| `links` | Shows what anything links to and what links to it, across kinds: `sextant links app.apk .MainActivity` gives the component's class, permissions, deep links and resources; `--depth 2` continues into the class's superclass and methods. A resource shows the files and methods that use it (`@string/app_name`), a menu shows the code that inflates it. The thing can be a key (`@string/x`, `class:L…;`, `method:…`, `permission:…`, `url:…`) or a component, class or permission name. |
| `q` | Runs a saved query for a common question: `unused-strings`, `untranslated-strings`, `string-users` (`--arg name=app_name`), `password-fields`, `activity-layouts`, `exported-components`, `dangerous-permissions`, `signing`, `webviews`, `hosts`, `jni-methods`, `native-libraries`, `native-sensitive-imports`, `service-providers`, `hidden-code`, `kotlin-classes`, `annotated` (`--arg type=…`), `broad-catches`, `fonts`, `largest-images`, `literal-users` (`--arg value=…`), `opcode-stats`, `biggest-packages`, `undecoded-files`. `sextant q` lists them with what each answers; `sextant q <query>` prints its SQL to adapt. |
| `files` | Lists every file inside the APK from the index: type, size, content (dex, elf, png, binary-xml, json, …), the resource it is (shrunk apps rename `res/` files, so `res/BW.xml` shows as `@mipmap/ic_launcher`) and the decoder that read it. `--status listed` shows files no decoder reads yet; `--by-size` puts the largest first. `--save <path> -o <file>` writes one file out from the bytes stored in the `.sqlite` file. `--grep <text>` prints matching lines from the text files (assets, configs, licenses). |
| `coverage` | Shows how much of the APK was decoded into rows: files and bytes per type, and the largest files that are only listed (size and hash). Every zip entry is always listed. |
| `cache` | Lists the indexes built for APKs given directly to a command (app, version, size, state), or deletes them with `--clear`. |
| `sql` | Runs read-only SQL over the app's `.sqlite` file (or the APK's cached index). It prints a table by default (`--json` gives one object per line, `--csv` gives CSV) and stops at 1,000 rows unless you pass `--limit`. `--schema` prints every table and view. |

Progress and errors go to **stderr**, data goes to **stdout**, so `--json` output is always safe to pipe.

### Exit codes

| Code | Meaning |
|---|---|
| 0 | Success |
| 1 | Usage error |
| 2 | Input error (missing file, not an APK, undecodable manifest) |
| 3 | Output error (output file exists without `--force`, is a folder, or is not writable) |
| 4 | Internal error (a bug, so please report it) |
| 5 | `findings --fail-on` found a finding at or above the threshold (for CI) |

## Output: one SQLite file

`sextant extract app.apk` writes `app.sqlite` and nothing else. It holds everything Sextant reads from the APK, in two forms:

- **Tables you query**: one table per kind of fact, normalized, with indexes and a full-text search table. `sextant sql app.sqlite --schema` prints all of them.
- **Full records**: the `records` table keeps every manifest, resource and insight record exactly as Sextant built it, so every command can rebuild the whole picture from this file alone. A test checks, on real apps, that what is read back equals what was written.

| Tables | Contents |
|---|---|
| `meta` | package, versions, SDKs, sha256, label, and the full app metadata (signing, flags, icons, counts) as `meta_json` |
| `permissions`, `components`, `deep_links`, `features`, `metadata`, `queries` | the manifest; the full decoded tree is in `records` (`kind = 'manifest'`) |
| `string_resources`, `resource_ids`, `resources`, `styles`, `drawables`, `layouts`, `menus`, `navigation_destinations`, `assets`, `xml_configs`, `resource_refs`, `icons` | resources, every locale, and who refers to what: layouts, menus, styles, the manifest and code |
| `classes`, `methods`, `fields`, `calls`, `strings`, `string_refs`, `field_refs`, `type_refs` | code structure from DEX bytecode, edges by integer id |
| `call_edges`, `string_uses`, `search_items` | the same edges and search hits with readable names (views) |
| `libraries`, `frameworks`, `sdks`, `endpoints`, `endpoint_sources`, `findings`, `finding_evidence` | insights, each with its evidence |
| `native_libraries`, `native_symbols` | native code, with JNI symbols mapped to Java |
| `instructions`, `opcodes`, `instruction_view` | every instruction of every method in code order: opcode, registers, literal, branch target and what it references |
| `annotations`, `annotation_values`, `method_lines`, `method_params`, `exception_handlers`, `kotlin_classes` | code details: every annotation with its values, each method's source line table, parameter names, exception handlers, and Kotlin metadata |
| `archive_entries`, `hidden_classes` | every entry of a zip, jar or APK inside the APK, and every class of dex code outside the app's own `classesN.dex`: code it can load at run time |
| `images`, `fonts` | every image (PNG, WebP, JPEG, GIF, anywhere) with its pixel size, and every font with its family, style and version |
| `signing_signers`, `signing_certificates`, `signing_digests`, `signing_lineage`, `signing_issues`, `signing_manifest` | signing as rows: every signer per scheme (with v3 SDK ranges), every certificate field and its bytes, rotation lineage, verifier issues, MANIFEST.MF digests |
| `file_text`, `file_properties`, `services` | the text of every text, JSON and textual XML file (assets, configs, licenses, …) with a full-text index; `.properties` files key by key; service-loader registrations |
| `elf_files`, `elf_sections`, `elf_needed`, `elf_symbols`, `elf_strings` | every ELF file in depth: architecture, soname, build id, sections, the libraries it loads, every imported and exported symbol, and its readable strings |
| `xml_nodes`, `xml_attrs` | every binary XML file (manifest, layouts, menus, drawables, colors, animations, …) node by node and attribute by attribute, with `ref` for values that point at a resource |
| `resource_values`, `resource_bag_items` | `resources.arsc` in every configuration: values and file paths, and the items of styles, plurals, arrays and attrs |
| `files` | every file inside the APK: type, size, hashes, content, resource name and the decoder that read it |
| `file_contents` | the uncompressed bytes of every file (dex files always), so the file holds the whole APK and no command needs it |
| `links`, `entities` | every edge between kinds of things (component → class, layout → resource, method → string, …) with readable keys and evidence; `entities` lists every key with its kind |
| `records` | full records (manifest tree, permissions, components, resources, insights) |
| `schema_docs` | what every table, view and column means; start here: `SELECT * FROM schema_docs WHERE "table" = 'links'` |
| `search` | FTS5 trigram index over names, strings, resources, endpoints, SDKs, libraries and native symbols |

Rules for the output:

- **Deterministic.** The same APK always gives the same rows: sorted records, stable ids, no timestamps.
- **Truthful.** A missing manifest attribute is `NULL`, never a guess. Derived values say so. For example, `exported_source` is `explicit` or `default`, and implied permissions carry `implied_by`.
- **Readable ids.** Code ids are smali descriptors, the format every Android tool understands:

| Kind | Example ID |
|---|---|
| class | `Lcom/app/Login;` |
| method | `Lcom/app/Login;->auth(Ljava/lang/String;)Z` |
| field | `Lcom/app/Login;->token:Ljava/lang/String;` |

Example queries:

```sql
-- Who calls Cipher.getInstance?
SELECT caller, count FROM call_edges WHERE callee LIKE 'Ljavax/crypto/Cipher;->getInstance%';

-- Exported components that need no permission
SELECT type, name FROM components WHERE exported = 1 AND enabled = 1 AND permission IS NULL;

-- Which layouts use the string app_name?
SELECT "from" FROM resource_refs WHERE ref = '@string/app_name' AND from_kind = 'layout';

-- Which methods show the string app_name, and with which call?
SELECT "from" AS method, attribute AS call FROM resource_refs WHERE ref = '@string/app_name' AND from_kind = 'code';

-- Methods that load a URL
SELECT value, method FROM string_uses WHERE value LIKE 'https://%';

-- Which Java methods are implemented in native code?
SELECT jni_class, jni_method, library FROM native_symbols WHERE jni_class IS NOT NULL;

-- Every password field in every layout
SELECT n.path, n.tag FROM xml_nodes n JOIN xml_attrs a ON a.node = n.id
WHERE a.name = 'android:inputType' AND a.value LIKE '%Password%';

-- Everything a screen pulls in, two steps out: its class, and the resources its layouts use
SELECT l2.dst FROM links l1 JOIN links l2 ON l2.src = l1.dst
WHERE l1.src = 'component:com.example.MainActivity';

-- Which files did no decoder read?
SELECT type, path, size_bytes FROM files WHERE decoded_by IS NULL ORDER BY size_bytes DESC;
```

Notes on code:

- Code is read straight from `classesN.dex` with dexlib2, so it works even where Java decompilation fails (obfuscated or packed apps).
- `calls` has one row per distinct (caller, callee, kind). `kind` is `virtual`, `static`, `direct`, `super`, `interface` or `polymorphic`. `offsets` are in 16-bit code units, as in `dexdump`, and at most 32 are kept (`count` is always exact).
- When a class is defined in several dex files, the first one wins, as on Android.
- `methods` holds **every** method reference: `defined = 1` for methods in the APK, 0 for external ones (framework, other libraries).

On size and speed: typical apps extract in 2–8 s into 50–250 MB. The largest corpus apps (50k+ classes) take 20–30 s and write about 1 GB; `--no-instructions` and `--no-content` make the file a third smaller.

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
- The data is also in `meta_json → icons` and the `icons` table, in `info`, and in an MCP `icon` tool.
- Every per-density icon file aapt2 picks (`application-icon-*`) is checked against these results for every APK in the corpus.

### Review findings

Findings are deliberately worded as things to check:

- Calls are matched together with the strings loaded in the same method. `weak-cipher` fires when a method loads `"AES/ECB/…"` (or `DES`, `RC4`, or bare `"AES"`, which defaults to ECB) and also calls `Cipher.getInstance`.
- `trust-all-certificates` fires when a `X509TrustManager.checkServerTrusted` implementation has an empty body, and `accept-all-hostnames` fires when a `HostnameVerifier.verify` has at most two instructions.
- Secrets (AWS keys, PEM private keys with key material, Stripe/Slack/GitHub tokens, JWTs, Google API keys) are masked in the output: only the first 6 and last 4 characters are shown.

On the corpus these point at real patterns. For example, AppManager's wireless-ADB pairing uses a trust-all TrustManager on purpose (self-signed certificates), and Signal uses `AES/ECB/NoPadding` for single-block operations. Context decides.

### Version diff

`sextant diff old.apk new.apk` (or two `.sqlite` files) compares two versions by stable names. Example, Signal 8.31.2 → 8.32.3 (both real releases):

```text
## components  (+3 −3 ~0)
  + activity org.thoughtcrime.securesms.mediaviewer.MediaViewerActivity = exported=0(explicit) …
## packages (classes)  (+11 −4 ~168)
  + org.signal.mediaviewer = 48
## resources  (+53 −17 ~0)
  + string/AssistantScreenSecurityBottomSheet__title = Screen security is enabled
```

R8-obfuscated class names can change between builds, so renamed classes show up as removed plus added. Package counts and resources are the more stable signals.

## For AI agents

### MCP server

`sextant mcp` runs a [Model Context Protocol](https://modelcontextprotocol.io) server on stdin/stdout, so an agent can analyze APKs itself. Tools: `extract`, `summary`, `find`, `class`, `callers`, `callees`, `links`, `sql`, `source`, `grep`, `insights`, `xref`, `reach`, `smali`, `icon`, `findings` and `endpoints`. After `extract`, the other tools work on that app without paths.

```bash
# Claude Code
claude mcp add sextant -- sextant mcp
```

```json
// Any MCP client (e.g. a JSON config with "mcpServers")
{ "mcpServers": { "sextant": { "command": "sextant", "args": ["mcp"] } } }
```

The agent can then be asked questions such as "extract ~/Downloads/app.apk and tell me which exported components reach network code".

### Commands

1. `sextant info app.apk --json` for an overview (the APK is indexed once, then cached).
2. Ask the index: `sextant find app.apk <text> --json`, `sextant class app.apk <name> --json`, `sextant sql app.apk "<query>" --json`.
3. `sextant sql app.apk --schema` lists every table and view; one SQLite file holds everything, so any SQL tool works on it too.

## How it is tested

Every test APK is a real app downloaded from F-Droid, GitHub releases or the developer's own site. None are built by us. The corpus of 39 apps plus 7 older releases (46 APKs, 3.3 GB) covers Java (tiny to huge), Kotlin and Compose, Flutter, React Native and Expo, .NET MAUI, Capacitor web wrappers, libGDX, Godot, SDL and C++ game engines, emulators, R8-obfuscated official builds, autofill and sync-adapter apps, and sizes from 0.2 MB to 275 MB. Each APK is pinned by URL and sha256 in `fixtures/external.lock`.

```bash
scripts/fetch-apks.sh        # download + verify the corpus (3.3 GB, not committed)
./gradlew build              # unit, golden-file, determinism, bad-input and CLI end-to-end tests
scripts/sweep.sh             # run the CLI over every APK with timings (one at a time, keeps a small SQLite file per app; --keep keeps the full ones)
scripts/compare-aapt2.sh     # cross-check package, versions, SDKs, label, permissions against aapt2
scripts/compare-dexdump.sh   # cross-check class and member counts against dexdump
./gradlew updateGolden       # regenerate fixtures/golden after an intended output change
```

Corpus runs are built to fit in little disk space. APKs are processed one at a time, each output is deleted (or, in the sweep, cut down to the few tables the cross-checks read) as soon as it has been checked, and golden tests hash code records in memory instead of writing them. A full run needs about one large APK's worth of space (≤ 2 GB). Both the sweep and the tests stop with a clear message when free space drops below `SEXTANT_MIN_FREE_GB` (5 GB for the sweep, 3 GB for tests).

## Limitations

- Split bundles (`.apks`, `.xapk`, `.apkm`) are merged into one APK; only the base's `resources.arsc` is decoded. `.aab` and encrypted `.apkm` files are not read.
- `exported` follows Android's static rules. Runtime behaviour (dynamic receivers, permission checks in code) is not visible here.
- v4 signatures live in a separate `.idsig` file and are not read.
- Call graphs and the code commands use the root `classesN.dex` files; dex code hidden in assets or archives is listed in `hidden_classes`; code downloaded at run time is not visible.
- Call edges are static. A call through an interface or superclass is recorded against that base method, so to find callers of an override, also ask about the base method.

Only analyze apps you own, apps with open-source licenses, or apps you have permission to examine.

## License

Apache-2.0
