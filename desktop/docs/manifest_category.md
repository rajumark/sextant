# AndroidManifest categories

Everything a decoded `AndroidManifest.xml` can contain, grouped the way the desktop app can show it. Counts come from the
46 corpus APKs in `fixtures/golden` (the same analyses the app opens). "Used by" is the number of APKs where the item
appears at least once. Treat the counts as a sample of real apps, not a full list of what Android allows.

## 1. Application identity (`manifest`)

| Item | Used by | What it is |
|---|---|---|
| `package` | 46 | The app's unique ID. |
| `versionCode`, `versionName` | 46 | Build number and version shown to users. |
| `compileSdkVersion`, `platformBuildVersion*` | 46 | The SDK the app was built with. |
| `installLocation` | 28 | Where the app may be installed (internal, external, auto). |
| `sharedUserId`, `sharedUserLabel` | 3 | Apps that share a process and data with another app. Deprecated. |
| `targetSandboxVersion` | 1 | Sandbox level for special installs. |

## 2. SDK range (`uses-sdk`)

| Item | Used by | What it is |
|---|---|---|
| `minSdkVersion` | 46 | Oldest Android version the app runs on. |
| `targetSdkVersion` | 46 | Android version the app is tested against (drives many behaviour changes). |

## 3. Permissions

| Item | Used by | What it is |
|---|---|---|
| `uses-permission` | 46 | A permission the app asks for. `maxSdkVersion` limits it to older Android. |
| `uses-permission-sdk-23` | 12 | A runtime permission only for Android 6 and newer. |
| `permission` (declared) | 54 defs | A permission this app defines for other apps to request. `protectionLevel` says who can get it. |

## 4. Features, libraries and screens

| Item | Used by | What it is |
|---|---|---|
| `uses-feature` | 199 | Hardware or software the app needs or prefers (`required` true or false). Examples: camera, GPS, touchscreen, Wi-Fi, leanback. |
| `uses-library` | 80 | Optional shared libraries on the device (for example `androidx.window.extensions`, `org.apache.http.legacy`). |
| `uses-native-library` | 1 | A vendor native library the app uses. |
| `supports-screens` | 13 | Which screen sizes and densities the app handles. |
| `layout` | 2 | Default size of a freeform window. |

## 5. Application flags (`application` attributes)

| Item | Used by | What it is |
|---|---|---|
| `allowBackup`, `fullBackupContent`, `dataExtractionRules` | 43 / 17 / 12 | Whether app data is backed up, and which parts. |
| `usesCleartextTraffic`, `networkSecurityConfig` | 11 / 17 | Whether plain HTTP is allowed, and the network rules file. |
| `debuggable` | 1 | Lets a debugger attach. Should be off in release builds. |
| `extractNativeLibs`, `requestLegacyExternalStorage` | 45 / 20 | Native library and old storage behaviour. |
| `appComponentFactory`, `name` | 43 / 36 | The custom application class or factory. |
| `theme`, `supportsRtl`, `largeHeap`, `hardwareAccelerated` | 40 / 32 / 14 / 6 | Look, layout direction, memory and rendering. |
| `localeConfig`, `enableOnBackInvokedCallback`, `resizeableActivity` | 19 / 17 / 15 | Languages, back gesture, and window resizing. |
| `appCategory`, `isGame` | 5 / 3 | Store category hints. |
| `hasFragileUserData`, `backupAgent`, `manageSpaceActivity` | 5 / 6 / 3 | Data-loss prompts and custom backup. |
| `memtagMode`, `banner`, `logo`, `description` | 3 / 12 / 3 / 1 | Memory tagging, TV banner, logo and description. |

## 6. Components

Each of these has its own menu in the app except Providers, which still sits under Components.

### Activities (`activity`), 963 declarations in 46 APKs
Screens. Key attributes: `exported` (642 set explicitly), `theme`, `configChanges`, `launchMode`, `parentActivityName`,
`screenOrientation`, `windowSoftInputMode`, `excludeFromRecents`, `noHistory`, `permission`, `process`,
`supportsPictureInPicture`, `turnScreenOn`, `showWhenLocked`, `documentLaunchMode`.

