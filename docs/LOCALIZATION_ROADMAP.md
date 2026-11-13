# R&MA Localization Roadmap

## Product requirement

Keep Persian as a fully supported language and add at least ten widely used languages. The initial target set is:

1. English
2. Arabic
3. Turkish
4. Urdu
5. Hindi
6. Simplified Chinese
7. Spanish
8. French
9. German
10. Russian
11. Portuguese

This list is a proposed baseline and can be adjusted after reviewing user demand and translation quality. Persian remains first-class; it is not removed or demoted.

## Implementation gates

1. Inventory every user-visible string in Compose screens, dialogs, validation, VPN status, notifications, settings, accessibility descriptions, and import/export flows.
2. Replace hard-coded UI strings with Android string resources; preserve stable resource names and avoid concatenated sentences.
3. Add locale resources using standard Android resource qualifiers and a predictable fallback to Persian or the system locale, with an explicit product decision on the default.
4. Add a language selector only after the locale-switch mechanism is selected for the app's minSdk 26 and targetSdk 35. Verify supported Android per-app language APIs and compatibility before adding dependencies.
5. Validate RTL layout in Persian, Arabic, and Urdu; use start/end layout semantics, mirrored icons only where meaningful, and test mixed LTR technical tokens such as IP addresses, endpoints, and WireGuard snippets.
6. Validate Simplified Chinese locale selection, font coverage, truncation, and line wrapping.
7. Add plural, number, date, and unit formatting tests; do not build localized strings by concatenating fragments.
8. Use reviewed translations for security warnings and VPN state messages. Machine translation may be a draft, never the final authority for safety-critical text.
9. Add screenshot/regression checks for all supported locales and verify that localization does not alter configuration bytes, cryptographic records, or tunnel behavior.

## Current status

Planning only. The current app has a small `strings.xml` resource set and substantial UI copy still needs inventory. No new locale has been added and no translation is claimed complete. Localization is intentionally scheduled after the secure metadata storage foundation and before public fixed-egress UI/release work.
