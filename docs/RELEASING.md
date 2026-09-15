# Sonorus release procedure

## Permanent identity

- Application ID and namespace: `io.github.cluno1.sonorus`
- Stable source: every reviewed push to `main`.
- Version name: `1.(stable workflow run / 1000).(stable workflow run % 1000)`.
- Version code: `1,000,000 + stable workflow run`; the workflow run must remain below 1,000,000.
- Release assets: `Sonorus-{version}-githubRelease-{abi}.apk` plus the universal `githubRelease.apk` and matching `.sha256` files.

Never change the application ID or signing certificate after the first customer release. Sonorus is a new Android app and does not replace or inherit private data from `chromahub.rhythm.app`; users can use backup/restore, then register the Sonorus installation as a new Catalog device.

## Signing key

Generate one Sonorus release keystore outside the repository, keep two encrypted offline backups, and record its SHA-256 certificate fingerprint with the release records. The keystore and passwords must never be committed.

The `sonorus-stable` GitHub Actions environment expects these encrypted secrets:

- `SONORUS_SIGNING_KEYSTORE`: base64-encoded keystore
- `SONORUS_STORE_PASSWORD`
- `SONORUS_KEY_ALIAS`
- `SONORUS_KEY_PASSWORD`
- `SONORUS_STABLE_MANIFEST_PRIVATE_KEY`: PEM Ed25519 key, restricted to the protected Stable release environment

It also requires these non-secret environment variables:

- `SONORUS_RELEASE_CERT_SHA256`: frozen APK signing certificate fingerprint
- `SONORUS_STABLE_MANIFEST_PUBLIC_KEY`: raw 32-byte Stable Ed25519 public key in Base64

Losing the key prevents upgrades of existing installations. A replacement key creates a different installation line.

## Local dry-run

With no `.config/keystore.properties`, Gradle intentionally falls back to the debug certificate for local testing only:

```bash
scripts/release_dry_run.sh 1.0.0
```

Inspect the generated APK package, label, version, signing certificate, icons, bundled GPL, and SHA-256 output. Only a reviewed commit pushed to `main` can publish Stable; the Stable workflow has no manual trigger.

## Publish and rollback

1. Start from a clean, reviewed commit and pass the dry-run.
2. Confirm the Stable workflow run-derived version is greater than every published Sonorus version.
3. Confirm the signing certificate fingerprint matches the first Sonorus release.
4. Push the reviewed commit to `main`.
5. Let the workflow generate and sign the Stable update manifest.
6. Let the server verify the Artifact, synchronize COS, and atomically switch `stable/latest.json`.
7. Let the workflow create `v{version}` on GitHub Releases and attach all five signed APKs plus their five SHA-256 files.
8. Verify the GitHub Release, authenticated updater discovery, install, and retained app data.

Do not replace a published binary under the same version code. If a release is bad, push a reviewed fix so the workflow publishes a higher version signed by the same key, and let clients upgrade forward.

## Before each public Stable push

Before updating `main`:

- review and test the exact commit that will become public;
- confirm the `sonorus-stable` environment remains restricted to `main`;
- confirm the permanent APK certificate and Stable manifest key backups are intact;
- verify the previous Stable package can discover, download, and install the new version;
- verify the GitHub Release contains the structured change summary, device/ABI guidance, build identity, and all ten expected assets;
- verify the public GitHub download and the authenticated app update path after publication.

Stable rollback is forward-only. Never overwrite an immutable server release, COS object, signed manifest, or published GitHub tag. A rerun for the same workflow run may only repair the matching Release assets and notes.
