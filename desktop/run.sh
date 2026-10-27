#!/bin/sh
# Runs the Sextant desktop app from source.
# Usage: ./run.sh            (from any directory)
set -e

cd "$(dirname "$0")/.."

if ! command -v gradle >/dev/null 2>&1; then
    echo "gradle not found. Install Gradle (brew install gradle) or add it to PATH." >&2
    exit 1
fi

exec gradle :desktop:run --console=plain "$@"
