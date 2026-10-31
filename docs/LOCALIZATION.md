# Languages and formatting

Indonesian uses Android's legacy resource-folder alias `values-in` while the public BCP 47 identifier remains `id`. Lint 8.7 reports this alias as an unused translation; device QA checks the actual rendered Indonesian labels.

Hydri bundles English (`en`), Spanish (`es`), French (`fr`), Simplified Chinese (`zh-Hans`), Korean (`ko`), Japanese (`ja`) and Indonesian (`id`). English is the fallback. No translation service or network access is used.

More → Settings → Language offers native language names and Follow System. Android 13+ uses the system LocaleManager, synchronized with system per-app language settings. Android 8–12 uses AppCompat 1.7.1 with automatic locale storage. MainActivity is an AppCompatActivity; changing languages can recreate its UI while the navigation state and ViewModel are retained. No process restart is required. Follow System selects the first supported device language; unsupported languages fall back to English. Traditional Chinese is not advertised as a Simplified Chinese translation.

Resources live in `app/src/main/res/values*/strings.xml`; supported languages are declared in `res/xml/locales_config.xml`. Language splits are disabled so every supported language remains bundled. Native Android fonts render CJK text. Proper names, original legal texts, user-entered bottle names and canonical data identifiers are intentionally retained. Built-in bottle names are translated for display without rewriting stored records.

`i18n/AppText.kt` supplies localized resources to Compose and background tasks using an application-derived configuration context. Locale changes refresh notification channel labels, launcher shortcuts and widgets. Existing pending reminders retain their scheduling; new messages use the active language. The app also handles Android's locale-change broadcast and refreshes on launch.

Dates and numbers follow the selected locale. Explicit date format, 12/24-hour time and measurement choices take precedence. Amount input supports local decimal/grouping separators and fullwidth digits, with validation before conversion to integer microlitres. Stored dates, timestamps, goals and amounts do not change when languages change. JSON/CSV keys remain stable technical identifiers.

Manual JSON backups add an optional `applicationLocales` field without changing the format number or database schema. Restore validates its supported language tags before replacing data. Old backups without that field leave the current language unchanged. Android cloud backup remains disabled.

When adding text, use string resources and numbered placeholders, not concatenated English sentences. Use plurals for counts and add every resource to all six translated catalogs. `LocalizationResourceTest` checks key coverage, duplicate keys, replacement characters and formatting signatures. `LocaleFormattingTest` checks numeric input, conversions and fallback behavior. Device tests exercise the actual picker, screens, onboarding, reminders, shortcuts and widgets in all seven languages. Translations have not been certified by independent native-speaking reviewers; language-specific feedback is welcome.
