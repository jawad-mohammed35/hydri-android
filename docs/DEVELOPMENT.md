# Development and data rules

Run the commands below from the repository root. Source licensing remains under review; these instructions do not select a license or grant additional reuse rights.

## Open and run

1. Clone this repository, then in Android Studio choose **Open** and select its root folder (the folder containing `settings.gradle.kts`).
2. Use JDK 17 for Gradle. Install Android SDK Platform 35 and accept SDK licenses if prompted. The Gradle wrapper downloads Gradle 8.14.6 and the pinned build dependencies on the first build. Building needs Internet access; using Hydri does not.
3. Allow Gradle sync, select the **app** run configuration, and select an API 26+ emulator in Device Manager. API 35 is recommended for initial testing.
4. Press **Run**. On first launch, complete the five short setup steps.

`local.properties` is machine-specific and ignored. Android Studio generates it automatically. On another computer, do not reuse the included local SDK path.

## Build and install from a terminal

PowerShell, from the project folder:

```powershell
.\gradlew.bat assembleDebug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.hydri.app/.MainActivity
```

Alternatively, drag the debug APK onto a running Android Emulator. Debug builds use Android's ordinary development signing configuration; no release signing secrets are generated or committed. Rebuild after changes with `gradlew.bat assembleDebug`, or press Run in Android Studio.

On this computer, you can also start the existing virtual device without Android Studio:

```powershell
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd mihraya_test
```

Release verification uses a separate QA emulator. Existing virtual devices and personal Hydri data are not reset.

Debug output is at `app/build/outputs/apk/debug/app-debug.apk`. For owner release builds, run `gradlew.bat assembleRelease`; the output is `app/build/outputs/apk/release/app-release.apk`. The release task requires the original local testing key and checks its public certificate fingerprint. It refuses a missing or different key instead of generating a replacement. Keys and credentials are not stored in Git.

## Features

- Five-step setup: welcome, name, live appearance preview, measurements, and preset/custom goal. Default bottles are added automatically; notifications are configured later.
- Four floating navigation tabs: Home, Bottles, Tracker, More. Tap or drag across the glass indicator to switch tabs, with optional haptics and keyboard focus feedback. In-page links and launcher shortcuts share tab navigation.
- Favorites-only quick-add, silent successful logging, swipe-to-delete in either direction with Undo, daily Undo and confirmed today-only Reset. Glass editors support custom amounts, fixed-date/time edits, immediate entry deletion, and bottle previews.
- Bottle creation, names, volumes, seven icon choices, six colors, favorites, and reorder controls.
- Monthly calendar, expandable sip history by date, interactive day/week/month charts, averages, completion rates, streaks, and six badges. Log past-day water from the selected calendar date.
- Dated daily goals, themes (system/light/dark), OLED backgrounds, six accents plus Android 12+ Material You device colors, reduced motion, haptics, metric/US fl oz, date/time preferences.
- Approximate WorkManager reminders, active hours, quiet hours, weekdays, stop-at-goal, evening check-in, completion notifications, sound/vibration preferences, permission-aware notifications.
- Themed glass reminder-time editors support 12/24-hour input, AM/PM, validation, keyboard handling, and animated Save/Cancel.
- Native launcher shortcuts: Log Water, Create Bottle, Tracker. Open Hydri once after installing/upgrading to register them, then long-press its launcher icon. Shortcuts reuse the current activity, wait for onboarding when needed, and do not replay after recreation.
- Responsive Glance widgets follow Hydri's appearance and accent; taller/wider sizes add favorite-bottle quick-add controls that work without opening the app.
- Dedicated Notifications, Hydration Guide, Data & Backup, and About pages with spring slide transitions and reduced-motion support. CSV export and JSON backup/restore use Android's file picker, with confirmation for destructive operations.
- Hydration Guide with US and Nordic reference tables, dated primary sources, and live measurement units. Total-water references include food and beverages; they are not drinking-water-only goals.

## Data and calculation rules

- Volumes use integer **microlitres**. One US fl oz = 29.5735295625 mL; input rounds to the nearest microlitre. Display rounding never rewrites stored values. Inputs accept 1–20,000 mL or equivalent. This is an input bound, not an intake recommendation.
- Entries store an instant, original zone, and explicit local date. Traveling does not move previously logged water between days. New entries use the current zone. Historical timestamps and edits use the entry's original zone, shown in the editor and beside times when traveling. Amount-only edits preserve the original instant, including during a DST overlap. Entries keep their original calendar date when edited. New Home logs use the current local date; calendar logs use the selected day. Time edits reject nonexistent DST times instead of silently shifting dates.
- Home follows the current local date and updates while open. Historical records are retained at midnight. Goals are effective from their stored local date until the next goal change; changing today's goal preserves earlier values. Empty dates after tracking began count as incomplete. Backdating the first record extends tracking to that date using the earliest goal.
- A day counts once when its total meets its goal. More water earns no extra streak credit. An unfinished today does not break yesterday's ongoing streak. A missed past day does. Badges are derived from records on each update; editing or deleting entries can revoke a badge or change its unlock date. This rule is visible in Tracker.
- Calendar weeks begin Monday. Week charts show the trailing seven days. Monthly charts show the displayed calendar month through today. Daily charts show the selected date. Averages include all tracked elapsed dates in that period, including zero-intake days and today.
- Data is in app-private Room and DataStore files. Android automatic cloud backup is disabled. JSON backups include entries, dated goals, bottles and preferences; imported files are limited to 10 MB and validated before replacement. CSV includes volume, effective goal, original zone and UTC timestamp. CSV is an export, not a restore format.

