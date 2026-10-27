# Usability Check: open an APK → first view (core wiring)
Date: 2026-10-27 · Mode: Review (with three small fixes applied, listed at the end) · Build: uncommitted
Data: real corpus (39 golden output folders; tuner, newpipe, obtainium extracted live) plus sample fixtures for dialog states

## Summary
The desktop app now runs the real engine in-process: `OutputWriter.extractTo` writes the same `<name>.sextant/` folder as `sextant extract`, and every permission and component line shown matches that folder byte for byte (checked by `CoreRepositoryTest`). The biggest problem is the analysis dialog: core has no progress callback and no cancel hook, so it shows elapsed time instead of real progress, and Cancel only stops waiting while the extraction keeps running. The best quick win is in core (a progress callback plus a cancellation check between stages), which a UI fix may not touch.

## Task scripts
| Script | Persona | Budget | Actual | Result |
|---|---|---|---|---|
| T12 First run → first useful insight | New user | ≤ 3, no dead end | 2 (Choose APK… → pick file), then the analysis opens itself on Overview with "Worth a look" | Pass, but no sample to try without an APK (see FR-01) |
| T10 Copy any identifier | All | 1 | 1 (copy buttons on package hash, names, links, fingerprints, file path) | Pass |
| T1 Release verdict on Overview | P2 | 0 actions | Insights are visible, but there is no single pass/attention verdict | Missing (opportunity, not in this scope) |

## Findings

### [Blocker] Analysis shows no real progress, and Cancel doesn't stop the work — PF-02
Where: `ui/home/Dialogs.kt` `ExtractionDialog`; `AppState.cancelExtraction`
What happens: the dialog shows an indeterminate bar and a seconds counter. Large apps (fennec, osmand) take a minute or more with no sign of which stage is running. Cancel closes the dialog, but the extraction runs to the end on a background thread, using CPU and writing the folder.
Who it hurts: everyone opening large APKs, most of all P1, who opens many.
Fix: needs core. `Extractor`/`OutputWriter.extractTo` should take an optional progress listener (stage + record counts) and check a cancellation flag between stages and every N code records. Not done here: `core` belongs to the CLI and was being changed by another session at the time. The UI already has the elapsed counter and a Cancel button to connect to it.

### [Major] No sample to try without an APK — FR-01
Where: `ui/home/HomeScreen.kt` `DropZone`
What happens: the home screen offers drop, Choose APK… and Open output folder…, but no "Try a sample". The former sample button was removed because sample data must never appear as real (EV-05).
Who it hurts: a new user without an APK at hand.
Fix: ship one small real F-Droid APK (for example tuner, about 1 MB, GPL) as an app resource and analyze it through the normal path. That needs a license and size decision, so it is written down here, not done.

### [Major] Old output folders may fail with a technical message — ER-04
Where: `data/SextantRepository.kt` `open` → `OutputReader.read`
What happens: a folder written by an older schema (before `implied_by`, `verified`, …) depends on whether core's JSON reader ignores or defaults missing fields. I did not verify this. If it fails, the dialog shows core's decoder message.
Fix: compare `meta.schema_version` with `Sextant.SCHEMA_VERSION` before reading, and offer "Analyze again" when the APK sits next to the folder.

### [Minor] Settings had no keyboard shortcut — KB-02
Fixed: File › Settings… with ⌘, (Ctrl+, elsewhere).

### [Minor] Split bundles were reported only as "Not an APK" — ER-02
Fixed: dropping .xapk/.apks/.apkm/.aab now says split bundles aren't supported yet and to open the bundle's base.apk. A test covers it.

### [Polish] The failure dialog repeated the output folder path
Fixed: the path is shown only while running or after success.

## Layered depth
| View | L0 verdict | L1 why | L2 raw | L3 query | Notes |
|---|---|---|---|---|---|
| Analysis dialog | Done, or the error | Hint line under the error | ✓ lines with real counts | Output folder path | L3 could be "Copy as CLI command" (AC-04) |
| Output | Folder and files | File descriptions | Capped preview (2,000 lines) | Terminal card with `sextant info`/`grep`/`jq` | `index.sqlite` shows how to query it |

## Passed
EV-02 (permission and component lines identical to the CLI files; counts from the same Extraction), EV-05 (no sample data in the app; fixtures only in tests), EV-06 (decoder warnings card on Overview), ER-01 (corrupt input → readable message and hint, tested), AC-05 (reveal in Finder/Explorer), KB-04 (native menu bar), PF-01 (lazy lists; DuckDuckGo 153 components, Signal 41,276-class stats render fine), PF-03 (opening a folder: slowest corpus app 26 ms, vlc), VA-01 (theme roles only).

## Not checked
- PF-04 and the large-APK timings in the live window: run in tests only, not timed by hand in the GUI.
- Drag and drop in the live window: the drop path (`AppState.openPath`) is tested, the OS drag gesture isn't.
- Native file dialogs: they can't be driven headlessly; the code path after picking is tested.
- Dark theme for the new dialogs: light screenshots only this time.
- KB-01/KB-03/FD-01 (keyboard completeness, palette): out of scope; the palette doesn't exist yet.
