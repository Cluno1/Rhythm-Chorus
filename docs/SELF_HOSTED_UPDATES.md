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

`.github/workflows/debug-update.yml` performs that sequence automatically after a push to the dedicated `debug` branch. Feature branches must be reviewed and explicitly approved before they are merged into `debug`; pushing a feature branch or `main` does not publish a Debug update. The workflow deliberately has no `workflow_dispatch` entry, so Debug publication cannot be started manually from the Actions UI, CLI, or API.

The workflow stores the signed bundle as an immutable GitHub Actions artifact, resolves the artifact API's short-lived download URL, and sends only that callback over pinned public SSH. `/usr/local/bin/sonorus-pull-update` then downloads the artifact through the server's loopback Mihomo proxy, verifies the GitHub artifact digest, ZIP allowlist, Ed25519 manifest signature, APK SHA-256, frozen certificate identity and monotonic version before switching `latest.json` atomically. The server never stores a GitHub token, and no HTTP callback port is opened.

Before either publisher switches `latest.json`, it invokes the narrowly privileged
`/usr/local/bin/sonorus-sync-update-cos`. The helper independently verifies the channel's
Ed25519 manifest and every local APK, uploads only the immutable
`{channel}/releases/{versionCode}/{sha256}` content-addressed key to the private
`sonorus-updates-1328751369` bucket, then performs a full COS read-back SHA-256 check. COS
credentials remain readable only by the `ubuntu` account; the deployment account can invoke this
single verifier/uploader through its fixed sudo rule and cannot read the credentials. A failed COS
upload leaves `latest.json` unchanged, so neither the browser nor the app is pointed at a missing
object.

The COS key intentionally has no `.apk` extension and uses
`application/octet-stream`: Tencent blocks APK/IPA delivery from default domains on buckets
created after 2024. The signed manifest still owns the real `.apk` file name, and Android writes
and validates the downloaded bytes under that name. Until a custom COS domain is configured, the
two unauthenticated browser URLs continue serving their APKs from the gateway so a browser receives
the correct installable file name.

Configure the `sonorus-debug` GitHub environment with the fixed Debug keystore/password secrets, Debug manifest private key, raw manifest public key, frozen APK certificate fingerprint, deployment SSH private key, pinned `SONORUS_UPDATE_KNOWN_HOSTS`, and the public `SONORUS_UPDATE_SSH_TARGET`. The SSH account must be dedicated to this job and use key-only authentication; host-key checking remains mandatory. It needs write access only to `/srv/sonorus-updates/debug` and has no general sudo access: its sole sudo rule runs the root-owned COS verifier as `ubuntu`, with a fixed channel/version command shape. The server puller is root-owned, uses `http://127.0.0.1:7890`, and accepts only GitHub Actions artifact hosts over HTTPS.

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
