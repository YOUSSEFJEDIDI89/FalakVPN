# CI workflow

`android.yml` in this folder is the GitHub Actions workflow for this project.

GitHub only recognises workflows that live in `.github/workflows/`, and the API client used to
publish this repository is not permitted to write into that directory (it is a protected path for
automation safety).

**To enable CI, copy the file into place:**

```bash
cp ci/android.yml .github/workflows/android.yml
git add .github/workflows/android.yml
git commit -m "ci: enable GitHub Actions workflow"
git push
```

## What the workflow does

| Step | Purpose |
|---|---|
| `Unit tests (core engine)` | runs `./gradlew :core:test` — the six engine tests, including the real TLS-through-tunnel test |
| `Upload test report` | publishes the JUnit HTML report as an artifact |
| `Build debug APK` | `./gradlew :app:assembleDebug` |
| `Build release APK` | `./gradlew :app:assembleRelease` — unsigned unless the signing secrets are configured |

## Signing the release build

The release build is signed only when these repository secrets are present:

| Secret | Meaning |
|---|---|
| `KEYSTORE_BASE64` | the `.jks` file, base64 encoded |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

Without them the release APK is produced unsigned and the build still succeeds — which keeps the
pipeline green on forks and pull requests.
