# Linden

An expense tracker app built with Kotlin Multiplatform and Compose Multiplatform, targeting Android and Desktop (JVM).

## Project Structure

- [shared](./shared/src) — shared UI and business logic
  - [commonMain](./shared/src/commonMain/kotlin) — code common to all targets
  - [androidMain](./shared/src/androidMain/kotlin) — Android-specific code
  - [jvmMain](./shared/src/jvmMain/kotlin) — Desktop (JVM)-specific code
- [androidApp](./androidApp/src) — Android application entry point
- [desktopApp](./desktopApp/src) — Desktop (JVM) application entry point

## Running the App

- Android: `./gradlew :androidApp:assembleDebug`
- Desktop: `./gradlew :desktopApp:run`

## Running Tests

- `./gradlew :shared:jvmTest`
