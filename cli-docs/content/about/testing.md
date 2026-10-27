# How Sextant is tested

Sextant is tested only against **real, downloaded APKs**: apps people actually ship, never APKs built for the test. The corpus is listed in `fixtures/external.lock` (URL + pinned sha256) and fetched with `scripts/fetch-apks.sh`.

## The corpus

46 APKs, chosen to cover the toolchains and shapes that break decoders:

| Kind | Apps |
|---|---|
| Kotlin / Java, small to huge | Binary Eye, Tuner, Open Camera, ConnectBot, Aurora Store, NewPipe, Seal, Mastodon, KeePassDX, DAVx5, App Manager, Termux:API, Nextcloud, Wikipedia, Element, Home Assistant, DuckDuckGo |
| R8-shrunk official builds | Signal |
| Native-heavy (C/C++, JNI, many ABIs) | Termux, Organic Maps, OsmAnd, Telegram X, Anki, Firefox (Fennec), VLC, PPSSPP, Minetest |
| Flutter | Obtainium, LocalSend, FluffyChat |
| React Native / Expo | Jitsi Meet, Standard Notes, Bluesky |
| Other frameworks | Bitwarden (.NET MAUI), Logseq (Capacitor), Godot, Shattered Pixel Dungeon (libGDX), Tux Paint (SDL), SuperTuxKart |
| Older releases (for `diff`) | Binary Eye, Aurora, NewPipe, Wikipedia, DAVx5, Bluesky, Signal |

## What runs

**Unit and integration tests** (`gradle test`) cover the manifest mapper, resource and icon resolution, code extraction, the index, search, call trees, reach, xref, insights, findings, diff, the CLI commands and the MCP server.

**Golden tests** extract every corpus APK and compare `meta.json`, the manifest files, insights and a digest of every code file with checked-in expectations in `fixtures/golden/`. Output must be byte-identical between runs.

**Cross-checks against Android's own tools:**

- `scripts/compare-aapt2.sh` compares package, versions, SDKs, label, permissions (including implied ones), launchable activity, features, libraries and icon files with `aapt2 dump badging`, on all 46 APKs.
- `scripts/compare-dexdump.sh` compares class, method and field counts and call sites with `dexdump`, on all 46 APKs.

**Sweep** (`scripts/sweep.sh`) runs the full CLI over the corpus one APK at a time, records time, result and warnings per APK, and fails if any APK fails. Outputs are trimmed after each APK so a full run fits in a few GB of disk (`SEXTANT_MIN_FREE_GB` stops it early when space runs low).

## Running it yourself

```bash
scripts/fetch-apks.sh          # ~3 GB download, verified by sha256
gradle test                    # unit + golden tests
scripts/sweep.sh               # full CLI over every APK
scripts/compare-aapt2.sh       # needs aapt2 from the Android SDK build-tools
scripts/compare-dexdump.sh     # needs dexdump from the build-tools
```
