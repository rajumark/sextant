#!/usr/bin/env bash
# Cross-check sextant's output against Google's aapt2 for every corpus APK.
#   scripts/compare-aapt2.sh [sweep-dir]   (run scripts/sweep.sh first; default build/sweep)
# Compares package, versionCode, versionName, minSdk, targetSdk, label, requested permission names, and that every
# per-density icon file aapt2 picks (application-icon-*) is among the files Sextant resolved for android:icon,
# optional features and required/optional libraries (exact), and that every required feature is listed by aapt2
# (aapt2 also adds implied features, so that one is a subset check).
set -uo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
out="${1:-$root/build/sweep}"
aapt2="$(ls -d "${ANDROID_HOME:-$HOME/Library/Android/sdk}"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1)"
[[ -x "$aapt2" ]] || { echo "aapt2 not found (set ANDROID_HOME)"; exit 1; }

failed=0
for apk in "$root"/fixtures/external/*.apk; do
  name="$(basename "$apk" .apk)"
  meta="$out/$name/meta.json"
  [[ -f "$meta" ]] || { echo "✕ $name: no sweep output"; failed=1; continue; }
  badging="$("$aapt2" dump badging "$apk" 2>/dev/null)"
  a_pkg=$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <<<"$badging")
  a_vc=$(sed -n "s/^package: .* versionCode='\([^']*\)'.*/\1/p" <<<"$badging")
  a_vn=$(sed -n "s/^package: .* versionName='\([^']*\)'.*/\1/p" <<<"$badging")
  a_min=$(sed -n "s/^minSdkVersion:'\([^']*\)'.*/\1/p" <<<"$badging")
  a_target=$(sed -n "s/^targetSdkVersion:'\([^']*\)'.*/\1/p" <<<"$badging")
  a_label=$(sed -n "s/^application-label:'\(.*\)'$/\1/p" <<<"$badging")
  a_feat_opt=$(sed -n "s/^ *uses-feature-not-required: name='\([^']*\)'.*/\1/p" <<<"$badging" | sort -u)
  a_feat_req=$(sed -n "s/^ *uses-feature: name='\([^']*\)'.*/\1/p" <<<"$badging" | sort -u)
  a_lib_req=$(sed -n "s/^uses-library:'\([^']*\)'.*/\1/p" <<<"$badging" | sort -u)
  a_lib_opt=$(sed -n "s/^uses-library-not-required:'\([^']*\)'.*/\1/p" <<<"$badging" | sort -u)
  a_icons=$(sed -n "s/^application-icon-[0-9]*:'\(.*\)'$/\1/p" <<<"$badging" | sort -u)
  a_perms=$(sed -n "s/^uses-permission\(-sdk-23\)\{0,1\}: name='\([^']*\)'.*/\2/p" <<<"$badging" | sort -u)

  s_pkg=$(jq -r '.package // ""' "$meta")
  s_vc=$(jq -r '.version_code // ""' "$meta")
  s_vn=$(jq -r '.version_name // ""' "$meta")
  s_min=$(jq -r '.min_sdk // ""' "$meta")
  s_target=$(jq -r '.target_sdk // ""' "$meta")
  s_label=$(jq -r '.label // ""' "$meta")
  feats="$out/$name/features.jsonl"
  s_feat_opt=$(jq -r 'select(.kind=="feature" and .required==false and .name!="glEsVersion") | .name' "$feats" | sort -u)
  s_feat_req=$(jq -r 'select(.kind=="feature" and .required!=false and .name!="glEsVersion") | .name' "$feats" | sort -u)
  s_lib_req=$(jq -r 'select(.kind=="library" and .required!=false) | .name' "$feats" | sort -u)
  s_lib_opt=$(jq -r 'select(.kind=="library" and .required==false) | .name' "$feats" | sort -u)
  s_icons=$(jq -r '.icons.icon.files[]?.path' "$meta" | sort -u)
  s_perms=$(jq -r 'select(.kind != "declared") | .name' "$out/$name/permissions.jsonl" | sort -u)

  diffs=()
  [[ "$a_pkg" == "$s_pkg" ]] || diffs+=("package aapt2=$a_pkg sextant=$s_pkg")
  [[ "$a_vc" == "$s_vc" ]] || diffs+=("versionCode aapt2=$a_vc sextant=$s_vc")
  [[ "$a_vn" == "$s_vn" ]] || diffs+=("versionName aapt2=$a_vn sextant=$s_vn")
  [[ "$a_min" == "$s_min" ]] || diffs+=("minSdk aapt2=$a_min sextant=$s_min")
  [[ "$a_target" == "$s_target" ]] || diffs+=("targetSdk aapt2=$a_target sextant=$s_target")
  [[ "$a_label" == "$s_label" ]] || diffs+=("label aapt2=$a_label sextant=$s_label")
  if [[ "$a_perms" != "$s_perms" ]]; then
    diffs+=("permissions: $(diff <(echo "$a_perms") <(echo "$s_perms") | grep '^[<>]' | tr '\n' ' ')")
  fi
  [[ "$a_feat_opt" == "$s_feat_opt" ]] || diffs+=("optional features: $(diff <(echo "$a_feat_opt") <(echo "$s_feat_opt") | grep '^[<>]' | tr '\n' ' ')")
  [[ "$a_lib_req" == "$s_lib_req" ]] || diffs+=("required libraries: $(diff <(echo "$a_lib_req") <(echo "$s_lib_req") | grep '^[<>]' | tr '\n' ' ')")
  [[ "$a_lib_opt" == "$s_lib_opt" ]] || diffs+=("optional libraries: $(diff <(echo "$a_lib_opt") <(echo "$s_lib_opt") | grep '^[<>]' | tr '\n' ' ')")
  missing_req=$(comm -13 <(echo "$a_feat_req") <(echo "$s_feat_req") | grep -v '^$' | tr '\n' ' ')
  [[ -z "$missing_req" ]] || diffs+=("required features aapt2 does not list: $missing_req")
  missing_icons=$(comm -23 <(echo "$a_icons") <(echo "$s_icons") | grep -v '^$' | tr '\n' ' ')
  [[ -z "$missing_icons" ]] || diffs+=("icon files aapt2 picks but sextant lacks: $missing_icons")
  if (( ${#diffs[@]} )); then
    echo "✕ $name"; printf '    %s\n' "${diffs[@]}"; failed=1
  else
    echo "✓ $name ($(wc -l <<<"$s_perms" | tr -d ' ') permissions, $(grep -c . <<<"$a_icons") icon densities)"
  fi
done
exit $failed
