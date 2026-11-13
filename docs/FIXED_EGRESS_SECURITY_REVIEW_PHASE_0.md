# R&MA Fixed-Egress Security Review — Phase 0

- Date: 2026-11-14
- Branch: `rebuild/rma-android-structure`
- Scope: source-level review of the existing profile store, ViewModel, WireGuard service, tunnel state, manifest, and fixed-egress metadata.
- Status: source review plus one narrowly scoped production hardening change; full CI/device validation pending.
- Protected branch: `main` must remain untouched.

## Executive decision

Do not enable fixed-egress mode or add an egress-verification request to the current connection path yet. The current code has no end-to-end identity binding between a persisted profile, a service start command, the active tunnel, and a verified public egress result. Adding only UI metadata or an IP lookup would create a false sense of assurance.

A zero-risk guarantee is not technically supportable. The release objective is instead to identify and test failure modes, fail closed where policy requires it, preserve existing profiles, and make every state observable and truthful.

## Confirmed source observations

### 1. Profile storage

File: `app/src/main/java/com/rescuevpn/app/data/security/SecureConfigStore.kt`

- Existing profile data is stored in AndroidX EncryptedSharedPreferences, with AES-256-SIV key encryption and AES-256-GCM value encryption.
- Profile name and raw WireGuard configuration are stored as separate entries.
- Profile writes and deletion use asynchronous `apply()`; `clearAll()` uses `commit()`.
- The store has no metadata record or schema/version field for fixed-egress policy.
- The current implementation throws a SecurityException if encrypted preference initialization fails; it does not fall back to plaintext.
- The existing encrypted store is a live compatibility boundary. Do not replace, clear, or migrate it until a tested read-only legacy adapter and crash-safe migration exist.

### 2. Profile loading and selection

File: `app/src/main/java/com/rescuevpn/app/ui/viewmodel/MainViewModel.kt`

- Profiles are loaded from stored IDs and parsed from their raw configuration.
- Parse failures are silently caught and omitted from the in-memory list. This can make a damaged stored profile appear to have disappeared.
- Selecting a profile immediately saves its ID and, while connected/connecting, calls `connect()`.
- The service start intent currently carries only profile name and raw configuration. It does not carry the stable profile ID or any fixed-egress policy/version.
- Therefore a profile-specific pin cannot yet be enforced end-to-end.

### 3. WireGuard service and state semantics

Files:
- `app/src/main/java/com/rescuevpn/app/core/vpn/RescueWireGuardService.kt`
- `app/src/main/java/com/rescuevpn/app/core/vpn/TunnelCommandPolicy.kt`
- `app/src/main/java/com/rescuevpn/app/core/vpn/TunnelStateManager.kt`

- Service commands are serialized through one coroutine consumer; when queued commands are coalesced, only the latest queued intent is retained.
- The service marks the connection `CONNECTED` when WireGuard reports `Tunnel.State.UP`; this is a tunnel/interface state, not proof of public egress.
- RX/TX counters and latest handshake are collected separately. None proves that a public IP matches the expected address.
- The service currently has no fixed-egress profile ID, no egress-verification state, no active-VPN-network-bound HTTP verifier, and no DNS/IPv6 leak test result.
- Initial review found the service used `rawConfig.hashCode()`, a 32-bit non-cryptographic hash that can collide. This issue has now been addressed on this branch by computing an in-memory SHA-256 digest of the normalized configuration before the already-active check. A regression test uses the known Java String hash collision pair `Aa` / `BB`. The digest is only an equality aid, not authentication. CI execution is still unverified.

### 4. Fixed-egress metadata

File: `app/src/main/java/com/rescuevpn/app/data/model/FixedEgressMetadata.kt`

- The metadata model is versioned and explicitly says that a public-IPv4 syntax/range check does not prove assignment, reachability, or ownership.
- Its current validation is a local candidate check only.
- The model is not yet connected to profile persistence, selection, service commands, or UI.
- It must never be treated as proof that the gateway belongs to the user or that the expected address is actually emitted by the tunnel.

### 5. Android application boundary

File: `app/src/main/AndroidManifest.xml`

- The app requests INTERNET and ACCESS_NETWORK_STATE.
- The VPN service is declared non-exported.
- `allowBackup=false` is set, which is consistent with the current device-keystore-bound encrypted storage design.
- These manifest facts do not establish VPN-bound egress verification or leak protection.

