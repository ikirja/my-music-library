#!/usr/bin/env bash
# Pre-commit validation for my-music-library. All stages must pass before a commit.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> 1/3  Generator unit tests (python3 -m unittest)"
python3 -m unittest discover -s .kilo/skills/update-library/tests -p 'test_*.py' -v

# App data is generated at build time from LIBRARY.html and is git-ignored, so generate it
# before the Android tests. Prefer an interpreter that has Pillow (WebP cover normalization).
PY="${MUSICLIB_PYTHON:-}"
if [ -z "$PY" ]; then
  for cand in python3 "$HOME/.venvs/mkpfs/bin/python"; do
    if command -v "$cand" >/dev/null 2>&1 && "$cand" -c "import PIL" >/dev/null 2>&1; then
      PY="$cand"; break
    fi
  done
fi
PY="${PY:-python3}"
export MUSICLIB_PYTHON="$PY"

echo "==> 2/3  Generate app data ($PY .kilo/skills/update-library/generate_library.py)"
"$PY" .kilo/skills/update-library/generate_library.py

echo "==> 3/3  Android unit tests (./gradlew :app:testDebugUnitTest)"
if [ -z "${JAVA_HOME:-}" ]; then
  for cand in "$HOME/.jdks/ms-21.0.12.1" /usr/lib/jvm/*; do
    [ -x "$cand/bin/java" ] && export JAVA_HOME="$cand" && break
  done
fi
./gradlew --no-daemon :app:testDebugUnitTest

echo "==> pre-commit OK"
