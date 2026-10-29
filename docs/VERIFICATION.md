# Hydri local verification — 2026-10-27

This report distinguishes executed checks from implemented features that still need wider testing. The debug APK is for the owner's evaluation. This verification was originally performed locally on 2026-10-27; the owner subsequently requested a private GitHub repository and a downloadable testing release on 2026-10-29.

## Environment

- Windows, Java: Eclipse Temurin 17.
- Android SDK: `C:\Users\jawad\AppData\Local\Android\Sdk`; Platform 35, Build Tools 34.0.0, platform-tools, emulator and an existing AVD were available.
- Android Studio was not found in the usual installation locations. The installed command-line Android toolchain was sufficient to build, install, launch and test the app.
- Used Gradle 8.11.1, Android Gradle Plugin 8.7.3, Kotlin 2.1.0, Compose BOM 2025.01.00, Room 2.7.2. These are pinned stable versions compatible with the installed SDK/toolchain.
- Emulator: existing `mihraya_test`, Android 15 / API 35, x86_64, 1080 × 2400. It was started headlessly with software graphics; no other application's data was cleared.
- Gradle distribution was checked against its official SHA-256 and the checksum is pinned in the wrapper.

## Executed results

- **Debug build: passed.** `:app:assembleDebug` generated an installable APK; ADB installation succeeded and the main activity launched.
- **JVM tests: 11 passed, zero failures.** Coverage includes microlitre/US-ounce conversions, invalid input, effective-date goals, streak gaps, unfinished today, badge revocation after edits, extra intake, reminder windows, weekdays, quiet hours, DST and future-day exclusion.
- **Android device tests: 7 passed.** The test runner reported `OK (7 tests)` on the API 35 emulator. It was invoked directly with ADB after building the test APKs, which preserves diagnostic screenshots; the included Gradle connected-test task is also usable.
- **Lint: passed with zero errors and 21 warnings.** Warnings: 16 newer-dependency suggestions, 1 preference for KSP over the functioning KAPT configuration, 4 widget target-cell attributes ignored on older APIs. Legacy widget minimum sizes provide the older-API fallback. Dependency upgrades are deliberately separate from this verified SDK 35 baseline.
- **No Internet permission.** The merged/installed manifest was inspected. Unneeded network-state and foreground-service permissions from WorkManager were removed. WorkManager's normal wake-lock and boot permissions remain for background scheduling.
- **Force-stop/relaunch: passed.** The sample total remained **1,000 / 2,000 mL (50%)** after the process was force-stopped and relaunched. Onboarding did not recur.
- **Local date/time-zone rollover: passed.** While the activity was open, temporarily changing the emulator from America/New_York to Pacific/Kiritimati advanced its local calendar date and Home showed **0 / 2,000 mL (0%)**. Restoring America/New_York restored the existing day's **1,000 mL**. The original emulator time zone was restored.
- **Visual review:** Home, calendar/day detail and OLED settings screenshots were inspected. A snackbar overlap found during testing was addressed with dismiss controls/replacement behavior. The Home visualization was compacted to leave more room for quick-add amounts above navigation.

## Device-test scope

The end-to-end Compose test actually interacted with the seven-step onboarding, all four tabs, preset logging, bottle creation and editing, goal editing, Dark/OLED appearance, accent selection, switching to US fl oz, custom logging, Undo, editing and deleting an entry with confirmation. It checks that the final stored number of records is correct and returns display preferences to Light/metric.

Repository tests exercised a JSON backup round trip containing settings, entries, goals and bottles; malformed-backup rejection without data replacement; CSV precision and spreadsheet-formula escaping; database close/reopen persistence; past-day goals; reminder enqueue/cancel behavior; and widget update invocation.

Additional tests bound **both widget sizes** to a real Android `AppWidgetHost`, waited for their remote views to render Hydri text, and called the medium widget's favorite-bottle action, checking the exact added amount. Temporary widget-host bindings were removed afterward. A notification test granted the emulator's runtime permission, posted a notification and verified it appeared in Android's active notifications.

## Remaining verification limits and design choices

- Runtime testing used one API 35 emulator. Physical phones, API 26–34 devices, tablets/foldables, landscape, maximum font scaling and a full TalkBack audit have not been exhaustively tested.
- Reminder work creation, cancellation, delivery-window rules and actual notification posting were verified. Long-duration delivery under Doze, manufacturer battery policies and a physical device reboot were not observed. WorkManager handles reboot restoration, and a time-change receiver reschedules local-time behavior.
- Widgets were tested in an Android widget host, including rendered content and quick-add. Manual placement/resizing in multiple manufacturers' launchers still needs owner testing. Background midnight refresh can be delayed by Android.
- Backup/restore and CSV data paths were tested through the repository. The real system file-picker wiring is implemented, but a complete interactive save/open round trip with every external document provider was not automated.
- Glass surfaces use layered translucency, gradients, thin highlights and rounded shapes. They do **not** implement live backdrop blur. This avoids applying blur to text or relying on expensive device-specific effects.
- Date rollover was verified by crossing the local date through a time-zone change; an overnight wait for a physical midnight was not performed. The same local-date observer drives both cases.
- The developer name is a configurable placeholder, as requested. No developer URLs/contact details were invented. No source license, release keystore, secure signing setup or public distribution was chosen on the owner's behalf.

## Artifacts and reproduction

- Installable APK: `../deliverables/Hydri-1.0.0-debug.apk`.
- Android's `apksigner verify` passed (APK Signature Scheme v2). The matching SHA-256 is in `deliverables/SHA256SUMS.txt`.
- Normal Gradle output: `../app/build/outputs/apk/debug/app-debug.apk`.
- Calculation test report: `../app/build/reports/tests/testDebugUnitTest/index.html`.
- Lint report: `../app/build/reports/lint-results-debug.html`.
- Raw test/build/lint reports are preserved locally and excluded from Git to avoid uploading machine-specific diagnostic output. Screenshots and this summary are included in the repository; the APK and its checksum are release assets.
- Run instructions and project structure: `../README.md`.

From PowerShell, use `gradlew.bat assembleDebug testDebugUnitTest lintDebug`; with an unlocked API 26+ emulator, use `gradlew.bat connectedDebugAndroidTest`. Device tests reset Hydri's own emulator data. Do not run them against personal hydration records.

After collecting evidence, the instrumentation test package and generated Hydri sample data were removed. Hydri was launched at its welcome flow, then the headless emulator started for this work was stopped. The app remains installed in the AVD for the owner's next run.
