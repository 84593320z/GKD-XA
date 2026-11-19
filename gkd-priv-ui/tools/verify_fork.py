#!/usr/bin/env python3
"""Read-only source/resource audit; does not invoke Gradle, Android, or authorization."""
import argparse
import collections
import hashlib
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
# Only presentation/resource adapters may differ from the pinned source manifest.
PRESENTATION = {
    "src/androidMain/kotlin/priv/kit/ui/PrivilegeScaffold.kt",
    *{
        f"src/commonMain/kotlin/priv/kit/ui/{name}.kt"
        for name in ("PrivilegePreviewScaffold", "PrivilegeScaffoldContent", "PrivilegeScreen")
    },
    "src/commonMain/kotlin/priv/kit/ui/adb/PrivilegeUiAdbPanelState.kt",
    *{
        f"src/commonMain/kotlin/priv/kit/ui/component/{name}.kt"
        for name in (
            "PrivilegeAdbComponents", "PrivilegeAdbPairingDialogs", "PrivilegeAdbPermissionPanels",
            "PrivilegeIconTooltip", "PrivilegeScaffoldAdbPanel", "PrivilegeScaffoldComponents",
            "PrivilegeScaffoldExternalStartPanel", "PrivilegeScaffoldHeader", "PrivilegeScaffoldRuntimePanels",
            "PrivilegeStaticTcpPanel", "PrivilegeSystemPromptOverlay", "PrivilegeTutorialButton",
            "PrivilegeUiStartAction", "PrivilegeWirelessAdbPanel", "PrivilegeUiIcons",
        )
    },
    "src/commonTest/kotlin/priv/kit/ui/PrivilegeUiAdbPanelStateTest.kt",
    "src/commonTest/kotlin/priv/kit/ui/PrivilegeUiStartActionTest.kt",
}


def local_path(relative):
    return ROOT / relative.replace("src/androidHostTest/", "src/test/").replace("src/commonTest/", "src/test/")


def digest(data):
    return hashlib.sha256(data).hexdigest()


def restore_host_presentation(source):
    source = source.removeprefix("// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.\n")
    source = source.replace("import priv.kit.ui.miuix.SnackbarHost\n", "import top.yukonga.miuix.kmp.basic.SnackbarHost\n")
    native = ("FabPosition", "SnackbarHost", "SnackbarHostState")
    for name in native:
        source = source.replace(f"import top.yukonga.miuix.kmp.basic.{name}\n", f"import androidx.compose.material3.{name}\n")
    for local, original in (
        ("PrivilegeMiuixTheme", "MaterialTheme"),
        ("PrivilegeScaffoldDefaults", "ScaffoldDefaults"),
        ("privilegeContentColorFor", "contentColorFor"),
    ):
        source = source.replace(f"import priv.kit.ui.miuix.{local}\n", f"import androidx.compose.material3.{original}\n")
        source = re.sub(r"\b" + local + r"\b", original, source)
    return source.replace("newestSnackbarData()", "currentSnackbarData")


def actions_under(path):
    return collections.Counter(
        name for file in path.rglob("*.kt")
        for name in re.findall(r"\bactions\.(\w+)", file.read_text())
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--upstream", type=Path, help="Optional official v0.20.0 repo or its priv-ui directory")
    args = parser.parse_args()
    original = {}
    for line in (ROOT / "UPSTREAM.sha256").read_text().splitlines():
        sha, relative = line.split("  ", 1)
        original[relative] = sha
    failures = []
    unchanged = 0
    for relative, sha in original.items():
        local = local_path(relative)
        if not local.is_file():
            failures.append(f"missing upstream file: {relative}")
        elif relative not in PRESENTATION:
            if digest(local.read_bytes()) != sha:
                failures.append(f"frozen backend/model/resource/test changed: {relative}")
            else:
                unchanged += 1
    host = "src/androidMain/kotlin/priv/kit/ui/PrivilegeScaffold.kt"
    restored = restore_host_presentation(local_path(host).read_text())
    if digest(restored.encode()) != original[host]:
        failures.append("permission/lifecycle host differs beyond native imports, render tokens and snackbar access")
    print(f"Frozen upstream files: {unchanged}/{len(original) - len(PRESENTATION)} byte-identical")
    print("Permission/lifecycle host: compared after reversing presentation-only substitutions")

    sources = list((ROOT / "src").rglob("*.kt"))
    for file in sources:
        if re.search(r"^import androidx\.compose\.material(?:3)?\.", file.read_text(), re.M):
            failures.append(f"legacy rendered component import: {file.relative_to(ROOT)}")
    strings = set()
    for file in (ROOT / "src").rglob("*.xml"):
        tree = ET.parse(file)
        if file.parent.name == "values" and file.name == "strings.xml":
            strings.update(node.attrib["name"] for node in tree.getroot())
    references = {
        name for file in sources
        for name in re.findall(r"\bR\.string\.(\w+)", file.read_text())
    }
    if references - strings:
        failures.append(f"unresolved Android string IDs: {sorted(references - strings)}")
    print(f"Android text resources: {len(references)} referenced IDs / {len(strings)} default strings; XML parsed")
    print("Material component imports: absent" if not any("legacy rendered" in f for f in failures) else "Material import audit FAILED")

    if args.upstream:
        upstream = args.upstream
        if (upstream / "priv-ui").is_dir():
            upstream /= "priv-ui"
        for source_set in ("androidMain", "commonMain"):
            before = actions_under(upstream / "src" / source_set)
            after = actions_under(ROOT / "src" / source_set)
            expected_extra = collections.Counter({"canInteract": 1}) if source_set == "commonMain" else collections.Counter()
            if before - after or after - before != expected_extra:
                failures.append(f"unexpected action wiring changes in {source_set}: missing={before-after}, extra={after-before}")
        print("Action dispatch references: compared with upstream (one added defensive tab interaction guard allowed)")
    print("Kotlin/Gradle compilation, unit tests and rendered/device behavior: NOT checked by this audit")
    if failures:
        raise SystemExit("\n".join(failures))
    print("PASS: bounded static fork audit")


if __name__ == "__main__":
    main()
