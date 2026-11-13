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

Source-level audit complete. The isolated metadata serialization layer has now been implemented and tested at source level; the next safe step is an authenticated encryption envelope with injected Keystore key material, followed by a separately reviewed storage adapter. The existing production profile read/write path remains unchanged.

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

## Follow-up implementation: strict metadata codec (serialization only)

Added `FixedEgressMetadataCodec` and focused unit tests on the development branch.

- Uses a fixed field order and an explicit record-format version.
- Encodes text fields as URL-safe Base64 without padding, avoiding delimiter ambiguity and supporting Unicode labels.
- Binds the serialized record to its profile ID and checks the caller-supplied ID during decode.
- Rejects unknown/missing/duplicate fields, unsupported format/schema versions, malformed UTF-8/Base64, oversized records, invalid enum values, and metadata that fails the model's semantic validation.
- Keeps the codec independent of Android APIs and external serialization dependencies, so it can be unit-tested as plain Kotlin.
- The codec is **not encryption, authentication, or tamper protection**. Its output must not be persisted as plaintext. The future encrypted-record layer must authenticate the profile ID and record format/schema as associated data (AAD).

No production storage writes, reads, migration markers, or service/ViewModel pinning behavior have been changed. Existing WireGuard profile records remain untouched. CI and Android build/test execution are still unverified.

## Follow-up implementation: authenticated AES-GCM envelope (no storage integration)

Added `EncryptedMetadataRecordCodec` with an injected `SecretKey` and focused tests.

- Uses the standard JCA transformation `AES/GCM/NoPadding`, a fresh 96-bit nonce from `SecureRandom`, and a 128-bit authentication tag.
- Authenticated additional data (AAD) binds a domain separator, envelope format version, and exact profile ID. Moving a record to another profile therefore fails authentication.
- Rejects unsupported envelope versions, non-canonical Base64, invalid nonce/ciphertext lengths, oversized envelopes, malformed fields, and authentication failures.
- Does not generate or persist keys. Production integration must supply a non-exportable Android Keystore AES key; tests use an ephemeral in-memory test key only.
- Does not write files, change SharedPreferences, or alter existing WireGuard records. It is an isolated cryptographic codec, not a complete storage/migration system.

Before production use, separately review Keystore key creation/alias policy, invalidation/recovery behavior, atomic encrypted-file persistence, crash recovery, and migration. CI execution and Android runtime compatibility remain unverified.


## Follow-up implementation: dedicated Android Keystore key provider

Added a separate MetadataKeyProvider contract and an Android implementation for the fixed-egress metadata AES key.

- The key is created lazily, only when a caller requests it; app startup and ordinary WireGuard profile operations do not create this key.
- Uses the Android Keystore provider with AES-256, GCM mode, no padding, and randomized encryption required.
- The alias is dedicated to the R&MA fixed-egress metadata format version 1; it is not derived from a profile ID and is not exposed to callers.
- The key is returned as a SecretKey handle and is never exported as raw key bytes.
- If the alias already exists but its key is unavailable, invalidated, or has an unexpected type, the provider fails closed. It does not silently replace the key, because doing so would strand ciphertext encrypted under the previous key.
- The provider is not yet called by SecureConfigStore, MainViewModel, or the VPN service. No metadata record is persisted, no profile pinning is enforced, and existing WireGuard configuration storage remains unchanged.

Validation limitation: these source files were committed and read back from the development branch, but no Android build, unit test, instrumentation test, or physical-device Keystore test has been run in this environment. In particular, hardware-backed key availability is not claimed.

## Next gated step

Add an app-private atomic encrypted-record store that composes the metadata codec with this key provider. Before connecting it to the UI or tunnel service, add instrumentation coverage for first-use key creation, reuse of an existing key, wrong-profile AAD rejection, ciphertext tampering, and key invalidation/unavailability. Preserve the current encrypted WireGuard store and do not start migration until crash recovery and read-back validation are demonstrated.


## Follow-up implementation: atomic encrypted metadata record store

Added a storage layer dedicated to fixed-egress metadata, separate from the existing WireGuard profile vault.

- `AndroidAtomicRecordFileFactory` stores records under the app's `noBackupFilesDir/fixed_egress_metadata` directory. The filename is a SHA-256 digest of the profile ID; raw profile IDs are not used as filenames.
- Each record contains only the UTF-8 AES-GCM envelope. The serialized plaintext metadata is never written to disk.
- Android `AtomicFile` is used for replace/write recovery. A failed write calls `failWrite`, preserving the prior complete file where the platform atomic-file contract permits.
- Every save reads the record back and authenticates/decrypts it before returning success.
- Reads of an absent record do not create a key. If ciphertext exists but the Keystore alias is absent/unavailable, load and overwrite fail closed; they do not generate a replacement key.
- The Keystore provider serializes alias access to reduce concurrent first-use key-generation races.
- Store operations are synchronized within the store instance. The Android file factory fixes the location to app-private no-backup storage; callers cannot select external storage through this constructor.
- A storage abstraction permits deterministic unit tests of save/read-back, missing-key behavior, profile binding, delete isolation, and simulated interrupted writes.

This is still an isolated metadata store. It is not wired into the UI, `SecureConfigStore`, profile import/deletion, `MainViewModel`, or the VPN service. It does not migrate or modify existing WireGuard profiles. Android `AtomicFile` behavior and Android Keystore behavior still require instrumentation tests on supported Android devices; local unit tests and CI have not been executed in this environment.

## Localization requirement recorded

The product requirement is to retain Persian and add at least ten widely used languages. The initial proposed language set for a later localization phase is English, Arabic, Turkish, Urdu, Hindi, Simplified Chinese, Spanish, French, German, Russian, and Portuguese. This is a planning baseline, not a claim that translations have been completed.

Before implementation, perform a complete UI string inventory (including Compose hard-coded text, notifications, errors, accessibility labels, and import/export messages), move user-visible text into Android string resources, and establish locale fallback and RTL layout tests. Then add professionally reviewed translations, an in-app language choice where appropriate, plural/number/date formatting, and screenshot/regression coverage. Arabic and Urdu require explicit RTL validation; Chinese requires Simplified Chinese locale handling. Localization must not alter VPN protocol, profile data, or security behavior.
