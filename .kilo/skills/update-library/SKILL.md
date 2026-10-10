---
name: update-library
description: Regenerate the static Android app data (library.json, lyrics.json, and cover assets)
  from /home/kirill/Music/LIBRARY.html, the source of truth for the music library. Use when asked
  to update, refresh, rebuild, or re-sync the app's library data, or after copy-to-usb changes USB
  positions.
---

# update-library

Regenerates **every** data asset in the app — `library.json`, `lyrics.json`, and one
normalized **cover** per album — from the read-only source of truth
`/home/kirill/Music/LIBRARY.html`. This is the only supported way to refresh in-app data;
never hand-edit `app/src/main/assets/library/**`.

## What it produces

- `app/src/main/assets/library/library.json` — schema meta (`schemaVersion`, `generatedAt`,
  `sourceHash`, `counts`) plus the artist → album → disc → track tree with stable ids and a
  `coverAsset` path per album.
- `app/src/main/assets/library/lyrics.json` — `trackId → { text, romaji, source }`.
- `app/src/main/assets/library/covers/<albumId>.webp` — one square cover per album where art
  exists (embedded `thumb`, else recovered from the album folder), longest side ≤ 1024 px,
  WebP q82.

## Requirements

- Python 3.10+ (stdlib only for parsing).
- **Pillow** with WebP support is strongly recommended for cover normalization. Any Python
  that can `import PIL` works; e.g. `/home/kirill/.venvs/mkpfs/bin/python` has it. Without
  Pillow (or WebP), the generator falls back to writing the embedded `thumb` bytes and
  copying on-disk cover images as-is (JPEG/copy fallback) and warns.

## Invocation

```bash
PY=.venv/bin/python    # or any python with Pillow; use `python3` if Pillow is installed
$PY .kilo/skills/update-library/generate_library.py \
  [--source /home/kirill/Music] [--library /home/kirill/Music/LIBRARY.html] \
  [--out app/src/main/assets/library] [--check] [--dry-run] [--force] \
  [--cover-px 1024] [--cover-quality 82] [--format webp] [--no-covers]
```

## Workflow

1. Confirm the Music root and `LIBRARY.html` path.
2. Run `--check --dry-run` to validate and print the plan (counts, cover coverage, warnings).
3. Run for real. It short-circuits when the source hash is unchanged (`--force` overrides).
   `--no-covers` skips cover asset writing and emits `coverAsset: null` for every album;
   such a partial file never short-circuits, so the next normal run restores the covers.
4. Run `./gradlew :app:testDebugUnitTest` to confirm the new assets parse (data-contract test).
5. Report counts, **cover coverage (`n/115`)** and any album without art, warnings, and any
   library `usb`-position changes.

## Guarantees

- The Music tree is **read-only**; the generator refuses to run when `--out` overlaps
  `--source` and only ever writes inside `--out`.
- Asset **filenames** are ASCII album ids; data **values** keep UTF-8 (Cyrillic/CJK).
- Writes are atomic (temp + rename) and deterministic; the result is re-read and verified
  (counts + referential integrity) before success is reported.
