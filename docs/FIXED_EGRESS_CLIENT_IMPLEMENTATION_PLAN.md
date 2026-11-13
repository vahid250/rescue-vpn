# R&MA Fixed-Egress Client Implementation Plan

- Date: 2026-11-13
- Branch: `rebuild/rma-android-structure`
- Parent decision: `docs/ADR_CUSTOMER_OWNED_FIXED_EGRESS.md`
- Status: Planning; no Android fixed-egress code has been changed by this plan.

## Current code observations

- `WireGuardProfile` currently represents parsed WireGuard configuration and exposes endpoint, AllowedIPs, DNS, and parsed config. It has no customer-owned gateway identity or fixed-egress policy.
- `MainViewModel` selects a profile and immediately applies it when the tunnel is connected/connecting. That behavior must be guarded by a future pinned-profile policy so a fixed-egress profile cannot be silently replaced.
- `SecureConfigStore` stores raw WireGuard configuration and profile names in EncryptedSharedPreferences. It currently has no versioned profile-metadata record.
- The dashboard distinguishes tunnel state and handshake freshness, but does not verify the public egress IP.
- The import flow accepts ordinary WireGuard .conf files. This path should remain backward-compatible.

## Implementation sequence

### Step 1 — Model and persistence (no networking changes)

- Add a versioned, non-secret profile metadata model separate from the raw WireGuard config.
- Initial fields: profile ID, gateway ownership classification (ordinary third-party vs customer-owned), fixed-egress enabled flag, optional expected public IPv4, and optional stable gateway label.
- Do not store provider passwords, cloud API tokens, server private keys, or any secret beyond the existing client WireGuard config.
- Preserve old profiles by treating absent metadata as ordinary/unpinned profiles.
- Add explicit metadata validation: IPv4 must be a valid public IPv4 address; reject loopback, private, link-local, multicast, unspecified, and malformed values.
- Add unit tests for missing/legacy metadata, valid public IPv4, invalid/private IPv4, and metadata round-trip.

### Step 2 — Pinned profile policy

- Introduce a small pure policy component that decides whether profile selection can replace the active profile.
- Ordinary profiles retain current behavior.
- When a fixed-egress profile is active, selection changes must not silently apply a different profile. Require an explicit user action to stop/unpin and switch.
- Keep service-level command serialization as the final protection against overlapping tunnel commands.
- Add tests for ordinary switching, pinned profile selection, explicit unpin, and reconnect to the same pinned profile.

### Step 3 — Egress verification (read-only)

- Add a small repository/service that requests the observed public IPv4 from a configurable, documented HTTPS endpoint.
- Run the request only after WireGuard reports connected and only through the active VPN network.
- Use strict timeouts, response-size limits, TLS validation, and a narrow IPv4 response parser.
- Never send or log WireGuard keys, config contents, account credentials, or unnecessary device identifiers.
- Display three distinct states: tunnel connected, egress IP verified/mismatch/unavailable, and destination-service permission (not knowable by the app).
- Do not call an IP “verified” until the observed public IPv4 exactly matches the user's expected value.
- Add tests with mocked responses; no exchange sites or authenticated services are used.

### Step 4 — DNS and IPv6 safety

- Document the IPv4-only gateway profile requirements.
- Test that IPv6 traffic is routed into the VPN and blocked at the gateway, or implement and validate a dual-stack stable-egress design.
- Test DNS resolution and leakage on mobile data and Wi-Fi.
- Do not claim fail-closed or leak protection based only on configuration inspection.

### Step 5 — UI and guided setup

- Add an optional fixed-egress setup section that explains customer ownership and third-party billing before the user enables it.
- Allow the user to mark a profile as customer-owned and optionally enter the expected public IPv4.
- Make clear that importing a WireGuard profile does not itself create or reserve an IP.
- Provide a guided manual VPS/WireGuard setup document; no in-app VPS purchase or cloud API integration.
- Preserve the existing import/connect flow for all current profiles.

## Release gates

- All existing WireGuard config tests and connection behavior remain green.
- Legacy profiles load without migration prompts or data loss.
- Fixed-egress profile pinning is enforced in both UI/ViewModel policy and tested service command flows.
- Egress verification is tied to the active VPN network, not the device's ordinary network.
- IPv6 and DNS leakage tests pass on supported Android versions and representative mobile/Wi-Fi networks.
- Tunnel connection is never presented as proof that a third-party service accepts the user's access method.
- No VPS, reserved IP, backend, or paid resource is created by R&MA or its developer.

## Safety constraints

- Keep all work on `rebuild/rma-android-structure`; do not modify `main`.
- Never ask the user to share a real WireGuard PrivateKey.
- Never use real exchange credentials or API keys in acceptance tests.
- Do not silently change gateway/IP while fixed-egress mode is enabled.
