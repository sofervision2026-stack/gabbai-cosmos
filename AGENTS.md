# Project Architecture

- Keep the application as a native Android Jetpack Compose project because its voice, file export, and installable APK workflows depend on Android platform APIs.
## Decisions (v2.5)
- All pickers go through SearchPickerDialog (a Dialog with a bounded list); never put a lazy list inside a DropdownMenu — it crashes on tap.
- Form AlertDialogs use usePlatformDefaultWidth = false with ~94% width, so labels fit on one line and the dialog lays out reliably.
- UI text is Georgian source wrapped in L()/Lf(); translations live in assets/i18n.json. Never call L() in enum constructors or other once-evaluated values — translate at read time.
- Amounts are stored with a currency code per record; money()/signedMoney() format them.
- Room schema changes need an explicit Migration (no destructive fallback) so users keep their data.
