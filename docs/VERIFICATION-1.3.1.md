# Hydri 1.3.1 verification

Version **1.3.1**, internal code **8**. Checked October 31, 2026 using the isolated Android 15 / API 35 QA emulator. A fresh baseline was captured from its installed **1.3.0** release before updating. No personal device was reset.

## Entry history and date rules

Sip History includes only recorded dates from today through six calendar days ago, newest first. Empty dates are not fabricated, and filtering does not remove database rows. Calendar totals, charts, streaks and badges continue using the full record set. Past dates can still be selected in the calendar, but their Log Water action is absent.

Normal entry creation, amount/time editing, deletion and deletion Undo go through the repository. Each operation checks the device's current local date inside its database transaction, after any wait for the database. Edits and deletes look up the persisted entry by ID rather than trusting a caller-supplied date. Edits preserve the date, original zone, name and bottle snapshots. New log timestamps must belong to today's date in the current device zone. Entry editors close on the existing UI date refresh; repository checks also reject stale actions before that refresh arrives.

Historical rows have no click/edit affordance, swipe deletion or accessibility delete action. Today's entries retain their editor and deletion controls. A pending Undo cannot restore yesterday's deleted entry after midnight. Today Reset affects only the date current when its transaction runs.

Existing explicitly confirmed **Data & Backup** operations—restore, delete all history and full reset—remain available as deliberate bulk data management. They are distinct from logging and per-entry editing. Room remains **schema 2**; no data migration or destructive fallback was added.

## Progress and navigation

Below and at 100%, the existing ring, wave, percentage and completion behavior remain unchanged. Above 100%, the same ring blends over 400 ms into a more saturated accent gradient and a subtle internal halo. The arc remains capped at one full circle while the displayed percentage retains the actual value. Returning to 100% or below restores the normal ring. Reduced motion removes the transition; no continuous glow animation, new background job or extra reward was added. Text retains the theme's contrast colors. Exceeding a goal is not presented as a health benefit.

More uses a dedicated 24 dp vector with four solid squares when selected, and the existing outlined icon otherwise. Icon tint, size, tab gestures and navigation hierarchy remain unchanged. Return-frame checks verify More stays selected and uses its filled icon after both toolbar and system Back from every secondary page.

## Results

- Debug, release and Android-test APKs built successfully with Gradle 8.14.6 / JDK 17. All **29 JVM tests passed**, including seven-day boundary/DST/year rollover checks, hydration calculations, time handling and seven-language resource/format validation.
- Debug and release lint completed with **0 errors, 32 warnings** each. Warnings include existing SDK/orientation/tooling guidance, the Indonesian `in`/`id` alias report, and now-unused legacy strings. No dependency or permission was added.
- All **54 device tests passed** in two runs: [51 core tests](qa-1.3.1/core.txt) and [3 seven-language UI tests](qa-1.3.1/languages.txt). Separate processes isolate the long locale-recreation loop from the other UI tests.
- The new repository tests use controlled clocks around midnight, year rollover and time-zone changes. They reject past/future logging, mismatched log timestamps, past edits/deletes, next-day Undo and edits that move the timestamp to another date. Current-day edit/delete/Undo still work and preserve original metadata. A held Room transaction queues writes while the date changes: the queued log is rejected and queued Today Reset preserves the previous day.
- UI checks cover the inclusive history window, descending dates, expand/collapse, historical rows without click actions, rollover of the visible list, historical totals/badges and the absence of a past-day calendar logging action. Existing upgrade, bottle snapshot, favorites, swipe/Undo/Reset, widget, reminder, shortcut, navigation and theme tests also passed.
- The actual Home progress component was rendered at 100%, 110% and 50% for all **28** combinations of Light/Dark/OLED/System and six custom accents plus native Material You. Tests check an intermediate animation frame, final/reset state, percentage semantics, changed ring pixels and reduced motion. Real emulator component captures were inspected. More's four quadrant centers are checked against the selected tint; toolbar/system Back checks cover all four More secondary pages and returning filled state.

All **6** component/navigation tests passed again at **720 × 1280 px, density 320 (360 × 640 dp), font scale 1.3**. This repeats all theme/accent ring checks, filled-icon pixels, calendar/history behavior and More return-frame checks. Representative small-screen captures were inspected; display/font overrides were restored afterward. See [raw results](qa-1.3.1/small-screen.txt).

## Signing and publication

The existing signing identity is preserved. It remains the original Android testing certificate; see [signing guidance](SIGNING.md). No replacement key or signing credential is committed. Apache LICENSE, NOTICE and third-party license assets remain unchanged. Existing stable releases are retained.

Local APK: **14,412,946 bytes**, SHA-256 `bfb0c05efd443d01bc6113d1bdf732fe55b2e5d11a2847a85fa26638067e96ac`. APK Signature Scheme v2 verifies against certificate SHA-256 `ed0e989eaa47ed777d32e9dcc9902f4462d349f5ad132bb8568971383b3ec9f0`. Installation over the captured 1.3.0 QA data and the original-data comparison passed. Public download verification is recorded on main after publication.

## Limits

Only API 35 emulator execution is claimed. No physical Samsung/OEM launcher test or older Android image was available. Midnight, DST, year-boundary and time-zone transitions use controlled clock/state tests, not changes to a user's device clock. Pixel checks and inspected captures cover the tested components and layouts; they do not establish performance benchmarks or every possible device/text scale.
