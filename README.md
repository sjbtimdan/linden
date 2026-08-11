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

## Checking Library Versions

Dependency updates are checked with the [Gradle Versions plugin](https://github.com/ben-manes/gradle-versions-plugin)
(applied via the `io.github.ben-manes.versions.settings` settings plugin in `settings.gradle.kts`).

Run `./gradlew dependencyUpdates` to print a report of dependencies, plugins, and Gradle itself that have newer
versions available:

```sh
./gradlew dependencyUpdates
```

- The report is also written to `build/dependencyUpdates/report.txt`.
- Use `-Drevision=release` to only show stable releases: `./gradlew dependencyUpdates -Drevision=release`.
- The plugin only reports updates — it never modifies `libs.versions.toml` or the build files.
- Dependency versions live in [`gradle/libs.versions.toml`](./gradle/libs.versions.toml).

