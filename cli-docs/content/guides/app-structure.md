# Components, startup, resources and SDKs

Four commands answer "what is this app made of?" without writing SQL. They all take an APK (indexed once, then cached) or a `.sqlite` file from `extract`.

| Command | Answers |
|---|---|
| `sextant components` / `sextant component` | Which activities, services, receivers and providers exist, which other apps can reach them, and why |
| `sextant app` | What runs when the process starts, in order, and which SDKs start with it |
| `sextant res` | Strings in every locale, ids, styles, drawables, layouts, menus, navigation, assets and `res/xml` configs |
| `sextant sdks` | SDKs and libraries by role, with versions an artifact in the APK proves; filters across many apps |

The examples below use Bitwarden 2024.10.0 from the [test corpus](../about/testing.md).

## Components and verdicts

```bash
sextant components app.sqlite
sextant components app.sqlite --exported --type activity
sextant components app.sqlite --tag deep-link --tag hidden-entry   # both tags
sextant components app.sqlite --search firebase --json
```

```text
type      name                                              exported  tags                    entry
activity  com.x8bit.bitwarden.MainActivity                  yes       launcher                App launcher icon
activity  crc641b016c64ce400e42.WebAuthCallbackActivity     yes       deep-link               bitwarden:
activity  crc64ba438d8f48cf7e75.IntermediateActivity        yes       hidden-entry
service   com.x8bit.bitwarden.Autofill.AutofillService      yes       sensitive-role,guarded  Handles (android.service.autofill.AutofillService)
service   com.x8bit.bitwarden.AutofillTileService           yes       system-role,guarded     Handles (android.service.quicksettings.action.QS_TILE)
receiver  com.google.firebase.iid.FirebaseInstanceIdReceiver yes      guarded                 Handles (com.google.android.c2dm.intent.RECEIVE)
```

Each component gets **verdict tags**, each with a one-line reason. The desktop app shows the same tags.

