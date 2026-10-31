# Hydri 1.3.0 verification

Version name **1.3.0**, internal code **7**. Verification performed October 30, 2026 on an isolated Android 15 / API 35 emulator. Personal devices and their data were not reset. Final test counts and download verification are recorded below before publication is complete.

## Changes and data compatibility

The selected primary tab comes from the actual navigation hierarchy. More's secondary pages share a parent navigation group; its saved state is restored when switching tabs. The glass indicator animates a tab-relative position initialized from the actual selection rather than a pixel position before measurement. First-frame tests cover all four More pages, toolbar Back, native edge Back and activity recreation while retaining intentional primary-tab animations.

Seven offline string catalogs cover screens, onboarding, dialogs, guide text, reminders, widget content, shortcuts and accessibility labels. The native per-app locale preference synchronizes with Android settings. Numeric input and date/time formatting respect the selected language and explicit measurement/time/date choices. Original legal texts and user-created names remain unchanged. See [localization maintenance](LOCALIZATION.md).

Room remains schema **2**, with no migration or destructive fallback added. Language switching does not rewrite records, goal amounts, bottle snapshots or settings. Manual backups retain format 1 with an optional validated locale field; older backups preserve the active language. The upgrade fixture was captured from the installed 1.2.3 release before installing 1.3.0 over it.

## Executed checks

- Debug APK, signed release APK and Android-test APK built successfully with Gradle 8.14.6 / JDK 17. All **26 JVM tests passed**, including translation coverage/placeholder validation, all seven locales' numeric round trips and unsupported-language fallback.
- Debug and release lint completed with **0 errors, 30 warnings** each. Warnings include existing compatibility/deprecation guidance and lint's `in`/`id` alias mismatch; Indonesian was verified on the actual device rather than renaming its folder to silence that warning.
- The final-build regression run executed **47 device tests: 46 passed and one hit Compose's pending-setContent idling timeout** during the long language/dialog loop. That exact test then passed separately in a fresh process, exercising all seven languages and their screens/dialogs. Thus all 47 distinct cases have a passing result across these runs; the whole-suite invocation is not presented as an uninterrupted 47/47 pass.

Raw results: [regression](qa-1.3.0/regression.txt), [focused language rerun](qa-1.3.0/language-focused.txt), [restart preparation](qa-1.3.0/restart-prepare.txt) and [restart verification](qa-1.3.0/restart-verify.txt).
- Coverage includes first-return-frame indicator state for all More pages and both Back methods; restored navigation/scroll position; dragging/reselection; Light, Dark, OLED and Material You; water logging/editing, swipe/Undo/Reset, favorites, bottle snapshots, history/badges, shortcuts, widget resizing/theme/palette/quick-add/synchronization, repository validation and upgrade preservation.
- Actual app-language picker checks assert independently known native Home labels, every native picker option, primary and secondary screens, name/goal/reset/sip/bottle/time dialogs, and unchanged entries/bottles/goals. Onboarding completes in all seven languages with localized decimal goal input. Actual bound widgets, generated notification bodies and ShortcutManager labels are tested in all seven languages. Captures were inspected for representative Latin and CJK layouts; custom/user names are intentionally preserved.

During development, regression checks caught and corrected More's nested-graph scroll restoration and Android's required Indonesian resource-folder alias. Early text-overflow checks incorrectly treated unused paragraph width as clipping; checks now inspect rendered line bounds and allow one pixel of height rounding. Translated dialog assertions are scoped to dialogs because labels can legitimately match background controls.

The system locale service was used to select Japanese externally; the actual UI hierarchy displayed Japanese Home text. A separate two-phase device test selected French, then the app was force-stopped and cold-launched between invocations. Both phases passed, verifying the persisted language, visible French navigation and decimal input before restoring the original locale. Follow System and activity recreation are also exercised in the language tests.

All **5** navigation/language tests passed again at **720 × 1280 px / density 320 (360 × 640 dp), font scale 1.3**. This includes all seven onboarding flows and the complete language/dialog loop, with rendered-line bounds checked for displayed text. Display and font overrides were restored afterward. See [raw small-screen results](qa-1.3.0/small-screen.txt).

## Dependency and license review

AppCompat 1.7.1 supplies backward-compatible per-app locale handling. Resolved runtime metadata was inspected for all **104** modules: Apache 2.0 and BSD-3-Clause, with Guava ListenableFuture inheriting Apache from its parent POM. The [runtime license inventory](license-inventory-1.3.0.json) records module metadata; existing Apache, EPL and BSD notices remain intact. A public OSV batch query on October 30 returned **no advisory matches** for these resolved coordinates; this is a point-in-time check, not a guarantee of no vulnerabilities.

The root Apache LICENSE and NOTICE remain unchanged. [Historical licensing findings](LEGACY-LICENSING.md) identify the adoption commit and distinguish older tags/APKs without issuing a new historical grant. No old artifacts, release descriptions or Git history were rewritten.

## Signing and publication

The release preserves the original installed-app certificate. It remains the original Android testing identity, not a newly created dedicated production key. See [signing guidance](SIGNING.md). No private key or password is committed. Earlier stable releases remain available.

Local release APK: **14,411,498 bytes**; SHA-256 `4813a835d49a5176c700301dffe1033ec8d018c545c79031d0ce0d1b287903fb`. APK Signature Scheme v2 verifies against the original certificate, and the signed release has been installed over the QA app with original-data comparison passing. Public download verification is recorded on main after the release becomes available.

## Limits

Only the installed API 35 emulator image was available. No claim is made for executed API 26–32, physical Samsung/other OEM launcher or independent native-speaker review. Automated language checks and inspected captures cover sampled visible layouts, not every possible text scale, translated string or launcher. Existing Android background scheduling and shortcut rate limits still apply.
