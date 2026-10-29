# Hydri for Android

A native, offline water tracker built with Kotlin and Jetpack Compose. No account, ads, analytics, backend, or Internet permission.

## Try Hydri on your Android phone

1. Open the release page on your Android phone—no GitHub sign-in is required.
2. Open [the Android testing release](https://github.com/jawad-mohammed35/hydri-android/releases/tag/v1.1.0).
3. Expand **Assets** and download **Hydri-1.1.0.apk**. Choose the APK, not the source-code archive.
4. Open the downloaded APK. If Android prompts, allow your browser or file manager to install apps, then choose **Install**.
5. Open **Hydri**. New installations show onboarding; upgrades keep your existing data and preferences. The app works offline after installation.

Requires Android 8.0 or newer. The release variant uses the same existing testing certificate as 1.0.0, allowing an in-place upgrade. It is a testing pre-release, not a production-key-signed build. The release includes `SHA256SUMS.txt` for verifying the APK. No account or GitHub sign-in is needed to download or use Hydri.

![Hydri Home](docs/home.png)

## Open and run

1. Clone this repository, then in Android Studio choose **Open** and select its root folder (the folder containing `settings.gradle.kts`).
2. Use JDK 17 for Gradle. Install Android SDK Platform 35 and accept SDK licenses if prompted. The Gradle wrapper downloads Gradle 8.11.1 and the pinned build dependencies on the first build. Building needs Internet access; using Hydri does not.
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

The 1.1.0 verification used a separate disposable QA emulator. Existing virtual devices and personal Hydri data were not reset.

Debug output is at `app/build/outputs/apk/debug/app-debug.apk`. For owner release builds, run `gradlew.bat assembleRelease`; the output is `app/build/outputs/apk/release/app-release.apk`. The release task requires the original local testing key and checks its public certificate fingerprint. It refuses a missing or different key instead of generating a replacement. Keys and credentials are not stored in Git.

## Features

- Five-step setup: welcome, name, live appearance preview, measurements, and preset/custom goal. Default bottles are added automatically; notifications are configured later.
- Four floating navigation tabs: Home, Bottles, Tracker, More.
- Favorites-only quick-add, swipe-to-delete with Undo, daily Undo and confirmed today-only Reset. Glass editors support custom amounts, fixed-date/time edits, and bottle previews.
- Bottle creation, names, volumes, seven icon choices, six colors, favorites, and reorder controls.
- Monthly calendar, expandable sip history by date, interactive day/week/month charts, averages, completion rates, streaks, and six badges. Log past-day water from the selected calendar date.
- Dated daily goals, themes (system/light/dark), OLED backgrounds, six accents, reduced motion, haptics, metric/US fl oz, date/time preferences.
- Approximate WorkManager reminders, active hours, quiet hours, weekdays, stop-at-goal, evening check-in, completion notifications, sound/vibration preferences, permission-aware notifications.
- Small and medium Glance widgets; medium includes quick-add actions for the first two favorite bottles.
- Dedicated Notifications, Hydration Guide, Data & Backup, and About pages. CSV export and JSON backup/restore use Android's file picker, with confirmation for destructive operations.

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

Long-press the Android launcher → Widgets → Hydri. Choose daily progress or progress with favorite bottles. Updates occur after in-app changes, widget quick-add, date/time broadcasts, and a periodic refresh. Android may delay background updates around midnight. Open Hydri to refresh immediately. Widget colors follow the system theme; Android widget capabilities differ from Compose screens.

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

Developer identity is configurable in `DeveloperInfo` in `MoreScreen.kt`. Version comes from `BuildConfig`. Add new library attributions to `THIRD_PARTY_NOTICES.md` and the in-app list. Add Room migrations when the schema changes; there is no destructive migration fallback.

## Tests

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
```

Device tests are guarded and skip unless explicitly enabled with `hydriQa=true`. Run them only on a disposable emulator. UI tests snapshot and restore entries, bottles, goals and preferences; they do not reset the application. Upgrade testing compares the original entries, bottle fields, goals and exact settings bytes against a captured 1.0.0 baseline.

To opt in for a single dedicated QA emulator, install the debug and Android-test APKs, then run:

```powershell
adb -s emulator-5580 shell am instrument -w -e hydriQa true -e class com.hydri.app.HydriFlowTest,com.hydri.app.RepositoryDeviceTest com.hydri.app.test/androidx.test.runner.AndroidJUnitRunner
```

`UpgradeDeviceTest` also requires an externally captured `upgrade-baseline.json` and skips when it is absent. Do not enable QA tests on a phone containing personal records.

See `docs/VERIFICATION-1.1.0.md` for current results and `docs/VERIFICATION.md` for the original 1.0.0 verification. A successful build is not a substitute for the owner's testing across devices.

## Release status

GitHub hosts the source and a versioned testing pre-release in the owner's public repository. APK binaries are attached to releases and excluded from source control. No release keystore or credentials are stored in the repository. Production signing, wider distribution, and the project's source license remain future owner decisions.
