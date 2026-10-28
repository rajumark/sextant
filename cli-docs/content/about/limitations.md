# Limitations

Sextant is a **static** analyzer. It reads what is in the APK; it never runs it. That brings some limits worth knowing:

## Code

- **Reflection, dynamic loading and native calls** are invisible to call trees and `reach`. A `Class.forName("…")` shows up as a string, not as an edge.
- **Virtual dispatch** is approximated. `callers` records calls against the method named in the bytecode (often an interface or superclass method). `reach` expands into overrides using the class hierarchy, but doesn't expand signatures with more than 25 implementations, to avoid connecting unrelated code.
- **Framework callbacks** (`onClick`, `onReceive`, lifecycle methods) are not linked to the code that registers them.
- **Obfuscated names** stay obfuscated in the index. Sextant doesn't guess original names. With the build's own `mapping.txt`, `source --mapping` and `grep --mapping` show the original names, and `deobf` translates names both ways; the index keeps the bytecode names.
- **Decompiling** (`source`) uses jadx and can fail on unusual bytecode. `smali` always works.
- **Flutter, React Native, Unity and other non-Java code** is detected and its native libraries and assets are listed, but Dart snapshots, Hermes bytecode and IL2CPP code aren't decompiled.

## Input

- **Split bundles** (`.apks`, `.xapk`, `.apkm`) are read: the base APK and its splits are merged into one APK in a local cache. Code, native libraries, assets and res/ files of every split are included, but only the base's `resources.arsc` is decoded, so values that exist only in a density or language split aren't resolved. The signature is checked on the base APK. Encrypted `.apkm` files and `.aab` (Play upload format; build APKs from it with `bundletool build-apks`) aren't read.
- **Code search** (`grep`) decompiles every class once and keeps the code in the local cache (about 300 MB for Signal's 41,000 classes; the first search takes a few minutes on such an app). Classes jadx can't decompile are counted in the summary line and are not searched.
- **Resources** use the default configuration for values (strings, bools, …). Icons are resolved for every configuration.
- Very large inputs are capped as a zip-bomb guard (manifest 32 MB, `resources.arsc` 512 MB, each dex 256 MB, each native library 256 MB).

## Findings

[Review findings](../reference/findings.md) are hints for a human or an agent to check. They aren't proven vulnerabilities and they don't replace a dynamic test. Expect false positives (e.g. a weak hash used for a cache key) and false negatives (anything reached via reflection).

## Size

Code JSONL files repeat full descriptors so every line stands on its own. For very large apps they can be hundreds of MB; `index.sqlite` is the compact form. Use `--no-code` for manifest-only work.