## Risks that block implementation

| Risk | Why it matters | Required control |
|---|---|---|
| Profile identity is lost at service boundary | Service cannot prove which stored profile/policy is active | Carry immutable profile ID and policy version through a validated internal command |
| Configuration identity | Original 32-bit hash could collide; source change now uses in-memory SHA-256 over normalized config | Regression test added; CI/device validation still required; digest is not authentication |
| Tunnel UP shown as CONNECTED | UI may imply internet/IP success when only interface activation is known | Separate tunnel, handshake, egress, and leak-test states |
| Silent profile parse failures | Existing profiles may appear lost | Report unreadable/invalid records distinctly; never auto-delete |
| No persisted metadata | Restart may lose pinning policy or expected IP | Versioned encrypted metadata bound to profile ID |
| Immediate profile switching | A fixed-egress profile could be replaced without an explicit unpin flow | Pure policy gate in ViewModel plus service-level enforcement |
| Unbound egress request | Normal device network could be queried outside the VPN | Bind the verifier socket to the exact active VPN Network; reject if unavailable |
| DNS/IPv6 behavior unverified | IPv4 match alone does not prove no alternate-route leak | Dedicated DNS and IPv6 tests; no protection claim before test evidence |
| Migration interruption/key loss | Existing private keys could become inaccessible | Preserve legacy vault; idempotent copy/read-back/validate/marker migration |

## Required state model

Do not overload the existing `VpnConnectionState.CONNECTED`. Keep the tunnel state and egress state separate.

Minimum egress states:
- `NOT_REQUIRED` — ordinary WireGuard profile.
- `PENDING` — fixed-egress profile is up; verification has not completed.
- `VERIFIED` — observed public IPv4 exactly matches the configured expected IPv4 through the active VPN network.
- `MISMATCH` — observed address differs; fixed-egress policy must not be reported as satisfied.
- `UNAVAILABLE` — no trustworthy result could be obtained.
- `CHECK_FAILED` — verifier, TLS, response, timeout, or network binding failed.

A stale result must not remain `VERIFIED` after reconnect, profile change, network handover, or configuration update. Each result must be bound to profile ID, configuration digest, active tunnel generation, and verification time.

## Implementation gates, in order

1. [Source change added; CI pending] Use SHA-256 over normalized configuration for the in-memory active-config identity; regression tests cover a known 32-bit hash collision. Verify command serialization and start/stop/switch races on CI and device.
2. [Pure policy and unit tests added; not wired into production yet] A standalone profile-pinning gate now blocks a different profile while a pin is present, permits the same pinned profile, and permits a switch only when the caller explicitly confirms unpin. Before production use, the pin must come from validated encrypted metadata and the rule must be enforced in both ViewModel and service command handling.
3. Design encrypted, versioned profile metadata storage without changing the legacy raw-config read path. Test crash recovery and key loss before migration.
4. Add a verifier that uses an explicitly bound socket on the active VPN network, strict HTTPS/TLS validation, short timeouts, response-size limits, narrow parsing, and no secret/device data in requests or logs.
5. Bind each verification result to the exact profile/config/tunnel generation; invalidate it on every relevant transition.
6. Add DNS and IPv6 leak tests on both mobile data and Wi-Fi. Do not claim fail-closed protection based on AllowedIPs inspection alone.
7. Preserve standard WireGuard import, selection, and connection behavior. Test upgrade from a build containing existing encrypted profiles.
8. Run unit tests, lint, debug build, and device-level upgrade/network tests. A source review or empty CI status is not a passing test.

## Explicit non-goals

- No VPS creation, purchase, account registration, API-token collection, or paid resource allocation.
- No central developer-operated VPN gateway.
- No automatic fallback from a pinned fixed-egress profile to another gateway.
- No claim that a destination service, exchange, website, country, or eligibility system will accept a particular IP.
- No modification of `main`.

## Review conclusion

The collision-safe in-memory configuration identity and its regression tests, plus a standalone profile-pinning policy and unit tests, have been added on the development branch. Neither test suite has yet been executed by verified CI or on a device. The pinning policy is intentionally not wired into production until its source of truth is securely persisted and both the ViewModel and service enforce it. The next safe task is to design and test the encrypted metadata record and migration strategy without altering existing WireGuard profile records. Do not begin egress networking or storage migration until those foundations are reviewed. The existing encrypted store must remain intact throughout this work.
