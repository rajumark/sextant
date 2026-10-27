#!/usr/bin/env bash
# Run `sextant extract` over the whole real-APK corpus and report time, result and warnings per APK.
#   scripts/sweep.sh [--keep] [output-dir]      (default: build/sweep)
# APKs are processed one at a time. After each one, the large outputs (code/, index.sqlite, native symbols,
# resource values) are deleted and only the small files the cross-check scripts read are kept, so a full run needs
# about one APK's worth of disk (≤ 2 GB) instead of tens of GB. --keep keeps everything.
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
  "$bin" extract --quiet "$apk" -o "$out/$name" 2>"$out/$name.err"
  rc=$?
  end=$(perl -MTime::HiRes=time -e 'printf "%.3f", time')
  secs=$(perl -e "printf '%.2f', $end - $start")
  if [[ $rc -eq 0 ]]; then
    summary="$("$bin" info "$out/$name" | awk '/^(Package|Exported|Signing)/ {sub(/^[A-Za-z]+ +/, ""); printf "%s | ", $0}')"
  else
    summary="FAILED: $(head -3 "$out/$name.err" | tr '\n' ' ')"; failed=1
  fi
  printf "%-14s %4s %7s  %s\n" "$name" "$rc" "$secs" "$summary"
  if ! $keep; then
    # Keep only what compare-aapt2.sh / compare-dexdump.sh and the findings review read.
    rm -rf "${out:?}/$name/code" "${out:?}/$name/index.sqlite" "${out:?}/$name/native/symbols.jsonl" "${out:?}/$name/resources"
  fi
  if [[ -f "$out/$name/meta.json" ]]; then
    perl -0ne 'while (/"warnings": \[(.*?)\]/sg) { for (split /",\s*"/, $1) { s/^\s*"|"\s*$//g; print "               ! $_\n" if length } }' "$out/$name/meta.json"
  fi
done
exit $failed
