# Quickstart

This walks through a first analysis in five minutes. The examples use [Binary Eye](https://f-droid.org/packages/de.markusfisch.android.binaryeye/), an open-source barcode scanner from F-Droid. Any APK works.

## 1. Extract

```bash
sextant extract binaryeye.apk
```

```text
✓ Read APK          8.3 MB · sha256 568cec6598db
✓ Decoded manifest  de.markusfisch.android.binaryeye 1.75.5 (180)
✓ Permissions       12 requested · 1 declared
✓ Components        18 (9 exported)
✓ Signing           v1, v2, v3
✓ Code              2936 classes · 16824 methods · 55703 call edges
✓ Files             930 in the APK, 619 decoded
✓ Wrote binaryeye.sqlite  31.8 MB in 2.9s
```

Progress goes to **stderr**, so it never mixes with data on stdout. Typical apps take 1–5 seconds; the largest (60k+ classes) take 15–25 seconds, mostly to build the search index.

!!! tip "Faster runs"
    `--no-code` skips code entirely and only reads the manifest, resources and signing, which takes well under a second.

## 2. Read the summary

Run:

```bash
sextant info binaryeye.sqlite
```

Every command also works directly on an APK (`sextant info app.apk`): it is indexed once into a cache and reused on every later run.

## 3. Search anything

```bash
sextant find binaryeye.sqlite "markusfisch.de/BinaryEye" --limit 4
```

```text
string     http://markusfisch.de/BinaryEye
           ← Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V
string     https://markusfisch.de/BinaryEye
           ← Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V
deep_link  http://markusfisch.de/BinaryEye*
           de.markusfisch.android.binaryeye.activity.CameraActivity
...
```

The string hit tells you **which method loads it**; the deep-link hit tells you **which activity opens it**.

## 4. Follow the code

```bash
sextant callers binaryeye.sqlite "Landroid/app/Activity;->getIntent" --depth 1 --fan-out 3
sextant source binaryeye.apk de.markusfisch.android.binaryeye.activity.CameraActivity.onResume
```

`source` prints decompiled Java for one method or class, with the same names as in the index.

## 5. Review

```bash
sextant findings binaryeye.sqlite
```

```text
[medium] Cleartext HTTP allowed  (1)  · cleartext
    android:usesCleartextTraffic=true allows unencrypted HTTP.
    - manifest application usesCleartextTraffic=true
[low] Components exported without permission  (6)  · exported-component
    ...
```

## What next

- [Investigating an app](../guides/investigating.md): `class`, `xref`, `callers`, `reach`, `smali`.
- [Output files](../reference/output.md): every file and field.
- [AI agents and MCP](../guides/ai-agents.md): let an agent do the digging.
