#!/usr/bin/env bash
# Download and verify the real-APK test corpus listed in fixtures/external.lock.
#   scripts/fetch-apks.sh              download missing APKs, verify every sha256
#   scripts/fetch-apks.sh --pin        also fill in missing sha256 values in the lock file
#   scripts/fetch-apks.sh <id>...      only these entries
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
lock="$root/fixtures/external.lock"
dest="$root/fixtures/external"
mkdir -p "$dest"

pin=false
if [[ "${1:-}" == "--pin" ]]; then pin=true; shift; fi
only=("$@")

sha() {
  if command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1"; else sha256sum "$1"; fi | awk '{print $1}'
}
failed=0

while IFS=$'\t' read -r id kind url expected; do
  [[ -z "$id" || "$id" == \#* ]] && continue
  if (( ${#only[@]} )) && [[ ! " ${only[*]} " == *" $id "* ]]; then continue; fi
  file="$dest/$id.apk"
  if [[ ! -f "$file" ]]; then
    echo "↓ $id ($kind)"
    curl -fL --retry 3 --silent --show-error -o "$file.part" "$url"
    mv "$file.part" "$file"
  fi
  actual="$(sha "$file")"
  if [[ "$expected" == "-" ]]; then
    if $pin; then
      perl -pi -e "s/^\Q$id\E\t(.*)\t-\$/$id\t\$1\t$actual/" "$lock"
      echo "📌 $id $actual"
    else
      echo "? $id has no pinned sha256 (run with --pin)"; failed=1
    fi
  elif [[ "$actual" != "$expected" ]]; then
    echo "✕ $id sha256 mismatch: expected $expected, got $actual"; failed=1
  else
    echo "✓ $id"
  fi
done < "$lock"

exit $failed
