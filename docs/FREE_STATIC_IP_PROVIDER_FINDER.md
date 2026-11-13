# R&MA Free Static-IP Provider Finder

- Review date: 2026-11-14
- Branch: `rebuild/rma-android-structure`
- Status: First in-app curated directory implemented; no provider account or server is created.

## Product behavior

The first version adds a searchable, offline catalog inside R&MA. It filters a small set of provider entries already included in the app and opens each provider's official documentation. It does **not** crawl the web, scrape third-party lists, test account eligibility, create cloud accounts, or provision VMs. The catalog is updated with reviewed app releases; entries include a status and important limitations to avoid presenting a conditional offer as guaranteed free.

## Reviewed providers

### Oracle Cloud Always Free — conditional candidate

Official source: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm

Oracle documents two compute paths within the Always Free allowance: up to two VM.Standard.E2.1.Micro instances (AMD, 1 GB RAM and 1/8 OCPU each) or OCI Ampere A1 Flex resources (ARM, up to 2 OCPUs and 12 GB RAM total, subject to the free-tier allocation rules). The E2.1.Micro networking description includes one public IP address and up to 50 Mbps internet bandwidth. A1 networking bandwidth scales with allocated OCPUs. Both are possible personal WireGuard gateway candidates; select a Linux ARM64 image for A1 and verify the architecture of all server packages.

Important qualifications:
- The Always Free compute instances must be created in the account's home region.
- Oracle may report temporary host-capacity shortages.
- Oracle can reclaim idle Always Free compute instances when the published utilization conditions are met.
- Account verification may require a payment card/phone number for many users.
- Do not assume that an instance public IP is a separately reserved, transferable address. Verify the address allocation type and reassignment behavior in the OCI console before relying on it; do not promise that the address survives deletion/replacement of the VM.
- Confirm current traffic, regional availability, account eligibility, and all possible charges in Oracle's official console before creating resources.

### Google Cloud Free Tier — not a free static-IPv4 recommendation

Official source: https://cloud.google.com/vpc/pricing

Google documents a Free Tier VM offer, and separately supports reserving static external IPv4 addresses. However, its published pricing charges for static and ephemeral external IPv4 addresses in use on standard VM instances (currently listed as USD 0.005/hour). Therefore this is not listed as a free static-IPv4 option in the app.

## Safety and scope

- The finder is informational and searchable offline; it does not claim to discover every provider on the public internet.
- Provider offers and prices change. The displayed review date is not a live availability check.
- Never enter cloud passwords, payment details, API tokens, or WireGuard private keys into the finder.
- R&MA does not automatically sign up, purchase, provision, or configure a provider account.
- A server's public IP is not necessarily residential, dedicated to one user, or accepted by a destination service.
- Users must check local law, provider acceptable-use terms, traffic charges, and destination-service rules.
- No provider account, VPS, static/reserved IP, or paid resource was created for this work.

## Next gates

1. Verify the new screen compiles and existing unit tests remain green in GitHub Actions.
2. Add a versioned remote catalog only with a pinned signing key, signature verification, strict schema/size limits, HTTPS, provider-domain allowlisting, rollback protection, and a safe cached fallback. Do not fetch arbitrary remote JSON or execute provider-supplied instructions.
3. Before adding a setup wizard, complete the fixed-egress pinning, secure metadata persistence, egress verification, and DNS/IPv6 safety gates already defined in the project ADR.
