# Hydri 1.2.3 verification

Version name **1.2.3**, version code **6**. Package `com.hydri.app`, minimum API 26, target API 35. Tested October 30, 2026 on the isolated `hydri_refinement_qa` Android 15/API 35 emulator, serial `emulator-5580`.

## Changes

Quick Add clips the entire interactive surface to the existing 28dp rounded card shape before its clickable indication. Ripple feedback and logging/haptics are retained. Newly logged bottle sips store the bottle ID and icon/color snapshots; edits/deletion of a bottle do not rewrite history. Widgets store the same snapshots. Custom and legacy entries use the default blue droplet without guessing associations.

Room schema **1 → 2** adds three nullable columns. No existing column, amount, date, timestamp, goal, bottle or preference is rewritten. The migration is registered without destructive fallback. JSON backups retain the optional snapshots; older backups without them remain valid. Entry editing and Undo preserve snapshots through `copy` and the original record.

The selected bottom tab uses its filled icon. Explicitly tapping the active primary tab emits a route-specific scroll request; drag crossings do not emit it. Screen scroll states remain saveable across tab switches. Reduced motion uses an immediate scroll. Nested More pages retain normal back behavior. The single activity requests portrait orientation; Android may override fixed orientation on some large-screen/windowing configurations.

The existing droplet is now an adaptive launcher icon with independent background, foreground and Android 13+ monochrome layers. The manifest uses the same resource for regular and round icons. A vector legacy fallback remains available. See [icon implementation and native mask previews](ICON.md).

Apache 2.0 was explicitly authorized by the owner. The root LICENSE matches the official text and existing bundled Apache text byte-for-byte (SHA-256 `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`). NOTICE, production Kotlin SPDX headers, README and About attribution are present. Existing dependency notices are preserved, including the newly documented BSD-licensed Glance protobuf runtime. See [licensing review](LICENSING-1.2.3.md).

## Build and automated checks

Debug APK, release APK, device test APK, `testDebugUnitTest`, `lintDebug`, and `lintRelease` build successfully with Gradle 8.14.6, JDK 17 and SDK 35. **19 unit tests pass.** Both lint reports have **0 errors and 27 warnings**: existing dependency/tooling suggestions, intentional fixed-orientation warnings, and icon qualifier/fallback warnings. The monochrome layer is explicitly verified on API 35 despite lint flagging the API-26 variant separately.

**41 emulator tests pass** at 1080×2340px, density 440, font scale 1.0. The suite includes migration and legacy-backup preservation, icon/color snapshots, custom fallback, actual held-button corner pixels in four appearances, installed adaptive/monochrome layers, safe-zone pixel bounds, 24/48/96/192px icons, mask renderings, filled/outlined tab state, scroll-to-top and restored positions, onboarding, dialogs, widgets, reminders, shortcuts, history and badges. Existing version-label tests now use BuildConfig; reset tests create their own current-day fixtures rather than relying on a previous day's baseline.

At **360×640dp** (720×1280px, density 320), font scale **1.3**, all **28 distinct small-screen cases were verified across the suite and targeted reruns**. The last complete small-screen invocation passed 27/28; its one failure was an existing navigation test acting before the outgoing About transition had disappeared. The helper now waits for outgoing/incoming destination state and finishes its scroll before tapping. All **5 navigation tests then passed** on the same APK and small-screen configuration. Earlier keyboard idling and row-appearance races were synchronized, with the two keyboard/back cases also passing in an isolated run. These were test-harness updates; the signed product APK did not change. Raw reports retain the failed small-screen invocation and successful targeted rerun rather than claiming a clean single 28-test invocation. Display size, density, font scale and native rotation settings were restored.

See [raw test results](test-results-1.2.3.txt).

## Upgrade and signing

Before installing the first 1.2.3 APK, the installed 1.2.2 QA app (version code 5, schema 1) supplied a baseline containing 2 entries, 4 bottles, 1 goal and all preferences. Installing with `adb install -r` succeeded. The upgrade test compares every original field/settings value and derived badge statistics; the newly added nullable fields are excluded only when null. A separate raw schema-1 database migration test verifies exact legacy records and a default-droplet representation without guessed bottle metadata.

The release remains non-debuggable and uses the unchanged original testing certificate, SHA-256 `ed0e989eaa47ed777d32e9dcc9902f4462d349f5ad132bb8568971383b3ec9f0`. `apksigner verify` passes. No signing key or identity was created, replaced or uploaded. See [private signing backup guidance](SIGNING.md).

The prepared APK is **13,323,963 bytes**, SHA-256 `cce8171f446bd9c2c1709e5a952eaa4ba5785869f2ae9f98b26489153a85c6e2`. Its manifest reports version 1.2.3 (6), the unchanged package ID, and the adaptive launcher resource.

Native Android checks also requested landscape rotation while Hydri was foregrounded; the activity remained in portrait. Original rotation settings were restored. Actual Pixel emulator app-drawer, Settings app-info and recent-apps captures were visually reviewed: the droplet is correctly masked and recognizable on each surface.

## Scope and limitations

Tests use synthetic data on the isolated emulator and restore snapshots after each scenario. No personal device was reset. Physical Samsung/other OEM launchers, API 26/older hardware, Android 16 large-screen overrides, TalkBack and frame-time benchmarking were not exercised. Icon mask images are Android-rendered resource studies, not claims of device screenshots from those manufacturers. Launcher-themed icons depend on launcher support and the device's themed-icon setting. No claim of universal launcher behavior or zero future regressions is made.

The reachable-history credential-pattern scan at commit `2ce0da3` covered 220 text blobs across 458 objects and found no matching credentials or sensitive paths. This is a bounded pattern scan, not an exhaustive guarantee. Existing repository safeguards and private signing storage remain unchanged.

## Published APK and release cleanup

[Hydri 1.2.3](https://github.com/jawad-mohammed35/hydri-android/releases/tag/v1.2.3) is published, non-draft, non-prerelease and marked latest. Its [public APK download](https://github.com/jawad-mohammed35/hydri-android/releases/download/v1.2.3/Hydri-1.2.3.apk) was downloaded anonymously and matched the size/hash above. That downloaded file passed signature verification, installed with `adb install -r`, launched with `Status: ok` / `LaunchState: COLD`, and passed the original-data preservation test again (1 test). GitHub detects the repository license as `Apache-2.0`.

Only after those checks, the owner-authorized pre-release entries and assets for v1.0.0-test.1, v1.1.0 and v1.2.0 were removed. No active GitHub workflows were present. Their source tags were checked before and after deletion and retain identical commit SHAs. Stable releases v1.2.1 and v1.2.2 and their assets remain available. See [release-history policy and cleanup record](RELEASE-HISTORY.md).
