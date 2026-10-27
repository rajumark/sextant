# Insights and app icons

Besides the raw facts, Sextant derives **insights**: what the app is built with and what it talks to. Each insight carries the evidence it is based on.

```bash
sextant insights app/
```

```text
## Frameworks (0)
none

## Sdks (1)
category  name                 classes
--------  -------------------  -------
ui        Material Components  23

## Top hosts (…)
host            urls
--------------  ----
markusfisch.de  8
...

## Libraries (46)
group              artifact     version
-----------------  -----------  -------
androidx.activity  activity     1.8.0
...
```

## Frameworks

Detected from fingerprints in files, classes and the manifest: Flutter, React Native, Hermes, Expo, .NET (Xamarin/MAUI), Capacitor, Cordova, Unity, Godot, libGDX, SDL, Qt, Cocos2d-x, Unreal Engine, Jetpack Compose, NativeActivity and Kotlin. Example evidence for Bitwarden: `libmonodroid.so`, `libmonosgen-2.0.so`, the assembly blobs and 306 `mono.android` classes.

## SDKs

About 110 well-known SDKs (analytics, ads, crash reporting, payments, push, networking, databases, maps…) are matched by **package prefix** (counting classes) and by **manifest entries** (component names and meta-data), which survive obfuscation. The rules are an editable JSON file: `core/src/main/resources/io/github/rajumark/sextant/sdks.json`.

## Libraries with exact versions

AndroidX and many other libraries ship `META-INF/<group>_<artifact>.version` files. Sextant reads them for exact Maven coordinates (Signal: 141 libraries), plus the Android Gradle Plugin version from `app-metadata.properties`.

## Endpoints

Every URL from code strings (with the method that uses it), string resources, deep links, manifest meta-data, text assets (React Native/Hermes bundles, configs) and native libraries. See [`endpoints`](../reference/commands.md#endpoints).

## Native code

ELF `.dynsym` tables are parsed for exported functions. JNI functions (`Java_…`) are decoded back to the Java class and method they implement, including `_1`, `_0xxxx` escapes and overload suffixes:

```bash
sextant sql app/ "SELECT jni_class, jni_method, library FROM native_symbols WHERE jni_class IS NOT NULL LIMIT 5"
```

## App icons

```bash
sextant icon app.apk                 # every file behind android:icon, best one marked
sextant icon app.apk --round         # android:roundIcon
sextant icon app.apk --layer foreground -o fg.png
sextant icon app.apk -o icon.png     # save the best bitmap
```

```text
icon  @mipmap/ic_launcher
  anydpi-v26     xml   -               548 B  res/BW.xml
  hdpi           png   72×72          2567 B  res/yn.png
  mdpi           png   48×48          1710 B  res/9w.png
  xhdpi          png   96×96          3363 B  res/FS.png
  xxhdpi         png   144×144        5145 B  res/RJ.png
  xxxhdpi        png   192×192        6982 B  res/o-.png  ← best
adaptive icon  res/BW.xml
  background  @color/accent  #b6d46f
  foreground  @drawable/ic_launcher_foreground  res/Qr.xml
  monochrome  @drawable/ic_launcher_monochrome  res/Vy.xml
```

- Icons are resolved from the manifest through `resources.arsc`, so they are found even when R8 resource shrinking renames files (`res/o-.png` above).
- The best file is the largest bitmap by pixel area (PNG and WebP sizes are read from the file headers). When the icon is only a vector, the vector is reported and `-o` suggests `--layer foreground` instead, because vector drawables are compiled binary XML, not images.
- Adaptive icons are split into background, foreground and monochrome layers, following `<inset>`/`<bitmap>` wrappers; solid-color layers report their color.
- `icon` on an APK is fast (manifest and resources only). It also works on an extraction folder, reading `meta.json`; saving with `-o` needs the APK.
