# AGENTS.md — my-music-library

Instructions for agents working in this repository. Read this before making changes.

## What this project is

A native, offline, read-only **Android** app (Kotlin + Jetpack Compose, Material 3) that
presents the entire contents of a local music catalogue `LIBRARY.html` (artists, albums,
tracks, USB play position, lyrics, statistics) shipped as static assets inside the APK.
There is **no backend, no network, and no runtime dependency on the Music folder.**

The app data under `app/src/main/assets/library/` is **generated at build time** from
`LIBRARY.html` and is **git-ignored** — it is never committed. A small self-contained
`example/LIBRARY.html` is bundled so a fresh clone builds without the Music tree.

See `plans/PLAN.md` for the full design and `README.md` for the feature overview.

## Repository layout

```
.                              # Gradle root (Kotlin DSL + version catalog)
├─ app/                        # single Android module
│  └─ src/main/assets/library/ # GENERATED app data (git-ignored) — do not hand-edit
├─ example/LIBRARY.html        # tiny example source of truth (used when no real one)
├─ .kilo/skills/update-library/# generator skill (SKILL.md + generate_library.py + tests/)
├─ plans/PLAN.md               # source plan (read-only reference)
└─ scripts/pre-commit.sh       # the pre-commit command
```

## Source of truth

`/home/kirill/Music/LIBRARY.html` is the **read-only** source of truth for library data; the
bundled `example/LIBRARY.html` is the fallback. The app never reads either at runtime. The
generator auto-discovers the source (flags → `MUSIC_LIBRARY`/`MUSIC_SOURCE` env → the real
Music tree → the example). To refresh app data, run the `update-library` skill or let the
build generate it; never edit `app/src/main/assets/library/**` by hand.

## Build & test commands

- Build debug: `./gradlew :app:assembleDebug` (runs `generateLibraryAssets` before `preBuild`)
- Compile unit tests: `./gradlew :app:testDebugUnitTest`
- **Pre-commit (definition):** `./scripts/pre-commit.sh`
  It runs, in order:
  1. the generator's Python unit tests (`python3 -m unittest`),
  2. the data generator (so the Android tests have assets), and
  3. `./gradlew :app:testDebugUnitTest`.
  All must be green before any commit.
- Instrumented UI tests (`./gradlew :app:connectedDebugAndroidTest`) require a device/emulator
  and are run manually; they are not part of pre-commit.

A JDK 17+ is required. The Android SDK path lives in `local.properties` (git-ignored). Python
3.10+ is required for the generator; use `-Ppython=/path/to/python` (Pillow recommended).

## Commit style

Conventional Commits: `feat:`, `fix:`, `refactor:`, `perf:`, `test:`, `docs:`, `chore:`,
`build:`, `ci:`. Subject in the imperative, lower-case, no trailing period. One logical
change per commit. **Never push** unless explicitly asked.

## Changelog

`CHANGELOG.md` follows [Keep a Changelog](https://keepachangelog.com/) and Semantic
Versioning. Add notable **user-facing** changes to the `[Unreleased]` section under
`Added` / `Changed` / `Fixed` / `Removed`. Entries in English. The release version is
`1.0.0`.

## Never commit

- `TEMP.md` / `TEMP_*.md` — developer-only decision logs. They are git-ignored and must
  stay untracked.
- `app/src/main/assets/library/**` — generated app data; git-ignored, produced at build time.
- `local.properties`, `.gradle/`, `build/`, `.idea/`, `.kotlin/`, `__pycache__/`, any secrets
  or keystores.

## Generated data rules

- Regenerate with the skill or the build; the generator hashes `LIBRARY.html` and
  short-circuits when unchanged (`--force` overrides).
- Asset **filenames** must be ASCII (album ids); data **values** keep UTF-8
  (Cyrillic/CJK). The whole pipeline is UTF-8 end to end.
- The generator must never write to the Music tree and must never write outside `--out`.
- The generated assets are **not committed**; do not add them to git.
