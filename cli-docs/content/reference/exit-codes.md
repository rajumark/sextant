# Exit codes and errors

| Code | Meaning | Examples |
|---|---|---|
| `0` | Success | |
| `1` | Usage error | Unknown command or option, missing argument |
| `2` | Input error | File not found, not a zip, no `AndroidManifest.xml`, undecodable manifest, bad SQL, unknown class or method |
| `3` | Output error | Output folder not empty without `--force`, not writable |
| `4` | Internal error | A bug; please report it with the APK's sha256 |
| `5` | Findings gate | `findings --fail-on <severity>` found at least one finding at or above that severity |

Errors are printed to **stderr** with a hint when there is an obvious fix:

```text
$ sextant extract app.apk -o busy/
Error: output directory busy is not empty
hint: pass --force to overwrite Sextant's files in it
```

`--force` only replaces Sextant's own files in the folder; nothing else is touched or deleted.

For an internal error, set `SEXTANT_DEBUG=1` to print the stack trace:

```bash
SEXTANT_DEBUG=1 sextant extract app.apk -o out/
```

## Hostile and broken input

Sextant never executes anything from an APK and never writes outside the output folder. Inflation is capped (manifest 32 MB, `resources.arsc` 512 MB, each dex 256 MB, each native library 256 MB) as a zip-bomb guard. Malformed parts become entries in `meta.json → warnings` instead of crashes: a dex that can't be read, methods with unreadable bytecode, an undecodable resource table, an unsigned or invalid signature.
