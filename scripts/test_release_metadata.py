"""Exercise the real release packager with controlled Android tool output."""
import argparse
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import release_metadata as release


class ReleaseMetadataTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "gkd-app").mkdir()
        (self.root / "version").mkdir()
        (self.root / "gkd-app/build.gradle.kts").write_text('versionCode = 153; versionName = "1.5.2"', encoding="utf-8")
        (self.root / "CHANGELOG.md").write_text("## v1.5.2（2026-11-16）\nRelease\n\n## v1.5.1（2026-11-12）\nPrevious\n", encoding="utf-8")
        data = dict(versionCode=153, versionName="1.5.2", changelog="v1.5.2", fileSize=0,
                    downloadUrl="https://github.com/84593320z/GKD-XA/releases/download/v1.5.2/GKD-XA-v1.5.2.apk",
                    versionLogs=[dict(code=153, name="1.5.2")])
        for channel in ("stable", "beta"):
            release.save(self.root / f"version/{channel}.json", data)
        self.apk = self.root / "input.apk"
        self.apk.write_bytes(b"x" * 1_000_001)
        self.args = argparse.Namespace(tag="v1.5.2", apk=self.apk, output_dir=self.root / "release", aapt2=None)
        self.badging = "package: name='li.songe.gkdx' versionCode='153' versionName='1.5.2'"
        self.signers = [release.RELEASE_SIGNER_SHA256]

    def pack(self):
        def output(command, **kwargs):
            if command[0] == "aapt2":
                return self.badging
            if command[0] == "apksigner":
                return "\n".join(f"Signer #{index + 1} certificate SHA-256 digest: {digest}" for index, digest in enumerate(self.signers))
            if command == ["git", "rev-parse", "HEAD"]:
                return "a" * 40
            self.fail(f"Unexpected external tool: {command[0]}")
        with patch.object(release, "ROOT", self.root), patch.object(release, "sdk_tool", side_effect=lambda name, explicit=None: name), patch.object(release.subprocess, "check_output", side_effect=output):
            release.package(self.args)

    def test_packages_exact_bytes_identity_and_certificate(self):
        self.pack()
        out = self.args.output_dir
        manifest = release.load(out / "release-manifest.json")
        digest = hashlib.sha256(self.apk.read_bytes()).hexdigest()
        self.assertEqual(manifest["sha256"], digest)
        self.assertEqual(manifest["fileSize"], self.apk.stat().st_size)
        self.assertEqual(manifest["signerSha256"], release.RELEASE_SIGNER_SHA256)
        self.assertEqual((out / manifest["asset"]).read_bytes(), self.apk.read_bytes())
        self.assertEqual((out / "SHA256SUMS").read_text(), f'{digest}  {manifest["asset"]}\n')
        for channel in ("stable", "beta"):
            data = release.load(out / f"{channel}.json")
            self.assertEqual((data["fileSize"], data["sha256"]), (manifest["fileSize"], digest))
        self.assertNotIn("Previous", (out / "RELEASE_NOTES.md").read_text(encoding="utf-8"))

    def test_rejects_missing_wrong_or_extra_certificate(self):
        for signers in ([], ["0" * 64], [release.RELEASE_SIGNER_SHA256, "0" * 64]):
            self.signers = signers
            with self.subTest(signers=signers), self.assertRaises(ValueError):
                self.pack()
            self.assertFalse(self.args.output_dir.exists())

    def test_rejects_apk_package_or_version_mismatch(self):
        for old, new in (("li.songe.gkdx", "other.package"), ("153", "152"), ("1.5.2", "1.5.1")):
            saved = self.badging
            self.badging = saved.replace(old, new)
            with self.subTest(field=old), self.assertRaises(ValueError):
                self.pack()
            self.badging = saved
            self.assertFalse(self.args.output_dir.exists())

    def test_rejects_tag_mismatch(self):
        self.args.tag = "v1.5.1"
        with self.assertRaises(ValueError):
            self.pack()
        self.assertFalse(self.args.output_dir.exists())

    def test_rejects_unexpected_artifact_size(self):
        self.apk.write_bytes(b"small")
        with self.assertRaises(ValueError):
            self.pack()
        self.assertFalse(self.args.output_dir.exists())

    def test_backfill_does_not_overwrite_advanced_channel(self):
        self.pack()
        channel = self.root / "version/beta.json"
        advanced = release.load(channel)
        advanced.update(versionCode=154, versionName="1.5.3", downloadUrl="https://example.test/new.apk")
        release.save(channel, advanced)
        release.backfill(argparse.Namespace(root=self.root, manifest=self.args.output_dir / "release-manifest.json"))
        self.assertEqual(release.load(channel), advanced)
        self.assertEqual(release.load(self.root / "version/stable.json")["fileSize"], self.apk.stat().st_size)


if __name__ == "__main__":
    unittest.main()
