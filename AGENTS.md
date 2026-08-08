# Linden

KMP + Compose Multiplatform expense tracker. Android + Desktop (JVM).

> ⚠️ `README.md` is stale — it still documents a `webApp`/Wasm target that was removed.
> Trust `settings.gradle.kts`: only `:androidApp`, `:shared`, `:desktopApp` exist.

## Modules

- `:shared` — shared UI and business logic (KMP library, Compose Multiplatform). Targets: `jvm()` + `android`. All UI lives here; `App.kt` handles screen navigation via a sealed `Screen`.
- `:androidApp` — Android entry point (`MainActivity`, appId `org.sjbtimdan.linden`).
- `:desktopApp` — Desktop/JVM entry point (`Main.kt` → Compose `Window`, main class `org.sjbtimdan.linden.MainKt`).

## Commands

```
./gradlew :shared:compileKotlinMetadata     # quick verify shared compiles
./gradlew :shared:compileKotlinJvm          # verify shared JVM target
./gradlew :shared:compileAndroidMain        # verify shared Android target
./gradlew :androidApp:compileDebugKotlin    # verify Android app
./gradlew :desktopApp:compileKotlin         # verify Desktop app
./gradlew :shared:jvmTest                   # run Kotest suite (commonTest + jvmTest)
./gradlew :androidApp:assembleDebug         # full Android debug build
./gradlew check                             # full check (see check.sh)
./gradlew :desktopApp:run                   # run Desktop app
```

The `:shared` Android target compiles via `compileAndroidMain`, **not** `compileDebugKotlin`
(that task only exists on `:androidApp`). `:shared` uses the AGP 9 `com.android.kotlin.multiplatform.library` plugin.

## Architecture & Gotchas

- Package root `org.sjbtimdan.linden`. Models in `.model`, DAOs in `.data`, screens/ViewModels in `.ui.<feature>`. Kotlin source files use PascalCase.
- SQLDelight is configured with `generateAsync = true` — the generated API is async:
  schema creation must be awaited (`LindenDatabase.Schema.create(driver).await()`), DB ops are `suspend`, and reactive reads use `.asFlow()` / `awaitAsList()`.
- `.sq` files live in `shared/src/commonMain/sqldelight/org/sjbtimdan/linden/`. Entity/query classes
  (e.g. `CategoryEntity`, `CategoryQueries`) are generated into the `.sq` file's package
  (`org.sjbtimdan.linden`); only the `LindenDatabase` class goes into the `packageName` set in `build.gradle.kts`
  (`org.sjbtimdan.linden.db`).
- `DatabaseDriverFactory` is an `expect class` (shared enables `-Xexpect-actual-classes`), with per-source-set
  actuals at `shared/src/{androidMain,jvmMain}/.../DatabaseDriverFactory.kt`.
- Desktop persists to `~/.linden/linden.db` (file-based); Android uses `AndroidSqliteDriver`.

## Conventions

- Conventional commits
- Version catalog at `gradle/libs.versions.toml`
- Kotlin 2.4.10, Compose Multiplatform 1.11.1, AGP 9.0.1, SQLDelight 2.3.2
- Tests: Kotest (`StringSpec`, `shouldBe`), JUnit Platform. DAO tests use an in-memory driver via
  `expect fun createTestSqlDriver()` (commonTest) with an actual in `jvmTest`. Compose UI tests live in `commonTest`.
- Configuration cache + build cache enabled (`gradle.properties`).
- Comment minimally: only add comments for obscure code which should be very rare.
- After each piece of work is done, scan through for omitted tests.