#!/usr/bin/env python3
"""Validate version inputs and bind release metadata to one verified APK."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
RELEASE_PACKAGE = "li.songe.gkdx"
MIN_APK_SIZE = 1_000_000
MAX_APK_SIZE = 256 * 1024 * 1024
# Verified from the published v1.5.1 APK (SHA-256 2c23ecd1...328708).
RELEASE_SIGNER_SHA256 = "9361daaec8ad21965fb3b658dcfc57ebaebfc0a06df157df6d60e0438e072ea9"

def load(path):
    return json.loads(path.read_text(encoding="utf-8"))

def save(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def identity(root):
    build = (root / "gkd-app/build.gradle.kts").read_text(encoding="utf-8")
    return (int(re.search(r"versionCode\s*=\s*(\d+)", build)[1]),
            re.search(r'versionName\s*=\s*"([^"]+)"', build)[1])

def validate(root, tag=None):
    code, name = identity(root)
    expected_tag = "v" + name
    if tag is not None and tag != expected_tag:
        raise ValueError(f"Tag {tag} does not match source version {expected_tag}")
    changelog = (root / "CHANGELOG.md").read_text(encoding="utf-8")
    first = re.search(r"^## (v\d+\.\d+\.\d+[^\s（]*)", changelog, re.M)
    if not first or first[1] != expected_tag:
        raise ValueError("CHANGELOG first section does not match source version")
    for channel in ("stable", "beta"):
        data = load(root / f"version/{channel}.json")
        if (data["versionCode"], data["versionName"]) != (code, name):
            raise ValueError(f"{channel}: identity mismatch")
        expected_url = f"https://github.com/84593320z/GKD-XA/releases/download/{expected_tag}/GKD-XA-{expected_tag}.apk"
        if data["downloadUrl"] != expected_url:
            raise ValueError(f"{channel}: artifact URL mismatch")
        size, digest = data["fileSize"], data.get("sha256")
        if not isinstance(size, int) or size < 0 or (size != 0 and size <= 1_000_000):
            raise ValueError(f"{channel}: invalid size")
        if digest is not None and (not re.fullmatch(r"[a-f0-9]{64}", digest) or size == 0):
            raise ValueError(f"{channel}: invalid digest")
        logs = data["versionLogs"]
        if not logs or (logs[0]["code"], logs[0]["name"]) != (code, name):
            raise ValueError(f"{channel}: missing current release log")
        codes = [item["code"] for item in logs]
        if codes != sorted(set(codes), reverse=True):
            raise ValueError(f"{channel}: duplicate or unsorted release logs")
        if not data["changelog"].startswith(expected_tag):
            raise ValueError(f"{channel}: stale changelog")
    return code, name

def sdk_tool(name, explicit=None):
    if explicit:
        return explicit
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if sdk:
        suffix = ".exe" if os.name == "nt" and name == "aapt2" else (".bat" if os.name == "nt" else "")
        paths = list((Path(sdk) / "build-tools").glob(f"*/{name}{suffix}"))
        if paths:
            return str(sorted(paths, key=lambda p: tuple(int(x) for x in re.findall(r"\d+", p.parent.name)))[-1])
    found = shutil.which(name)
    if not found:
        raise ValueError(f"Android SDK tool unavailable: {name}")
    return found

def package(args):
    code, name = validate(ROOT, args.tag)
    apk = args.apk.resolve()
    badging = subprocess.check_output([sdk_tool("aapt2", args.aapt2), "dump", "badging", str(apk)], text=True, encoding="utf-8")
    match = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    if not match or match.groups() != (RELEASE_PACKAGE, str(code), name):
        raise ValueError("Built APK package/version differs from the tagged release")
    certificates = subprocess.check_output(
        [sdk_tool("apksigner"), "verify", "--print-certs", str(apk)], text=True, encoding="utf-8")
    signers = set(re.findall(r"certificate SHA-256 digest: ([a-fA-F0-9]{64})", certificates))
    if {signer.lower() for signer in signers} != {RELEASE_SIGNER_SHA256}:
        raise ValueError("APK signing certificate differs from the previous official release")
    with apk.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    size = apk.stat().st_size
    if not MIN_APK_SIZE < size <= MAX_APK_SIZE:
        raise ValueError("Unexpected APK size")
    out = args.output_dir.resolve()
    out.mkdir(parents=True, exist_ok=True)
    asset_name = f"GKD-XA-{args.tag}.apk"
    shutil.copyfile(apk, out / asset_name)
    for channel in ("stable", "beta"):
        data = load(ROOT / f"version/{channel}.json")
        data.update(fileSize=size, sha256=digest)
        save(out / f"{channel}.json", data)
    commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    save(out / "release-manifest.json", {"tag": args.tag, "commit": commit, "package": RELEASE_PACKAGE, "versionCode": code, "versionName": name, "fileSize": size, "sha256": digest, "signerSha256": RELEASE_SIGNER_SHA256, "asset": asset_name})
    (out / "SHA256SUMS").write_text(f"{digest}  {asset_name}\n", encoding="ascii")
    text = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    sections = re.split(r"(?=^## )", text, flags=re.M)
    notes = next(section for section in sections if section.startswith("## " + args.tag + "（"))
    (out / "RELEASE_NOTES.md").write_text(notes.strip() + "\n", encoding="utf-8")
    print(f"Verified {asset_name}: versionCode={code}, bytes={size}, sha256={digest}")

def validate_published_manifest(manifest, published_release, tag, commit, source_identity):
    code, name = source_identity
    asset_name = f"GKD-XA-{tag}.apk"
    expected = {
        "tag": "v" + name, "commit": commit, "package": RELEASE_PACKAGE,
        "versionCode": code, "versionName": name, "asset": asset_name,
        "signerSha256": RELEASE_SIGNER_SHA256,
    }
    if tag != expected["tag"] or any(manifest.get(key) != value for key, value in expected.items()):
        raise ValueError("Published manifest does not match the tagged source")
    size, digest = manifest.get("fileSize"), manifest.get("sha256")
    if type(size) is not int or not MIN_APK_SIZE < size <= MAX_APK_SIZE:
        raise ValueError("Invalid APK size in the published manifest")
    if not isinstance(digest, str) or not re.fullmatch(r"[a-f0-9]{64}", digest):
        raise ValueError("Invalid APK SHA-256 in the published manifest")
    if published_release.get("draft") is not False or published_release.get("tag_name") != tag:
        raise ValueError("Release is not published for the requested tag")
    apk_assets = [asset for asset in published_release["assets"] if asset["name"] == asset_name]
    if len(apk_assets) != 1 or (apk_assets[0]["size"], apk_assets[0].get("digest")) != (size, f"sha256:{digest}"):
        raise ValueError("Published APK does not match the release manifest")


def backfill(args):
    manifest = load(args.manifest)
    published_release = load(args.published_release)
    validate_published_manifest(manifest, published_release, args.tag, args.commit, identity(ROOT))
    expected_url = f'https://github.com/84593320z/GKD-XA/releases/download/{manifest["tag"]}/{manifest["asset"]}'
    expected_identity_and_url = (manifest["versionCode"], manifest["versionName"], expected_url)
    changed = 0
    for channel in ("stable", "beta"):
        path = args.root / f"version/{channel}.json"
        data = load(path)
        identity_and_url = (data["versionCode"], data["versionName"], data["downloadUrl"])
        if identity_and_url != expected_identity_and_url:
            print(f"Skip {channel}: main metadata does not match this release")
            continue
        if (data["fileSize"], data.get("sha256")) == (manifest["fileSize"], manifest["sha256"]):
            print(f"Skip {channel}: already backfilled")
            continue
        data.update(fileSize=manifest["fileSize"], sha256=manifest["sha256"])
        save(path, data)
        changed += 1
    print(f"Backfilled {changed} matching channels")


def main():
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command", required=True)
    check = commands.add_parser("validate")
    check.add_argument("--tag")
    pack = commands.add_parser("package")
    pack.add_argument("--tag", required=True)
    pack.add_argument("--apk", type=Path, required=True)
    pack.add_argument("--output-dir", type=Path, default=ROOT / "release")
    pack.add_argument("--aapt2")
    fill = commands.add_parser("backfill")
    fill.add_argument("--root", type=Path, required=True)
    fill.add_argument("--manifest", type=Path, required=True)
    fill.add_argument("--published-release", type=Path, required=True)
    fill.add_argument("--tag", required=True)
    fill.add_argument("--commit", required=True)
    args = parser.parse_args()
    if args.command == "validate":
        print("Release metadata valid:", validate(ROOT, args.tag))
    elif args.command == "package":
        package(args)
    else:
        backfill(args)

if __name__ == "__main__":
    main()
