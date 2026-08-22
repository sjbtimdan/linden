# Linden

An expenses tracker written in KMP for Android and Desktop

## Skills

Read `../compose-kotlin-agent-skills/SKILL.md` first, then load the matching reference file
from `references/` for the topic (e.g. `08-kmp-cmp.md`, `01-architecture.md`, `02-compose-ui.md`, `11-testing.md`).
`context7` and `task-management` skills are installed under `.opencode/skills/`; the OAC context system
lives in `.opencode/context/` (standards/workflows under `core/`, loaded via ContextScout).

## Modules

- `:shared` — shared UI and business logic (KMP library, Compose Multiplatform; targets `jvm()` + `android`).
  All UI lives here. `App.kt` owns screen navigation via a sealed `Screen` class
  (Ledger/History/Settings/CategoryList/AccountList/Rates) in a `NavigationBar` scaffold with `AnimatedContent` transitions.
- `:androidApp` — Android entry (`MainActivity`, appId `org.sjbtimdan.linden`, minSdk 24, targetSdk 37, edge-to-edge).
- `:desktopApp` — Desktop/JVM entry (`Main.kt` → Compose `Window`, main class `org.sjbtimdan.linden.MainKt`).

## Commands

```
./gradlew :shared:compileKotlinMetadata     # quick verify shared compiles
./gradlew :shared:compileKotlinJvm          # verify shared JVM target
./gradlew :shared:compileAndroidMain        # verify shared Android target
./gradlew :androidApp:compileDebugKotlin    # verify Android app
./gradlew :desktopApp:compileKotlin         # verify Desktop app
./gradlew :shared:jvmTest                   # run Kotest suite (commonTest + jvmTest)
./gradlew :androidApp:assembleDebug         # full Android debug build
./gradlew :desktopApp:run                   # run Desktop app
./gradlew check                             # full check — no CI pipeline (see check.sh)
./gradlew detekt                            # formatting check (ktlint ruleset, all modules)
./gradlew detekt --auto-correct             # auto-fix formatting violations
./gradlew :desktopApp:renderIcon            # regenerate master icon into build/icon-render/
./gradlew dependencyUpdates -Drevision=release  # check newer dependency versions (report in build/)
```

Formatting is enforced by Detekt (2.0.x, plugin `dev.detekt`) with the ktlint-wrapper ruleset — see
`detekt.yml` at the repo root. `./gradlew check` runs the detekt tasks; the pre-commit hook
(`.githooks/pre-commit`, active via `git config core.hooksPath .githooks`) additionally runs
`./gradlew detekt --auto-correct` on commits with staged `.kt` files and re-stages the fixes;
commit is blocked only when violations can't be auto-corrected.

The `:shared` Android target compiles via `compileAndroidMain`, **not** `compileDebugKotlin`
(that task only exists on `:androidApp`). `:shared` uses the AGP 9 `com.android.kotlin.multiplatform.library` plugin
and also defines an instrumentation test builder (`androidx.test.runner.AndroidJUnitRunner`) — device-only tests.

`./gradlew check` also runs the Kover coverage gate (`koverVerifyJvm`, 50% min) on the shared JVM variant —
coverage is only enforced there because the Android target runs device tests only, which Kover doesn't support.

## Architecture & Gotchas

- Package root `org.sjbtimdan.linden`. Models in `.model`, DAOs in `.data`, screens/ViewModels in `.ui.<feature>`,
  backup import in `.imports` (`IvyImporter`), description prediction in `.predictions` (`DescriptionPredictor` —
  heuristic scoring of past entries, pure functions, no DB). Kotlin source files use PascalCase.
- Money is stored as integer minor units (`Long`), never `Double`/`BigDecimal` — `450` = 4.50. All currencies
  (CHF/EUR/GBP/HKD/JPY/SGD/USD) use a 2-decimal minor unit. `formatAmount` in `ui/entry/MoneyFormat.kt` is an
  `expect`/`actual` using the platform locale (`java.text.NumberFormat`, thousands grouping); `parseAmount` is pure
  common code that accepts grouped input ("1,000", "1.000", "1 000"). `amount` columns in `.sq` files are `INTEGER`.
- `Entry` is a sealed interface (`ExpenseEntry` / `IncomeEntry` / `TransferEntry`) in `model/Entry.kt`, carrying
  `createdAt: Instant` and `createdZone: TimeZone`. Entries carry no currency — it's defined by `account.currency`
  (`toAccount.currency` for transfers). Transfers carry `toAccount`/`toAmount` (`toAmount` is null when both accounts
  share a currency); adding a field touches all subclass branches plus the `Entry.sq` insert/update and `EntryDao` mapping.
- SQLDelight is configured with `generateAsync = true` — the generated API is async: schema creation must be awaited
  (`createLindenDatabase(driver)` in `data/DatabaseDriverFactory.kt` wraps `LindenDatabase.Schema.create(driver).await()`),
  DB ops are `suspend`, reactive reads use `.asFlow()` / `awaitAsList()`.
