# Releasing

This document covers the steps to produce and publish a release of Linden.

Email for testing: crafted-devs@proton.me

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

## Minification (R8)

Minification and resource shrinking are enabled for release builds (`isMinifyEnabled =
true`, `isShrinkResources = true` in `androidApp/build.gradle.kts`). The `proguard-rules.pro`
file is empty — R8 consumer rules from Compose, SQLDelight, Ktor, and kotlinx.serialization
are applied automatically.

### Verified (2026-09-02)

- `assembleRelease` completes with **zero** R8 missing-class warnings or errors.
- Key runtime-critical classes are **kept** (not stripped):
  - `MainActivity` (entry point)
  - SQLDelight generated classes (`LindenDatabase`, `EntryQueries`, `CategoryQueries`,
    `AccountQueries`, `SettingsQueries`, `FxRateQueries`)
  - kotlinx.serialization (`parseFxRatesResponse`, `FxRates` model)
- Mapping/usage/seeds files generated under `androidApp/build/outputs/mapping/release/`.

**Caveat:** Build-time verification only — no runtime test on a device/emulator yet.
The definitive test is installing the minified release APK and exercising the full app
(add entries, check FX rates, backup/restore, ledger views).

## Native debug symbols

The release AAB contains two prebuilt native libraries from dependencies:
`libandroidx.graphics.path.so` (Compose graphics) and `libdatastore_shared_counter.so`
(DataStore, pulled in via Firebase). Play Console warns on upload that the bundle has
no native debug symbols; this is expected and safe to ignore:

- Both `.so` files are shipped already stripped by their publishers (no `.symtab` or
  `.debug_*` sections — verified), so there is no metadata for AGP to extract and the
  warning cannot be cleared for them.
- It is advisory, not a blocker: native debug symbols are not in Play's technical
  quality requirements (only 64-bit and 16 KB page-size support are enforced). Native
  frames inside those two libraries just stay unsymbolicated in Android vitals.
- If Linden ever adds its own native code, set `ndk { debugSymbolLevel = "SYMBOL_TABLE" }`
  on the release build type; AGP then packages symbols into the AAB automatically and
  Play picks them up (requires an NDK — AGP 9.4 defaults to NDK 28.2.13676358).

## Store listing assets

Store assets live in `docs/store-assets/` (icon, feature graphic, screenshots);
listing copy lives in `docs/store-listings/`. The phone screenshots carry these
overlay captions:

| File | Caption |
|---|---|
| `screen-1-ledger-entries.png` | All your money, one timeline |
| `screen-2-entry-calculator.png` | Add entries in seconds |
| `screen-3-ledger-accounts.png` | Track accounts in any currency |
| `screen-4-ledger-categories.png` | Know where it goes |
| `screen-5-insights.png` | 12 months at a glance |
