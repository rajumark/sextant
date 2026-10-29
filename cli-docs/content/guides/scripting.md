# Scripting with SQL and JSON

Everything Sextant knows is in one SQLite file, available two ways: **SQL** over that file (with `sextant sql` or any SQLite tool) and **`--json` output** from commands.

## Saved queries

`sextant q` answers common questions without writing SQL. `sextant q` lists the queries, `sextant q <query>` prints the SQL so you can adapt it, and `sextant q <query> app.apk` runs it (`--json`, `--csv`, `--limit` as for `sql`):

```bash
sextant q unused-strings app.apk
sextant q password-fields app.apk
sextant q string-users app.apk --arg name=app_name
sextant q hosts app.apk --csv > hosts.csv
```

## Text files and JSON assets

Every text, JSON and textual XML file is a row in `file_text`, so assets and configs are searchable and JSON is queryable:

```bash
sextant files app.apk --grep "api_key"                       # matching lines with file and line number
sextant sql app.apk "SELECT path, json_extract(text, '$.version') FROM file_text WHERE json_valid(text)"
sextant sql app.apk "SELECT key, value FROM file_properties WHERE path LIKE '%firebase%'"
```

## What the tables mean

Every file explains itself: `schema_docs` has one row per table and view and one per column, with a description.

```bash
sextant sql app.apk "SELECT \"column\", type, description FROM schema_docs WHERE \"table\" = 'files'"
```

## SQL

`sextant sql` runs read-only SQL over the app's SQLite file. Output is a table by default, `--json` (one object per line) or `--csv`; it stops at 1000 rows unless you pass `--limit` (0 = no limit). `sextant sql app.sqlite --schema` prints every table and view, and the full schema is also in [SQLite index schema](../reference/index-schema.md).

```bash
sextant sql app.sqlite "SELECT type, count(*) AS n, sum(exported) AS exported FROM components GROUP BY type ORDER BY n DESC"
```

```text
type      n   exported
--------  --  --------
activity  13  7
service   2   1
provider  2   0
receiver  1   1
4 rows
```

Useful queries:

```sql
-- Who calls Cipher.getInstance?
SELECT caller, count FROM call_edges WHERE callee LIKE 'Ljavax/crypto/Cipher;->getInstance%';

-- Methods that load a URL
SELECT value, method FROM string_uses WHERE value LIKE 'https://%';

-- Biggest packages
SELECT package, count(*) AS n FROM classes GROUP BY package ORDER BY n DESC LIMIT 10;

-- Which Java methods are implemented in native code?
SELECT jni_class, jni_method, library FROM native_symbols WHERE jni_class IS NOT NULL;

-- Hosts referenced from code (not just resources)
SELECT e.host, count(*) FROM endpoints e JOIN endpoint_sources s ON s.endpoint = e.id WHERE s.kind = 'code' GROUP BY e.host;

-- Who writes a field
SELECT m.descriptor FROM field_refs r JOIN methods m ON m.id = r.method
WHERE r.field = 'Lcom/example/Session;->token:Ljava/lang/String;' AND r.kind = 'write';

-- Full-text search from SQL (what `find` uses)
SELECT i.kind, i.ref, i.text FROM search JOIN search_items i ON i.search_rowid = search.rowid WHERE search MATCH 'Cipher';
```

You can also open the `.sqlite` file with any SQLite tool (`sqlite3`, DB Browser for SQLite, Python's `sqlite3` module…).

## Many APKs

Extraction output is deterministic, so whole folders of APKs are easy to sweep:

```bash
for apk in apks/*.apk; do
  sextant extract --quiet --force "$apk" -o "out/$(basename "$apk" .apk).sqlite"
done

# Every app that uses a given SDK
for db in out/*.sqlite; do
  sqlite3 "$db" "SELECT '$db' FROM sdks WHERE name = 'Firebase Crashlytics'"
done
```

For fast manifest-only sweeps, add `--no-code` (well under a second per APK).
