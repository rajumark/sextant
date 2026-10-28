# Output files

`sextant extract app.apk -o app/` writes this folder. Every file is UTF-8 with `\n` line endings. Keys are `snake_case` in a fixed order, and lists are sorted, so the same APK always gives byte-identical files.

```text
app/
  summary.md          ← read this first (short, written for people and agents)
  meta.json           ← identity, SDKs, application flags, signing, layout, code + insight counts, icons, warnings
  manifest.json       ← the full decoded AndroidManifest.xml as a tree
  permissions.jsonl   ← one permission per line
  components.jsonl    ← one component per line (activities, aliases, services, receivers, providers)
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
    frameworks.jsonl  ← Flutter, React Native, .NET MAUI, Unity, … with evidence
    sdks.jsonl        ← known SDKs by category, with class and manifest evidence
    libraries.jsonl   ← exact Maven coordinates + versions
    endpoints.jsonl   ← every URL and where it was found
    findings.jsonl    ← review points with evidence
  resources/
    values.jsonl      ← default strings, bools, integers, colors, dimens
    strings.jsonl     ← strings, plurals and arrays in every locale
    ids.jsonl         ← resource ids (R.id)
    styles.jsonl      ← styles and themes with parents and items
    drawables.jsonl   ← drawables and mipmaps per configuration: path, format, size
    layouts.jsonl     ← layouts: root, views with attributes, ids, custom views, fragments, includes
    menus.jsonl       ← menu items
    navigation.jsonl  ← navigation graph destinations
    assets.jsonl      ← files under assets/: kind, size, hidden code flagged
    xml.jsonl         ← res/xml configs with a summary (network security, backup, FileProvider paths, …)
    refs.jsonl        ← who references which resource (`@type/name`)
  native/
    libraries.jsonl   ← .so files per ABI: size, sha256, exported symbols, JNI methods
    symbols.jsonl     ← exported functions; JNI symbols mapped to Java
  index.sqlite        ← all of the above, normalized, with full-text search
```

`--no-code` skips `code/` (and code-based insights); `--no-index` skips `index.sqlite`.

## Conventions

- **`null` means absent.** A manifest attribute that isn't set is `null`, never a guessed default. Values Sextant derives say so (`exported_source`, `implied_by`).
- **IDs are smali descriptors**: `Lcom/a/B;` (class), `Lcom/a/B;->m(I)V` (method), `Lcom/a/B;->f:I` (field).
- **Offsets** in `code/*.jsonl` are 16-bit code units (decimal), the same unit `dexdump` and `smali` (`#@hex`) use. At most 32 are kept per record; `count` is always exact.
- **Strings** with unpaired UTF-16 surrogates (possible in DEX) are written as `\uXXXX` escapes, so the files stay valid UTF-8.
- **`schema_version`** in `meta.json` changes only when the shape of the files changes, independently of the tool version (currently **8**).

## meta.json

| Field | Meaning |
|---|---|
| `schema_version`, `tool` | Output format version; tool name and version |
| `file` | APK file name, size, sha256 |
| `package`, `version_name`, `version_code` | Identity |
| `min_sdk`, `target_sdk`, `compile_sdk` | SDK levels (`null` if absent or a preview codename, with a warning) |
| `label`, `application_class` | Resolved app name; custom `Application` class |
| `flags` | Every `<application>` attribute that matters plus identity extras: `debuggable`, `allow_backup`, `uses_cleartext_traffic`, `network_security_config`, `extract_native_libs`, `test_only`, `full_backup_content`, `data_extraction_rules`, `backup_agent`, `request_legacy_external_storage`, `large_heap`, `memtag_mode`, `locale_config`, `app_category`, `profileable_shell`, `install_location`, `shared_user_id`, `target_sandbox_version`, `supports_screens`, … |
| `signing` | `verified`, `schemes` (`v1`, `v2`, `v3`, `v3.1`) and certificates (sha256, sha1, subject, issuer, validity) |
| `layout` | Number of dex files, native ABIs |
| `code` | Counts (classes, methods, fields, call edges and sites, string refs, field and type refs, Kotlin classes, duplicates, unreadable methods) and the biggest packages; `null` with `--no-code` |
| `insights` | Counts and short lists: frameworks, SDK categories, hosts, locales, JNI methods, AGP version, findings per severity |
| `icons` | `icon` and `round_icon`, each with every file per configuration, the best file and adaptive layers |
| `warnings` | Anything Sextant couldn't read cleanly (it keeps going and says so here) |

## components.jsonl