## Reminder behavior

WorkManager survives process death and device reboot. Delivery is approximate; Doze, battery restrictions and manufacturer policies may delay it. Active/quiet hours and weekday checks use the local time at delivery, including time-zone changes. Equal start/end means the whole day; quiet hours take precedence. No selected weekdays means no reminders. Evening checks target 20:00 local and are suppressed outside the configured windows or when the goal is reached. Completion notifications fire when logging crosses today's goal, including widget logging; entry edits do not create celebrations. Undoing and recrossing can notify again.

Notification permission is requested when enabling reminders or completion notifications. If blocked, use More → Notifications → Android notification settings. Android owns channel preferences and can override sound/vibration settings. Hydri requests neither exact-alarm access nor broad storage access.

## Widgets

Long-press the Android launcher → Widgets → Hydri. Both widget entries resize horizontally and vertically. Compact sizes show progress; tall sizes offer one favorite bottle, and wider sizes offer two. Spacious layouts also show bottle names and remaining water. With no favorites, the widget directs you to add one in Hydri.

Widgets follow Hydri's Light, Dark, Follow System, OLED and accent choices. Match Device Colors uses Android 12+ wallpaper colors, including native color resources that respond to palette changes; earlier Android versions use Water blue. Widgets use native rounded surfaces and progress graphics rather than unsupported blur effects.

Updates follow app changes, edits/deletions, Undo, Reset, goals, appearance settings, and widget quick-add. Quick-add writes directly to the same offline database and refreshes progress without opening an activity. Live Glance compositions observe data; date/time broadcasts and one 30-minute WorkManager refresh while widgets are installed cover inactive sessions. Android may delay background work around midnight. No foreground service or frequent permanent polling is used.

## Source layout

```text
app/src/main/java/com/hydri/app/
  MainActivity.kt, HydriApplication.kt
  data/          Room entities/DAO, DataStore settings, repository, backup/CSV
  domain/        conversion, formatting, streaks and reminder-window rules
  ui/            ViewModel, design components, onboarding, dialogs, four pages
  background/    WorkManager, time receiver, Glance widgets and actions
app/src/main/res/       vector icon, theme, widget definitions
app/src/main/assets/    bundled license texts
app/src/test/           JVM calculation tests
app/src/androidTest/    emulator repository and Compose interaction tests
docs/                   verification report and screenshots
```

About credits Jawad Mohammed and links to his LinkedIn profile. The footer displays the version name from `BuildConfig`; Android retains the version code internally. Add new library attributions to `THIRD_PARTY_NOTICES.md` and the in-app list. Add Room migrations when the schema changes; there is no destructive migration fallback.

## Tests

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
```

Device tests are guarded and skip unless explicitly enabled with `hydriQa=true`. Run them only on a dedicated QA emulator. UI tests snapshot and restore entries, bottles, goals and preferences; they do not reset the application. Upgrade testing compares the original records, all existing preference values, and derived badge statistics against a captured 1.2.1 baseline. No schema changes are needed for 1.2.2.

To opt in for a single dedicated QA emulator, install the debug and Android-test APKs, then run:

```powershell
adb -s emulator-5580 shell am instrument -w -e hydriQa true -e class com.hydri.app.UpgradeDeviceTest,com.hydri.app.PolishDeviceTest,com.hydri.app.StableRefinementDeviceTest,com.hydri.app.LauncherShortcutDeviceTest,com.hydri.app.NavigationAndAppearanceDeviceTest,com.hydri.app.WidgetSyncDeviceTest,com.hydri.app.HydriFlowTest,com.hydri.app.RepositoryDeviceTest com.hydri.app.test/androidx.test.runner.AndroidJUnitRunner
```

`UpgradeDeviceTest` also requires `upgrade-1.2.1.json` in the app's external-files directory and skips when it is absent. Before upgrading the QA installation, `BaselineCaptureDeviceTest` captures it only with `-e captureBaseline true`. `WidgetSyncDeviceTest` temporarily changes the QA emulator's palette/night mode, restores them afterward, and requires shell widget-binding permission. Do not enable QA tests on a phone containing personal records.

See [1.2.2 verification](VERIFICATION-1.2.2.md) for current results, [1.2.1 verification](VERIFICATION-1.2.1.md), [1.2.0 verification](VERIFICATION-1.2.0.md), [1.1.0 verification](VERIFICATION-1.1.0.md) and [original verification](VERIFICATION.md) for prior releases. A successful build is not a substitute for the owner's testing across devices.

## Release status

GitHub hosts the public source and versioned stable releases. APK binaries are attached to releases and excluded from source control. The stable APK retains the original local testing certificate for compatible updates; no signing keystore or credentials are stored in the repository. A production signing migration and the project's source license remain owner decisions. See [signing guidance](SIGNING.md) and [the licensing review](SECURITY-AND-LICENSING.md).

Windows builds can encounter a temporary Gradle transform-cache rename lock. Stop Gradle daemons and retry with --no-watch-fs and --max-workers=1. Do not disable antivirus protection or delete the signing keystore to resolve a cache problem.
