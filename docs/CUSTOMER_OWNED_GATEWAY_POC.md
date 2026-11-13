# R&MA Customer-Owned WireGuard Gateway — Proof-of-Concept Runbook

Date: 2026-11-13
Branch: `rebuild/rma-android-structure`
Status: Design/runbook only. No server has been provisioned and no provider account or paid resource has been created.

## 1. Goal and ownership boundary

Build a small, single-customer proof of concept in infrastructure controlled by that customer. The customer owns the VPS/cloud account, reserved public IPv4 allocation, billing relationship, and gateway administration. R&MA acts only as the Android WireGuard client.

This design is for lawful, authorized remote connectivity and stable source-IP allowlisting where the destination service and the user's jurisdiction permit it. A stable IP does not override a service's geographic, residency, sanctions, or VPN rules and does not guarantee acceptance by any exchange.

No exchange credentials, API secrets, or trading account data are needed for this proof of concept.

## 2. Minimum deployment shape

- One supported Linux LTS VPS, initially one region and one customer.
- One provider-reserved static public IPv4 address, explicitly confirmed as retained/reassignable under the provider's current product terms.
- One WireGuard interface and one Android client peer for the first test.
- IPv4 forwarding and source NAT (SNAT/masquerade) from the WireGuard subnet to the provider-facing interface.
- DNS resolver reachable only through the tunnel for the client profile.
- Explicit IPv6 fail-closed policy for the first IPv4-only trial.
- Firewall default-deny inbound policy; expose only the chosen WireGuard UDP port and tightly restricted administration access.
- Monitoring for host reachability, WireGuard service health, disk/CPU/memory, and traffic volume.

The gateway must not depend on an IP address that can silently change when a VM is stopped, rebuilt, or migrated. The reserved-IP attachment and reassignment behavior must be verified with the selected provider before deployment.

## 3. Key and profile handling

1. Generate the server private key on the gateway itself.
2. Generate the Android client's private key on the Android device (or another trusted client-side environment).
3. Exchange only public keys during peer enrollment.
4. Keep the server private key on the gateway. Never place it in the Android app, Git repository, CI variables, screenshots, logs, or support messages.
5. Keep the Android private key inside Android app secure storage; never ask the user to paste it into chat.
6. Assign a unique tunnel address to each peer. Record peer identity and revocation state without retaining unnecessary personal data.
7. If a client is lost, revoke its public key on the gateway and issue a new peer. Do not reuse a compromised key.

The first prototype should use manually provisioned configuration. Do not build a public provisioning API until authentication, authorization, key lifecycle, abuse controls, and privacy retention are designed.

## 4. Network policy

### IPv4
- Enable kernel IPv4 forwarding.
- Permit forwarding only between the WireGuard interface and the intended egress interface, with stateful return traffic.
- Apply source NAT so outbound traffic exits with the reserved public IPv4.
- Restrict peer-to-peer traffic between client peers unless a documented use case requires it.
- Set an appropriate MTU and verify behavior on mobile networks.

### DNS
- Advertise a resolver address reachable through the tunnel.
- Ensure the resolver's upstream traffic exits through the gateway.
- Do not advertise a public resolver that causes client DNS to bypass the tunnel.
- Validate DNS resolution and leak behavior from the Android device, not only from the server.

### IPv6 — first prototype
The first trial is IPv4-egress-only. The Android profile must route IPv6 into the VPN interface as well, while the gateway firewall deliberately drops forwarded IPv6. This prevents the phone from silently using its ordinary network for IPv6 when Android's VPN/lockdown behavior is correctly configured.

Before claiming leak protection, test on each supported Android version and mobile/Wi-Fi network. If the device or profile cannot reliably route IPv6 into the VPN, do not enable the fixed-IP mode on that configuration. A later dual-stack design may provide stable IPv6 egress instead.

## 5. Failure and fixed-IP behavior

