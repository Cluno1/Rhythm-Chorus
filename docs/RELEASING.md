# Sonorus release procedure

## Permanent identity

- Application ID and namespace: `io.github.cluno1.sonorus`
- Stable tag format: `vMAJOR.MINOR.PATCH`
- Version code: `MAJOR * 1,000,000 + MINOR * 1,000 + PATCH`; minor and patch are limited to 0–999.
- Release assets: `Sonorus-{version}-githubRelease-{abi}.apk` plus the universal `githubRelease.apk` and matching `.sha256` files.

Never change the application ID or signing certificate after the first customer release. Sonorus is a new Android app and does not replace or inherit private data from `chromahub.rhythm.app`; users can use backup/restore, then register the Sonorus installation as a new Catalog device.

## Signing key

Generate one Sonorus release keystore outside the repository, keep two encrypted offline backups, and record its SHA-256 certificate fingerprint with the release records. The keystore and passwords must never be committed.

The GitHub Actions environment expects these encrypted secrets:

- `SONORUS_SIGNING_KEYSTORE`: base64-encoded keystore
- `SONORUS_STORE_PASSWORD`
- `SONORUS_KEY_ALIAS`
- `SONORUS_KEY_PASSWORD`

Losing the key prevents upgrades of existing installations. A replacement key creates a different installation line.

## Local dry-run

With no `.config/keystore.properties`, Gradle intentionally falls back to the debug certificate for local testing only:

```bash
scripts/release_dry_run.sh 1.0.0
```

Inspect the generated APK package, label, version, signing certificate, icons, bundled GPL, and SHA-256 output. A manual CI `workflow_dispatch` creates signed artifacts without creating a public Release; it still requires the fixed signing secrets. Only a reviewed `vMAJOR.MINOR.PATCH` tag can publish a Release.

## Publish and rollback

1. Start from a clean, reviewed commit and pass the dry-run.
2. Confirm the version is greater than every published Sonorus version.
3. Confirm the signing certificate fingerprint matches the first Sonorus release.
4. Push the annotated stable tag.
5. Verify all ABI/universal APKs, checksums, source tag, GPL notice, and updater discovery.

Do not replace a published binary under the same tag. If a release is bad, mark it clearly, publish a higher patch version signed by the same key, and let clients upgrade forward.

## Deferred before a public release

The current repository is a local Sonorus rebrand/build baseline, not an authorization to publish. Before the first public release:

- create or rename the `Cluno1/Sonorus` GitHub repository and configure protected release environments;
- provision the permanent keystore secrets, add an expected certificate SHA-256 secret, and enforce an exact certificate match in CI;
- enforce that a proposed tag and Android `versionCode` are greater than every historical Sonorus release;
- complete stable-only updater cleanup and end-to-end update/install testing against real GitHub Release assets;
- finish explicit translations for newly added About/legal text in every supported locale;
- resolve the bundled `sonivox.sf2` provenance/license blocker and verify exact notices for every bundled font/library listed in `THIRD_PARTY_NOTICES.md`.

Until these are complete, local APKs are development verification artifacts only.
