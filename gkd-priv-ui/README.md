# GKD-XA native Miuix privilege UI

Android-only source fork of `io.github.priv-kit:priv-ui:0.20.0`. Its authorization
backend is retained; the rendered components are Miuix **0.9.4**, not a Material
color/theme adapter. Namespace and public entry points remain `priv.kit.ui`.

## Integration

`settings.gradle.kts` includes `:gkd-priv-ui`, and `gkd-app` depends on that project
instead of the published UI. Backend artifacts remain pinned to `priv-core:0.20.0`
and `priv-shared:0.20.0`. Do not add the published `priv-ui` or `priv-ui-android`
back to the classpath: their namespaces are intentionally identical.

The existing app ViewModel, configuration and back action are retained. The
app's `PrivilegeKitTheme` now only inherits native content colors; it is not a
Material adapter. AGP built-in Kotlin compiles both retained Android and common
source directories in this Android-only library. Keep the app's existing
`packaging.jniLibs.useLegacyPackaging = true`.

## Why the source fork is necessary

The published `PrivilegeScaffold` accepts bars, snackbar, colors and tutorial
callbacks but does not expose slots for its mode panels, fields, controls,
confirmation/error dialogs or system guidance. The controlled `PrivilegeScreen`
is public, but the Android ViewModel-to-screen/action mapping and permission-host
coordinator are internal. Reimplementing a partial page using public methods
would lose important permission/focus/recreation behavior. This fork keeps that
host visible and functional, rather than mounting an invisible scaffold.

## Preserved flows and native rendering

- Root, wireless ADB, notification and no-notification pairing, static TCP,
  externally supplied providers (including Shizuku), manual shell commands.
- Startup ownership and START/CANCEL/CANCELLING gates, stop/restart confirmation,
  TCP-switch warning and controls, timeout/error acknowledgements.
- Notification/local-network permission launchers and host UUID ownership;
  resume/pause/window-focus listeners, disposal/configuration-change handling,
  system-prompt ownership and feedback collection.
- Pairing-code state/validation, numeric-password keyboard and Done action;
  all alternate notification input instructions and stop/cancel/help actions.
- Permission-restriction help/details/copy, battery-optimization and local-network
  cards, fingerprint autosizing/wrapping, startup logs/copy/clear, full shell
  command selection/copy and tutorial URL overrides.

The render layer uses native Miuix Scaffold, TabRow, Surface, Text, TextField,
Button, IconButton, SnackbarHost and tooltip components. Miuix squircle paths,
typography and inherited palette are used throughout. Slot adapters preserve
icons and controller callbacks without a Material dependency.

Dialogs use a plain **Compose window host** with native Miuix Card/buttons/text.
This is intentional: Miuix 0.9.4 `WindowDialog` does not expose independent
outside-tap and back policies, while authorization dialogs require them. Both
upstream `DialogProperties` flags remain enforced. Small-screen cards are
bottom-aligned, wide-screen cards centered, IME/safe-drawing insets respected;
body content is bounded and actions wrap. The pairing form retains its own
scroll container and adaptive three-action layout. No reflected/private API is
used. System permission dialogs, notifications and external-provider windows
are still rendered by Android/the provider, as before.

The mode selector uses Miuix TabRow geometry, palette and glyphs with explicit
per-item `selectable(enabled=...)` semantics, preserving upstream mode gates and
the `canInteract()` guard. This avoids advertising nonfunctional click actions
during startup. Long names can wrap at large font scales. Short-height dialogs
scroll title, body and actions together; normal-height dialogs reserve actions.
The snackbar retains Miuix timing/swipe handling while letting full recovery
instructions wrap and scroll instead of truncating after two lines.

Android-only presentation text is resolved through `R.string` and
`androidx.compose.ui.res.stringResource`. The original English and Chinese XML
are unchanged and shared with synchronous notifications, as upstream intended.
No generated Compose resource sources/runtime are needed. Common presentation
resource-return types change from `StringResource` to Android resource-ID `Int`;
this is not a multiplatform-compatible replacement for third-party callers.

## Pinned provenance and licensing

- Upstream repository: https://github.com/priv-kit/priv-kit
- Tag: `v0.20.0`
- Commit: `c906fbe5f5cfa2f1094f58e7593757d118b8df83`
- All 79 upstream Android/common Kotlin files were compared byte-for-byte with
  the official `priv-ui-android:0.20.0` source archive before modification; all
  matched. XML/manifest and five selected tests come from the same commit.
- Official source archive SHA-256:
  - `priv-ui-android-0.20.0-sources.jar`:
    `6115ce4a41069b3a9106d1da67ac85e94a7a2fd8b30ccc679a44bfea8fc03815`
  - `priv-ui-0.20.0-sources.jar`:
    `b93ee753a2f6fe9af66d038036371f69254a787bdf318c5b3a4e807db16a0339`
- Maven publication identifies the license as Apache-2.0 and the author as
  `lisonge`. Its original POM is retained in `UPSTREAM.pom`; the standard license
  text is retained in `LICENSE`. Upstream did not ship a LICENSE file at that
  tag, so this attribution is based on the official release metadata.
- `UPSTREAM.sha256` records unmodified upstream hashes. `MODIFIED_FILES.txt`
  records the 22 modified presentation/resource-adapter files (including two
  test adapters), each of which carries a modification notice. All other
  imported text files are byte-identical after CRLF is normalized to LF.
- Native API review used Miuix tag `v0.9.4`, commit
  `39c40f99844227b853f0049a0933b1f3ae6c00ba`. Miuix is an external Apache-2.0
  dependency, not copied into this module.

## Validation and remaining integration work

The following read-only audit does **not** invoke a build, test runner, device,
permission launcher or authorization action:

```sh
python3 gkd-priv-ui/tools/verify_fork.py
# Optional original-action-wiring comparison against the pinned checkout:
python3 gkd-priv-ui/tools/verify_fork.py --upstream /path/to/priv-kit-0.20.0
```

It checks frozen upstream hashes; restores only presentation substitutions in
`PrivilegeScaffold` to compare its full original source hash; rejects Material
component imports; parses XML and resolves all referenced Android string IDs.
With `--upstream`, it also compares action-dispatch reference counts.

Five selected upstream test files (25 tests) cover startup/cancel ownership,
ADB/TCP/pairing presentation gates, tutorial routing, mode gating and
permission-coordinator host/result ownership. Application-side Robolectric tests
render only `PrivilegePreviewScaffold`, which uses in-memory simulated state.
They do not instantiate the real permission host, create authorization requests
or execute service-start actions.

See `docs/miuix-ui-review.md` for final build/test results and explicitly
unverified physical-device behavior. This static audit alone does not establish
runtime correctness. Android system permission dialogs, provider-owned windows
and notification-system chrome remain owned by their respective processes.
