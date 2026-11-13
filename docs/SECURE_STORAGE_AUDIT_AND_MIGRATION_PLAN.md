# R&MA Secure Storage Audit and Migration Plan

Date: 2026-11-13
Branch: `rebuild/rma-android-structure`

## Scope

This document records a source-level audit of the current WireGuard profile storage and defines a data-preserving migration plan. It does not change storage behavior and does not authorize changes to the protected `main` branch.

## Current implementation observed

File: `app/src/main/java/com/rescuevpn/app/data/security/SecureConfigStore.kt`

- Uses `EncryptedSharedPreferences` with AES-256-SIV key encryption and AES-256-GCM value encryption.
- Obtains the master key through `MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)`.
- Stores each profile's raw configuration and display name under separate keys in the same editor transaction.
- Stores the selected profile ID in the same encrypted preference file.
- Uses `apply()` for ordinary writes and `commit()` for `clearAll()`.
- Fails closed when encrypted preference initialization throws; no plaintext fallback is present.
- The class currently has no explicit schema/version marker, migration state, or recovery report.

File: `MainViewModel.kt`

- Constructs `SecureConfigStore` during ViewModel initialization.
- Loads all stored profile IDs, reads each config, and validates/parses it.
- Silently skips a profile when parsing throws; this can hide a damaged or incompatible stored entry from the user.
- Adds and deletes profiles through the store. Wipe-all calls disconnect and then clears the encrypted preference file.

File: `AndroidManifest.xml`

- Application has `android:allowBackup="false"`, which avoids restoring encrypted preference ciphertext without its Keystore key through Android Auto Backup.
- This is conservative for the current encrypted store, but means profile backup/restore is not provided.

File: `app/build.gradle.kts`

- Uses `androidx.security:security-crypto:1.1.0`.
- The API family used here is deprecated in the stable 1.1.0 release. A dependency version change alone is not a migration.

## Risks and design decisions

1. **Do not replace the current store in-place yet.** Existing users may have profiles encrypted with the current master key. Removing the old preference file or key too early would permanently lose access to those profiles.
2. **No plaintext intermediate file.** Migration must never write raw WireGuard private keys to ordinary SharedPreferences, logs, cache, external storage, or a temporary plaintext file.
3. **Fail closed.** If the new Keystore key cannot be created or the encrypted destination cannot be verified, retain the legacy store and report migration failure. Do not silently continue with an empty profile list.
4. **Atomicity and rollback.** Copy all profile IDs, names, raw configurations, and selected profile ID into the new encrypted store; read them back and validate every record; only then set a durable migration-complete marker. Keep the legacy encrypted store until a later release after successful field validation.
5. **Crash recovery.** Migration must be idempotent. On process restart, an incomplete migration should safely retry without overwriting a valid new store with empty data.
6. **Key invalidation.** Android Keystore key loss/invalidation must be surfaced as a recoverable user-facing error. The app must not generate a replacement key and pretend old ciphertext is readable.
7. **Asynchronous writes.** Existing `apply()` calls are asynchronous. A migration must use explicit completion checks for the new destination and must not mark completion before durable writes are verified.
8. **Damaged records.** Current loading silently drops invalid profiles from the in-memory list. A future storage health report should distinguish unreadable/corrupt entries from syntactically invalid WireGuard configurations without exposing private-key contents.
9. **Backup policy.** Keep `allowBackup=false` until a deliberate encrypted export/import or device-transfer design exists. Do not enable backup of ciphertext tied to a device-bound Keystore key.

## Recommended target design

- Replace the deprecated preference wrapper with a small encrypted record store using Android Keystore directly (AES/GCM/NoPadding) and a versioned, authenticated record format.
- Keep profile IDs and metadata inside authenticated ciphertext initially; do not expose endpoint/private key material in logs.
- Use a dedicated storage abstraction and a versioned envelope (format version, nonce/IV, ciphertext, and authenticated associated data binding the record ID and schema version).
- Use atomic file replacement for encrypted records, with strict app-private internal storage permissions. Never persist plaintext records.
- Separate key management, record serialization, encrypted I/O, and migration orchestration so each can be unit-tested.
- Consider an established, maintained cryptographic library only after verifying its current Android support, maintenance status, and migration compatibility. Do not invent a custom cryptographic protocol.

## Required implementation sequence

1. Add tests for current-store behavior and a migration fixture representing the existing encrypted preference layout. Tests must not contain real user keys.
2. Implement and test the new encrypted record format independently, including tamper detection, wrong record ID/AAD, missing key, invalid nonce, truncated data, and process-crash-safe atomic writes.
3. Implement a read-only legacy adapter that can read the existing encrypted preferences without changing or deleting them.
4. Implement idempotent migration with copy -> read-back -> validate -> durable completion marker.
5. Add a clear UI/error path for migration failure; never silently show an empty vault if encrypted data exists but cannot be read.
6. Run unit tests, Android lint, debug build, and inspect merged manifest/backup behavior.
7. Install as an upgrade over a test build containing seeded legacy encrypted profiles; verify every profile and selected profile survive. Test app restart and interrupted migration.
8. Only after successful upgrade testing, consider retiring the legacy reader in a later release.

## Current decision

Audit complete; implementation intentionally deferred. The next safe coding step is to add isolated tests and design the new storage abstraction while preserving the existing production read path. No production storage code has been modified by this audit.

## Follow-up finding: profile identifier width

A source inspection found that newly imported profile IDs were generated with
`UUID.randomUUID().toString().take(8)`, truncating a UUID to an eight-character
identifier. This unnecessarily reduces the identifier space and is unsuitable
as the long-term key namespace for per-profile metadata.

- New profiles now use the complete canonical UUID string.
- Existing persisted profile IDs are not rewritten, renamed, or migrated.
- The existing encrypted preference keys remain addressable because their IDs
  are treated as opaque strings.
- A focused unit test checks canonical full-UUID shape. CI execution remains
  unverified; source changes are not a claim that tests passed.

This is a compatibility-preserving prerequisite only. It does not yet add
metadata persistence, alter WireGuard config records, or enable profile pinning.