### Activity aliases (`activity-alias`), 107 in 46 APKs
A second entry point for an activity (for example, switching the launcher icon). Key attributes: `targetActivity`,
`enabled`, `exported`, `icon`, `roundIcon`, `label`.

### Services (`service`), 480 declarations
Background work. Key attributes: `exported`, `permission`, `process`, `foregroundServiceType` (102 typed services),
`isolatedProcess`, `directBootAware`, `stopWithTask`.

### Receivers (`receiver`), 374 declarations
Broadcast listeners. Key attributes: `exported`, `enabled`, `permission`, `directBootAware`, `process`. Most receive
`BOOT_COMPLETED`, widget updates or push events.

### Providers (`provider`), 170 declarations
Content sharing. Key attributes: `authorities`, `exported`, `grantUriPermissions` (80), `readPermission`,
`writePermission`, `permission`, `initOrder`, `directBootAware`, `process`, `multiprocess`, `syncable`.
- `path-permission` (8 APKs): per-path read and write permissions on a provider (`pathPrefix`, `readPermission`,
  `writePermission`).

## 7. Intent filters (inside any component)

| Item | Used by | What it is |
|---|---|---|
| `intent-filter` | 1105 | The conditions under which a component is started. `priority`, `autoVerify` (61), `label`, `icon`, `order`. |
| `action` | 1566 | What the intent wants to do. Top: `VIEW` 308, `MAIN` 144, `SEND` 69, `BOOT_COMPLETED` 61, quick-settings tiles 56, widget updates 36, NFC 28, media search 27. |
| `category` | 930 | Extra context. Top: `DEFAULT` 438, `BROWSABLE` 268 (opens from web links), `LAUNCHER` 126 (home screen), `MULTIWINDOW_LAUNCHER` 30, `LEANBACK_LAUNCHER` 13 (TV). |
| `data` | 632 | Which content matches: `scheme` (632), `host` (783), `pathPattern` (1232), `pathPrefix` (103), `mimeType` (350), `path`, `pathSuffix`, `sspPattern`. |

## 8. Metadata and extra settings

| Item | Used by | What it is |
|---|---|---|
| `meta-data` | 693 | Name and value pairs for libraries and features. Common names: `FILE_PROVIDER_PATHS`, `app.shortcuts`, initializers (WorkManager, emoji, profile installer), `appwidget.provider`, `PARENT_ACTIVITY`, `searchable`. |
| `property` | 17 | Named values for the system or stores (for example, opt-outs). |
| `profileable` | 3 | Lets profiling tools attach to the app. |
| `attribution` | 2 | Tags for attributing data use to a feature. |

## 9. Queries (what the app looks for on the device)

| Item | Used by | What it is |
|---|---|---|
| `queries` | 33 | Declares which other apps and intents this app can see (Android 11 package visibility). |
| `package` | 40 | An app it needs to see by name (for example Google Maps, Google Play services). |
| `intent` / `provider` | 138 / few | Intents or content providers it checks for. |

## 10. Signing and verification (outside the manifest file, shown in the app's Signing page)

Signature schemes v1 to v4, certificate fingerprints and validity. Already in the app.

---

## Where these live in the desktop app today

| Area | Screen | Status |
|---|---|---|
| Permissions | Permissions | Done, with summaries from the permission bank. |
| Activities, services, receivers, aliases | Own pages | Done (same layout as Permissions). |
| Providers | Components | Shown with the other types; no own page yet. |
| Manifest tree (everything above) | Manifest | Done (tree, XML, JSON). |
| Icons | Icons | Done (list and details, filters planned). |
| Signing | Signing | Done. |

## Candidates for new pages (not built yet)

1. **Application** (section 5): flags such as backup, cleartext and debuggable, with plain-English explanations.
2. **Intent filters** (section 7): one list across all components, filtered by action, category and data.
3. **Features and libraries** (section 4): required hardware and shared libraries.
4. **Metadata** (section 8): name and value pairs.
5. **Queries** (section 9): what the app can see on the device.
6. **Providers** (section 6): an own page, like the other component types.
