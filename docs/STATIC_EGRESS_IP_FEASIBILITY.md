# R&MA Static Egress IP / Fixed-IP Mode Feasibility

Date: 2026-11-13
Branch: `rebuild/rma-android-structure`

## Executive decision

A fixed public egress IP is technically feasible, but it is primarily a **gateway infrastructure and service-operations feature**, not an Android-only feature. R&MA today is a VPN client; a fixed-IP service requires one or more VPN gateways with provider-assigned static public IPv4 addresses, routing/NAT, monitoring, capacity management, abuse controls, and a secure provisioning path.

A static IP can make a user's observed source IP stable. It does **not** guarantee exchange login acceptance, account safety, a residential reputation, or permission to access a service from a restricted jurisdiction.

## Critical compliance boundary

- Never market or implement fixed IP as a way to evade an exchange's country, residency, sanctions, or VPN restrictions.
- Binance's current terms surfaced during this review explicitly say users must not use a VPN to modify their IP to circumvent jurisdiction restrictions. The applicable terms depend on the user's legal entity, residence, and region; users must verify the terms that apply to them.
- Binance separately documents IP allowlisting for API keys and recommends trusted-IP restrictions. That is a distinct security control for API traffic and does not mean that a VPN IP guarantees browser login approval.
- R&MA must not promise that Binance or any other exchange will accept an IP. Users should confirm that their access method and jurisdiction are permitted by the exchange.

## Product model options

### Option A — Shared static egress IP
Multiple R&MA users exit through the same provider-assigned static IP.

Pros: lowest cost and simplest first deployment.
Cons: shared reputation, correlated rate limits/abuse reports, users can affect one another, and the IP may be recognized as a datacenter/VPN address.

### Option B — Dedicated static egress IP per customer
Each customer or organization receives a separately assigned static public IP (or a dedicated gateway/IP allocation).

Pros: isolation, stable address for the customer's own allowlists, less cross-customer reputation coupling.
Cons: materially higher IPv4 and gateway costs, more provisioning/support, and still no guarantee of exchange acceptance.

### Option C — Customer-owned gateway
The customer deploys R&MA-compatible gateway infrastructure in an account they control and retains the provider relationship and IP allocation.

Pros: strongest ownership/control boundary and direct control over provider/account.
Cons: more difficult setup and support; requires a deployment guide and secure enrollment.

**Recommendation:** design the protocol and UI to support both shared-static and dedicated/customer-owned gateways, but do not launch a public shared pool until the operational, abuse, privacy, and compliance model is defined. For trading-oriented users, dedicated/customer-owned is the more defensible option where permitted.

## Required architecture

1. **Gateway layer (new infrastructure):** WireGuard gateways on cloud/VPS providers with explicitly reserved static public IPv4 addresses. Document provider, region, ASN/datacenter classification, ownership, renewal, and replacement procedure. A static address must be reserved at the provider; merely keeping a server running is not a sufficient guarantee.
2. **Stable egress:** gateway performs IPv4 forwarding and source NAT through the reserved address. Define IPv6 explicitly: either provide stable IPv6 egress too or disable/block IPv6 through the tunnel to avoid accidental alternate egress. Route DNS through a controlled resolver and prevent DNS leakage.
3. **Client profile:** each profile carries a gateway identity and endpoint; do not embed server private keys or customer secrets in the Android app. Provision client keys securely and revoke/rotate them server-side.
4. **Fixed-IP mode:** pin the selected gateway. Disable automatic cross-region/server switching while fixed-IP mode is active. If the gateway fails, fail closed or show a clear disconnected state; never silently fall back to a different public IP.
5. **Redundancy:** if a secondary gateway is offered, disclose that it has a different IP and require the customer to allowlist both addresses before enabling failover. Do not claim seamless fixed-IP failover if the egress IP changes.
6. **Status and diagnostics:** show the expected egress IP, observed public egress IP, gateway region/provider label, last verification time, and a mismatch warning. Never infer or claim exchange acceptance.
7. **Operations/security:** gateway health checks, patching, capacity limits, DDoS/abuse handling, key revocation, access logs minimization, privacy policy, incident response, and cost monitoring are launch requirements.

## R&MA Android implementation sequence

Phase 0 — Product and compliance decisions:
- Define target customer regions and permitted use cases.
- Review the exact terms of each target exchange and local law.
- Decide shared vs dedicated vs customer-owned gateway and who pays for static IPv4/gateway operation.
- Define retention/privacy and support policies.

Phase 1 — Infrastructure proof of concept (outside Android):
- Provision one test gateway with a reserved static IPv4 address in a permitted region.
- Configure WireGuard forwarding/NAT, DNS, IPv6 policy, firewall, and monitoring.
- Verify egress IP stability across client reconnects and gateway process restarts.
- Verify that a deliberate gateway failure does not silently route traffic outside the tunnel.
- Verify DNS and IPv6 leak behavior.

Phase 2 — Client support:
- Add a gateway metadata model (gateway ID, display region, expected egress IP, fixed-IP capability, optional secondary IP).
- Add a fixed-IP/pinned-gateway policy to the connection coordinator.
- Suppress automatic server switching while pinned.
- On unexpected egress-IP mismatch or tunnel loss, disconnect/fail closed and explain the reason.
- Keep the current WireGuard-only stable path working; do not couple this to the OpenVPN 3 work.

Phase 3 — Controlled pilot:
- Test on multiple Android versions and networks.
- Verify public IPv4/IPv6/DNS from the device, reconnect behavior, network changes, and failure handling.
- Test with non-sensitive IP-check endpoints first. Do not use real trading credentials or API keys for acceptance testing.
- Only then consider a limited pilot with consenting users in permitted regions.

## Acceptance criteria

- The observed public IPv4 is the reserved gateway IP on every successful full-tunnel check.
- DNS requests use the configured resolver; no DNS leak is observed.
- IPv6 is either tunneled with a stable egress or blocked while the VPN is active.
- If the tunnel drops or the gateway is unavailable, traffic does not silently escape over the ordinary network (where Android platform behavior permits, use Android's always-on/lockdown controls and clearly guide the user).
- Fixed-IP mode never silently changes to a different gateway/IP.
- The app clearly distinguishes "tunnel connected", "expected egress IP verified", and "service/exchange access permitted"; it must not conflate these states.

## Current project status

This document is a feasibility/design decision only. No gateway infrastructure, static IP subscription, backend provisioning service, fixed-IP client mode, or exchange-specific integration has been deployed. The existing R&MA APK remains a WireGuard client and does not itself provide a static public IP.
