# Linden

KMP + Compose Multiplatform expense tracker. Android + Desktop.

## Modules

- `:shared` — shared UI and business logic (KMP library, Compose Multiplatform)
- `:androidApp` — Android entry point (`MainActivity`)
- `:desktopApp` — Desktop/JVM entry point (`Main.kt` → Compose `Window`)

## Targets

Android and JVM (Desktop).

## Commands

```
./gradlew :shared:compileKotlinMetadata     # quick verify shared compiles
./gradlew :androidApp:compileDebugKotlin    # verify Android
./gradlew :desktopApp:compileKotlin         # verify Desktop
./gradlew :androidApp:assembleDebug         # full Android debug build
./gradlew :desktopApp:run                   # run Desktop app
```

## Conventions

- Conventional commits
- Package: `org.sjbtimdan.linden`, model classes under `.model`
- Kotlin source files use PascalCase (e.g. `SettingsScreen.kt`, `ThemeMode.kt`)
- Version catalog at `gradle/libs.versions.toml`
- Kotlin 2.4.10, Compose Multiplatform 1.11.1
- Configuration cache enabled (set in `gradle.properties`)