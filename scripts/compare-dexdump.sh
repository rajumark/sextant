#!/usr/bin/env bash
# Cross-check sextant's code counts against Google's dexdump for every corpus APK.
#   scripts/compare-dexdump.sh [sweep-dir]   (run scripts/sweep.sh first; default build/sweep)
# Checks: classes (+ duplicates) == dexdump class_defs; methods + fields == dexdump member definitions.
set -uo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
out="${1:-$root/build/sweep}"
dexdump="$(ls -d "${ANDROID_HOME:-$HOME/Library/Android/sdk}"/build-tools/*/dexdump 2>/dev/null | sort -V | tail -1)"
[[ -x "$dexdump" ]] || { echo "dexdump not found (set ANDROID_HOME)"; exit 1; }
work="$(mktemp -d)"; trap 'rm -rf "${work:?}"' EXIT

failed=0
for apk in "$root"/fixtures/external/*.apk; do
  name="$(basename "$apk" .apk)"
  meta="$out/$name/meta.json"
  [[ -f "$meta" ]] || { echo "✕ $name: no sweep output"; failed=1; continue; }
  rm -rf "${work:?}"/* && unzip -qo "$apk" 'classes*.dex' -d "$work" 2>/dev/null
  d_classes=0; d_members=0
  for dex in "$work"/classes*.dex; do
    [[ -f "$dex" ]] || continue
    dump="$("$dexdump" "$dex" 2>/dev/null)"
    d_classes=$(( d_classes + $(grep -c '^Class #' <<<"$dump") ))
    d_members=$(( d_members + $(grep -cE '^ +#[0-9]+ +: \(in L' <<<"$dump") ))
  done
  s_classes=$(jq '.code.classes + .code.duplicate_classes' "$meta")
  dup=$(jq '.code.duplicate_classes' "$meta")
  s_members=$(jq '.code.methods + .code.fields' "$meta")
  if [[ "$d_classes" == "$s_classes" && ( "$d_members" == "$s_members" || "$dup" != "0" ) ]]; then
    echo "✓ $name  classes $s_classes  members $s_members$( [[ $dup != 0 ]] && echo "  ($dup duplicate classes: member count not compared)")"
  else
    echo "✕ $name  classes dexdump=$d_classes sextant=$s_classes  members dexdump=$d_members sextant=$s_members"; failed=1
  fi
done
exit $failed
