# Sonorus build variants

Sonorus keeps the upstream `github` and `fdroid` flavors for source compatibility, but only the GitHub flavor is an official Sonorus release target today.

| Variant | Application ID | First-party updates | Intended use |
| --- | --- | --- | --- |
| `githubDebug` | `io.github.cluno1.sonorus.debug` | enabled | development |
| `githubRelease` | `io.github.cluno1.sonorus` | enabled | signed GitHub Releases |
| `fdroidDebug` / `fdroidRelease` | corresponding Sonorus IDs | disabled | reproducible/FOSS compatibility; not a claimed store listing |

Catalog-only boundaries remain in every flavor: upstream streaming providers and their automatic network access stay disabled. `FIRST_PARTY_UPDATES` is a separate capability and points only to stable `Cluno1/Sonorus` Releases.

```bash
./gradlew testGithubDebugUnitTest assembleGithubDebug
scripts/release_dry_run.sh 1.0.0
```

APK names use `Sonorus-{version}-{variant}-{abi}.apk`; the build also emits a universal APK. Do not distribute a debug-signed release as a customer update.
