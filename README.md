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
- Python 3.10+ for the data generator (Pillow recommended, see below).

## Build & test

```bash
./gradlew :app:assembleDebug            # build the debug APK
./gradlew :app:testDebugUnitTest        # unit tests
./scripts/pre-commit.sh                 # the pre-commit definition (generator + unit tests)
./gradlew :app:connectedDebugAndroidTest  # instrumented UI tests (needs a device)
```

## Data pipeline

`/home/kirill/Music/LIBRARY.html` is the **read-only** source of truth. The project-local
Kilo skill **`update-library`** regenerates every data asset:

```bash
# run with any Python that has Pillow for normalized WebP covers
python3 .kilo/skills/update-library/generate_library.py --check --dry-run
python3 .kilo/skills/update-library/generate_library.py
```

It writes `app/src/main/assets/library/{library.json,lyrics.json,covers/…}` — generated
output that must **never** be hand-edited. The generator hashes `LIBRARY.html` and
short-circuits when unchanged (`--force` overrides). See
`.kilo/skills/update-library/SKILL.md` for the full contract.

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
