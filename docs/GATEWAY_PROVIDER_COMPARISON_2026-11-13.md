# R&MA Gateway Provider Comparison

Review date: 2026-11-13
Branch: `rebuild/rma-android-structure`
Scope: one customer-owned WireGuard proof-of-concept gateway; no purchase or deployment authorized.

## Executive recommendation

**Provisional first choice: Hetzner Cloud in Germany or Finland, only if that region is lawful and suitable for the customer's intended connectivity.** The published CX23 price is €5.49/month excluding VAT, and the Primary IPv4 price is €0.50/month excluding VAT, for an estimated baseline of **€5.99/month excluding VAT**. Hetzner documents 20 TB included outgoing traffic for EU CX Cloud servers and publishes a WireGuard application image. This is the clearest low-cost path in the reviewed material.

This is not a final region recommendation. The destination service's terms, the customer's legal location, latency, provider account eligibility, and actual network performance must be checked first. A VPN or fixed IP must not be used to bypass geographic, residency, sanctions, or service restrictions.

## Comparison

| Provider / region | VM baseline | Static IPv4 | Transfer | Assessment |
|---|---:|---:|---:|---|
| Hetzner EU (Germany/Finland), CX23 | €5.49/mo | €0.50/mo | 20 TB outgoing included for EU CX | Approx. €5.99/mo before VAT; best documented low-cost candidate |
| Hetzner US, CPX11 | $17.49/mo | $0.60/mo | US traffic varies by plan (1–5 TB) | Approx. $18.09/mo before tax; materially higher than EU |
| DigitalOcean, Basic 1 GiB | $6/mo | Reserved IP free while attached to Droplet | 1,000 GiB included | Simple pricing and movable reserved IP within same datacenter; AUP prohibits open proxies, so obtain written confirmation for intended private VPN use before provisioning |
| Vultr | Exact suitable VM price not verified in this review | $0.004/hour, about $3/mo | Exact plan allowance not verified here | Reserved IP is persistent/reassignable within same region; full monthly gateway cost remains unverified |

## Provider-specific findings

### Hetzner
- The 15 June 2026 price table lists CX23 in Germany/Finland at €5.49/month excluding IPv4 and VAT.
- Primary IPv4 is €0.50/month excluding VAT.
- Hetzner's traffic documentation lists 20 TB outgoing traffic for EU CX Cloud servers; US allowances vary from 1–5 TB depending on plan.
- Hetzner publishes a WireGuard app image and setup instructions.
- The cloud service agreement prohibits fake source IPs and other network abuse. The published WireGuard app is evidence of a supported self-hosted VPN use case, not permission for prohibited activity.
- Primary IP resources can be managed independently of servers, but the exact reassignment procedure and operational behavior must be confirmed in the console before relying on it as a recovery plan.

### DigitalOcean
- Basic 1 GiB / 1 vCPU / 25 GiB SSD plan: $6/month, with 1,000 GiB transfer.
- Reserved IPs are free while attached to a Droplet; an unattached reserved IPv4 is $5/month.
- Reserved IPs can be reassigned between Droplets in the same datacenter; they are region/datacenter-specific.
- Its current AUP prohibits operating open proxies and similar network services. It does not clearly resolve whether a private, single-customer WireGuard gateway is treated as an open proxy. Obtain written support clarification before selecting it; do not assume it is allowed.

### Vultr
- Reserved IP price: $0.004/hour (approximately $3/month).
- Reserved IP can be attached to an instance in the same location; only one instance can use it at a time.
- Vultr documentation says a normal instance IP remains assigned only while that instance exists; a Reserved IP is the transferable/persistent option.
- The exact VM tier, included transfer, full monthly total, and the provider's interpretation of the proposed private VPN use remain unverified. Do not select it until those are checked against official current documentation or support.

## Decision and risk notes

1. Do not compare only the advertised VM price. The total includes VM, reserved IPv4, VAT/tax, transfer overages, backups/snapshots if enabled, and any support or management charges.
2. The cheapest candidate is not automatically the correct region. Legal permission and the destination service's applicable terms take priority.
3. Provider-reserved IP is not a residential IP and does not guarantee any service will accept it.
4. A stable IP does not provide high availability. If the gateway fails, the first prototype must fail closed rather than silently switch to a different egress IP.
5. Bandwidth estimates per user cannot be responsibly calculated until the intended monthly usage and number of devices are known.
6. No provider account was created, no payment method was entered, no server/IP was provisioned, and no code or protected `main` branch was changed.

## Next technical gate

Before provisioning, confirm the intended lawful region and use case, then obtain written provider clarification where policy wording is ambiguous. If the customer approves a Hetzner EU trial, verify current availability, CX23 + Primary IPv4 checkout total including VAT, the exact Primary IP detach/reassign behavior, and the account's traffic limit in the live console. Only then prepare a deployment checklist; do not create resources without explicit purchase approval.

## Official sources

- Hetzner 15 June 2026 price adjustment: https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/
- Hetzner Primary IP overview and pricing: https://docs.hetzner.com/cloud/servers/primary-ips/overview/
- Hetzner cloud traffic allowances: https://docs.hetzner.com/robot/general/traffic/
- Hetzner WireGuard app: https://docs.hetzner.com/cloud/apps/list/wireguard/
- Hetzner Cloud and vServer service agreement: https://www.hetzner.com/legal/cloud-server/
- DigitalOcean Droplet pricing: https://www.digitalocean.com/pricing/droplets
- DigitalOcean Reserved IP details: https://docs.digitalocean.com/products/networking/reserved-ips/details/
- DigitalOcean Reserved IP pricing: https://docs.digitalocean.com/products/networking/reserved-ips/details/pricing/
- DigitalOcean Acceptable Use Policy: https://www.digitalocean.com/legal/acceptable-use-policy
- Vultr Reserved IP pricing: https://docs.vultr.com/support/products/network/are-reserved-ips-free
- Vultr Reserved IP attachment: https://docs.vultr.com/products/compute/instances/cloud-compute/networking/reserved-ips
- Vultr static vs dedicated IP explanation: https://docs.vultr.com/support/products/compute/how-do-i-get-a-vultr-compute-instance-dedicated-ip-address
