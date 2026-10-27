# Changelog

All notable changes to Sextant. Versions follow semver; `schema_version` in `meta.json` tracks the output format separately.

## Unreleased (schema 8, index schema 7)

- **Component verdicts:** 18 tags with a reason each, the same in the CLI and the desktop app: launcher, app-link, deep-link, hidden-entry, unguarded, sensitive-role, system-role, boot, foreground, task-hijack, lock-screen, uri-grants, file-provider, isolated, direct-boot, exported-by-default, guarded, disabled.
- **`sextant components`** (filter by type, tag, exported, search) and **`sextant component`** (verdicts and reasons, attributes, entry points in plain words, deep links, App Link hosts, aliases, adb commands, the exact manifest entry).
- **`sextant app`:** the Application class and the startup order (AppComponentFactory, attachBaseContext, content providers by initOrder with androidx.startup initializers, onCreate); with an extracted folder, the calls of each step and the SDKs started at launch.
- **`sextant res`:** strings in every locale (plurals, arrays), resource ids, styles and themes, drawables, layouts, menus, navigation graphs, assets and res/xml configs, plus `string-keys` (used/unused, URL, email, key-like, format, untranslated) and `refs` (who uses which resource). References keep their `@type/name` and the resolved value. Written to `resources/*.jsonl` and new index tables (`string_resources`, `resource_ids`, `styles`, `drawables`, `layouts`, `menus`, `navigation_destinations`, `assets`, `xml_configs`, `resource_refs`).
- **res/xml summaries:** network security config (cleartext, pins, user CAs, debug overrides), backup and data-extraction rules, FileProvider paths. Assets whose bytes are DEX, ELF or zip under another name are flagged as hidden code.
- **`sextant sdks`:** SDKs and libraries by role with versions from META-INF `*.version`, Maven `pom.properties`, Play services/Firebase `.properties` and OkHttp's user agent; across many apps with `--category`, `--sdk` and `--below VERSION`.
- **Manifest coverage:** every remaining `<activity>`, `<service>`, `<provider>`, `<application>`, `<uses-permission>` and `<permission>` attribute and `<attribution>` tags are structured; a corpus test fails on any attribute that is neither structured nor deliberately raw.
- **Desktop:** Activities, Services, Receivers, Providers and Aliases rebuilt on the verdicts (summary strip, filters with counts, sort, group by package, multi-select copy, context menu, keyboard), the Components page removed, a ⌘K command palette, and new Application, SDKs & libraries and nine resource pages. Every detail pane has a Manifest entry and Search on web / Ask AI.

## 1.0.0: first public release

- **CLI 1.0.0:** everything built in 0.1.0 to 0.10.0 (see CHANGELOG.md). One `sextant-1.0.0.zip` runs on macOS, Linux and Windows with Java 17 or newer.
- **Desktop app 1.0.0:** a window for opening an APK and browsing its overview, manifest, permissions, components, signing, icons and output. Installers for macOS (`.dmg`), Windows (`.msi`) and Linux (`.deb`).
- Releases are published by pushing a commit whose message is exactly `#go`.
- **Documentation site** at https://rajumark.github.io/sextant/cli/ (MkDocs Material, source in `cli-docs/`). The command and index-schema reference pages are generated from the code by `scripts/build-cli-docs.sh`.
- **`findings --sarif`** prints SARIF 2.1.0 for GitHub code scanning, GitLab and security dashboards (validated against the OASIS schema).
- **`findings --fail-on <severity>`** exits with the new code 5 when any finding is at or above the severity, for CI gates.
- `findings --json` now prints the same records as `insights/findings.jsonl` (evidence uses `where`, as in the file).
- **Fixed:** host counts and top hosts included custom-scheme deep links (`binaryeye://scan` counted as host "scan"). They now count only network URLs (http, https, ws, wss, ftp, ftps); `endpoints` still lists everything.
- **Fixed on Windows:** output is always UTF-8, so call trees, arrows and non-Latin strings no longer turn into `?` (and `--json` stays valid).
- **Fixed:** float and double field constants printed differently on Java 17 than on Java 19+ (`Double.toString` changed), so output depended on the Java version. They now always use the shortest form, on every JDK.
- `mcp --help` lists the tools from the server itself, so it can't go stale.

