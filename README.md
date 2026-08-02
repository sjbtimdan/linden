# Linden

An expense tracker app built with Kotlin Multiplatform and Compose Multiplatform, targeting Android and Web (Wasm).

## Project Structure

- [shared](./shared/src) — shared UI and business logic
  - [commonMain](./shared/src/commonMain/kotlin) — code common to all targets
  - [androidMain](./shared/src/androidMain/kotlin) — Android-specific code
  - [wasmJsMain](./shared/src/wasmJsMain/kotlin) — Web (Wasm)-specific code
- [androidApp](./androidApp/src) — Android application entry point
- [webApp](./webApp/src) — Web (Wasm) application entry point

## Running the App

- Android: `./gradlew :androidApp:assembleDebug`
- Web: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`

## Running Tests

- Web: `./gradlew :shared:wasmJsTest`