- `.sq` files live in `shared/src/commonMain/sqldelight/org/sjbtimdan/linden/`. Entity/query classes
  (e.g. `CategoryEntity`, `CategoryQueries`) are generated into the `.sq` file's package (`org.sjbtimdan.linden`);
  only the `LindenDatabase` class goes into the `packageName` set in `build.gradle.kts` (`org.sjbtimdan.linden.db`).
- `Import.sq` declares no table — it only exposes `last_insert_rowid()`, used by `IvyImporter` to resolve
  auto-increment IDs when restoring a backup in a transaction.
- No SQLDelight migrations exist (schema version 1). Editing an `.sq` table won't auto-migrate the persisted
  desktop DB at `~/.linden/linden.db` — add a `.sqm` migration, or delete the local DB.
- Startup is async: `AppRoot` calls the suspend `createAppDependencies(driver)` (in `AppDependencies.kt`) — both
  `MainActivity` and desktop `Main` hop to `Dispatchers.IO` — and shows a `CircularProgressIndicator`
  (`testTag("loading")`) until ready. `AppDependencies` is a lazy composition root (DAOs, ViewModels, shared
  `HttpClient`); theme + default currency are read from `SettingsDao` with fallbacks `ThemeMode.SYSTEM` /
  `Currency.CHF` (nothing is written at startup). Theme applies live via `SettingsViewModel.themeMode`.
- FX rates power cross-currency totals: `FxRatesFetcher` (in `.data`) calls Frankfurter
  (`api.frankfurter.dev/v1/latest`) over Ktor (OkHttp on Android, CIO on JVM), responses decode via
  `parseFxRatesResponse` (kotlinx.serialization). Rates refresh once at startup (`App.kt` `LaunchedEffect`), cached in
  `FxRateEntity` (`FxRateDao`), consumed via `FxRatesRepository`; `RatesFlowProvider` exposes default currency + rates
  as `StateFlow`s. Tests inject `FakeFxRatesSource` — never hit the real API.
- `DatabaseDriverFactory` is an `expect class` (shared enables `-Xexpect-actual-classes`), with per-source-set
  actuals at `shared/src/{androidMain,jvmMain}/.../DatabaseDriverFactory.kt`. Desktop persists to `~/.linden/linden.db`;
  Android uses `AndroidSqliteDriver`.
- `ui/BackHandler` is a `@Composable expect`: the Android actual wires `androidx.activity.compose.BackHandler` so the
  system back cancels in-progress edits; the JVM actual is a no-op.
- Balance/total aggregation is pushed into SQL (`Entry.sq` `accountDeltas` / `categoryTotals` queries) and converted
  to the default currency once per currency group. `accountsWithEntries` blocks changing the currency of an account
  that has entries.
- `IvyImporter` replaces all rows in one transaction, infers `CategoryType` (Expense/Income/Both) from usage, maps
  "initial balance"/"adjust balance" titles onto `Account.initialBalance`, and routes currency-mismatched transactions
  to split accounts named `IVY: <name> (<currency>)`.
- History has three view modes (`HistoryViewMode`: Entries/Accounts/Categories) with period navigation, search and
  type filters; "nothing in the future" and "balance at period end" rules are enforced in `HistoryViewModel`.

## Conventions

- Conventional commits
- Version catalog at `gradle/libs.versions.toml`
- Kotlin 2.4.10, Compose Multiplatform 1.11.1 (material3 pinned separately at `1.11.0-alpha07`), AGP 9.3.1,
  SQLDelight 2.3.2, Ktor 3.5.2, Kotest 6.2.4, Kover 0.9.9 (Gradle wrapper 9.7.1; compileSdk/targetSdk 37, minSdk 24,
  JVM target 11)
- Tests: Kotest (`StringSpec`, `shouldBe`), JUnit Platform, Compose UI tests via `runComposeUiTest` (v2 API).
  `createTestSqlDriver()` has a JVM-only actual, so the suite runs via `:shared:jvmTest`. Reuse the commonTest helpers
  instead of wiring up in-memory DBs per test: `lindenDatabase()` / `createTestSqlDriver()` live in
  `data/TestSqlUtils.kt`; Compose harnesses (`onTestMain`, `withApp`, `withViewModel`, `withAccountViewModel`,
  `withSettingsViewModel`, `withRatesViewModel`, `withLedgerViewModel`, `withHistoryViewModel`) live in `ui/Utils.kt`.
  JVM tests pin `user.language=en` / `user.country=US` so `formatAmount` assertions are locale-deterministic.
- Configuration cache + build cache enabled (`gradle.properties`).
- Each Composable goes in its own file (e.g. `DayHeader` lives in `DayHeader.kt`, not inside a screen file).
- Comment minimally: only add comments for obscure code which should be very rare.
- After each piece of work is done, scan through for omitted tests.
