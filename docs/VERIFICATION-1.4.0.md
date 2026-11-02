# Hydri 1.4.0 verification

Version **1.4.0**, internal code **9**. Verification date: November 2, 2026. Testing uses the isolated Android 15 / API 35 Hydri emulator; the other running emulator and personal devices are untouched. A fresh baseline was captured from its installed **1.3.1** APK before updating.

## Tracker

The selected-date intake/logging card under the calendar is removed. Calendar date selection and its status markers remain; each date also exposes its amount, goal and percentage to accessibility services. Home remains the water logging entry point, and today's existing sips can still be edited from history.

Intake offers two equally sized Week/Month controls. A restored Day or unknown range immediately renders Week and normalizes its saved value. Week retains the trailing seven calendar days; Month shows the browsed month through today. Chart selection, totals, averages and completion counts remain. The explanatory completed-days paragraph and Badges subheading are removed; the badge cards and calculation rules are unchanged.

Horizontal calendar drags beyond the directional touch slop and distance threshold request one month on release. Vertical scrolling and short horizontal movements do not navigate. Arrows use the same month rules. A 240 ms horizontal slide/fade and bounded height transition settle before another month request is accepted; reduced motion removes these transitions. Next-month navigation stops at the current month, as the existing arrow did. Browsing retains the selected date and uses each displayed month's actual dates and records.

## Bottle shapes and data

Existing IDs **0–6** keep their original icons and names. IDs **7–13** append travel mug, can, mason jar, tall glass, insulated flask, straw tumbler and squeeze bottle. The new 24 dp native vectors use matching 2 dp rounded strokes. The selector shows two horizontally scrollable rows plus the selected shape name. Names and accessibility labels are bundled in all seven languages.

The shared icon catalog supplies Bottles, Home quick-add, sip snapshots and bottle previews. Both live bottle IDs and entry snapshot IDs are validated against the stable ID table during restore. New shapes persist as integers in the existing schema; Room remains **version 2** and manual backups remain **format 1**. No migration or destructive fallback was added. Old records, goals, colors, favorites, preferences and achievements are retained. Widgets retain their existing text-based bottle controls.

## Executed QA

- Debug, signed release and Android-test APKs built successfully with Gradle 8.14.6 / JDK 17. All **34 JVM tests passed**, including removed-Day fallback, calendar/month boundaries, leap February, stable shape IDs, hydration calculations and complete translation/placeholder checks.
- Debug and release lint completed with **0 errors, 32 warnings** each. Existing compatibility/tooling and unused-resource guidance remains; no dependency or permission was added.
- All **60 regression device tests passed** in two runs: [57 core cases](qa-1.4.0/core.txt) and [3 seven-language cases](qa-1.4.0/languages.txt). Tests check both swipe directions, arrows, month limits, year rollover, selection/indicator association, rapid-gesture gating, short/vertical gesture rejection, equal range-control widths, chart date counts and the removed Tracker sections.
- Every one of the 14 icons was rendered and checked for a distinct silhouette in all **24 theme/bottle-color combinations** (Light, Dark, OLED and Material You; six bottle colors). Actual editor tests selected and saved each of the seven new IDs, logged its preset from Home, and checked the resulting sip icon and color. Locale tests select the new squeeze-bottle option and check its translated label and visible text bounds in all seven languages.
- Persistence tests close/reopen a separate on-disk QA database, restore backups containing all old/new shape IDs, retain sip snapshots after bottle deletion, and reject unknown IDs without changing entries. Existing data-policy, badges, navigation, floating-bar gestures, widget synchronization/themes, reminders, shortcuts and upgrade tests also passed.

A separate two-phase probe saved an insulated flask and its sip snapshot, then the app was force-stopped and cold-launched between invocations. Both phases passed and the saved shape/color rendered from the reopened app database. Its original QA snapshot was restored afterward. See [preparation](qa-1.4.0/restart-prepare.txt) and [verification](qa-1.4.0/restart-verify.txt). All **7** small-screen cases also passed at **720 × 1280 px / density 320 (360 × 640 dp), font scale 1.3**: calendar/vertical/rapid gestures, Tracker controls, all shape themes/colors, actual new-shape editor/logging, and More return behavior. [Raw results](qa-1.4.0/small-screen.txt). Representative captures were inspected, and display/font overrides were restored.

## Signing and release

The original package name, Apache LICENSE/NOTICE and signing identity are preserved. The certificate remains the original Android testing identity; see [signing guidance](SIGNING.md). No private signing file or credential is committed. Previous stable releases are retained.

Local APK: **14,417,286 bytes**, SHA-256 `46d16b7971e8dd84ca4fd7231a39039e2c3a430eec31a3b448661222feabe058`. APK Signature Scheme v2 verifies against the existing certificate SHA-256 `ed0e989eaa47ed777d32e9dcc9902f4462d349f5ad132bb8568971383b3ec9f0`. Installation over the captured 1.3.1 data and original-data comparison passed. The [1.4.0 release](https://github.com/jawad-mohammed35/hydri-android/releases/tag/v1.4.0) is published with draft=false, prerelease=false and latest status. Its APK was downloaded without authentication, matched the tested bytes by SHA-256, and its signature verified. The downloaded file installed with `adb install -r`, cold-launched with Status: ok, and passed the captured 1.3.1 entries/bottles/goals/preferences/badge-statistics comparison again. [Downloaded-APK upgrade result](qa-1.4.0/downloaded-upgrade.txt). Stable releases v1.2.1, v1.2.2, v1.2.3, v1.3.0 and v1.3.1 and their assets were verified as retained.

## Limits

Only API 35 emulator execution is claimed. Physical Samsung/other OEM devices and other Android API levels were not tested. Component captures are actual Android renders of the shared UI components, not invented application screenshots. No performance benchmark or exhaustive review of every screen size/font setting is claimed.
