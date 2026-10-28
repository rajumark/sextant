# Code search, obfuscated apps and bundles

Three things jadx users reach for that `find` and the index don't cover: searching the decompiled code itself, reading an obfuscated build with its mapping, and opening the split bundles that app stores hand out.

## Search inside the code

`find` searches names and string constants. `grep` searches the decompiled Java, so it finds calls, field use and expressions:

```console
$ sextant grep binaryeye.apk "getSharedPreferences("
de.markusfisch.android.binaryeye.app.BinaryEyeApp.onCreate:51: SharedPreferences sharedPreferences = getSharedPreferences("builtin:expert", 0);
su.onClick:59: SharedPreferences sharedPreferencesE = (str == null || str.length() == 0) ? yx0Var.e() : context3.getSharedPreferences(str, 0);
…
6 matching lines in 2799 classes (2.7 s)
```

Each line is `class.method:line: code`, where the line number is the line in the decompiled class, as `sextant source` prints it. `--json` gives the class and method as descriptors (`Lyi0;->S(Lm91;I)Ljava/lang/String;`), ready for `callers` or `smali`.

- `--regex` treats the text as a regular expression (`"addJavascriptInterface|evaluateJavascript"`); `--case` matches case.
- `--package com.app` searches only those classes.
- `--limit` stops early (default 200).

The first search decompiles every class, in parallel, and keeps the code in a local cache keyed by the APK's hash. Later searches only read text: on Signal (41,000 classes) the first search took about 4 minutes, and the next ones about 11 seconds. `--no-cache` skips the cache. The cache lives in the system temp folder, or wherever `-Dsextant.cache=…` points; nothing leaves the machine.

## Obfuscated apps

`info` shows how much of the code was renamed:

```text
Obfuscation  heavy · 89% short class names · 2,606 in the default package · source files stripped (100%)
```

`heavy` means more than 30% of classes have R8/ProGuard-style names (`a`, `yi0`); `light` means 5–30%. Signal keeps its names but strips source file names, which shows as `none · … · source files stripped`. The counts are in `meta.json` under `code.obfuscation`.

If you have the build's `mapping.txt` (your own release, or one shipped with a crash report), pass it to `source` and `grep`:

```console
$ sextant source app.apk com.example.LoginManager --mapping mapping.txt
$ sextant grep app.apk "LoginManager" --mapping mapping.txt
$ sextant deobf mapping.txt LoginManager
com.example.LoginManager -> a.b.c
```

With a mapping, decompiled code uses the original class, method and field names (jadx marks each with `renamed from`), and `source` accepts the original class name. The index and the other commands keep the bytecode names, so a descriptor from `grep --json` still works with `callers` and `xref`.

## Split bundles

`.apks` (bundletool, SAI), `.xapk` (APKPure) and `.apkm` (APKMirror) work wherever an APK does:

```console
$ sextant info app.xapk
…
DEX / ABIs   3 dex · arm64-v8a
Bundle       xapk · base com.example.apk · 3 splits (1 abi, 1 density, 1 language)
```

The base APK and every split are merged once into a local cache: dex files from feature splits, native libraries from ABI splits, and assets and res/ files from all splits. The signature is checked on the base APK as shipped. Only the base's `resources.arsc` is decoded. `.aab` is the Play upload format, not an installable bundle: build APKs from it with `bundletool build-apks` and pass the `.apks`.
