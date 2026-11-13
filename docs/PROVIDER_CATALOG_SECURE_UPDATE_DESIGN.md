# R&MA Provider Catalog — Secure Update Design

## Current state

The app embeds a small, curated provider catalog in the APK. Search works offline. This is intentional: no third-party provider response, remote JSON, server address, shell command, or WireGuard configuration is downloaded or executed by the finder.

## Why automatic updates are not enabled yet

HTTPS protects transport, but a remote catalog alone is not sufficient. If the repository, hosting account, or update path is compromised, unsigned data could replace official links, misstate costs, or direct users to malicious pages. A future update mechanism must authenticate catalog contents independently of transport.

## Required design before implementation

1. **Detached signature:** publish a compact JSON catalog and a detached RSA/SHA-256 signature. The app embeds only the public key. The signing private key must be generated and held offline or in a dedicated signing system; it must never be committed to GitHub or included in an APK.
2. **Key custody decision:** do not create or embed a production key until its owner, backup, rotation, and compromise-recovery procedure are agreed. Public-key rotation requires a trusted app update or a separately designed cross-signing process.
3. **Strict transport:** HTTPS only; fixed catalog host/path; no redirects to arbitrary hosts; short connection/read timeouts; response size cap (64 KiB); no cleartext fallback.
4. **Strict schema:** require a supported schema version, monotonically increasing catalog version, issue/expiry timestamps, and a bounded number of entries. Reject unknown critical fields, duplicate IDs, malformed URLs, control characters, and oversized strings.
5. **URL policy:** allow only HTTPS links on explicitly approved provider documentation domains and the project's own documentation host. Never allow file:, intent:, javascript:, custom schemes, IP-literal hosts, or arbitrary redirect destinations.
6. **No executable payload:** catalog data may contain display text and approved documentation links only. It must never contain shell commands, scripts, VPN configuration, private keys, credentials, provisioning instructions interpreted as code, or app update binaries.
7. **Signature before parse/use:** verify the exact downloaded bytes before trusting any field. Then parse and validate the schema and URLs.
8. **Rollback and expiry:** persist the highest accepted catalog version. Reject lower versions and expired catalogs. If refresh fails, retain the last-known-good verified catalog; if none exists, use the embedded catalog.
9. **Atomic cache:** write to a temporary file, flush, then atomically replace the previous verified cache. Never replace a good catalog with an invalid or partially downloaded one.
10. **Privacy:** no account IDs, device IDs, VPN profile names, public IPs, or usage telemetry are sent during catalog refresh. Use no user-specific query parameters.

## Release gates

- Agree who controls the signing key and how it is backed up.
- Add deterministic tests for valid signatures, modified bytes, wrong keys, expired catalogs, rollback, oversized payloads, malformed JSON, duplicate IDs, disallowed schemes/domains, and interrupted writes.
- Confirm behavior on the minimum supported Android API (26) before choosing a signature algorithm/provider.
- Add a user-visible last-updated timestamp and clearly label provider availability as unverified until checked on the provider's own site.
- Keep refresh manual or infrequent; do not poll continuously.

## Current implementation decision

Automatic remote refresh is **not enabled** in this release. The bundled offline catalog remains authoritative until the signing-key custody and verification design are implemented and tested. No private signing key has been generated, stored, or committed.