| Tag | Meaning |
|---|---|
| `launcher` | Has a launcher icon (`MAIN` + `LAUNCHER`) |
| `app-link` | Verified App Link: `autoVerify` on a `VIEW` + `BROWSABLE` filter with http/https hosts |
| `deep-link` | Handles `VIEW` intents for URI patterns (from browsers too when the filter is `BROWSABLE`) |
| `hidden-entry` | Exported activity with no launcher icon, no links and no permission: only other apps start it, so check the intents it accepts |
| `unguarded` | Exported service, receiver or provider with no permission: any app can start, bind, broadcast to or query it |
| `sensitive-role` | A system role that sees user data or acts for the user: accessibility, autofill, credential provider, keyboard, notification listener, device admin, VPN, call screening, SMS, account authenticator, … |
| `system-role` | A standard system job: JobScheduler, Quick Settings tile, FCM, sync adapter, widget, media browser, … |
| `boot` | Starts after boot (`BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `QUICKBOOT_POWERON`) |
| `foreground` | Foreground service, with its `foregroundServiceType` |
| `task-hijack` | Activity with a `taskAffinity` other than the package or `allowTaskReparenting="true"`: review for task hijacking (same rule as the `task-affinity` finding) |
| `lock-screen` | Activity shown above the lock screen (`showWhenLocked` or `showOnLockScreen`) |
| `uri-grants` | Provider with `grantUriPermissions="true"` (for any path, or the listed paths) |
| `file-provider` | A `FileProvider`: shares files as `content://` URIs from the folders in its paths XML |
| `isolated` | `isolatedProcess="true"`: a sandboxed process with no app permissions |
| `direct-boot` | Runs before the user unlocks the device (`directBootAware`) |
| `exported-by-default` | Exported only because it has an intent filter and no explicit `android:exported` |
| `guarded` | Exported but protected by a permission |
| `disabled` | `android:enabled="false"` |

`sextant component` explains one component. The name can be the full class name or a unique suffix such as `.MainActivity`:

```bash
sextant component app.sqlite .WebAuthCallbackActivity
```

```text
crc641b016c64ce400e42.WebAuthCallbackActivity  (activity)
Exported: yes · enabled: yes

## Verdicts
  deep-link: Opens from links in browsers and other apps (1 pattern).

## Attributes
  launchMode  singleTop
  noHistory   true

## Entry points
  Opens bitwarden: from browsers and apps

## Deep links
  bitwarden:

## adb
  # Start
  adb shell am start -n com.x8bit.bitwarden/crc641b016c64ce400e42.WebAuthCallbackActivity

## Manifest entry
  <activity
      android:exported="true"
      android:launchMode="singleTop"
      android:name="crc641b016c64ce400e42.WebAuthCallbackActivity"
      android:noHistory="true">
      <intent-filter>
          ...
```

With `--json`, both commands print `component` (the full component record), `review` (tags and reasons) and `attributes`.

## Startup order

```bash
sextant app app.apk       # the startup order, with the calls of each step and the SDKs they start
```

```text
Application class: com.x8bit.bitwarden.MainApplication
Extends: crc6488302ad6e9e4df1a.MauiApplication → android.support.multidex.MultiDexApplication → android.app.Application
AppComponentFactory: androidx.core.app.CoreComponentFactory

## Startup order
   1. AppComponentFactory creates the Application and components: androidx.core.app.CoreComponentFactory
   2. Application.attachBaseContext: com.x8bit.bitwarden.MainApplication
   3. ContentProvider.onCreate (initOrder 1999999999): mono.android.MultiDexLoader
   4. ContentProvider.onCreate (initOrder 1999999998): mono.MonoRuntimeProvider
   5. ContentProvider.onCreate (initOrder 100): com.google.firebase.provider.FirebaseInitProvider
   6. ContentProvider.onCreate: androidx.core.content.FileProvider
   7. ContentProvider.onCreate: androidx.startup.InitializationProvider
   8.   androidx.startup Initializer.create: androidx.emoji2.text.EmojiCompatInitializer
   ...
  11. Application.onCreate: com.x8bit.bitwarden.MainApplication

## SDKs reached at startup (2)
  Firebase Core (platform)  via Lcom/google/firebase/FirebaseApp;->initializeApp(Landroid/content/Context;)Lcom/google/firebase/FirebaseApp;
  Google Play Services Base (platform)  via Lcom/google/android/gms/common/util/ProcessUtils;->getMyProcessName()Ljava/lang/String;
```

Android creates content providers before `Application.onCreate`, so SDKs that initialize themselves through a provider (Firebase, androidx.startup) run before any of the app's own code. "SDKs reached at startup" follows the calls from each step through the index. Like `reach`, it is a static approximation.

## Resources

```bash
sextant res app.sqlite strings                    # every string, plural and array, all locales
sextant res app.sqlite strings --config fr        # one configuration
sextant res app.sqlite string-keys --flag unused  # string keys with flags
sextant res app.sqlite xml                        # res/xml configs with a summary
sextant res app.sqlite refs                       # who references which resource
sextant res app.sqlite layouts activity_main      # one entry in full, and what uses it
```

`KIND` is one of `strings`, `string-keys`, `ids`, `styles`, `drawables`, `layouts`, `menus`, `navigation`, `assets`, `xml` or `refs`. References keep the `@type/name` they were written with plus the resolved value. `refs` includes code: every method that loads a resource id (as a `const 0x7f…` in R8 builds or an `R$type.name` field otherwise), with the first call after it, usually the one it is passed to (`setContentView`, `getString`, `inflate`).

| `string-keys --flag` | Keys that are |
|---|---|
| `used` / `unused` | Referenced (or not) from code, layouts or other resources |
| `url` / `email` | URLs / email addresses |
| `secret-like` | Shaped like a key or token |
| `format` / `html` | Format strings (`%s`, `%1$d`) / HTML markup |
| `untranslated` | Present only in the default configuration |
| `empty` | Empty strings |

```text
$ sextant res app.sqlite string-keys --flag url
name                   kind    locales  used by  flags       value
firebase_database_url  string  0        0        unused,url  https://bitwarden-c2b35.firebaseio.com
```

`res xml` summarizes the configs a reviewer cares about:

- **network security config**: cleartext, user CAs, pins, debug overrides
- **backup and data extraction rules**
- **FileProvider paths**: which directories are shared

```text
name                     config   kind                     summary
filepaths                default  file-provider-paths      cache=cache-path:. temp_camera_images=files-path:camera_temp/
network_security_config  default  network-security-config  base-cleartext=true user-certs=trusted domain-configs=1 pin-sets=0 …
```

`res assets` flags assets whose bytes are DEX, ELF or zip under another name as **hidden code**.

The same data is in the SQLite file (`string_resources`, `resource_ids`, `styles`, `drawables`, `layouts`, `menus`, `navigation_destinations`, `assets`, `xml_configs`, `resource_refs`); see [Output](../reference/output.md) and the [index schema](../reference/index-schema.md).

## SDKs and versions

```bash
sextant sdks app.sqlite
```

```text
com.x8bit.bitwarden 2024.10.0
Roles: analytics, auth, cryptography, image-loading, platform, push, reactive, serialization, ui

## SDKs (11)
category       sdk                        version  classes  evidence
analytics      Microsoft App Center       ?        221
platform       Firebase Core              17.1.0   59       firebase-components.properties
serialization  Gson                       2.10.1   217      META-INF/maven/com.google.code.gson/gson/pom.properties
ui             Material Components        1.9.0    1307     META-INF/com.google.android.material_material.version
...
```

A version is shown only when an artifact in the APK proves it: META-INF `*.version` files, Maven `pom.properties`, Play services and Firebase `.properties` files, or OkHttp's user agent string. Otherwise the version is `?`. Sextant never guesses one from class names.

With several apps, `sdks` shows which SDKs appear where, and the filters select apps:

```bash
sextant sdks apps/*/                                   # SDK × app overview
sextant sdks apps/*/ --category ads                    # apps that contain an ads SDK
sextant sdks apps/*/ --sdk okhttp --below 4.9.0        # apps with OkHttp older than 4.9.0
sextant sdks apps/*/ --sdk firebase --sdk sentry --json
```

`--category` and `--sdk` can be repeated; every one must match. `--below` needs exactly one `--sdk` and skips apps where that SDK's version is unknown.