- Pin the profile to one gateway identity and endpoint while fixed-IP mode is enabled.
- Do not automatically switch to another region, server, or public IP.
- If the gateway is unreachable, show a disconnected/error state. Never silently fall back to the ordinary network or a different VPN endpoint.
- Android's Always-on VPN and “Block connections without VPN” (lockdown) should be evaluated and clearly guided where available; they are device-level safeguards, not a substitute for testing.
- If a second gateway is introduced later, disclose its different egress IP and require the customer to allowlist it before any failover can be enabled.

The UI must keep three facts separate:
1. WireGuard tunnel is connected.
2. Observed public egress IP matches the expected reserved IP.
3. The destination service permits this user's access method and jurisdiction.

Only item 2 can be verified by R&MA's network check. R&MA must not claim item 3.

## 6. Proof-of-concept acceptance tests

Do not use real exchange accounts, API keys, passwords, or trading credentials.

| Test | Pass condition |
|---|---|
| Reserved IP ownership | Provider dashboard/API confirms the reserved IPv4 is attached to the gateway and its retention behavior is understood |
| First connection | Android connects to the intended WireGuard peer without repeated manual toggles |
| Egress identity | A reputable public IP-check endpoint reports the reserved IPv4 |
| Reconnect stability | Egress IPv4 remains identical after disconnect/reconnect and WireGuard service restart |
| DNS path | DNS queries resolve and the observed resolver path is through the gateway; no direct-network DNS leak is detected |
| IPv6 policy | IPv6 is routed into the VPN and blocked at the gateway for this IPv4-only prototype; no ordinary-network IPv6 egress is observed |
| Gateway outage | Stopping the gateway causes a clear failure; traffic does not silently use the ordinary network when lockdown is enabled |
| Key revocation | Removing the test peer prevents that client from reconnecting |
| Network changes | Wi-Fi/mobile transitions do not silently change the expected egress IP |
| Resource health | CPU, memory, disk, bandwidth, and service health remain within the selected plan's limits |

Record test date, Android version, network type, gateway software versions, and pass/fail evidence. Do not record private keys or full user traffic.

## 7. Operational safeguards before any pilot

- Keep the operating system and WireGuard packages patched.
- Restrict SSH/admin access to trusted source addresses or a separate management path; use key-based administration.
- Use provider firewall plus host firewall, and verify rules after reboot.
- Apply peer-level address allocation and rate/abuse monitoring.
- Define a contact and process for abuse reports, compromise, key revocation, and gateway replacement.
- Minimize logs; do not capture browsing contents or DNS query histories unless strictly necessary and transparently disclosed.
- Monitor provider bandwidth quotas, overage pricing, reserved-IP charges, and regional restrictions.
- Back up configuration securely without copying private keys into the app or repository.
- Document recovery and IP replacement. If the provider changes the public IP, treat it as a new allowlist entry and require explicit customer approval.

## 8. R&MA client changes — deliberately deferred

No client code should be changed until the external gateway passes the acceptance tests above. Once a working test gateway exists, the client-side work can be scoped narrowly:

1. Add gateway metadata: stable gateway ID, endpoint, display label, expected egress IPv4, and fixed-IP capability.
2. Add a pinned-gateway policy to the existing serialized connection coordinator.
3. Disable automatic profile switching only while a fixed-IP profile is pinned.
4. Add an explicit “verify egress IP” result with timestamp and mismatch warning.
5. On mismatch or gateway loss, report failure and do not select another gateway automatically.
6. Preserve the current WireGuard path for ordinary profiles and add regression tests for rapid switching, cancellation, reconnect, and process recreation.

Do not add exchange-specific logic or claim exchange acceptance.

## 9. Decisions required before provisioning

These are intentionally not guessed:
- Which cloud/VPS provider and region the customer will use.
- Whether the provider offers a genuinely reserved/reassignable IPv4 in that region.
- Which Linux LTS image and supported WireGuard package version will be used.
- The expected number of simultaneous devices and monthly data volume.
- The customer's local legal requirements and the destination service's applicable terms.

No purchase, subscription, server deployment, DNS/domain registration, or change to the protected `main` branch is authorized by this document.
