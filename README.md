# R&MA Android VPN Client

R&MA is an Android client built around the official WireGuard tunnel backend.

## Project layout
- `app/`: Android application module and all Kotlin/resources/tests.
- `.github/workflows/build_apk.yml`: the single active GitHub Actions workflow.
- Root Gradle files: project and module configuration.

## Build
GitHub Actions runs unit tests and creates a debug APK on pushes to the configured branches and on manual dispatch.
Open the **Actions** tab, select **R&MA Android CI**, and download the `RMA-debug-apk` artifact from a successful run.

## Important limitations
- An APK build passing does not prove a live VPN connection works on a device.
- A valid WireGuard configuration and a reachable WireGuard server are required.
- The project does not provide free servers or generate private keys/configurations.
- The debug APK is for testing and is not a signed release build.

## Current development branch
`rebuild/rma-android-structure`

The main branch is kept unchanged until the reorganized project and its tests are reviewed.
