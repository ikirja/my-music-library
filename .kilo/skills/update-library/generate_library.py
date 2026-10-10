#!/usr/bin/env python3
"""Regenerate the static Android app data from /home/kirill/Music/LIBRARY.html.

Produces, inside --out only:
  library.json           schema meta + artist/album/disc/track tree (+ coverAsset)
  lyrics.json            trackId -> { text, romaji, source }
  covers/<albumId>.<fmt> one normalized square cover per album where art is available

The Music tree is treated as strictly read-only. The script refuses to run if --out
overlaps --source. Asset filenames are ASCII (album ids); data values keep UTF-8.

Pillow is used for cover normalization/WebP. If Pillow (or WebP support) is missing,
it falls back to writing the embedded ``thumb`` bytes as ``.jpg`` and copying on-disk
cover images as-is, and warns (JPEG fallback).
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import io
import json
import os
import re
import sys
import datetime as _dt
from pathlib import Path

SCHEMA_VERSION = 1
IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".webp", ".bmp", ".gif", ".jfif", ".tif", ".tiff"}
COVER_NAME_PRIORITY = ["cover", "folder", "front", "albumart"]

# ---------------------------------------------------------------------------
# Pillow is optional.
# ---------------------------------------------------------------------------
try:  # pragma: no cover - exercised implicitly by the environment
    from PIL import Image, ImageOps

    try:
        from PIL import features as _pil_features

        _HAS_WEBP = bool(_pil_features.check("webp"))
    except Exception:  # pragma: no cover
        _HAS_WEBP = False
    _HAS_PIL = True
except Exception:  # pragma: no cover
    Image = None  # type: ignore
    ImageOps = None  # type: ignore
    _HAS_PIL = False
    _HAS_WEBP = False


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def slug_ascii(value: str) -> str:
    """Lower-case ASCII slug; falls back to a short hash for non-ASCII titles."""
    s = re.sub(r"[^a-z0-9]+", "-", str(value).lower()).strip("-")
    if not s:
        s = "x-" + hashlib.sha1(str(value).encode("utf-8")).hexdigest()[:8]
    return s


def track_id(file: str) -> str:
    return "t-" + hashlib.sha1(file.encode("utf-8")).hexdigest()[:12]


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def extract_const_line(text: str, name: str):
    """Slice ``const NAME = <json>;`` on a single physical line and parse it."""
    marker = f"const {name} = "
    start = text.index(marker) + len(marker)
    end = text.index("\n", start)
    chunk = text[start:end].strip()
    if chunk.endswith(";"):
        chunk = chunk[:-1].strip()
    return json.loads(chunk)


# ---------------------------------------------------------------------------
# Album art
# ---------------------------------------------------------------------------
def scan_disk_cover(album_dir: Path):
    """Case-insensitive recursive scan of an album folder, priority ordered."""
    if not album_dir or not album_dir.is_dir():
        return None
    files = [
        p
        for p in album_dir.rglob("*")
        if p.is_file() and p.suffix.lower() in IMAGE_EXTS
    ]
    if not files:
        return None

    def rank(p: Path):
        stem = p.stem.lower()
        rel = p.relative_to(album_dir).as_posix().lower()
        try:
            neg_size = -p.stat().st_size
        except OSError:
            neg_size = 0
        for i, name in enumerate(COVER_NAME_PRIORITY):
            if stem == name:
                return (i, neg_size)
        for i, name in enumerate(("artwork", "albumart")):
            if name in rel:
                return (10 + i, neg_size)
        if "scans" in rel and "front" in stem:
            return (20, neg_size)
        if "scans" in rel:
            return (21, neg_size)
        return (50, neg_size, rel)

    files.sort(key=rank)
    return files[0]


def _decode_data_uri(uri: str) -> bytes:
    payload = uri.split(",", 1)[1]
    return base64.b64decode(payload)


def _normalize_image_bytes(raw: bytes, px: int, quality: int, fmt: str) -> bytes:
    im = Image.open(io.BytesIO(raw))
    im = ImageOps.exif_transpose(im)
    if im.mode not in ("RGB", "L"):
        im = im.convert("RGB")
    side = min(px, max(im.width, im.height))
    im = ImageOps.fit(im, (side, side), method=Image.LANCZOS)
    out = io.BytesIO()
    save_kwargs = {"quality": quality, "method": 6}
    if fmt == "webp":
        im.save(out, format="WEBP", **save_kwargs)
    else:
        im.convert("RGB").save(out, format="JPEG", quality=quality)
    return out.getvalue()


def resolve_cover(album, album_dir: Path, out_dir: Path, album_id: str, args):
    """Return (coverAsset_or_None, warning_or_None). Writes the asset if resolved."""
    embedded = album.get("thumb")
    raw = None
    source_kind = None

    if embedded and isinstance(embedded, str) and embedded.startswith("data:"):
        raw = _decode_data_uri(embedded)
        source_kind = "thumb"

    if raw is None:
        disk = scan_disk_cover(album_dir)
        if disk is not None:
            raw = disk.read_bytes()
            source_kind = "disk:" + disk.name

    if raw is None:
        return None, None

    want_fmt = args.format
    can_convert = _HAS_PIL and (want_fmt != "webp" or _HAS_WEBP)
    ext = want_fmt if can_convert else "jpg"
    asset_rel = f"covers/{album_id}.{ext}"
    asset_path = out_dir / asset_rel

    if args.no_covers:
        return None, None
    if args.dry_run or args.check:
        return asset_rel, None

    try:
        if can_convert:
            data = _normalize_image_bytes(raw, args.cover_px, args.cover_quality, want_fmt)
            warning = None
        else:
            # JPEG fallback: embedded thumb bytes are written as-is; disk files are copied as-is.
            data = raw
            if want_fmt == "webp" and _HAS_PIL and not _HAS_WEBP:
                warning = f"{album_id}: WebP unavailable, wrote JPEG for {source_kind}"
            else:
                warning = f"{album_id}: Pillow unavailable, copied raw image ({source_kind})"
        asset_path.parent.mkdir(parents=True, exist_ok=True)
        tmp = asset_path.with_suffix(asset_path.suffix + ".tmp")
        tmp.write_bytes(data)
        os.replace(tmp, asset_path)
        return asset_rel, warning
    except Exception as exc:  # noqa: BLE001 - report and continue
        return None, f"{album_id}: cover failed from {source_kind}: {exc}"


# ---------------------------------------------------------------------------
# Transform
# ---------------------------------------------------------------------------
def build_library(data, lyrics_raw, source_hash: str, generated_at: str, out_dir: Path,
                  args):
    warnings = []
    used_album_ids = set()
    used_artist_ids = set()
    counts = {
        "artists": 0,
        "albums": 0,
        "discs": 0,
        "tracks": 0,
        "lyrics": 0,
        "covers": 0,
        "durationSeconds": 0,
    }
    lyrics_out = {}
    track_ids = set()

    artists_out = []
    for artist in data:
        name = artist.get("name", "")
        aid = slug_ascii(name)
        if aid in used_artist_ids:
            aid = f"{aid}-{len(used_artist_ids)}"
        used_artist_ids.add(aid)
        counts["artists"] += 1

        albums_out = []
        for album in artist.get("albums", []):
            title = album.get("title", "")
            year = album.get("year")
            folder = album.get("folder", "")
            alid = f"{aid}-{year if year is not None else 'na'}-{slug_ascii(title)}"
            if alid in used_album_ids:
                alid = f"{alid}-{len(used_album_ids)}"
            used_album_ids.add(alid)
            counts["albums"] += 1

            discs_out = []
            album_dur = 0.0
            album_tracks = 0
            first_track_file = None
            for disc in album.get("discs", []):
                counts["discs"] += 1
                tracks_out = []
                for t in disc.get("tracks", []):
                    file = t.get("file")
                    if not file:
                        warnings.append(f"{alid}: track without file, skipped")
                        continue
                    if first_track_file is None:
                        first_track_file = file
                    tid = track_id(file)
                    if tid in track_ids:
                        warnings.append(f"{alid}: duplicate track id {tid} ({file})")
                    track_ids.add(tid)
                    dur = float(t.get("dur") or 0.0)
                    album_dur += dur
                    album_tracks += 1
                    counts["tracks"] += 1
                    counts["durationSeconds"] += dur
                    lyr = lyrics_raw.get(file)
                    has_lyrics = bool(lyr and lyr.get("l"))
                    tracks_out.append(
                        {
                            "id": tid,
                            "title": t.get("title", ""),
                            "n": t.get("n"),
                            "durationSeconds": round(float(t.get("dur") or 0.0), 3),
                            "sizeBytes": int(t.get("size") or 0),
                            "file": file,
                            "usb": t.get("usb"),
                            "hasLyrics": has_lyrics,
                        }
                    )
                    if has_lyrics:
                        lyrics_out[tid] = {
                            "text": lyr.get("l", ""),
                            "romaji": bool(lyr.get("r", False)),
                            "source": lyr.get("s"),
                        }
                discs_out.append({"name": disc.get("name"), "tracks": tracks_out})

            # Resolve album art.
            album_dir = None
            if first_track_file:
                album_dir = (args.source / os.path.dirname(first_track_file)).resolve()
            cover_asset, warn = resolve_cover(album, album_dir, out_dir, alid, args)
            if warn:
                warnings.append(warn)
            if cover_asset:
                counts["covers"] += 1

            albums_out.append(
                {
                    "id": alid,
                    "folder": folder,
                    "title": title,
                    "year": year,
                    "trackCount": album_tracks,
                    "durationSeconds": round(album_dur, 3),
                    "coverAsset": cover_asset,
                    "discs": discs_out,
                }
            )

        artists_out.append({"id": aid, "name": name, "albums": albums_out})

    counts["lyrics"] = len(lyrics_out)

    library = {
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": generated_at,
        "sourceHash": f"sha256:{source_hash}",
        "counts": counts,
        "artists": artists_out,
    }
    return library, lyrics_out, warnings


# ---------------------------------------------------------------------------
# IO
# ---------------------------------------------------------------------------
def _atomic_write_json(path: Path, obj) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(obj, ensure_ascii=False, separators=(",", ":"))
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(text, encoding="utf-8")
    os.replace(tmp, path)


def verify(out_dir: Path, library, lyrics) -> None:
    lib_path = out_dir / "library.json"
    lyr_path = out_dir / "lyrics.json"
    reloaded = json.loads(lib_path.read_text(encoding="utf-8"))
    reloaded_lyrics = json.loads(lyr_path.read_text(encoding="utf-8"))
    assert reloaded["counts"] == library["counts"], "counts mismatch after write"
    assert len(reloaded["artists"]) == library["counts"]["artists"]
    assert reloaded_lyrics == lyrics, "lyrics mismatch after write"
    n_tracks = 0
    for artist in reloaded["artists"]:
        for album in artist["albums"]:
            ca = album.get("coverAsset")
            if ca:
                p = out_dir / ca
                if not p.is_file():
                    raise AssertionError(f"missing cover asset {ca}")
                if not re.match(r"^[ -~]+$", ca):
                    raise AssertionError(f"non-ASCII asset path {ca}")
            for disc in album["discs"]:
                for t in disc["tracks"]:
                    n_tracks += 1
    assert n_tracks == library["counts"]["tracks"], "track count mismatch"


def main(argv=None) -> int:
    here = Path(__file__).resolve()
    project_root = here.parents[3]  # .kilo/skills/update-library -> project root
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--source", type=Path, default=Path("/home/kirill/Music"))
    ap.add_argument("--library", type=Path, default=None)
    ap.add_argument("--out", type=Path, default=project_root / "app/src/main/assets/library")
    ap.add_argument("--check", action="store_true", help="validate and show plan, write nothing")
    ap.add_argument("--dry-run", action="store_true", help="compute but write nothing")
    ap.add_argument("--force", action="store_true", help="ignore the source-hash short-circuit")
    ap.add_argument("--cover-px", type=int, default=1024)
    ap.add_argument("--cover-quality", type=int, default=82)
    ap.add_argument("--format", choices=["webp", "jpg"], default="webp")
    ap.add_argument("--no-covers", action="store_true")
    ap.add_argument("--generated-at", default=None, help="override generatedAt (reproducible builds)")
    args = ap.parse_args(argv)

    args.source = args.source.resolve()
    args.out = args.out.resolve()
    lib_path = (args.library or (args.source / "LIBRARY.html")).resolve()

    if args.dry_run:
        args.check = True

    # Safety: never write into the Music tree, never write outside --out.
    if args.out == args.source or args.source in args.out.parents or args.out in args.source.parents:
        print(f"refusing: --out {args.out} overlaps --source {args.source}", file=sys.stderr)
        return 2
    if not lib_path.is_file():
        print(f"source library not found: {lib_path}", file=sys.stderr)
        return 2

    source_hash = sha256_file(lib_path)

    # Short-circuit when already up to date (unless forced / checking).
    if not args.force and not args.check and not args.no_covers:
        existing = args.out / "library.json"
        if existing.is_file():
            try:
                prev = json.loads(existing.read_text(encoding="utf-8"))
                # Only short-circuit a *complete* run: a covers-less file (from `--no-covers`)
                # must be regenerated so missing cover assets are restored.
                if (
                    prev.get("sourceHash") == f"sha256:{source_hash}"
                    and prev.get("counts", {}).get("covers", 0) > 0
                ):
                    print("up to date (source unchanged); nothing to do")
                    return 0
            except Exception:  # noqa: BLE001
                pass

    text = lib_path.read_text(encoding="utf-8")
    data = extract_const_line(text, "DATA")
    lyrics_raw = extract_const_line(text, "LYRICS")

    if not data:
        print("source DATA is empty", file=sys.stderr)
        return 3

    if args.generated_at:
        generated_at = args.generated_at
    elif os.environ.get("SOURCE_DATE_EPOCH"):
        ts = int(os.environ["SOURCE_DATE_EPOCH"])
        generated_at = _dt.datetime.fromtimestamp(ts, _dt.timezone.utc).strftime(
            "%Y-%m-%dT%H:%M:%SZ"
        )
    else:
        generated_at = _dt.datetime.now(_dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    library, lyrics, warnings = build_library(
        data, lyrics_raw, source_hash, generated_at, args.out, args
    )

    # Report the plan.
    c = library["counts"]
    total_albums = c["albums"]
    print(
        f"{c['artists']} artists · {c['albums']} albums · {c['discs']} discs · "
        f"{c['tracks']} tracks · {c['lyrics']} lyrics · {c['covers']}/{total_albums} covers"
    )
    if _HAS_PIL and _HAS_WEBP:
        print(f"cover pipeline: Pillow {Image.__version__} (webp)")
    elif _HAS_PIL:
        print("cover pipeline: Pillow (no webp) — JPEG fallback")
    else:
        print("cover pipeline: no Pillow — raw JPEG/copy fallback")
    for w in warnings[:40]:
        print(f"  warn: {w}")
    if len(warnings) > 40:
        print(f"  ... {len(warnings) - 40} more warnings")

    if args.check:
        print("check/dry-run: no files written")
        return 0

    # Write.
    _atomic_write_json(args.out / "library.json", library)
    _atomic_write_json(args.out / "lyrics.json", lyrics)
    verify(args.out, library, lyrics)

    lib_bytes = (args.out / "library.json").stat().st_size
    lyr_bytes = (args.out / "lyrics.json").stat().st_size
    cov_bytes = sum(p.stat().st_size for p in (args.out / "covers").glob("*")) if (
        args.out / "covers"
    ).is_dir() else 0
    print(
        f"wrote library.json ({lib_bytes//1024} KiB), lyrics.json ({lyr_bytes//1024} KiB), "
        f"covers ({cov_bytes//1024} KiB)"
    )
    print("done")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