## 0.10.0 (Version 10): full manifest coverage

- All `<application>` flags are now structured in `meta.json → flags` (backup, testOnly, legacy storage, largeHeap, memtag, locale config, category, profileable, …), together with the identity attributes `installLocation`, `sharedUserId`, `targetSandboxVersion` and `supports-screens`.
- New files `features.jsonl` (uses-feature with OpenGL ES version, uses-library, uses-native-library), `metadata.jsonl` (meta-data and property per scope) and `queries.jsonl` (package visibility), each with an index table.
- Components gain provider `readPermission`, `writePermission`, `grantUriPermissions`, `path-permission` and `grant-uri-permission` paths, plus `launchMode`, `taskAffinity`, `allowTaskReparenting`, `excludeFromRecents`, `screenOrientation`, `foregroundServiceType`, `isolatedProcess`, `directBootAware` and `label`. Intent filters gain `ssp*`, `label` and `order`. The components table has the new columns.
- **Fixed:** providers protected by `readPermission` + `writePermission` (without `android:permission`) were reported as "exported without permission". They are now protected; providers with only one of the two get a new `exported-provider-partial` finding.
- New findings: `test-only`, `shared-user-id`, `profileable-shell`, `legacy-storage`, `uri-grants-any-path` and `task-affinity`.
- `summary.md` and `info` show features, libraries, queries, metadata and `sharedUserId`.
- `compare-aapt2.sh` now also checks optional features and required/optional libraries (exact) and required features (subset).
- Meta schema 7, index schema 6. All new fields have defaults, so older readers keep working.
- Low-disk corpus runs. The sweep processes one APK at a time and trims each output (`--keep` keeps everything). Golden tests use a per-APK temp folder deleted right after its check, and hash code records in memory (`CodeDigestSink`, proven identical to hashing the files). Both stop early below a free-space threshold (`SEXTANT_MIN_FREE_GB`). Before this, a class-level temp folder held all 46 extractions and a desktop test leaked 77 GB, which filled the disk.
- `OutputWriter.extractTo` gains `writeCodeFiles` and `extraCodeSink`.

## 0.9.0 (Version 9): app icons

- `android:icon` and `android:roundIcon` are resolved through resources.arsc to every file per configuration, with format, pixel size (PNG and WebP headers: VP8, VP8L, VP8X) and byte size. This works with R8-shortened resource paths.
- Adaptive icons are decoded into their background, foreground and monochrome layers, following `<inset>`/`<bitmap>` wrappers and reporting solid colors.
- The best file is the largest bitmap, falling back to a vector.
- New command: `sextant icon <apk|dir>` (`--round`, `--layer`, `-o` to save, `--json`), plus an MCP `icon` tool.
- `meta.json` gains `icons` (schema 6); the index gains an `icons` table (index schema 5); `summary.md` and `info` show the icon.
- `scripts/compare-aapt2.sh` now checks that every per-density icon file aapt2 picks is among Sextant's results.

## 0.8.0 (Version 8): cross-references and reachability

- Code extraction now also records field reads and writes (`code/field_refs.jsonl`) and type uses (`code/type_refs.jsonl`: new, const-class, check-cast, instance-of, arrays), with matching index tables. Meta schema 5, index schema 4.
- `sextant xref` shows where a class or field is used.
- `sextant reach` finds call paths from an entry point to targets, using class-hierarchy dispatch into overrides. Signatures with more than 25 implementations are not expanded (found on Signal, where `Runnable.run` connected unrelated code).
- `sextant smali` prints baksmali disassembly with code offsets that match the code files.
- New MCP tools: `xref`, `reach` and `smali`.

