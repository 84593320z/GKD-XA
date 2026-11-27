#!/usr/bin/env python3
"""Small read-only architecture gate; rendering correctness is checked by Compose tests separately."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "gkd-app/src/main/kotlin/li/gkd/app"
FORK = ROOT / "gkd-priv-ui"


def audit():
    errors = []
    sources = list(APP.rglob("*.kt")) + list((FORK / "src").rglob("*.kt"))
    for path in sources:
        text = path.read_text()
        if re.search(r"^import androidx\.compose\.material3\.", text, re.M):
            errors.append(f"Material3 renderer reintroduced: {path.relative_to(ROOT)}")
        # Material vector resources for specialized symbols are allowed; Material controls are not.
        imports = re.findall(r"^import (androidx\.compose\.material\.[^\n]+)", text, re.M)
        if any(not name.startswith("androidx.compose.material.icons.") for name in imports):
            errors.append(f"Material renderer reintroduced: {path.relative_to(ROOT)}")
        rel = path.relative_to(ROOT).as_posix()
        if ("/ui/" in rel or "/feature/" in rel) and re.search(r"\.collectAsState\s*\(", text):
            errors.append(f"Use collectAsStateWithLifecycle instead of collectAsState: {rel}")
    build = (ROOT / "gkd-app/build.gradle.kts").read_text()
    if 'implementation(project(":gkd-priv-ui"))' not in build:
        errors.append("The application must use the reviewed local authorization renderer")
    if "implementation(libs.priv.kit.ui)" in build or "priv-ui-android:" in build:
        errors.append("Do not combine the source fork with the published UI artifact")
    versions = (ROOT / "gradle/libs.versions.toml").read_text()
    if not re.search(r'^miuix = "0\.9\.4"$', versions, re.M):
        errors.append("Update the pinned native API audit and rendering tests before changing Miuix")
    aliases = {"PerfSwitch": "GkSwitch", "PerfCheckbox": "GkCheckbox",
               "TextSwitch": "GkTextSwitch", "SettingItem": "GkSettingItem"}
    for alias, target in aliases.items():
        if f"= {target}(" not in (APP / f"ui/component/{alias}.kt").read_text():
            errors.append(f"{alias} must share the {target} renderer, not fork its visual behavior")
    for relative in ("ui/component/GkSwitch.kt", "ui/component/GkCheckbox.kt"):
        if "GkUi.MinTouchTarget" not in (APP / relative).read_text():
            errors.append(f"Missing unified minimum touch target: {relative}")
    for path in [ROOT / "gkd-app/src/main/res/values/strings.xml",
                 *list((FORK / "src").rglob("*.xml"))]:
        ET.parse(path)
    navigation = (APP / "ui/app/MainNavigation.kt").read_text()
    routes = re.findall(r"entry<([A-Za-z0-9_]+)>", navigation)
    if len(routes) != len(set(routes)):
        errors.append("Duplicate navigation destinations in UI registry")
    print(f"Inspected {len(sources)} Kotlin sources and {len(routes)} navigation destinations")
    print("Domain vector glyphs and system permission windows are intentionally outside the renderer ban")
    return errors


if __name__ == "__main__":
    problems = audit()
    if problems:
        print("\n".join(problems), file=sys.stderr)
        raise SystemExit(1)
    print("PASS: native Miuix architecture gate (not a screenshot or device test)")
