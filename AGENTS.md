# Linden

KMP + Compose Multiplatform expense tracker. Android only.

## Modules

- `:shared` — shared UI and business logic (KMP library, Compose Multiplatform)
- `:androidApp` — Android entry point (`MainActivity`)

## Targets

Android only.

## Commands

```
./gradlew :shared:compileKotlinMetadata   # quick verify shared compiles
./gradlew :androidApp:compileDebugKotlin  # verify Android
./gradlew :androidApp:assembleDebug       # full Android debug build
```

## Conventions

- Conventional commits
- Package: `org.sjbtimdan.linden`, model classes under `.model`
- Kotlin source files use PascalCase (e.g. `SettingsScreen.kt`, `ThemeMode.kt`)
- Version catalog at `gradle/libs.versions.toml`
- Kotlin 2.4.10, Compose Multiplatform 1.11.1
- Configuration cache enabled (set in `gradle.properties`)