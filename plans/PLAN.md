# Music Library — Android App v1.0.0 Implementation Plan

Status: **Draft for approval** · Target release: **1.0.0** · Platform: native Android (Kotlin, Android Studio)
Source of truth: `/home/kirill/Music/LIBRARY.html` (read-only)

> **Implementation note (2026-10-10).** Where this plan says generated assets live "in repo"
> (e.g. §6.1, milestone M1), the shipped implementation deviates deliberately: app data is
> **generated at build time** and is **git-ignored**. A bundled `example/LIBRARY.html` lets a
> fresh clone build without the Music tree. See `README.md`, `AGENTS.md`, and `CHANGELOG.md`.

---

## 1. Summary

Build a native, offline, read-only Android app that presents the entire contents of the
local `LIBRARY.html` music catalogue with a modern **Material You / Material 3 Expressive**
design. All data is **static**: it is generated from `LIBRARY.html` into app assets by a
project-local Kilo skill, **`update-library`**, and shipped inside the APK. There is no
backend, no network, and no runtime dependency on the Music folder.

The app exposes, as first-class destinations, everything present in `LIBRARY.html`:
**artists, albums, the full track list, the USB play position, and lyrics** — plus a
global search and library statistics.

**Album cover art is a required feature wherever art is available.** Embedded thumbnails in
`LIBRARY.html` are used directly; for albums that only have a cover file on disk, the
`update-library` skill recovers it from the album folder (`cover.*`, `folder.*`, `front.*`, …)
and normalizes it. Only albums with no art anywhere fall back to a generated gradient +
initials placeholder (matching the HTML's `hue()`/`initials()` behaviour).

---

## 2. Source of truth: `LIBRARY.html` analysis

### 2.1 File facts
- Path: `/home/kirill/Music/LIBRARY.html`, ~4.97 MB, 421 lines (data lives on two very long lines).
- Structure: single self-contained HTML page (dark theme, cards, chips nav, search,
  collapsible albums, lyrics modal, print stylesheet) with embedded JS data:
  - `const DATA = [ … ]` — the catalogue tree (line 244, ~3.6 MB; includes base64 cover thumbnails).
  - `const LYRICS = { … }` — lyrics keyed by track `file` path (line 245, ~1.3 MB).
  - Rendering code (lines 246–421) defines the feature set to reproduce.

### 2.2 `DATA` schema (exact)

```
Artist   { name: String, albums: Album[] }
Album    { folder: String, year: Int, title: String, discs: Disc[],
           tracks: Int, dur: Float(seconds), cover_src: String?, thumb: String? }
Disc     { name: String?, tracks: Track[] }        // name null = single disc
Track    { title: String, n: Int, dur: Float(seconds), size: Int(bytes),
           file: String, usb: Int? }                // usb null = "N/A"
```

- `cover_src` — relative, URL-encoded path to the original cover (e.g. `Agata%20Kristi/…/cover.jpg`), may be absent.
- `thumb` — base64 `data:image/jpeg;base64,…` downscaled cover, present on 86/115 albums.
- `usb` — **1-based global position in the USB play order**, or `null` = N/A (not on the stick).

### 2.3 `LYRICS` schema (exact)

```
LYRICS[<track.file>] = { l: String (lyrics text, "\n"-separated),
                         s: String? (source: "lrclib" | "lrclib-search" | "lrclib-title"),
                         r: Bool?   (true → romaji, shown as a "Romaji" badge) }
```

### 2.4 Current library statistics (from the file)

| Metric | Value |
|---|---|
| Artists | 14 |
| Albums | 115 |
| Discs | 129 (14 multi-disc albums) |
| Tracks | 1,578 |
| Total duration | ≈ 117 h 58 m |
| Total source size | ≈ 72.55 GB (lossless) |
| Lyrics entries | 1,030 (907 `lrclib`, 112 `lrclib-search`, 11 `lrclib-title`; 434 romaji) |
| Albums with **embedded** art (`thumb`) | 86 |
| Albums with a cover **on disk only** | present for many of the 29 without a thumb — e.g. Metallica `Front.jpg`, Gackt `front.jpg`/`Scans/front.jpg`, Zemfira `Cover.jpg`, several Marilyn Manson/PIERROT `cover.jpg`; the skill recovers these |
| Albums with **no art anywhere** | few; app renders the gradient + initials placeholder |
| `usb` populated | **none today** (all `null`) → USB screen needs a good empty state |
| Year range | 1983 – 2025 |
| Characters | Cyrillic + CJK present in titles/lyrics (must stay UTF-8) |

### 2.5 Semantics to preserve
- **Play order** (from the page footer): artist **A→Z** → album **by release year** → disc → track.
  The `usb` number is the 1-based position of a track in that global order.
- **Album covers**: an album shows its cover image when one is available and falls back to a
  deterministic gradient (from a hash of the title) with album initials otherwise. **The app must
  bundle and display cover art for every album where art is available** — from the embedded `thumb`
  and from a disk image for albums that only have a file. Album covers appear on the Albums grid,
  artist album cards, the album-detail hero, the track/lyrics header, and the On-USB list.
- **Lyrics availability**: a track row shows a play/lyric marker when `LYRICS[file].l` exists;
  the lyrics view shows a "Romaji" badge when `r` is true and the `source` in the footer.
- **Sizes/durations**: use the same formatting rules (`m:ss` for tracks, `h mm` for totals,
  MB with 0 decimals ≥ 10 MB else 1 decimal).

### 2.6 Quirks / constraints
- Data lines are JavaScript object literals (valid JSON after stripping `const NAME = ` and the
  trailing `;`); the generator must slice reliably, not regex the whole file.
- `usb` is written back by the separate `copy-to-usb` skill; the app data is regenerated only
  when `update-library` is run, so USB info is a snapshot (documented in About).
- Filenames in the Music tree are ASCII, but **data values contain Cyrillic/CJK**. The app and
  assets must be UTF-8 throughout; only generated **asset filenames** must stay ASCII.
- `cover_src` points at files under the Music tree; the app must **not** depend on those paths at
  runtime — every cover is baked into assets by the skill. On-disk cover names are inconsistent
  and case-varied (`cover.jpg`, `folder.jpg`, `front.jpg`, `Front.jpg`, `Cover.jpg`, sometimes in
  an `ArtWork/` or `Scans/` subfolder), so the skill scans case-insensitively.

---

## 3. Goals and non-goals

### Goals (v1.0.0)
1. Browse all 14 artists, 115 albums, and 1,578 tracks offline.
2. Display **album cover art wherever available** (embedded thumbnail or recovered from disk),
   with a graceful gradient + initials placeholder only when no art exists.
3. Show per-track number, duration, size, **USB position**, and lyrics presence.
4. Dedicated destinations for Artists, Albums, Tracks, Lyrics, and On-USB.
5. Global search across artists, albums, and tracks (matching the HTML behaviour).
6. Material You 3 (Expressive) design: dynamic color, adaptive layout, edge-to-edge, motion.
7. A project-local Kilo skill **`update-library`** that regenerates all app data (including cover
   assets) from `LIBRARY.html`.

### Non-goals (v1.0.0)
- No audio playback (library is descriptive metadata only).
- No editing of the library inside the app; edits happen in the Music folder.
- No network, accounts, cloud sync, or analytics.
- No USB copy logic in the app (stays in the existing `copy-to-usb` skill).

---

## 4. Architecture and tech stack

Single-module app, clean layering, unidirectional data flow.

| Concern | Choice |
|---|---|
| Language | Kotlin 2.x, JDK 17 |
| Build | Gradle (Kotlin DSL) + version catalog `libs.versions.toml` |
| UI | Jetpack Compose + **Material 3 Expressive** (`androidx.compose.material3`), Compose BOM |
| Adaptive UI | `androidx.compose.material3.adaptive` `NavigationSuiteScaffold` (bar ↔ rail ↔ drawer) |
| Navigation | `androidx.navigation:navigation-compose` (type-safe routes) |
| Serialization | `kotlinx.serialization` (JSON assets → models) |
| Images | Coil 3 (`coil-compose`) loading `file:///android_asset/covers/…` + memory/disk cache |
| DI | Hilt (small app; optional — manual DI is acceptable) |
| Concurrency | Coroutines + `StateFlow`; asset parse off main thread |
| Settings | `androidx.datastore` (theme mode, dynamic-color toggle) |
| Testing | JUnit, Turbine, Compose UI test, Roborazzi (screenshots, optional) |
| SDK | `minSdk 24`, `compileSdk`/`targetSdk` = latest stable (36) |
| Package | `com.kirill.musiclibrary` (configurable) |

**Layering**
- `data`: asset loader + repository (in-memory, read-only).
- `domain`: pure models + use cases (search, USB ordering, stats).
- `ui`: screens, ViewModels, theme, components.

**No Room / no database.** Data is immutable and read-only, so a parsed in-memory graph with
`StateFlow` is simpler, faster, and adequate. (Decision D1.)

---

## 5. Project structure

```
my-music-library/
├─ AGENTS.md                         # project instructions (new)
├─ plans/PLAN.md                     # THIS file
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle/libs.versions.toml
├─ .kilo/
│  └─ skills/
│     └─ update-library/
│        ├─ SKILL.md                 # skill frontmatter + workflow
│        └─ generate_library.py      # LIBRARY.html → app assets (incl. covers)
└─ app/
   ├─ build.gradle.kts
   └─ src/main/
      ├─ assets/library/            # GENERATED (do not hand-edit)
      │  ├─ library.json            # meta + artist/album/disc/track tree (+ coverAsset)
      │  ├─ lyrics.json             # trackId → { text, romaji, source }
      │  └─ covers/<albumId>.webp   # one cover per album where art is available
      ├─ res/                       # themes, launcher icon, strings
      └─ java/com/kirill/musiclibrary/
         ├─ MusicLibraryApp.kt
         ├─ MainActivity.kt
         ├─ data/  (AssetLibrarySource, LibraryRepositoryImpl, dto/)
         ├─ domain/ (model/, SearchUseCase, UsbOrderUseCase, LibraryStats)
         └─ ui/
            ├─ theme/ (Color, Type, Theme, ExpressiveShapes)
            ├─ nav/   (Destinations, LibraryNavHost, AdaptiveScaffold)
            └─ screens/ (Home, ArtistDetail, AlbumDetail, Tracks, Lyrics,
                         Usb, Search, Settings) + components/
```

> `app/src/main/assets/library/` is **generated output** owned by the `update-library` skill.

---

## 6. Data pipeline and assets

### 6.1 Generated assets
| Asset | Content | Approx. size |
|---|---|---|
| `library.json` | schema meta + full artist tree with stable ids and `coverAsset` | ~0.4–0.6 MB |
| `lyrics.json` | `trackId → { text, romaji, source }` (1,030 entries) | ~1.2–1.4 MB |
| `covers/<albumId>.webp` | one cover per album where art is available (embedded `thumb` or a disk image), max 1024 px, WebP q82 | ~4–7 MB |

Total added to the APK: **~6–9 MB** (Android compresses assets in the APK; JSON compresses well).
Hi-res originals (up to ~45 MB on disk) are **not** bundled — covers are normalized downscaled copies.

### 6.2 JSON contract (frozen for v1.0.0)

```jsonc
// library.json
{
  "schemaVersion": 1,
  "generatedAt": "2026-10-10T05:00:00Z",
  "sourceHash": "sha256:…",          // hash of LIBRARY.html at generation time
  "counts": { "artists": 14, "albums": 115, "discs": 129, "tracks": 1578,
              "lyrics": 1030, "covers": 0, "durationSeconds": 424690 },
  "artists": [{
    "id": "agata-kristi",
    "name": "Agata Kristi",
    "albums": [{
      "id": "agata-kristi-1995-opium",
      "folder": "Agata Kristi - 1995 - Opium",
      "title": "Opium", "year": 1995,
      "trackCount": 13, "durationSeconds": 3152.9,
      "coverAsset": "covers/agata-kristi-1995-opium.webp",   // null only if no art anywhere
      "discs": [{
        "name": null,
        "tracks": [{
          "id": "t-3f9a1c2b8d70",       // stable: sha1(file)[:12]
          "title": "ХалиГалиКришна",     // UTF-8 preserved
          "n": 1, "durationSeconds": 341.5, "sizeBytes": 38871632,
          "file": "Agata Kristi/…/01 - KhaliGaliKrishna.flac",
          "usb": null,                   // int position or null
          "hasLyrics": true
        }]
      }]
    }]
  }]
}
```

```jsonc
// lyrics.json  (trackId → lyric)
{ "t-3f9a1c2b8d70": { "text": "…\n…", "romaji": false,
                      "source": "lrclib", "language": null } }
```

> `counts.covers` is filled with the actual number of cover assets written (target: every album
> that has art either embedded or on disk).

### 6.3 Kotlin models (shape)

```kotlin
@Serializable data class Library(val schemaVersion:Int, val generatedAt:String,
    val sourceHash:String, val counts:Counts, val artists:List<Artist>)
@Serializable data class Artist(val id:String, val name:String, val albums:List<Album>)
@Serializable data class Album(val id:String, val folder:String, val title:String,
    val year:Int?, val trackCount:Int, val durationSeconds:Double,
    val coverAsset:String?, val discs:List<Disc>)
@Serializable data class Disc(val name:String?, val tracks:List<Track>)
@Serializable data class Track(val id:String, val title:String, val n:Int?,
    val durationSeconds:Double, val sizeBytes:Long, val file:String,
    val usb:Int?, val hasLyrics:Boolean)
@Serializable data class Lyric(val text:String, val romaji:Boolean, val source:String?)
```

### 6.4 Album art pipeline (required feature)
Album covers are required wherever art exists. The generator resolves art per album in priority
order and always writes one normalized asset:

1. **Embedded thumbnail** — decode `thumb` (base64 data URI) when present (86 albums).
2. **On-disk cover** — for albums without `thumb`, search the album folder recursively and
   case-insensitively, in priority order:
   `cover.*`, `folder.*`, `front.*`, `albumart.*`, `*/ArtWork/*`, `*/Scans/*front*`,
   else the largest image file in the folder. (Observed names include `cover.jpg`, `folder.jpg`,
   `front.jpg`, `Front.jpg`, `Cover.jpg`.)
3. **None** — leave `coverAsset = null`; the app renders the deterministic gradient + initials.

Normalization: fit to a square, downscale so the longest side ≤ **1024 px**, encode **WebP q82**
(overridable via `--cover-px`, `--cover-quality`, `--format`). Output filename is the ASCII album
id, so Unicode titles never appear in asset paths. `coverAsset` points at the asset; the app loads
it with Coil, downsamples for grids/lists, and derives a small blurred copy for the album-detail
hero backdrop.

### 6.5 Repository behaviour
- Parse `library.json` once on first access (off main thread), expose `StateFlow<LibraryUiState>`
  (Loading → Ready / Error).
- Flatten to derived indices on load: `allAlbums`, `allTracks`, `tracksByUsb` (usb ordered asc),
  `tracksWithLyrics`, `artistById`, `albumById`, `trackById`.
- Lyrics loaded **lazily** (first lyrics request) from `lyrics.json`, cached in memory.
- Search is a pure use case over pre-lowered, normalized text (artist + album + track).

---

## 7. `update-library` Kilo skill

**Purpose:** regenerate every data asset in the app — `library.json`, `lyrics.json`, and all
**cover images** — from the current `LIBRARY.html`. This is the only supported way to refresh
in-app data.

**Location:** `my-music-library/.kilo/skills/update-library/SKILL.md` (+ `generate_library.py`).
Project-local skills are discovered at `<project>/.kilo/skills/<name>/SKILL.md`.

### 7.1 SKILL.md frontmatter (planned)
```yaml
---
name: update-library
description: Regenerate the static Android app data (library.json, lyrics.json, and cover assets)
  from /home/kirill/Music/LIBRARY.html, the source of truth for the music library. Use when asked
  to update, refresh, rebuild, or re-sync the app's library data, or after copy-to-usb changes USB
  positions.
---
```

### 7.2 Generator script behaviour (`generate_library.py`)
Invocation:
```
python3 .kilo/skills/update-library/generate_library.py \
  [--source /home/kirill/Music] [--library /home/kirill/Music/LIBRARY.html] \
  [--out app/src/main/assets/library] [--check] [--dry-run] [--force] \
  [--cover-px 1024] [--cover-quality 82] [--format webp] [--no-covers]
```

Steps:
1. **Hash & short-circuit** — `sha256(LIBRARY.html)`; if unchanged vs `library.json.sourceHash`
   and no `--force`, report "up to date" and exit (keeps runs idempotent and fast).
2. **Parse** — slice `const DATA = …` up to `\nconst LYRICS` and `const LYRICS = …` to end of line;
   `json.loads` each. (Robust string slicing, not full-file regex.)
3. **Validate** — non-empty tree; every track has `file`, `dur`, `size`, `title`; counts match;
   every lyrics key maps to an existing track (drop/report orphans); note tracks lacking lyrics.
4. **Transform** — assign stable ids (`artist`/`album` slugs, `track = sha1(file)[:12]`), compute
   `hasLyrics`, per-album `trackCount`/`durationSeconds`, and library `counts`.
5. **Album art (required)** — for each album resolve art by priority (embedded `thumb` → on-disk
   cover scan → none), normalize to a square ≤`--cover-px` **WebP**, and write
   `covers/<albumId>.<fmt>`; set `coverAsset`. Report coverage (`n/115`) and list any album with
   no art anywhere. `--no-covers` skips asset writing (metadata still carries `coverAsset=null`).
6. **Lyrics** — emit `lyrics.json` (trackId keyed), preserving UTF-8.
7. **Write** — deterministic key ordering; atomic write (temp + rename); only ever writes inside
   `--out`. Emit `library.json` with meta (`generatedAt`, `sourceHash`, `counts`).
8. **Verify** — re-read and re-parse the generated files; assert counts and referential integrity
   (every non-null `coverAsset` file exists); fail loudly on any mismatch.
9. **Report** — "X artists · Y albums · Z tracks · L lyrics · C covers"; counts changed vs previous;
   warnings (albums with no art, no-lyrics tracks, orphan lyrics); output bytes.

Safety: never writes to the Music tree; refuses if `--out` overlaps `--source`; all asset filenames
ASCII; data values keep Unicode. Python 3 stdlib for parsing; **Pillow** is used for cover
normalization/WebP — if Pillow (or WebP support) is unavailable, fall back to writing the embedded
`thumb` bytes as `.jpg` and copying disk images as-is, and warn (JPEG fallback). Album-folder disk
scans are read-only.

### 7.3 Skill workflow (what the agent does when invoked)
1. Read the SKILL.md and confirm the Music root / `LIBRARY.html` path.
2. Run `--check --dry-run` to validate and show a plan (counts, cover coverage, warnings).
3. Run for real; then run the app's unit "asset contract" test (fast) to confirm the new assets parse.
4. Report counts, **cover coverage (`n/115` and any album without art)**, warnings, and any library
   `usb`-position changes.

---

## 8. UI / UX — Material You 3 (Expressive)

### 8.1 Theming
- **Dynamic color** via `dynamicLightColorScheme`/`dynamicDarkColorScheme` on Android 12+ (API 31);
  a curated fallback seed scheme (deep indigo / teal accent echoing the HTML palette) below it.
- Light / dark / **system** modes, plus a **dynamic color** on/off switch in Settings (DataStore).
- Edge-to-edge (`enableEdgeToEdge`), Material 3 **Expressive** type scale and expressive shape
  scale, tonal surfaces, and motion physics (springy transitions, expressive loading indicators).
- Compose BOM + `material3-adaptive` to target the newest stable M3 components, with graceful
  fallback to stable 1.x components where an Expressive API is unavailable.

### 8.2 Navigation (adaptive, from `NavigationSuiteScaffold`)
Primary destinations (phone bottom bar ↔ tablet rail ↔ expanded permanent drawer):
1. **Home** — hero stats (artists · albums · tracks · duration), a **featured cover-art carousel**,
   search entry, "On USB" summary, "Browse artists" and "By year" shortcuts.
2. **Artists** — A→Z list/grid with initial sections; tap → **Artist detail**.
3. **Albums** — all albums as a **cover-art grid** (cover image or placeholder); sort by
   year / artist / title; tap → **Album detail**.
4. **Tracks** — flat, searchable track list showing `n`, title, duration, size, USB badge, lyrics flag.
5. **Lyrics** — browse only tracks that have lyrics; search within lyrics titles.
`On USB` and `Settings` live in the top-app-bar overflow / "More" (or as rail items on wide screens)
to keep the phone bar within Material's 5-item guidance.

### 8.3 Screens
- **Artist detail**: header with name + stats (`N albums · M tracks · duration`) and a list of album
  cards, **each showing its cover art** (or placeholder).
- **Album detail**: cover-art hero — a blurred cover backdrop (like the HTML `.bg`) behind the
  **cover image**, title, year chip, and stat chips; collapsible disc sections for multi-disc
  albums; track rows with number, title, duration, size, **USB `#n` / `N/A`** badge (accent when
  set), and a lyrics indicator; tap → Track/Lyrics.
- **Track / Lyrics detail**: **cover thumb**, `title`, `artist — album · year`, scrollable lyrics
  (`pre-wrap`, selectable, scroll-to-top on open), **Romaji** badge, footer
  (`source · duration · size · USB position`), "No lyrics found" empty state.
- **On USB**: tracks ordered by `usb` ascending, grouped by artist/album, **each with its cover
  thumb**; empty state explains the stick is not populated yet and points to the `copy-to-usb`
  skill. Auto-updates when data is regenerated.
- **Search**: Material 3 `SearchBar`/`DockedSearchBar` with live results across artists, albums,
  tracks, and lyrics; matches the HTML's combined matching (album match reveals its tracks).
- **Settings/About**: library stats, `generatedAt`, `sourceHash`, **cover coverage**, lyrics
  coverage, theme and dynamic-color controls, app version `1.0.0`.

### 8.4 Reproduced formatting rules
`m:ss` per track; `h mm` for long totals; MB with 0 decimals when ≥ 10 MB else 1; USB as `#n` /
`N/A`. **Covers show the image when available; otherwise a deterministic gradient + initials**
(from a hash of the album title) matching the HTML.

### 8.5 Accessibility & polish
TalkBack content descriptions (including album covers), ≥48 dp touch targets, dynamic font scaling,
colour-contrast-safe tonal surfaces, predictive back, and rotation/state preservation.

---

## 9. Testing strategy

- **Data contract (unit)**: parse generated `library.json`/`lyrics.json`; assert counts
  (14/115/129/1578/1030), every track id unique, every lyrics key resolves, `usb` ordering, and
  **every non-null `coverAsset` file exists**.
- **Generator (unit)**: run `generate_library.py` against a tiny fixture `LIBRARY.html` (with and
  without embedded `thumb`) plus a fixture cover file; assert deterministic output, disk-cover
  recovery, and the same schema.
- **Domain (unit)**: search matching rules; USB ordering; stats calculation.
- **UI (instrumented)**: navigation to each destination; album → track → lyrics flow; **cover art
  renders and placeholder appears when absent**; USB empty state; search; dark/light/dynamic theming.
- **Screenshots (Roborazzi, optional)** for visual regression of key screens.
- **Perf smoke**: cold-start parse time, cover loading in the Albums grid, and scroll jank budget
  on a low-end profile.

---

## 10. Milestones and tasks (v1.0.0)

| # | Milestone | Key deliverables | Acceptance |
|---|---|---|---|
| M0 | Project bootstrap | Gradle + catalog, Compose, M3 theme, `MainActivity`, adaptive scaffold, empty destinations | App builds & runs; 5 destinations navigate |
| M1 | Data pipeline + skill | `generate_library.py` (incl. cover extraction), `SKILL.md`, generated assets in repo | `--check` passes; assets parse; counts match HTML; covers recovered from disk |
| M2 | Data layer | DTOs, asset source, repository, indices, lazy lyrics, Coil cover loading | Unit "data contract" test green |
| M3 | Browse (+covers) | Home, Artists, Artist detail, Albums grid, Album detail, Tracks | All artists/albums/tracks visible with correct stats and cover art |
| M4 | Lyrics + search | Track/Lyrics detail, Lyrics destination, global Search | Lyrics render; romaji badge; search matches HTML |
| M5 | USB | On-USB destination, USB badges, empty state | Ordered by `usb`; N/A handling correct |
| M6 | Material You polish | Dynamic color, expressive shapes/motion, adaptive layouts, Settings | Works phone/tablet, light/dark, API 24 & 35 |
| M7 | Hardening & release | Tests, a11y, performance, release build, signing config, version 1.0.0 | Signed APK; tests green; docs updated |

Each milestone ends with a green build + tests and a short decision-log entry.

---

## 11. Risks and mitigations

| Risk | Mitigation |
|---|---|
| APK size from data + covers | Normalized downscaled WebP covers (max 1024 px), compressed assets, no hi-res bundling (~6–9 MB total) |
| Albums missing **embedded** art | Skill scans the album folder for `cover/folder/front` images by default; placeholder only when no art exists anywhere |
| Pillow / WebP unavailable in the environment | Fall back to embedded `thumb` as JPEG and copy disk images as-is; warn; app renders whatever asset exists |
| Lyrics memory/latency | Lazy-load `lyrics.json`, cache, keep out of initial parse |
| Dynamic color only on API 31+ | Curated fallback scheme + toggle; test API 24 and 35 |
| `usb` is all `null` today | Dedicated empty state; data refreshes via `update-library` after `copy-to-usb` |
| Data line parsing brittleness | Slice `const` blocks and `json.loads`; validate with fixture tests |
| Unicode (Cyrillic/CJK) corruption | UTF-8 end-to-end; ASCII rule applies only to generated asset filenames |
| Static snapshot staleness | About screen shows `generatedAt` + `sourceHash`; skill short-circuits on unchanged source |

---

## 12. Decisions recorded

- **D1** — Ship data as JSON assets + in-memory repository (no Room) — read-only static data.
- **D2** — Jetpack Compose + **Material 3 Expressive**, adaptive `NavigationSuiteScaffold`.
- **D3** — **Album cover art is required wherever available**: bundle one normalized WebP per album
  (embedded `thumb`, else recovered from the album folder), max 1024 px q82; gradient + initials
  placeholder only when no art exists anywhere.
- **D4** — The `update-library` skill lives at project `.kilo/skills/update-library/`.
- **D5** — Package `com.kirill.musiclibrary`; `minSdk 24`, `targetSdk` latest stable.
- **D6** — Data regeneration is a Python generator invoked by the skill; assets are generated
  output, never hand-edited.
- **D7** — Audio files stay on disk; the app shows metadata only (no playback in v1.0.0).

---

## 13. Appendix — quick reference

- **Play order**: artist A→Z → album (release year) → disc → track; `usb` = 1-based global position.
- **Artists (14)**: Agata Kristi, Ayumi Hamasaki, Dir en Grey, Gackt, Garbage, L'Arc~en~Ciel,
  Linkin Park, Marilyn Manson, Metallica, MUCC, Mumiy Troll, Mylene Farmer, PIERROT, Zemfira.
- **Covers**: 86 embedded thumbs; remainder recovered from on-disk `cover/folder/front` images;
  normalized to `covers/<albumId>.webp` (≤1024 px, q82); placeholder when no art.
- **Formatting**: duration `m:ss`/`h mm`; size MB (0 dp ≥10 MB, else 1 dp); USB `#n`/`N/A`.
- **Generated-at runtime**: nothing — all data (JSON + covers) is packaged at build time.
