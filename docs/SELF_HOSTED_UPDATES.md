# Sonorus self-hosted updates

Sonorus uses two immutable update tracks. `githubDebug` is compiled with `UPDATE_CHANNEL=debug` and application ID `io.github.cluno1.sonorus.debug`; `githubRelease` is compiled with `UPDATE_CHANNEL=stable` and application ID `io.github.cluno1.sonorus`. The app has no runtime channel or source selector.

## Key material

Keep four long-lived private keys outside Git:

- fixed Android Debug APK keystore;
- permanent Android Release APK keystore;
- Debug Ed25519 manifest key;
- Stable Ed25519 manifest key.

Back up each private key twice offline. Record the two APK certificate SHA-256 values. Export only the raw 32-byte Ed25519 public keys as standard Base64 and inject them at build time:

```bash
./gradlew assembleGithubDebug \
  -PsonorusDebugManifestPublicKey="$SONORUS_DEBUG_MANIFEST_PUBLIC_KEY"

./gradlew assembleGithubRelease \
  -PsonorusStableManifestPublicKey="$SONORUS_STABLE_MANIFEST_PUBLIC_KEY"
```

An empty or wrong public key causes update checks to fail closed. Never put an Ed25519 private key, APK keystore, password, Catalog token, or COS credential in Gradle properties committed to Git.

Published Debug builds must also provide `.config/debug-keystore.properties`, using the same four property names as the Release keystore file. If it is absent, Gradle deliberately uses the machine-local Android debug key for ordinary development; such an APK must never be placed in the Debug update track.

## Build a signed manifest

`scripts/build_update_manifest.py` verifies the APK package, version, single signer, frozen certificate and minimum SDK before it creates an immutable version directory. It signs canonical JSON with an external PEM Ed25519 key:

```bash
scripts/build_update_manifest.py \
  --channel stable \
  --apk app/build/outputs/apk/github/release/Sonorus-1.0.1-githubRelease-arm64-v8a.apk \
  --abi arm64-v8a \
  --manifest-key /secure/sonorus-stable-manifest.pem \
  --expected-certificate-sha256 "$SONORUS_RELEASE_CERT_SHA256" \
  --published-at 2026-09-06T12:00:00Z \
  --release-note "First self-hosted update" \
  --output-root build/sonorus-updates
```

Copy the generated `{channel}/releases/{versionCode}` directory to a temporary server directory, verify it on the server, then atomically rename it into `/srv/sonorus-updates`. Replace `{channel}/latest.json` last using a temporary file plus `rename`. Never overwrite an existing version directory or publish a mutable `latest.apk`.

Use the repository publisher rather than copying individual files. It revalidates size and SHA-256 locally and remotely, rejects non-increasing versions, installs the immutable directory first, and switches `latest.json` last:

```bash
scripts/publish_update_bundle.py \
  --channel stable \
  --bundle-root build/sonorus-updates \
  --ssh-target sonorus-deploy@175.178.242.232 \
  --remote-root /srv/sonorus-updates \
  --expected-certificate-sha256 "$SONORUS_RELEASE_CERT_SHA256"
```

`.github/workflows/debug-update.yml` performs that sequence automatically after a successful `main` build. Configure the `sonorus-debug` GitHub environment with the fixed Debug keystore/password secrets, Debug manifest private key, manifest public key, frozen APK certificate fingerprint, deployment SSH private key, pinned `SONORUS_UPDATE_KNOWN_HOSTS`, and the public `SONORUS_UPDATE_SSH_TARGET`. The SSH account must be dedicated to this job, have no sudo access, and use key-only authentication; host-key checking remains mandatory. The deployment account needs write access only to `/srv/sonorus-updates`.

Stable remains deliberately manual: `.github/workflows/release.yml` creates and retains the signed Stable bundle, but does not switch the server pointer. Download and inspect that artifact, then run the publisher with `--channel stable` over the same pinned public SSH path. The Stable deployment account needs write access only to `/srv/sonorus-updates/stable`.

The public gateway must expose only authenticated `GET/HEAD /v2/app-updates/latest` and `/v2/app-updates/files/{versionCode}/{fileName}`. Range requests require the same device proof. There is deliberately no public update upload API.

## Required verification

Before switching either `latest.json`, verify that:

- `versionCode` is greater than the current value in that track;
- the application ID, APK certificate and manifest public key belong to that track;
- `HEAD`, full GET, one Range request, ETag and SHA-256 all match;
- Debug cannot retrieve Stable and Release cannot retrieve Debug;
- the prior installed build upgrades in place and retains its data/device registration.

Rollback is forward-only once any device installs a version: fix the source and publish a higher `versionCode` with the same APK key. Never replace a published APK.
