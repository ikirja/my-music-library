# my-music-library

A native, **offline, read-only** Android app (Kotlin + Jetpack Compose, Material 3) that
presents the entire contents of the local music catalogue `LIBRARY.html` — artists, albums,
discs, tracks, the USB play position, lyrics, and library statistics — shipped as static
assets inside the APK.

There is **no backend, no network, and no runtime dependency on the Music folder.**

## Features

- Browse all **14 artists · 115 albums · 129 discs · 1,578 tracks** offline.
- **Album cover art wherever available** (87 albums): embedded thumbnails plus covers
  recovered from disk, normalized to `covers/<albumId>.webp` (≤1024 px, WebP q82). Albums
  with no art anywhere fall back to a deterministic gradient + initials placeholder.
- Per-track number, duration, size, **USB position** (`#n` / `N/A`), and a lyrics indicator.
- Dedicated destinations: **Home, Artists, Albums, Tracks, Lyrics**, plus **On USB**,
  **Search**, and **Settings/About**.
- Global search across artists, albums, and tracks (an album match reveals its tracks).
- Material 3 with Material You dynamic colour on Android 12+, a curated fallback scheme,
  light/dark/system modes, adaptive bottom bar ↔ navigation rail, and edge-to-edge layout.

Not in v1.0.0: audio playback, in-app editing, network/accounts, USB copy logic.

## Requirements

- JDK 17+ (JDK 21 works).
- Android SDK with platform 35 (path in `local.properties`, git-ignored).
- Python 3.10+ (a Pillow-equipped interpreter is recommended for WebP cover normalization).

## Build & test

App data is generated automatically before the build, so a fresh clone builds straight away
using the bundled `example/LIBRARY.html`:

```bash
./gradlew :app:assembleDebug            # generates app data, then builds the debug APK
./gradlew :app:testDebugUnitTest        # unit tests (generates data first)
./scripts/pre-commit.sh                 # the pre-commit definition (generator + unit tests)
./gradlew :app:connectedDebugAndroidTest  # instrumented UI tests (needs a device)
```

## Data pipeline (build-time, not committed)

The app data under `app/src/main/assets/library/` — `library.json`, `lyrics.json`, and
`covers/<albumId>.webp` — is **generated at build time** from a `LIBRARY.html` file and is
**git-ignored**. It is never committed, so app changes don't churn generated files and any
clone can build for any catalogue.

The source of truth is discovered automatically, in this order:

1. `--library` / `--source` flags, or the `MUSIC_LIBRARY` / `MUSIC_SOURCE` environment vars;
2. `/home/kirill/Music/LIBRARY.html` and `/home/kirill/Music` when present;
3. the bundled **`example/LIBRARY.html`** (a small, self-contained demo).

The `generateLibraryAssets` Gradle task runs the generator before `preBuild`, and
`scripts/pre-commit.sh` runs it before the Android unit tests.

To build with your own catalogue:

```bash
MUSIC_LIBRARY=/home/kirill/Music/LIBRARY.html MUSIC_SOURCE=/home/kirill/Music \
  ./gradlew :app:assembleDebug

# or run the generator directly (also: --check --dry-run, --force, --no-covers, …)
python3 .kilo/skills/update-library/generate_library.py
```

Covers are normalized to WebP q82 (≤1024 px) when Pillow is available; pick the interpreter
with `-Ppython=/path/to/python`. Without Pillow the generator falls back to copying raw images
and warns. The generator hashes `LIBRARY.html` and short-circuits when unchanged (`--force`
overrides). See `.kilo/skills/update-library/SKILL.md` for the full contract and
`example/LIBRARY.html` for the data format.

## Architecture

Single Gradle module, three layers, unidirectional data flow:

```
app/src/main/java/com/kirill/musiclibrary/
├─ data/     AssetLibrarySource, AssetLibraryRepository, SettingsRepository (DataStore)
├─ domain/   pure models, Format rules, LibraryIndex, SearchUseCase, UsbOrderUseCase
└─ ui/       theme, adaptive nav, screens, components (custom asset-cover loader)
```

No Room/database: the data is immutable and read-only, so the parsed in-memory graph with
`StateFlow` is simpler and adequate. DI is manual (no Hilt) for this small app.

See `plans/PLAN.md` for the full design and `AGENTS.md` for contributor rules.
