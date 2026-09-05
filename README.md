# Sonorus

Sonorus is an independently maintained Android music player for device audio, a private Catalog, and MusicXML scores. It is built with Kotlin, Jetpack Compose, Material 3, Media3, Room, and alphaTab.

The public source and signed release APKs are intended to live at [Cluno1/Sonorus](https://github.com/Cluno1/Sonorus). Until that repository and its first release exist, build from source; no F-Droid, IzzyOnDroid, OpenAPK, or other store listing is claimed.

## Build

Requirements: JDK 17 and the Android SDK.

```bash
./gradlew testGithubDebugUnitTest assembleGithubDebug
```

The production application ID is `io.github.cluno1.sonorus`; debug builds use `io.github.cluno1.sonorus.debug`. A new application ID means Sonorus can coexist with the upstream app. Existing private app data is not inherited automatically; use backup/restore and register the device again.

## Releases and updates

Sonorus checks only stable releases from `Cluno1/Sonorus`. Release assets use the form `Sonorus-{version}-githubRelease-{abi}.apk`, with a universal APK as fallback, and each APK is accompanied by SHA-256. Long-term upgrades require the same application ID, the same offline-backed release signing key, and a strictly increasing version code.

See [RELEASING.md](docs/RELEASING.md) for the local dry-run and CI secret names. This repository does not contain a keystore, passwords, Catalog credentials, or COS credentials.

## Privacy

Device music stays on the device. Sonorus can contact the configured Catalog/COS service and, when enabled, the first-party Sonorus GitHub Releases endpoint. See the privacy policy bundled in the app for details.

## Upstream and license

Sonorus is an independent, unofficial derivative of [Rhythm](https://github.com/cromaguy/Rhythm), originally created by Anjishnu Nandi and Team ChromaHub. Sonorus is not affiliated with or endorsed by the Rhythm authors or Team ChromaHub. Original copyright and SPDX notices are retained in source files and Git history.

The Android client remains licensed under [GNU GPL v3 or later](LICENSE). alphaTab is licensed under MPL-2.0; other bundled libraries and assets retain their own licenses. Music, artwork, lyrics, scores, fonts, and SoundFont content are not automatically covered by the client GPL.
