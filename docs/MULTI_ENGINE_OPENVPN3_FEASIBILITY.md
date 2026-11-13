# R&MA Multi-Engine Architecture and OpenVPN 3 Feasibility

Date: 2026-11-13
Branch: `rebuild/rma-android-structure`

## Decision summary

OpenVPN 3 Core is a technically credible candidate for a second Android client engine. It is a C++ client library (not a server) and OpenVPN states it is used as the core of OpenVPN Connect on Android and other platforms. Its Android integration is not a drop-in Gradle dependency: the documented client API is C++, Android bindings use SWIG/JNI, and the Android VPN tunnel must be connected through a `TunBuilderBase` implementation and Android `VpnService`.

Official references reviewed:
- https://github.com/OpenVPN/openvpn3
- https://openvpn.github.io/openvpn3/
- https://github.com/OpenVPN/openvpn3/blob/master/LICENSE.md

## Feasibility findings

1. **Core/API**: OpenVPN 3 exposes `openvpn::ClientAPI::OpenVPNClient`; a client implementation supplies event/log callbacks, evaluates a profile, then runs `connect()` on a worker thread. The connection call remains active until the session ends.
2. **Android tunnel integration**: `TunBuilderBase` is the abstraction intended to connect the core to an OS VPN API. Android integration must correctly implement interface addresses, routes, DNS, MTU, file-descriptor lifecycle, pause/resume, and cancellation around `VpnService`.
3. **JNI boundary**: the repository includes SWIG interface material (`client/ovpncli.i`) and documents Java bindings. R&MA will need a narrow JNI layer rather than allowing UI code to call C++ directly.
4. **Profile handling**: OpenVPN 3 expects inline profile material for certificates/keys; profiles using external file references require a deliberate merge/import step. The future importer must handle credentials and certificates without logging or writing secrets in plaintext.
5. **Build complexity**: integration requires Android NDK/CMake builds and native libraries for each shipped ABI. The current app already packages four ABIs for WireGuard; the OpenVPN engine must be built and verified for each selected ABI and supported Android API level.
6. **Licensing**: upstream states OpenVPN 3 is available under AGPLv3 or MPL 2.0, with a special OpenSSL linking permission for older OpenSSL versions. Before distributing binaries, choose and comply with the applicable license path and preserve required notices/source obligations. This is a release gate, not a detail to postpone.
7. **No server implementation**: OpenVPN 3 Core is a client library; it does not provide a server.

## Recommended integration boundary

Do not mix OpenVPN-specific types into Compose screens, ViewModels, or the existing WireGuard service.

Introduce a protocol-neutral control layer with:
- `VpnEngine`: engine identity, capability declaration, start, stop, state events, traffic counters, last-handshake/peer-evidence abstraction, and structured errors.
- `VpnProfile`: protocol discriminator plus opaque, protocol-specific profile payload stored through the secure store.
- `EngineRegistry`: maps a profile protocol to an installed engine; WireGuard remains the default and only production engine until OpenVPN passes tests.
- `ConnectionCoordinator`: owns serialized Start/Stop/switch intent and ensures only one engine owns the Android VPN session at a time.
- Protocol-specific adapters: current WireGuard backend adapter first; OpenVPN 3 adapter later.

Do not pretend OpenVPN has WireGuard's handshake metric. Expose protocol-neutral connection evidence (connected event, bytes in/out, last packet/activity, transport state) and render protocol-specific diagnostics accurately.

## Implementation gates

A. Stabilize and test the existing WireGuard engine and its latest-intent-wins coordinator.
B. Complete the secure profile storage migration design and tests before changing the storage format.
C. Extract a protocol-neutral coordinator while keeping WireGuard behavior and APK size stable.
D. Build a separate OpenVPN 3 native proof-of-concept in CI for arm64-v8a first; verify ABI, JNI lifecycle, cancellation, logs redaction, and VPN descriptor ownership.
E. Implement Android `VpnService` integration and test connect/disconnect/reconnect, network changes, process death, permission revocation, and coexistence prevention with WireGuard.
F. Complete license review and add notices/source offer as required before distributing an APK containing OpenVPN 3.
G. Only after all gates pass, enable OpenVPN profile import and engine selection in the user interface.

## Current codebase observations

- `RescueWireGuardService` directly constructs and owns `GoBackend`, parses WireGuard `Config`, creates a WireGuard `Tunnel`, polls WireGuard statistics, and serializes tunnel commands.
- `MainViewModel` directly starts the WireGuard service and stores only WireGuard profiles.
- This is a working, tested single-engine design, but it is not yet a multi-engine abstraction.
- A safe first refactor is to extract protocol-neutral command/state contracts and tests without changing the active WireGuard backend or UI behavior.
- The current storage store is WireGuard-specific in its key naming and model. A future profile envelope must add a protocol identifier without exposing private configuration content.

## Status

Research and architecture assessment complete. No OpenVPN native code, dependency, license payload, or runtime behavior has been added to the APK. WireGuard remains the sole active engine. The next coding task is a behavior-preserving coordinator abstraction, followed by the independently gated storage migration.
