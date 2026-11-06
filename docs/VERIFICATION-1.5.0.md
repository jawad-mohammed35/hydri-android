# Hydri 1.5.0 verification

Version **1.5.0**, internal code **10**. Verification date: November 5, 2026. Tests use the isolated Android 15 / API 35 emulator, serial emulator-5580. The original **1.4.0** entries, bottles, goals and preferences were captured before installing this update.

## Custom logging

New Custom Water opens with an empty amount field. Focus is requested after the dialog window gains focus and its input is attached; Android's numeric keyboard opens without a second tap. mL uses numeric input; L and US fl oz use decimal input. Done hides the keyboard and never submits an invalid amount. The shared glass dialog uses a full-window coordinate space with explicit keyboard resize handling. On Android 11+, system-bar fitting is disabled at the native window level so Compose can apply the same system/IME insets consistently. This fixes partially covered actions on small enlarged-text layouts. Its body scrolls while actions stay pinned; dismissal hides the keyboard. See the [Android window insets API](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#setFitInsetsTypes(int)).

An optional localized Name field starts with the localized Custom water label. Blank/default input retains the existing `Custom water` sentinel so that default labels follow the application language. User names preserve Unicode and internal spacing, with only outside whitespace trimmed. Today's custom names can be edited; bottle-based names and snapshots remain unchanged. Home and Tracker use the existing entry-name renderer, and custom entries keep their blue droplet.

The existing Room name column is reused. Room remains schema **2**, backups remain format **1**, and no migration, reset, destructive fallback or rewrite of historical records was introduced. Backup restore now supports longer custom entry names instead of the bottle-name limit; bottles retain their existing validation. Current-day editing restrictions remain enforced inside repository transactions.

## Widgets

Both existing widget providers, classes and metadata identities are preserved. They use Glance `SizeMode.Exact` to render dimensions supplied by each launcher, rather than choosing from six fixed layouts. Both metadata files already support horizontal and vertical resizing. The progress provider prioritizes its ring and a compact favorite row; the favorite-bottle provider prioritizes quick actions, adding a second row at larger heights. Wide layouts place ring and amount side by side; tall layouts stack them. Small layouts prioritize intake and progress. Larger fonts omit the brand row at minimum height, and the amount's font fits its own available width without scaling the whole layout. Quick-add targets are 48 dp high.

Existing Room/DataStore observation, revision updates, background date updates and native boot/time receivers are retained. Widget quick-add logs without opening an activity. All app mutation paths continue refreshing the widgets; theme, accent, dynamic colors, measurement and language handling are preserved. Shared ring bitmaps remain cached, and no new background service, network access or dependency was added.

## Daily Flow

Below and at the goal, the existing ring, text and progress behavior remain. Above the goal, a continuous intensity equals `(total / goal - 1)` capped at 1. A finite 550 ms transition adjusts accent saturation/brightness, gradient highlights, inner halo and ring glow. Intensity reaches its maximum at 200%; removing water reduces it smoothly, and returning to 100% removes the effect. Percentages remain uncapped. Reduced motion removes these transitions. There is no repeating animation, shimmer timer or message encouraging excessive intake.

## Executed checks

- Debug, signed release and Android-test APKs built successfully. All **38 JVM tests passed**, including Unicode/default names, continuous/capped intensity, widget sizing rules and translation/placeholder coverage.
- Debug lint: **0 errors, 32 warnings**. Release lint: **0 errors, 34 warnings**. Warnings concern compatibility, dependency-update, orientation, unused-resource and icon guidance. No dependency or permission was added.
- All **64 emulator regression cases passed**: [52 core](qa-1.5.0/core.txt), [4 custom logging/ring](qa-1.5.0/custom-logging.txt), [5 widget](qa-1.5.0/widgets.txt), and [3 multilingual](qa-1.5.0/languages.txt). Core cases cover navigation, bottles/icons, calendar, history/date policies, deletion/Undo/Reset, settings, reminders and shortcuts. New cases check actual keyboard visibility/focus, invalid Done input, Unicode names, editing/backup/bottle protection, Home/Tracker name display, and ring intensity increasing/decreasing/capping across Light, Dark, OLED and Material You. Existing ring pixel checks also exercise all accents.
- Both providers rendered at **100x80, 100x340, 160x120, 160x200, 180x340, 240x110, 240x160, 275x190, 320x220 and 360x360 dp**. Additional cases check localized amount ellipsis and vertical text bounds at four narrow/wide/tall sizes in all seven languages, with mL/L/oz preferences. Live quick-add, app logging, edit/delete, Undo/reset, goal, palette, theme and language changes passed. Quick-add did not open an Activity.
- All **5 small-screen cases passed** at **360x640 dp, font scale 1.3**: the four custom logging/ring cases and the seven-language widget clipping matrix. Display/font overrides were restored. An initial assertion ran before Android delivered dialog window focus; the final test waits for that event and passes without changing app focus behavior.
- A two-phase [prepare](qa-1.5.0/restart-prepare.txt)/[verify](qa-1.5.0/restart-verify.txt) probe force-stopped and cold-launched the app. A custom Unicode name, bottle shape/color and sip snapshot survived; the original QA snapshot was restored. The [original 1.4.0 data comparison](qa-1.5.0/local-upgrade.txt) passed for records, bottles, goals, preferences and derived statistics/badges.
- A separate native widget host retained both provider IDs and dimensions through an actual emulator reboot. Both [preparation](qa-1.5.0/widget-reboot-prepare.txt) and [restored rendering verification](qa-1.5.0/widget-reboot-verify.txt) passed.

A final [10-case packaging pass](qa-1.5.0/final-package.txt) verified logging, onboarding/goal/bottle flows, native keyboard behavior, and original-data preservation on the exact uploaded bytes. The separate [small-screen keyboard boundary check](qa-1.5.0/keyboard-boundary.txt) compares both action buttons with the native IME touchable region, including the dialog window offset.

Representative native dialog, ring and widget captures were inspected. Component/host captures are actual Android renders, not invented screenshots.

## Release and compatibility

The APK is **14,434,218 bytes**, SHA-256 `18a90c57b8e40244ec5bccd904d392f2f01a12489b4d4bd8f2a42686ff4869c0`. APK Signature Scheme v2 verification passed, with the original certificate SHA-256 `ed0e989eaa47ed777d32e9dcc9902f4462d349f5ad132bb8568971383b3ec9f0`. The package name and Apache 2.0 LICENSE/NOTICE are unchanged. The original Android testing signing identity is intentionally retained; see [signing guidance](SIGNING.md). No signing file or private credential is committed.

The signed candidate installed over 1.4.0, launched successfully and passed the original-data comparison. The tested installed APK hash matches the staged artifact. Public-download verification will be recorded after publishing.

## Limits

Execution is limited to the API 35 emulator and Android's native AppWidgetHost. Physical Samsung One UI and other Android API levels were unavailable. Automated resizing uses a native test AppWidgetHost, not manual Pixel Launcher resize gestures; no physical-launcher compatibility claim is made. Launchers can impose their own minimum sizes and grids; Hydri adapts to the actual dimensions they provide. A post-reboot Android system-process warning appeared while the launcher was active. A complete emulator restart without wiping data recovered it; subsequent cold launches and the native keyboard-boundary check passed. No battery-duration benchmark, frame-rate benchmark, or exhaustive coverage of every possible density/font size is claimed.