```json
{"type":"activity","name":"org.example.DeepLinkActivity","exported":true,"exported_source":"default","enabled":true,
 "permission":null,"process":null,"launcher":false,"target_activity":null,"authorities":[],
 "intent_filters":[{"actions":["android.intent.action.VIEW"],"categories":["android.intent.category.BROWSABLE","android.intent.category.DEFAULT"],
   "data":[{"scheme":"https","host":"example.org","path_prefix":"/join", "…": "…"}],"priority":0,"auto_verify":true,"label":null,"order":null}],
 "read_permission":null,"write_permission":null,"grant_uri_permissions":null,"path_permissions":[],"grant_uri_paths":[],
 "launch_mode":"singleTask","task_affinity":null,"exclude_from_recents":null,"allow_task_reparenting":null,
 "screen_orientation":null,"foreground_service_type":null,"isolated_process":null,"direct_boot_aware":null,"label":null}
```

- `name` is always fully qualified (`.Main` and `Main` are expanded with the package).
- `exported` is the **effective** value: an explicit `android:exported` wins; otherwise components with an intent filter are exported, and providers are exported only for apps whose minSdk or targetSdk is 16 or lower. `exported_source` says which (`explicit` or `default`).
- `launcher` is true for activities with `MAIN` + `LAUNCHER`.
- Intent filter `data` also carries `ssp`, `ssp_prefix`, `ssp_pattern`, `path_suffix`, `path_advanced_pattern`, `port` and `mime_type`.

## permissions.jsonl

```json
{"name":"android.permission.CAMERA","kind":"uses-permission","max_sdk":null,"dangerous":true,"protection_level":null,"custom":false,"implied_by":null}
{"name":"android.permission.READ_EXTERNAL_STORAGE","kind":"implied","max_sdk":28,"dangerous":true,"protection_level":null,"custom":false,"implied_by":"android.permission.WRITE_EXTERNAL_STORAGE"}
```

`kind` is `uses-permission`, `uses-permission-sdk-23`, `implied` (granted by Android without being listed, same rules as `aapt2`) or `declared` (defined by this app, with `protection_level`).

## features.jsonl, metadata.jsonl, queries.jsonl

```json
{"kind":"feature","name":"android.hardware.camera","required":null,"version":null}
{"kind":"feature","name":"glEsVersion","required":true,"version":"3.1"}
{"kind":"library","name":"org.apache.http.legacy","required":false,"version":null}
```

```json
{"scope":"application","kind":"meta-data","name":"com.google.android.geo.API_KEY","value":"…","resource":null}
{"scope":"com.example.UploadService","kind":"meta-data","name":"android.app.shortcuts","value":null,"resource":"@xml/shortcuts"}
```

```json
{"kind":"package","name":"com.google.android.apps.maps","actions":[],"categories":[],"data":[]}
{"kind":"intent","name":"VIEW https:","actions":["android.intent.action.VIEW"],"categories":[],"data":[{"scheme":"https", "…": "…"}]}
```

`required: null` means the manifest doesn't say (Android treats features and libraries as required by default).

## code/*.jsonl

```json
{"id":"Lde/markusfisch/android/binaryeye/activity/CameraActivity;","name":"de.markusfisch.android.binaryeye.activity.CameraActivity","package":"de.markusfisch.android.binaryeye.activity","superclass":"Lb6;","interfaces":[],"access":["public","final"],"source_file":"…","dex":"classes.dex","kotlin":false,"annotations":[],"methods":23,"fields":26}
{"caller":"Lde/…/CameraActivity;->onResume()V","callee":"Landroid/app/Activity;->getIntent()Landroid/content/Intent;","kind":"virtual","count":1,"offsets":[84]}
{"value":"https://markusfisch.de/BinaryEye","method":"Lde/…/CameraActivity;->onResume()V","count":1,"offsets":[124]}
{"method":"Lde/…/CameraActivity;->onResume()V","field":"Lde/…/CameraActivity;->c1:Z","kind":"write","count":2,"offsets":[…]}
{"method":"Lde/…/CameraActivity;->onResume()V","type":"Lde/…/WelcomeActivity;","kind":"class","count":1,"offsets":[…]}
```

- `calls.jsonl` has one line per distinct (caller, callee, kind); `kind` is `virtual`, `static`, `direct`, `super`, `interface` or `polymorphic`.
- When a class is defined in several dex files, the first definition wins, as on Android (`meta.code.duplicate_classes`).
- Code files are large for big apps because every line is self-contained (Signal: ~470 MB). `index.sqlite` is the compact, normalized form.

## insights, resources and native

See [Insights and app icons](../guides/insights.md), [Components, startup, resources and SDKs](../guides/app-structure.md) and [Review findings](findings.md) for what these contain; each line carries the evidence it is based on.
