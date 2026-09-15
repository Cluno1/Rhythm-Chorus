#!/usr/bin/env python3

import sys
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).parent))
import generate_release_notes as release_notes


class GenerateReleaseNotesTest(unittest.TestCase):
    def test_classifies_conventional_commits(self) -> None:
        change = release_notes.classify_commit(
            "feat(lyrics): add word-level timestamping", "abc1234"
        )
        self.assertEqual("feature", change.category)
        self.assertEqual("Lyrics: add word-level timestamping", change.text)

        fix = release_notes.classify_commit(
            "fix(scores): preserve renderer under R8", "def5678"
        )
        self.assertEqual("fix", fix.category)

    def test_ignores_merge_commits(self) -> None:
        self.assertIsNone(
            release_notes.classify_commit("merge: synchronize debug", "abc1234")
        )

    def test_renders_download_and_contact_information(self) -> None:
        notes = release_notes.render_notes(
            tag="v1.2.3",
            version_code="1002003",
            repository="Cluno1/Sonorus",
            previous_tag="v1.2.2",
            changes=[
                release_notes.Change("feature", "Scores: add viewer", "abc1234")
            ],
        )
        self.assertIn("Sonorus-1.2.3-githubRelease-arm64-v8a.apk", notes)
        self.assertIn("https://discord.gg/KaGCYshewX", notes)
        self.assertIn("mailto:clunojames@gmal.com", notes)
        self.assertIn("compare/v1.2.2...v1.2.3", notes)


if __name__ == "__main__":
    unittest.main()
