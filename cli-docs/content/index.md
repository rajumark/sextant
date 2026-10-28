---
hide:
  - navigation
---

# Sextant CLI

**Turn an Android APK into structured data that people, scripts and AI agents can read.**

Sextant reads an APK's manifest, resources, signing information and DEX bytecode, and writes everything into one SQLite file you can search and query. APK in, SQLite out. It never runs the app and needs no device, emulator or Android SDK.

```bash
sextant extract app.apk                       # everything into app.sqlite
sextant info app.sqlite                        # one-screen summary
sextant find app.sqlite "api.example.com"      # where does this URL come from?
sextant findings app.sqlite --severity medium  # what should a reviewer look at?
```

<div class="grid cards" markdown>

-   :material-file-tree: **Everything as data**

    ---

    Manifest, permissions, components, intent filters, features, queries, metadata, signing, icons, classes, methods, fields, call edges, strings, field and type references, all as deterministic rows in one SQLite file.

    [:octicons-arrow-right-24: Output](reference/output.md)

-   :material-database-search: **Query it like a database**

    ---

    One SQLite file with full-text search over names, strings, URLs, resources, SDKs and native symbols. Use `find`, `class`, `xref`, `callers`, `reach`, or raw read-only SQL.

    [:octicons-arrow-right-24: Investigating an app](guides/investigating.md)

-   :material-shield-search: **Review points with evidence**

    ---

    Exported components, cleartext traffic, WebView bridges, weak crypto, trust-all TLS, embedded secrets and more. Every finding points at the exact component, method or string.

    [:octicons-arrow-right-24: Security review](guides/security-review.md)

-   :material-sitemap-outline: **Components, startup, resources, SDKs**

    ---

    Verdicts with reasons for every component, the startup order and the SDKs it starts, strings in every locale, layouts, `res/xml` configs, and SDK versions proven by artifacts.

    [:octicons-arrow-right-24: Components, startup, resources and SDKs](guides/app-structure.md)

-   :material-robot-outline: **Built for AI agents**

    ---

    `info` for a one-read overview, `--json` on every command, and `sextant mcp`, a Model Context Protocol server with 17 tools.

    [:octicons-arrow-right-24: AI agents and MCP](guides/ai-agents.md)

-   :material-compare-horizontal: **Compare releases**

    ---

    `sextant diff old/ new/` shows what changed between two versions: permissions, components, SDKs, library versions, hosts, code packages, strings and resources.

    [:octicons-arrow-right-24: Comparing versions](guides/diff.md)

-   :material-check-decagram: **Verified on real apps**

    ---

    Tested against 46 real APKs (Flutter, React Native, .NET MAUI, Unity-style engines, obfuscated releases…) and cross-checked with Google's `aapt2` and `dexdump`.

    [:octicons-arrow-right-24: How Sextant is tested](about/testing.md)

</div>

## What it looks like

```text
$ sextant info binaryeye.sqlite
Package      de.markusfisch.android.binaryeye
Label        Binary Eye
Version      1.75.5 (180)
SDK          min 21 · target 37
Signing      v1, v2, v3 · sha256 9E:CD:BF:06…
Permissions  12 requested (5 dangerous) · 1 declared
Components   13 activity · 0 activity-alias · 2 service · 1 receiver · 2 provider
Exported     9 components · 2 with deep links
Launcher     de.markusfisch.android.binaryeye.activity.SplashActivity
DEX / ABIs   1 dex · arm64-v8a, armeabi-v7a, x86, x86_64
Icon         res/o-.png (192×192 xxxhdpi) · @mipmap/ic_launcher · adaptive res/BW.xml
Manifest     2 features · 2 libraries · 7 metadata · 2 queries
Insights     1 SDKs · 46 libraries · 24 URLs on 12 hosts · 12 native libs · 87 locales
Code         2936 classes · 16824 methods · 55703 call edges · 0 Kotlin classes
```

## Principles

Deterministic
:   The same APK always gives byte-identical output: sorted records, fixed key order, no timestamps or absolute paths.

Truthful
:   A missing manifest attribute is `null`, never a guess. Derived values say how they were derived (`exported_source`, `implied_by`). Findings are worded as things to check, not as proven vulnerabilities.

Local only
:   Sextant reads a file and writes files. It makes no network calls and collects no telemetry.

Static
:   Nothing is executed. Code is read straight from bytecode with dexlib2, so analysis works even where decompilers fail. `source` (via jadx) is only used on demand.

!!! note "Use responsibly"
    Only analyze apps you own, apps with open-source licenses, or apps you have permission to examine.
