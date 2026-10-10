# AGENTS.md — my-music-library

Instructions for agents working in this repository. Read this before making changes.

## What this project is

A native, offline, read-only **Android** app (Kotlin + Jetpack Compose, Material 3) that
presents the entire contents of the local music catalogue `LIBRARY.html` (artists, albums,
tracks, USB play position, lyrics, statistics) shipped as static assets inside the APK.
There is **no backend, no network, and no runtime dependency on the Music folder.**

See `plans/PLAN.md` for the full design and `README.md` for the feature overview.

## Repository layout

```
.                              # Gradle root (Kotlin DSL + version catalog)
├─ app/                        # single Android module
│  └─ src/main/assets/library/ # GENERATED app data — do not hand-edit
├─ .kilo/skills/update-library/# generator skill (SKILL.md + generate_library.py)
├─ plans/PLAN.md               # source plan (read-only reference)
└─ scripts/pre-commit.sh       # the pre-commit command
```

## Source of truth

`/home/kirill/Music/LIBRARY.html` is the **read-only** source of truth for library data.
The app never reads it at runtime. To refresh app data, run the `update-library` skill;
never edit `app/src/main/assets/library/**` by hand.

## Build & test commands

- Build debug: `./gradlew :app:assembleDebug`
- Compile unit tests: `./gradlew :app:testDebugUnitTest`
- **Pre-commit (definition):** `./scripts/pre-commit.sh`
  It runs, in order:
  1. the generator's Python unit tests (`python3 -m unittest`), and
  2. `./gradlew :app:testDebugUnitTest`.
  Both must be green before any commit.
- Instrumented UI tests (`./gradlew :app:connectedDebugAndroidTest`) require a device/emulator
  and are run manually; they are not part of pre-commit.

A JDK 17+ is required. The Android SDK path lives in `local.properties` (git-ignored).

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
- `local.properties`, `.gradle/`, `build/`, `.idea/`, `.kotlin/`, any secrets or keystores.
- Generated `app/src/main/assets/library/**` is committed **only** via the `update-library`
  skill output; never edit it by hand.

## Generated data rules

- Regenerate with the skill; the generator hashes `LIBRARY.html` and short-circuits when
  unchanged (`--force` overrides).
- Asset **filenames** must be ASCII (album ids); data **values** keep UTF-8
  (Cyrillic/CJK). The whole pipeline is UTF-8 end to end.
- The generator must never write to the Music tree and must never write outside `--out`.