## 0.7.0 (Version 7): review findings

- New output `insights/findings.jsonl`, plus `findings` and `finding_evidence` tables, a `findings` search kind, a `summary.md` section, the `sextant findings` command and an MCP `findings` tool.
- Manifest rules: debuggable, cleartext (explicit or default below targetSdk 28), allowBackup, exported providers and components without permission (launcher activities excluded).
- Code rules: WebView JavaScript, JS bridges, file URL access, dynamic code loading, external commands, seeded SecureRandom; weak hash and weak cipher/ECB (calls correlated with strings loaded in the same method); trust-all TrustManagers and accept-all HostnameVerifiers (by instruction count).
- Secret rules (code strings and resources): AWS keys, PEM private keys with key material, Stripe live keys, Slack and GitHub tokens, JWTs, Google API keys. Values are masked.
- `source` falls back to the enclosing class for classes jadx inlines (anonymous classes and lambdas).
- Provider authorities given as resource references (`@string/authority`) are now resolved (found on Nextcloud and VLC).
- `meta.json` schema 4 (finding counts); index schema 3.
- Releases 0.5.0, 0.6.0 and 0.7.0 were finished in one working tree and ship under the single tag v0.7.0.

## 0.6.0 (Version 6): version diff

- `sextant diff <old> <new>` compares two extractions across 15 sections (app facts, permissions, components, deep links, frameworks, SDKs, libraries, hosts, URLs, native libraries, JNI methods, package class counts, classes, string constants, resources). Text output or `--json`; totals are exact and lists are capped.
- The corpus gained 7 older releases (Binary Eye, Aurora, NewPipe, Wikipedia, DAVx5, Bluesky, Signal) as diff pairs: 46 APKs in total.
- `scripts/sweep.sh` accepts `SEXTANT_BIN`, so a frozen build can be swept while development continues.

## 0.5.0 (Version 5): insights and MCP server

- New output: `insights/frameworks.jsonl`, `insights/sdks.jsonl`, `insights/libraries.jsonl`, `insights/endpoints.jsonl`, `resources/values.jsonl`, `native/libraries.jsonl` and `native/symbols.jsonl`. Every record carries its evidence.
- Framework detection covers Flutter, React Native, Hermes, Expo, .NET (Xamarin/MAUI), Capacitor, Cordova, Unity, Godot, libGDX, SDL, Qt, Cocos2d-x, Unreal, Jetpack Compose, NativeActivity and Kotlin.
- SDK detection uses about 110 editable rules (package prefixes plus manifest evidence); prefix lookups go through a hash map.
- Exact library versions and the AGP version come from `META-INF`.
- Endpoints come from code, resources, deep links, manifest values, text assets and native libraries. URLs found in binary data are reduced to `scheme://host`.
- A small ELF reader handles 32/64-bit and both endiannesses, and JNI symbols are decoded to Java (including `_1`, `_0xxxx` escapes and overload suffixes).
- Locales and default resource values are reported.
- New commands: `sextant insights` and `sextant endpoints`. `find --kind` gains `resource`, `endpoint`, `sdk`, `library` and `native_symbol`.
- `index.sqlite` gains 8 tables; search rowids now keep the kind in 4 bits (index schema 2). `meta.json` gains an `insights` block (schema 3).
- Bugs fixed: wildcard deep-link hosts (`*.bandcamp.com`) crashed endpoint building; empty JSONL files contained a lone newline.
- `SEXTANT_DEBUG=1` prints a stack trace for internal errors.
- `sextant mcp` is a Model Context Protocol server (JSON-RPC 2.0 over stdio, no extra dependencies) with tools extract, summary, find, class, callers, callees, sql, source, insights and endpoints. A session remembers the last extracted app.
- `info` shows frameworks and insight counts.

## 0.4.0 (Version 4): navigation

