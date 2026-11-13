# R&MA Static Egress IP — Provider Comparison

Review date: 2026-11-13
Branch: `rebuild/rma-android-structure`
Scope: infrastructure feasibility only; no provider account, VPS, IP, or paid service has been created.

## Decision summary

For the first controlled WireGuard gateway proof of concept, shortlist **Vultr Reserved IP** and **Hetzner Cloud Floating IP**. Keep DigitalOcean Reserved IP and OVHcloud Additional IP as alternatives pending region, billing, account eligibility, and network-behavior checks.

Do not choose by IP price alone. The selected address must remain under the account's control across gateway replacement, be assignable in the chosen region, and be usable as the source-NAT address for all client egress. Provider reservation does not itself configure Linux routing, NAT, IPv6, DNS, or fail-closed behavior.

## Provider comparison

| Provider feature | Publicly documented behavior | Published IP price found | Important constraints |
|---|---|---:|---|
| Vultr Reserved IP | Dedicated reserved IPv4/IPv6 can be created in a selected location and attached to an instance; reserved IPs can be moved to a replacement instance and persist independently of the instance. | USD 3/month (USD 0.004/hour) | One instance at a time; same selected location. |
| Hetzner Cloud Floating IP | Public IPv4/IPv6 can be assigned and reassigned to Cloud servers; useful for failover and server replacement. | IPv4 EUR 3/month; IPv6 EUR 1/month, excl. VAT | One server at a time; server must have a Primary IP of same address family and be in the same network zone. Requires OS network configuration. |
| DigitalOcean Reserved IP | Public static IPv4/IPv6 can be reassigned among Droplets and used for automated failover. | Free while assigned to a Droplet | Region/datacenter-specific; one Reserved IP per Droplet; default account quota is three; IPv4 Droplet anchor-IP requirements apply in some cases. |
| OVHcloud VPS Additional IP | Additional public IPs can be migrated between compatible OVHcloud services; VPS supports up to 16 additional IPs. | USD 2.39/IP/month shown on the VPS options page | Verify exact VPS plan/region compatibility and routing procedure before purchase. |

Prices and availability are snapshots of public provider documentation reviewed on 2026-11-13; they can change and may exclude tax, currency conversion, or regional differences. Confirm the final quote in the provider control panel before any purchase.

## Source links (official provider documentation)

- Vultr Reserved IP overview and failover: https://docs.vultr.com/product-overviews/reserved-ips
- Vultr Reserved IP provisioning and location selection: https://docs.vultr.com/products/network/reserved-ips/provisioning
- Vultr Reserved IP pricing: https://docs.vultr.com/support/products/network/are-reserved-ips-free
- Hetzner Floating IP overview and pricing: https://docs.hetzner.com/cloud/floating-ips/overview/
- Hetzner Floating IP network-zone and configuration FAQ: https://docs.hetzner.com/cloud/floating-ips/faq/
- DigitalOcean Reserved IP details and pricing: https://docs.digitalocean.com/products/networking/reserved-ips/details/
- DigitalOcean Reserved IP limits: https://docs.digitalocean.com/products/networking/reserved-ips/details/limits/
- OVHcloud VPS IP mobility: https://www.ovhcloud.com/en/vps/vps-ip/
- OVHcloud VPS options and Additional IP price: https://www.ovhcloud.com/en/vps/options/

## R&MA-specific evaluation

### Preferred first test: Vultr Reserved IP

Reasons:
- The provider explicitly documents reservation independent of the compute instance.
- The reserved address can be attached to a replacement instance in the same location.
- IPv4 and IPv6 are supported as reserved address types.
- The published IPv4 reservation price is straightforward to budget.

Before selecting it, test whether a reserved IP can be configured as the actual outbound source address for the WireGuard gateway using provider-supported networking. Verify source NAT, inbound endpoint reachability, reverse-path behavior, and address continuity after detaching and reattaching to a replacement instance.

### Strong alternative: Hetzner Floating IP

Reasons:
- Clear, low published IPv4 reservation price.
- Explicit reassignment between servers for failover.
- Network zones cover multiple European and US locations.

Constraints:
- The target server needs a Primary IP of the same address family.
- Floating IP and server must be in the same network zone.
- Linux-side address and route configuration is required; provider assignment alone is insufficient.

### Alternatives

DigitalOcean is attractive because an assigned Reserved IP is free and its documentation describes reassignment/failover. Its same-datacenter scope, account quota, and anchor-IP details must be included in deployment automation.

OVHcloud offers migratable Additional IPs and documents VPS support, but the exact plan/region and the operational steps for routing/NAT need a dedicated validation before it becomes the first POC.

## Proof-of-concept gate (no purchase authorized by this document)

For any candidate provider, the test plan must verify:

1. The IP is reserved in the provider account before client rollout.
2. A WireGuard client exits through that exact IPv4 on repeated reconnects.
3. The address remains unchanged after gateway process restart and VM replacement/reassignment.
4. IPv6 is either routed through a separately controlled stable egress or blocked while the VPN is active.
5. DNS uses the configured resolver and does not leak outside the tunnel.
6. Gateway failure does not silently fall back to ordinary device internet.
7. The app reports tunnel state and verified egress-IP state separately.
8. No exchange credentials or API keys are used during network acceptance testing.

## Product and compliance boundary

A fixed public IP is not a residential IP and does not guarantee acceptance by an exchange or other third party. R&MA must not advertise the feature as a way to evade jurisdiction, residency, sanctions, or VPN restrictions. Users must confirm that their access method is permitted under the applicable service terms and local law.

## Current status

This comparison is documentation only. No VPS, Reserved IP, Floating IP, backend provisioning service, or paid subscription has been created. The Android application remains a WireGuard client; the gateway and fixed-egress mode are not implemented.
