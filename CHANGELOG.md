# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-10-10

### Added

- Native, offline, read-only Android app (Kotlin + Jetpack Compose, Material 3) presenting the
  full music catalogue: artists, albums, discs, tracks, USB play positions, lyrics, statistics.
- Adaptive navigation (bottom bar ↔ navigation rail) with Home, Artists, Albums, Tracks, and
  Lyrics destinations, plus On USB, Search, and Settings/About.
- Album cover art wherever available: embedded thumbnails and covers recovered from disk,
  normalized to `covers/<albumId>.webp` (≤1024 px, WebP q82); deterministic gradient + initials
  placeholder only when no art exists.
- Global search across artists, albums, and tracks; album matches reveal their tracks.
- Material You dynamic colour (Android 12+), curated fallback scheme, light/dark/system modes,
  and an edge-to-edge layout.
- Project-local `update-library` Kilo skill (`generate_library.py`) that regenerates
  `library.json`, `lyrics.json`, and all cover assets from `LIBRARY.html`, with source
  auto-discovery, hashing/short-circuit, validation, atomic writes, and referential-integrity
  verification.
- App data is **generated at build time** (the `generateLibraryAssets` Gradle task runs before
  `preBuild`) and is **git-ignored** — never committed. A self-contained `example/LIBRARY.html`
  is bundled so a fresh clone builds without the Music tree; the generator discovers its source
  from `--library`/`--source`, the `MUSIC_LIBRARY`/`MUSIC_SOURCE` environment variables, or the
  real `/home/kirill/Music/LIBRARY.html` when present, falling back to the example.
- Unit tests (data contract over the generated assets, formatting, search, USB ordering),
  generator tests, an instrumented navigation smoke test, and the `scripts/pre-commit.sh` gate.
