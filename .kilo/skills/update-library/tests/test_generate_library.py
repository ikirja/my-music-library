"""Unit tests for the update-library generator (run with `python3 -m unittest`).

These run with the system Python, which may or may not have Pillow; the assertions are written
to hold in both the Pillow (WebP) and the JPEG/copy fallback paths.
"""

import base64
import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
SCRIPT = HERE.parent / "generate_library.py"

# A valid 1x1 PNG, used both as an embedded thumb and as an on-disk cover.
PNG_1x1 = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
)
PNG_URI = "data:image/png;base64," + base64.b64encode(PNG_1x1).decode("ascii")

DATA = [
    {
        "name": "Test Artist",
        "albums": [
            {
                "folder": "Test Artist - 2001 - Alpha",
                "year": 2001,
                "title": "Alpha",
                "tracks": 2,
                "dur": 161.5,
                "cover_src": "Test Artist/Test Artist - 2001 - Alpha/cover.png",
                "thumb": PNG_URI,
                "discs": [
                    {
                        "name": None,
                        "tracks": [
                            {"title": "One", "n": 1, "dur": 100.0, "size": 1048576,
                             "file": "Test Artist/Test Artist - 2001 - Alpha/01 - One.flac", "usb": None},
                            {"title": "Два", "n": 2, "dur": 61.5, "size": 2097152,
                             "file": "Test Artist/Test Artist - 2001 - Alpha/02 - Dva.flac", "usb": 3},
                        ],
                    }
                ],
            },
            {
                "folder": "Test Artist - 2002 - Beta",
                "year": 2002,
                "title": "Beta",
                "tracks": 1,
                "dur": 30.0,
                "cover_src": "Test Artist/Test Artist - 2002 - Beta/cover.png",
                "discs": [
                    {
                        "name": None,
                        "tracks": [
                            {"title": "Three", "n": 1, "dur": 30.0, "size": 1048576,
                             "file": "Test Artist/Test Artist - 2002 - Beta/01 - Three.flac", "usb": None},
                        ],
                    }
                ],
            },
            {
                "folder": "Test Artist - 2003 - Gamma",
                "year": 2003,
                "title": "Gamma",
                "tracks": 1,
                "dur": 20.0,
                "discs": [
                    {
                        "name": None,
                        "tracks": [
                            {"title": "Four", "n": 1, "dur": 20.0, "size": 1048576,
                             "file": "Test Artist/Test Artist - 2003 - Gamma/01 - Four.flac", "usb": 1},
                        ],
                    }
                ],
            },
        ],
    }
]

LYRICS = {
    "Test Artist/Test Artist - 2001 - Alpha/02 - Dva.flac": {
        "l": "line1\nline2",
        "s": "lrclib",
        "r": True,
    }
}


class GeneratorTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.source = self.root / "Music"
        self.out = self.root / "out"
        (self.source / "Test Artist" / "Test Artist - 2001 - Alpha").mkdir(parents=True)
        (self.source / "Test Artist" / "Test Artist - 2002 - Beta").mkdir(parents=True)
        # Beta has only an on-disk cover; Alpha uses the embedded thumb; Gamma has no art.
        (self.source / "Test Artist" / "Test Artist - 2002 - Beta" / "cover.png").write_bytes(PNG_1x1)
        self.library = self.root / "LIBRARY.html"
        data_js = json.dumps(DATA, ensure_ascii=False, separators=(",", ":"))
        lyr_js = json.dumps(LYRICS, ensure_ascii=False, separators=(",", ":"))
        self.library.write_text(
            "<html><body>\n"
            f"const DATA = {data_js};\n"
            f"const LYRICS = {lyr_js};\n"
            "</body></html>\n",
            encoding="utf-8",
        )

    def tearDown(self):
        self.tmp.cleanup()

    def run_gen(self, *extra, library=None, source=None, out=None):
        cmd = [
            sys.executable, str(SCRIPT),
            "--source", str(source or self.source),
            "--library", str(library or self.library),
            "--out", str(out or self.out),
            "--generated-at", "2026-10-10T05:00:00Z",
            *extra,
        ]
        return subprocess.run(cmd, capture_output=True, text=True)

    def load(self, name, out=None):
        return json.loads(((out or self.out) / name).read_text(encoding="utf-8"))

    def test_generates_expected_schema_and_counts(self):
        res = self.run_gen("--force")
        self.assertEqual(res.returncode, 0, res.stderr + res.stdout)
        lib = self.load("library.json")
        self.assertEqual(lib["schemaVersion"], 1)
        self.assertTrue(lib["sourceHash"].startswith("sha256:"))
        c = lib["counts"]
        self.assertEqual(c["artists"], 1)
        self.assertEqual(c["albums"], 3)
        self.assertEqual(c["discs"], 3)
        self.assertEqual(c["tracks"], 4)
        self.assertEqual(c["lyrics"], 1)
        self.assertEqual(c["covers"], 2)

    def test_track_ids_are_stable_sha1_of_file(self):
        self.run_gen("--force")
        lib = self.load("library.json")
        tracks = lib["artists"][0]["albums"][0]["discs"][0]["tracks"]
        expected = "t-" + hashlib.sha1(
            "Test Artist/Test Artist - 2001 - Alpha/02 - Dva.flac".encode("utf-8")
        ).hexdigest()[:12]
        self.assertEqual(tracks[1]["id"], expected)
        self.assertEqual(tracks[1]["title"], "Два")  # UTF-8 preserved

    def test_lyrics_are_keyed_by_track_id_with_romaji(self):
        self.run_gen("--force")
        lib = self.load("library.json")
        lyr = self.load("lyrics.json")
        track_id = lib["artists"][0]["albums"][0]["discs"][0]["tracks"][1]["id"]
        self.assertEqual(list(lyr.keys()), [track_id])
        self.assertEqual(lyr[track_id]["text"], "line1\nline2")
        self.assertTrue(lyr[track_id]["romaji"])
        self.assertEqual(lyr[track_id]["source"], "lrclib")

    def test_cover_recovery_from_thumb_and_disk(self):
        self.run_gen("--force")
        lib = self.load("library.json")
        albums = {a["title"]: a for a in lib["artists"][0]["albums"]}
        self.assertIsNotNone(albums["Alpha"]["coverAsset"])   # embedded thumb
        self.assertIsNotNone(albums["Beta"]["coverAsset"])    # on-disk cover.png
        self.assertIsNone(albums["Gamma"]["coverAsset"])      # no art anywhere
        for title in ("Alpha", "Beta"):
            asset = albums[title]["coverAsset"]
            self.assertTrue((self.out / asset).is_file(), asset)
            self.assertRegex(asset, r"^covers/[a-z0-9.-]+\.(webp|jpg)$")

    def test_deterministic_and_string_escaped(self):
        self.run_gen("--force")
        first = (self.out / "library.json").read_bytes()
        self.run_gen("--force")
        second = (self.out / "library.json").read_bytes()
        self.assertEqual(first, second)
        text = first.decode("utf-8")
        self.assertIn("Два", text)
        self.assertNotIn("\\u", text)  # ensure_ascii=False

    def test_short_circuits_when_source_unchanged(self):
        self.run_gen("--force")
        res = self.run_gen()
        self.assertEqual(res.returncode, 0)
        self.assertIn("up to date", res.stdout.lower())

    def test_check_writes_nothing(self):
        res = self.run_gen("--check")
        self.assertEqual(res.returncode, 0)
        self.assertFalse((self.out / "library.json").exists())

    def test_refuses_out_overlapping_source(self):
        bad_out = self.source / "nested"
        res = self.run_gen("--force", out=bad_out)
        self.assertEqual(res.returncode, 2)
        self.assertIn("refusing", res.stderr.lower())


if __name__ == "__main__":
    unittest.main()
