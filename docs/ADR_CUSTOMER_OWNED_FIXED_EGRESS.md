# ADR: Customer-Owned Fixed Egress for R&MA

- Date: 2026-11-13
- Status: Accepted by project owner
- Applies to: `rebuild/rma-android-structure`
- Supersedes: any earlier proposal that assumes R&MA will operate a shared or developer-funded fixed-egress gateway for the initial product.

## Decision

R&MA will remain independent of developer-operated central VPN infrastructure. The developer will not purchase, rent, operate, or subsidize VPS instances, public IP addresses, or a shared VPN gateway for this feature.

Fixed-egress capability is an optional, customer-owned deployment mode:

1. A customer who needs a stable public egress IP obtains and controls their own VPS/cloud account or another authorized gateway with a provider-confirmed stable public address.
2. The customer (or a trusted administrator acting for them) configures a WireGuard gateway on that infrastructure.
3. The customer imports the resulting client WireGuard profile into R&MA.
4. R&MA connects as a WireGuard client and, in fixed-egress mode, pins the selected profile/gateway. The application does not create or rent infrastructure and does not promise that a destination service will accept the address.

Existing ordinary WireGuard profiles remain supported. Users who do not need their own fixed IP can continue using compatible third-party profiles; the egress address in that case belongs to the selected provider/gateway and is not represented as customer-owned or exclusive.

## Cost and IP policy

- No infrastructure cost is incurred by the R&MA developer for the customer-owned mode.
- VPS, public IPv4, traffic, taxes, and optional reserved/floating-IP charges are paid directly by the customer to their chosen provider.
- A provider-assigned primary public IPv4 may be sufficient for the first prototype if the provider explicitly confirms that it remains stable for the lifetime of that instance.
- A separately reserved/floating IP is optional for the first prototype and should be considered only when address retention across VM replacement or reassignment is required.
- Never describe an IP as permanent without checking the provider's current product terms and lifecycle behavior.

## Security boundaries

- Server private keys remain on the customer's gateway. They must never be embedded in the APK, committed to Git, placed in CI secrets, or sent to support/chat.
- Client private keys should be generated and retained on the Android device using the strongest compatible secure-storage design; only public keys are exchanged with the gateway.
- The first release uses manual profile import or a locally executed, auditable setup guide. It does not store cloud-provider API tokens and does not provision infrastructure through a central control plane.
- Do not add a public provisioning API until authentication, authorization, key rotation/revocation, abuse controls, privacy, and operational responsibilities are separately reviewed.
- Fixed-egress mode must not silently fall back to a different gateway or ordinary network route after tunnel failure.
- Tunnel-connected state and verified public-egress-IP state must be displayed separately.

## Android implementation direction

1. Preserve the current WireGuard connection path and profile compatibility.
2. Add explicit gateway/profile metadata only where needed: customer-owned flag, stable gateway identifier, expected egress IPv4 (optional), and fixed-egress/pinned policy.
3. When pinned, suppress automatic profile/server switching. A manual switch requires the user to disable fixed-egress mode or explicitly confirm the new gateway and expected IP.
4. Add an opt-in egress verification step using a documented HTTPS IP-check endpoint. Treat the returned address as observed data, not proof of destination-service acceptance.
5. If the observed IPv4 differs from the configured expected address, show a mismatch and do not label the fixed IP as verified.
6. Define and test IPv6 behavior before release. The initial IPv4-only profile must route IPv6 into the VPN and block forwarded IPv6 at the gateway, or otherwise demonstrate an equally strong no-leak design. Do not claim leak protection without device/network tests.
7. Add a guided customer setup document and a validation checklist before any public launch.

## Non-goals for the first release

- No developer-owned shared VPN pool.
- No per-user automatic VPS purchasing or provisioning.
- No exchange-specific login integration, credentials, API keys, or guarantee of access.
- No automatic cross-region failover in fixed-egress mode; a replacement gateway may have a different IP and requires explicit user confirmation and allowlist updates where applicable.

## Compliance and acceptable-use boundary

This feature is for lawful, authorized connectivity and stable source-IP allowlisting where the destination service and the user's jurisdiction permit the access method. A static datacenter IP does not change residency, sanctions, or geographic eligibility and does not override a service's terms. R&MA must not be marketed or implemented as a method to bypass such restrictions.

## Acceptance criteria

- R&MA can import and connect to a customer-configured WireGuard gateway without any developer-operated server.
- The customer can identify who owns and bills for the gateway and public IP.
- Reconnects use the same pinned gateway.
- Successful egress verification reports the expected public IPv4; mismatch is visible and never silently accepted.
- Tunnel loss does not silently switch to another IP or ordinary network traffic; Android lockdown behavior is evaluated and documented.
- IPv6 and DNS leakage tests pass on the supported Android versions and network types before the mode is described as protected.
- Ordinary WireGuard profiles continue to work independently of this optional mode.

## Repository safety

All work for this decision and its implementation stays on `rebuild/rma-android-structure`. The `main` branch must remain untouched.
