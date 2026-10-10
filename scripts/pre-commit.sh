#!/usr/bin/env bash
# Pre-commit validation for my-music-library. Both stages must pass before a commit.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> 1/2  Generator unit tests (python3 -m unittest)"
python3 -m unittest discover -s .kilo/skills/update-library/tests -p 'test_*.py' -v

echo "==> 2/2  Android unit tests (./gradlew :app:testDebugUnitTest)"
if [ -z "${JAVA_HOME:-}" ]; then
  for cand in "$HOME/.jdks/ms-21.0.12.1" /usr/lib/jvm/*; do
    [ -x "$cand/bin/java" ] && export JAVA_HOME="$cand" && break
  done
fi
./gradlew --no-daemon :app:testDebugUnitTest

echo "==> pre-commit OK"
