# Hydri 1.1.0 verification — 2026-10-29

## Delivered build

Version **1.1.0**, version code **2**, application ID `com.hydri.app`, Android 8.0+ (API 26), target/compile SDK 35. The downloadable APK is the **release variant**, with debugging disabled. It uses the **same existing Android testing certificate** as the published 1.0.0 APK so it can update that installation without an uninstall. It is not signed with a new production identity.

Signing certificate SHA-256: `ed0e989eaa47ed777d32e9dcc9902f4462d349f5ad132bb8568971383b3ec9f0`.

Release signing validation checks the existing certificate on every build and fails if the key is missing or different. No key or credential was generated, replaced, or committed. APK signature verification passed with Android's `apksigner` (v2 signature). Package inspection confirmed version code/name, min/target SDK, and no debuggable flag.

## Implemented refinements

- Five onboarding screens: welcome, name, immediate theme preview, units, and goal. Suggested goals and a bottom Custom card remain; bottles and notifications are no longer onboarding steps. Name keyboard requests word capitalization without rewriting the input. Default bottles are initialized automatically.
- Value-anchored, rounded translucent dropdowns. More links to Notifications, Hydration Guide, Data & Backup, and About. About puts developer details before acknowledgments; the main More footer reads the build version.
- Home shows only favorites, with an empty state linking to Bottles. Today's sips support swipe-delete and restoration through the snackbar's Undo. Daily Flow includes Undo and confirmed today-only Reset. Home's goal editor and decorative footer are removed.
- Custom glass editors for amounts, goals and bottles. Bottle previews reflect the entered name, volume, icon and color. Time entry is inline within the sip editor. Dates are assigned by Home/the selected calendar day, and existing entries retain their original date and zone. DST gaps are rejected; amount-only edits preserve the original instant during overlaps.
- Tracker retains calendar/statistics and replaces always-open entry lists with expandable dated Sip History. The goal card is removed and achievements are labeled Badges. User-facing text is shorter throughout.

## Executed checks

- **Builds:** `assembleDebug`, `assembleRelease`, `assembleDebugAndroidTest` passed. Debug and release lint tasks passed with **zero errors**; existing tool/dependency and older-widget-attribute warnings remain documented in the reports. No additional libraries or online services were added.
- **Unit tests:** **15 passed** (11 existing calculation tests, 4 new fixed-date/DST time tests).
- **Actual release APK on emulator:** **12 device tests passed**. The debug instrumentation package was signed with the same certificate and used to exercise the non-debuggable release variant directly.
- **Interaction tests (5):** onboarding and live theme changes; dropdown placement/settings subpages/goal editing; bottle preview and favorites-only Home; swipe-delete/restore, daily Undo and confirmed Reset; time/amount editing, fixed historical dates, expandable history and Badges.
- **Repository/background tests (6):** backup/restore round trip and malformed input rejection; CSV precision; disk persistence; reminder scheduling/cancellation; widget updates; both bound widget sizes rendering and favorite quick-add; notification posting. Some individual test methods cover several of these checks.
- **Upgrade integrity test (1):** exact field comparisons for the original entries, bottles and dated goals, plus a SHA-256 comparison of the original settings bytes.
- **Compact display:** the onboarding scenario additionally passed at **360 × 640 dp**, normal text, with the default choices and navigation visible without a swipe.
- **Enlarged text:** the onboarding scenario additionally passed at **360 × 640 dp, 150% font scale**, using scrolling as a fallback where needed. Navigation remained accessible with the keyboard open.
- **Themes:** onboarding exercised Light, Dark, Follow system and OLED, including pixel checks for immediate preview and persistence after completion. Settings/goal editing were exercised in Dark/OLED; logging and bottle editors were exercised in Light.
- **Visual checks:** selected-value menu anchoring, custom logging/time input above the keyboard, compact/large-text onboarding, Home, OLED settings and expanded history screenshots were inspected.

The full release-variant run reported `OK (12 tests)`. The two extra display runs each reported `OK (1 test)`. Tests are not claimed for physical devices.

## Upgrade and data safety

A separate disposable `hydri_refinement_qa` AVD (API 35, x86_64) was created under the ignored local tools directory. Existing AVDs and personal app data were not reset. Version 1.0.0 was installed there and populated with synthetic fixtures. Its two entries, four bottles, dated goal and complete preferences were captured before installing 1.1.0 with `adb install -r`.

The release update installed successfully using the matching signing identity. It displayed the existing name and **1,000 / 2,000 mL** total. The upgrade test then confirmed all original fields and preference bytes survived. UI tests snapshot and restore their synthetic data around each scenario. Device tests require an explicit `hydriQa=true` opt-in; they must be run only in an isolated QA emulator. Upgrade tests also require a separately captured baseline.

The Room entities/schema remain at **version 1**, unchanged. Only DAO operations were added, so no migration or destructive database fallback was needed. There is no startup reset or replacement of existing user preferences.

## Remaining limits

- Runtime QA used one API 35 emulator in several display configurations. The owner should still verify the update on a personal phone; other Android versions, manufacturers, tablets and full TalkBack coverage were not exhaustively tested.
- Reminder scheduling, window calculations and notification posting passed. Long-duration delivery under Doze/manufacturer restrictions was not observed during this session.
- Backup/restore and CSV data paths passed repository tests. Dedicated page navigation passed; full interactive file-provider save/open flows were not automated for every document provider.
- Glass styling uses native Compose surfaces, translucency, highlights and rounded shapes; live backdrop blur is not implemented.
- One Windows Gradle cache file lock interrupted a final build attempt. Stopping the daemon and rerunning with two workers resolved it; the successful builds and tests listed above were executed afterward.

## Reproduction and artifacts

Build: `gradlew.bat assembleRelease testDebugUnitTest lintDebug lintRelease`. Owner release builds require the original local testing key. Other developers can use `assembleDebug` for isolated development.

On a dedicated QA emulator with the matching debug test APK installed:

```powershell
adb -s emulator-5580 shell am instrument -w -e hydriQa true com.hydri.app.test/androidx.test.runner.AndroidJUnitRunner
```

The upgrade test skips without its baseline file. Source UI tests restore their snapshot after each scenario. The QA AVD retains its synthetic fixtures for reproducibility; no personal data was used.

The release APK and its SHA-256 are in the GitHub release assets. The original `v1.0.0-test.1` release is retained. Source implementation commit: `414e42f` (later documentation/test-synchronization commits do not change app behavior). Raw local test/build logs remain under `.tools/` and Gradle reports under `app/build/`, excluded from Git.
