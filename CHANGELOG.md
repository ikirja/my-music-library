# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- App data (`library.json`, `lyrics.json`, `covers/`) is now generated at build time from
  `LIBRARY.html` and is **no longer committed**; a self-contained `example/LIBRARY.html` is
  bundled so a fresh clone builds without the Music tree. The generator auto-discovers the
  real source (`MUSIC_LIBRARY`/`MUSIC_SOURCE` env or the default Music path) and falls back to
  the example.

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
  `library.json`, `lyrics.json`, and all cover assets from `/home/kirill/Music/LIBRARY.html`,
  with hashing/short-circuit, validation, atomic writes, and referential-integrity verification.
- Unit tests (data contract over the generated assets, formatting, search, USB ordering),
  generator tests, an instrumented navigation smoke test, and the `scripts/pre-commit.sh` gate.