- `sextant callers` and `sextant callees` show call trees with depth and fan-out limits, external and cycle markers, and `--json`. Methods resolve from full descriptors, `Class.method`, dotted names or bare names, and overloads are grouped.
- `sextant source <apk> <class|method>` prints decompiled Java via jadx (jadx-dex-input), on demand.
- The index is built on a background thread while the JSONL is written: about 20% faster on the largest apps (Element 18 → 15 s, Signal 14.5 → 11.7 s).
- Resource references that point at other references are followed (Wikipedia's label is now "Wikipedia", not `@string/app_name_prod`).
- The real-APK corpus grew from 18 to 39 apps, adding .NET MAUI, Capacitor, Expo, libGDX, Godot, SDL, C++ games, emulators, tiny legacy Java, autofill, sync adapters and very large permission and manifest sets. All 39 pass the sweep, aapt2 (39/39) and dexdump (39/39).

## 0.3.0 (Version 3): query

- `extract` builds `index.sqlite` in the same streaming pass. It is normalized: every method reference has one row in `methods` (`defined` = 1 for APK methods), and `calls` and `string_refs` use integer ids. `call_edges` and `string_uses` give the readable form.
- Full-text search uses a contentless FTS5 trigram index (substring search). Each rowid encodes the source row, and the `search_items` view shows what each rowid points at.
- New commands: `sextant find`, `sextant class`, `sextant sql` (with `--json`, `--csv`, `--limit` and `--schema`). The index is opened read-only.
- `--no-index` skips the index. `summary.md` tells agents about the query commands.
- The index is byte-identical across runs (same SQLite version); golden tests pin row counts per table.
- The jar ships SQLite natives for 64-bit macOS, Linux (glibc and musl) and Windows only (23 MB → 16 MB).
- Native access is enabled in the launcher and jar manifest, so JDK 22+ prints no warning.

## 0.2.0 (Version 2): code structure

- `extract` now also writes `code/classes.jsonl`, `code/methods.jsonl`, `code/fields.jsonl`, `code/calls.jsonl` and `code/strings.jsonl`, read from DEX bytecode with dexlib2. Nothing is decompiled.
- Smali-style IDs for classes, methods and fields. Records are streamed to disk in sorted order, so memory stays low on large apps.
- Call edges are aggregated per (caller, callee, kind) with exact counts and bytecode offsets. String constants are listed per loading method. Constant field initial values are included.
- `meta.json` gains a `code` block (counts, Kotlin classes, duplicate classes, biggest packages); `schema_version` is now 2.
- `summary.md` and `info` show code counts. `--no-code` gives a fast manifest-only run.
- Unpaired UTF-16 surrogates in DEX strings are escaped (they crashed UTF-8 output on 3 corpus APKs).
- Output files are created 0644 instead of 0600.
- Class, member, call-site and string counts were cross-checked against dexdump on all 18 corpus APKs, with exact matches.

## 0.1.0 (Version 1): app identity and manifest

- `sextant extract <apk>` writes `summary.md`, `meta.json`, `manifest.json`, `permissions.jsonl` and `components.jsonl`.
- `sextant info <dir|apk> [--json]` prints a summary of an extracted folder or an APK.
- Manifest and resources are decoded with jadx-core's resource decoders only; no DEX code is loaded.
- `@string/…`, `@bool/…` and `@integer/…` references are resolved from resources.arsc.
- Components carry the effective `exported` value with `exported_source` (`explicit` or `default`), launcher detection, deep links, aliases and provider authorities.
- Permissions are requested, declared and implied (same rules as aapt2), with a `dangerous` flag.
- Signing schemes (v1, v2, v3, v3.1) and certificate fingerprints come from Google's apksig.
- Output is byte-identical across runs.
- Exit codes are 0, 1 (usage), 2 (input), 3 (output) and 4 (internal).
- Tested against 18 downloaded real APKs and cross-checked with aapt2.
