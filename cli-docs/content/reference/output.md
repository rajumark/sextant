# Output: one SQLite file

APK in, SQLite out. `sextant extract app.apk` writes `app.sqlite` and nothing else: no folder, no JSON or JSONL files. The file holds every byte of the APK too (`file_contents`), so `smali`, `source`, `grep`, `icon -o` and `files --save` work from it without the APK; `--no-content` keeps only the dex files' bytes. Every command takes that file, or the APK itself (indexed once into a cache, then reused).

```bash
sextant extract app.apk                 # writes ./app.sqlite
sextant extract app.apk -o out/app.sqlite --force
sextant info app.sqlite
sextant sql app.sqlite --schema
```

The file holds everything Sextant reads from the APK, in two forms:

- **Tables you query.** One table per kind of fact, normalized, with indexes and a full-text search table. The full schema, with a comment on every column, is in [SQLite schema](index-schema.md).
- **Full records.** The `records` table keeps every manifest, resource and insight record exactly as Sextant built it (one JSON row each), so every command can rebuild the whole picture from the file alone. A test checks on real apps that what is read back equals what was written.

## What is where

| Tables | Contents |
|---|---|
| `meta` | package, versions, SDKs, sha256, label, and the full app metadata (signing, flags, icons, counts, warnings) as `meta_json` |
| `permissions`, `components`, `deep_links`, `features`, `metadata`, `queries` | the manifest; the full decoded tree is in `records` (`kind = 'manifest'`) |
| `string_resources`, `resource_ids`, `resources`, `styles`, `drawables`, `layouts`, `menus`, `navigation_destinations`, `assets`, `xml_configs`, `resource_refs`, `icons` | resources in every locale, and who refers to what: layouts, menus, styles, the manifest and code |
| `classes`, `methods`, `fields`, `calls`, `strings`, `string_refs`, `field_refs`, `type_refs` | code structure from DEX bytecode, edges by integer id |
| `call_edges`, `string_uses`, `search_items` | the same edges and search hits with readable names (views) |
| `libraries`, `frameworks`, `sdks`, `endpoints`, `endpoint_sources`, `findings`, `finding_evidence` | insights, each with its evidence |
| `native_libraries`, `native_symbols` | native code, with JNI symbols mapped to Java |
| `instructions`, `opcodes`, `instruction_view` | every instruction of every method in code order: opcode, registers, literal, branch target and what it references (`--no-instructions` leaves them out) |
| `annotations`, `annotation_values`, `method_lines`, `method_params`, `exception_handlers`, `kotlin_classes` | code details: every annotation with its values, each method's source line table, parameter names, exception handlers, and Kotlin metadata |
| `images`, `fonts` | every image (PNG, WebP, JPEG, GIF, anywhere) with its pixel size, and every font with its family, style and version |
| `signing_signers`, `signing_certificates`, `signing_digests`, `signing_lineage`, `signing_issues`, `signing_manifest` | signing as rows: every signer per scheme (with v3 SDK ranges), every certificate field and its bytes, rotation lineage, verifier issues, MANIFEST.MF digests |
| `file_text`, `file_properties`, `services` | the text of every text, JSON and textual XML file (assets, configs, licenses, …) with a full-text index; `.properties` files key by key; service-loader registrations |
| `elf_files`, `elf_sections`, `elf_needed`, `elf_symbols`, `elf_strings` | every ELF file in depth: architecture, soname, build id, sections, the libraries it loads, every imported and exported symbol, and its readable strings |
| `xml_nodes`, `xml_attrs` | every binary XML file (manifest, layouts, menus, drawables, colors, animations, …) node by node and attribute by attribute, with `ref` for values that point at a resource; `sextant xml` prints a file rebuilt from them |
| `resource_values`, `resource_bag_items` | `resources.arsc` in every configuration: values and file paths, and the items of styles, plurals, arrays and attrs |
| `files` | every file inside the APK: type, size, CRC-32, SHA-256, content, resource name and the decoder that read it |
| `file_contents` | the uncompressed bytes of every file (dex files always; the others unless `--no-content`), so the file holds the whole APK |
| `links`, `entities` | every edge between kinds of things (component → class, layout → resource, method → string, …) with readable keys and evidence; `entities` lists every key with its kind |
| `records` | full records: manifest tree, permissions, components, features, metadata, queries, resources, insights |
| `schema_docs` | what every table, view and column means; start here: `SELECT * FROM schema_docs WHERE "table" = 'links'` |
| `search` | FTS5 trigram index over names, strings, resources, endpoints, SDKs, libraries and native symbols |

## Rules

- **Deterministic.** The same APK always gives the same rows: sorted records, stable ids, no timestamps.
- **Truthful.** A missing manifest attribute is `NULL`, never a guess. Derived values say so: `exported_source` is `explicit` or `default`, and implied permissions carry `implied_by`.
- **Readable ids.** Code ids are smali descriptors, the format every Android tool understands:

| Kind | Example ID |
|---|---|
| class | `Lcom/app/Login;` |
| method | `Lcom/app/Login;->auth(Ljava/lang/String;)Z` |
| field | `Lcom/app/Login;->token:Ljava/lang/String;` |

- **Permissions.** `kind` is `uses-permission`, `uses-permission-sdk-23`, `implied` (granted by Android without being listed, the same rules as aapt2) or `declared` (defined by the app).
- **Calls.** One row per distinct (caller, callee, kind). `kind` is `virtual`, `static`, `direct`, `super`, `interface` or `polymorphic`. `offsets` are in 16-bit code units, as in `dexdump`, and at most 32 are kept (`count` is always exact).
- **Methods.** `methods` holds every method reference: `defined = 1` for methods in the APK, 0 for external ones (framework, other libraries).
- **Duplicates.** When a class is defined in several dex files, the first one wins, as on Android.

## Example queries

```sql
-- Who calls Cipher.getInstance?
SELECT caller, count FROM call_edges WHERE callee LIKE 'Ljavax/crypto/Cipher;->getInstance%';

-- Exported components that need no permission
SELECT type, name FROM components WHERE exported = 1 AND enabled = 1 AND permission IS NULL;

-- Which layouts use the string app_name?
SELECT "from" FROM resource_refs WHERE ref = '@string/app_name' AND from_kind = 'layout';

-- Which methods show the string app_name, and with which call?
SELECT "from" AS method, attribute AS call FROM resource_refs WHERE ref = '@string/app_name' AND from_kind = 'code';

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

-- A full component record, exactly as built
SELECT json FROM records WHERE kind = 'component' LIMIT 1;
```

## Size and speed

Typical apps extract in 1–5 seconds into 30–200 MB. The largest corpus apps (60k+ classes) take 15–25 seconds, mostly to build the full-text index. `--no-code` skips the code tables and takes well under a second.

## The APK cache

Given an APK instead of a `.sqlite` file, a command indexes it once into a cache keyed by the APK's SHA-256 (the platform's user cache folder, or `SEXTANT_CACHE`) and reads it from there on every later run. `sextant cache` lists the cached apps; `sextant cache --clear` deletes them. A new Sextant version rebuilds an index on next use.
