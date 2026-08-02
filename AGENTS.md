# Linden

KMP + Compose Multiplatform expense tracker. Android + Web (Wasm only).

## Modules

- `:shared` — shared UI and business logic (KMP library, Compose Multiplatform)
- `:androidApp` — Android entry point (`MainActivity`)
- `:webApp` — Web/Wasm entry point (`main.kt` → `ComposeViewport`)

## Targets

Android and WasmJS. Legacy JS target removed intentionally — do not re-add.

## Commands

```
./gradlew :shared:compileKotlinMetadata   # quick verify shared compiles
./gradlew :androidApp:compileDebugKotlin  # verify Android
./gradlew :webApp:compileKotlinWasmJs     # verify Web/Wasm
./gradlew :androidApp:assembleDebug       # full Android debug build
./gradlew :webApp:wasmJsBrowserDevelopmentRun  # run Web in browser
./gradlew :shared:wasmJsTest              # run Web tests
```

## Conventions

- Conventional commits
- Package: `org.sjbtimdan.linden`, model classes under `.model`
- Version catalog at `gradle/libs.versions.toml`
- Kotlin 2.4.10, Compose Multiplatform 1.11.1
- Configuration cache enabled (set in `gradle.properties`)