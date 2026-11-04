#!/usr/bin/env bash
# Run `sextant extract` over the whole real-APK corpus and report time, result and warnings per APK.
#   scripts/sweep.sh [--keep] [output-dir]      (default: build/sweep)
# APKs are processed one at a time into <out>/<name>.sqlite. After each one, the big file is replaced by a small
# one holding only the tables the cross-check scripts read (meta, features, permissions), so a full run needs about
# one large APK's worth of disk (≤ 2 GB) instead of tens of GB. --keep keeps the full files.
# Stops early when free disk space drops below SEXTANT_MIN_FREE_GB (default 5).
# Exit code is non-zero if any APK fails.
set -uo pipefail

keep=false
if [[ "${1:-}" == "--keep" ]]; then keep=true; shift; fi
root="$(cd "$(dirname "$0")/.." && pwd)"
bin="${SEXTANT_BIN:-$root/cli/build/install/sextant/bin/sextant}"
out="${1:-$root/build/sweep}"
min_free_gb="${SEXTANT_MIN_FREE_GB:-5}"

free_gb() { df -Pk "$1" | awk 'NR==2 {printf "%d", $4 / 1048576}'; }
[[ -x "$bin" ]] || { echo "build first: ./gradlew :cli:installShadowDist"; exit 1; }
rm -rf "$out" && mkdir -p "$out"

failed=0
printf "%-14s %4s %7s  %s\n" apk rc seconds result
for apk in "$root"/fixtures/external/*.apk; do
  name="$(basename "$apk" .apk)"
  if (( $(free_gb "$out") < min_free_gb )); then
    echo "stopping: only $(free_gb "$out") GB free (< $min_free_gb GB); free some space or set SEXTANT_MIN_FREE_GB"
    failed=1
    break
  fi
  start=$(perl -MTime::HiRes=time -e 'printf "%.3f", time')
  db="$out/$name.sqlite"
  "$bin" extract --quiet --force "$apk" -o "$db" 2>"$out/$name.err"
  rc=$?
  end=$(perl -MTime::HiRes=time -e 'printf "%.3f", time')
  secs=$(perl -e "printf '%.2f', $end - $start")
  if [[ $rc -eq 0 ]]; then
    summary="$("$bin" info "$db" | awk '/^(Package|Exported|Signing)/ {sub(/^[A-Za-z]+ +/, ""); printf "%s | ", $0}')"
  else
    summary="FAILED: $(head -3 "$out/$name.err" | tr '\n' ' ')"; failed=1
  fi
  printf "%-14s %4s %7s  %s\n" "$name" "$rc" "$secs" "$summary"
  if [[ -f "$db" ]]; then
    sqlite3 "$db" "SELECT value FROM meta WHERE key = 'meta_json'" | jq -r '.warnings[]? | "               ! " + .'
    if ! $keep; then
      # Keep only what compare-aapt2.sh / compare-dexdump.sh read, in a small SQLite file.
      small="$out/$name.small.sqlite"
      rm -f "$small"
      sqlite3 "$db" "ATTACH '$small' AS s; CREATE TABLE s.meta AS SELECT * FROM meta; CREATE TABLE s.features AS SELECT * FROM features; CREATE TABLE s.permissions AS SELECT * FROM permissions;"
      mv "$small" "$db"
    fi
  fi
done
exit $failed
