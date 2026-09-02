# Releasing

This document covers the steps to produce and publish a release of Linden.

## Release signing

Linden uses **Play App Signing** (recommended by Google). You keep an **upload key**
locally to sign the AAB you upload; Google manages the app signing key that signs the
final APK distributed to users.

### Generate the upload keystore (do this once, well before uploading)

The keystore is a local, offline artifact — you can create it at any time, independent
of the Play Console. It is **irreversible**: you must keep it (and its password) safe
forever, because losing it means you can never update the app under the same signing key.

```bash
keytool -genkeypair -v \
  -keystore ~/.linden/release.keystore \
  -alias linden \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype PKCS12
```

It prompts for a strong password and identity details (name/org — anything reasonable
works). Use a **strong, unique password** and store it in a password manager.

### Configure signing

The release build reads its signing config from a git-ignored `keystore.properties`
file at the repo root:

```properties
storeFile=/Users/<you>/.linden/release.keystore
storePassword=<your-store-password>
keyAlias=linden
keyPassword=<your-key-password>
```

`androidApp/build.gradle.kts` loads this file and applies it to the `release` buildType.
If `keystore.properties` is missing, the release buildType has no signing config (the
`storeFile`/passwords are null), so a release build will fail to sign — which is the
intended safety behaviour.

### Never commit secrets

- `keystore.properties` is git-ignored — never commit it.
- The keystore itself lives outside the repo (`~/.linden/`), so it can't be committed.
- **Back up** the keystore file and its password to a safe, offline location.

### Build a signed release

```bash
./build-android-release.sh   # runs ./gradlew :androidApp:assembleRelease
```

The signed AAB/APK is produced under `androidApp/build/outputs/`.