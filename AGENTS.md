# Linden

KMP + Compose Multiplatform expense tracker. Android + Desktop (JVM).

Only `:androidApp`, `:shared`, `:desktopApp` exist (see `settings.gradle.kts`).

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
./gradlew check                             # full check — no CI pipeline (see check.sh)
./gradlew :desktopApp:run                   # run Desktop app
```

The `:shared` Android target compiles via `compileAndroidMain`, **not** `compileDebugKotlin`
(that task only exists on `:androidApp`). `:shared` uses the AGP 9 `com.android.kotlin.multiplatform.library` plugin.

## Architecture & Gotchas

- Package root `org.sjbtimdan.linden`. Models in `.model`, DAOs in `.data`, screens/ViewModels in `.ui.<feature>`,
  backup import in `.imports` (`IvyImporter`, `ZipFilePicker`). Kotlin source files use PascalCase.
- Money is stored as integer minor units (`Long`), never `Double`/`BigDecimal` — `450` = 4.50. Convert via
  `formatAmount` / `parseAmount` in `ui/ledger/MoneyFormat.kt`; `amount` columns in `.sq` files are `INTEGER`.
- SQLDelight is configured with `generateAsync = true` — the generated API is async:
  schema creation must be awaited (`LindenDatabase.Schema.create(driver).await()`), DB ops are `suspend`, and reactive reads use `.asFlow()` / `awaitAsList()`.
- `.sq` files live in `shared/src/commonMain/sqldelight/org/sjbtimdan/linden/`. Entity/query classes
  (e.g. `CategoryEntity`, `CategoryQueries`) are generated into the `.sq` file's package
  (`org.sjbtimdan.linden`); only the `LindenDatabase` class goes into the `packageName` set in `build.gradle.kts`
  (`org.sjbtimdan.linden.db`).
- `Import.sq` declares no table — it only exposes `last_insert_rowid()`, used by `IvyImporter` to resolve
  auto-increment IDs when restoring a backup in a transaction.
- `DatabaseDriverFactory` is an `expect class` (shared enables `-Xexpect-actual-classes`), with per-source-set
  actuals at `shared/src/{androidMain,jvmMain}/.../DatabaseDriverFactory.kt`.
- Desktop persists to `~/.linden/linden.db` (file-based); Android uses `AndroidSqliteDriver`.

## Conventions

- Conventional commits
- Version catalog at `gradle/libs.versions.toml`
- Kotlin 2.4.10, Compose Multiplatform 1.11.1, AGP 9.1.1, SQLDelight 2.3.2 (Gradle wrapper 9.6.1)
- Tests: Kotest (`StringSpec`, `shouldBe`), JUnit Platform. `createTestSqlDriver()` has a JVM-only actual,
  so the suite runs via `:shared:jvmTest`. Reuse the commonTest helpers instead of wiring up in-memory DBs
  per test: `lindenDatabase()` / `createTestSqlDriver()` live in `data/TestSqlUtils.kt`; Compose harnesses
  `onTestMain`, `withViewModel`, `withAccountViewModel`, `withSettingsViewModel`, `withLedgerViewModel` live
  in `ui/Utils.kt`. Compose UI tests live in `commonTest`.
- Configuration cache + build cache enabled (`gradle.properties`).
- Comment minimally: only add comments for obscure code which should be very rare.
- After each piece of work is done, scan through for omitted tests.