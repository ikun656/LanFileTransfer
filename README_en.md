# Lan File Transfer (Android)

A pure native TCP + UDP broadcast LAN file-transfer app. No server, no cloud, no account required.

## Usage
1. Connect both phones to the same Wi-Fi.
2. Sender: pick a file → start sending (broadcasts its own address).
3. Receiver: tap "Scan sender" → tap the device once it appears → the file is saved to the Download folder automatically.

Transfer protocol:
- Discovery: `UDP broadcast 255.255.255.255:54321`, payload `LANFILE|<ip>:<tcpPort>|<name>`
- Transfer: `TCP <tcpPort>` sends two text lines first (file name, size), then the raw file bytes

## Build (local)
Requires Android SDK (platform-34, build-tools 34.0.0) and JDK 17:
```sh
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/LanFileTransfer-vX.Y.Z.apk` (filename carries the version, matching the git tag)

## Automated build & release (GitHub Actions)
- Pushing a `v*` tag triggers it: the release APK is built automatically and published to GitHub Releases.
- For a **signed APK**, configure these in Repository Settings → Secrets:
  - `KEYSTORE_BASE64`: Base64 of the keystore file (`base64 -w0 your.jks`)
  - `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
  - Without these, an unsigned APK is built (sideload/debug only).

## License
See [LICENSE](LICENSE). Personal download and use is allowed; modification, redistribution, or commercial use is not.
