# Linden

An expenses tracker written in KMP for Android and Desktop

## Codebase Mental Map

New agents with a fresh context should read the project-intelligence files first to understand the
codebase without re-reading all the source code:

- `.opencode/context/project-intelligence/navigation.md` — overview of the mental map
- `.opencode/context/project-intelligence/technical-domain.md` — stack, architecture, structure, decisions
- `.opencode/context/project-intelligence/business-domain.md` — why the project exists
- `.opencode/context/project-intelligence/business-tech-bridge.md` — business → technical mapping
- `.opencode/context/project-intelligence/decisions-log.md` — key architectural decisions
- `.opencode/context/project-intelligence/living-notes.md` — gotchas, patterns, technical debt

## Skills

Read `../compose-kotlin-agent-skills/SKILL.md` first, then load the matching reference file
from `references/` for the topic (e.g. `08-kmp-cmp.md`, `01-architecture.md`, `02-compose-ui.md`, `11-testing.md`).
`context7` and `task-management` skills are installed under `.opencode/skills/`; the OAC context system
lives in `.opencode/context/` (standards/workflows under `core/`, loaded via ContextScout).

## MCP

Check if IntelliJ MCP is available.
Use it for searching files, compiling and testing as it is faster.
When running tests: if a test configuration is not available (e.g. for jvmTests) then create it.

## Tests

Always run tests at the end of each change. Use ./check.sh as that runs everything.
When developing, it's quicker to use IntelliJ MCP to run tests.

## Modules

- `:shared` — shared UI and business logic (KMP library, Compose Multiplatform; targets `jvm()` + `android`).
  All UI lives here. `App.kt` owns screen navigation via a sealed `Screen` class
  (Entry/Ledger/Settings/CategoryList/AccountList/Rates) in a `NavigationBar` scaffold with `AnimatedContent` transitions.
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
./gradlew check                             # full check — CI runs this too (.github/workflows/check.yml)
./gradlew detekt                            # formatting check (ktlint ruleset, all modules)
./gradlew detekt --auto-correct             # auto-fix formatting violations
./gradlew :desktopApp:renderIcon            # regenerate master icon into build/icon-render/
./gradlew dependencyUpdates -Drevision=release  # check newer dependency versions (report in build/)
```

Formatting is enforced by Detekt (2.0.x, plugin `dev.detekt`) with the ktlint-wrapper ruleset — see
`detekt.yml` at the repo root. `./gradlew check` runs the per-module `detekt` tasks. There is no
pre-commit hook (`git config core.hooksPath` still points at `.githooks`, but that directory doesn't
exist), so run `./gradlew detekt --auto-correct` yourself after edits.

The `:shared` Android target compiles via `compileAndroidMain`, **not** `compileDebugKotlin`
(that task only exists on `:androidApp`). `:shared` uses the AGP 9 `com.android.kotlin.multiplatform.library` plugin
and also defines an instrumentation test builder (`androidx.test.runner.AndroidJUnitRunner`) — device-only tests.

`./gradlew check` also runs the Kover coverage gate (`koverVerifyJvm`, 50% min) on the shared JVM variant —
coverage is only enforced there because the Android target runs device tests only, which Kover doesn't support.

## Architecture & Gotchas

- Package root `org.sjbtimdan.linden`. Models in `.model`, DAOs in `.data`, screens/ViewModels in `.ui.<feature>`,
  backup/restore in `.backup` (`LindenBackupManager` — JSON dump of all tables, invoked from Settings),
  import in `.imports` (`IvyImporter`), entry prediction in `.predictions` (`DescriptionPredictor`,
  `FieldPredictor` — heuristic scoring of past entries, pure functions, no DB) consumed via
  `EntrySuggestionsProvider` in `ui/entry`. Kotlin source files use PascalCase.
- Money is stored as integer minor units (`Long`), never `Double`/`BigDecimal` — `450` = 4.50. All currencies
  (CHF/EUR/GBP/HKD/JPY/SGD/USD) use a 2-decimal minor unit. `formatAmount` in `ui/entry/MoneyFormat.kt` is an
  `expect`/`actual` using the platform locale (`java.text.NumberFormat`, thousands grouping); `parseAmount` is pure
  common code that accepts grouped input ("1,000", "1.000", "1 000"). `formatAmountCompact` (pure common code) shortens
  read-only displays of amounts ≥ 1,000,000.00 to "1.25m"/"1.235b" (fixed '.', trimmed zeros, half-up rounding);
  never use it to pre-fill edit fields — `parseAmount` can't parse the suffix. `amount` columns in `.sq` files are `INTEGER`.
- Amount entry in the entry dialog uses an exact calculator (`CalculatorModel`/`AmountCalculator` in `ui/entry`):
  arithmetic on reduced fractions (Long numerator/denominator, never floats), evaluated left-to-right with no
  operator precedence, rounded to two decimals only for display and commit.
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
  (`testTag("loading")`) until ready. `AppDependencies` is the composition root from `AppDependencies.kt`: DAOs,
  repository and ViewModels are created eagerly, only `httpClient` is `by lazy` (its engine is expensive to build and
  tests injecting a fake FX source never trigger it). Theme + default currency are read from `SettingsDao` with
  fallbacks `ThemeMode.SYSTEM` / `Currency.CHF` (nothing is written at startup). Theme applies live via
  `SettingsViewModel.themeMode`.
- FX rates power cross-currency totals: `FxRatesFetcher` (in `.data`) calls Frankfurter
  (`api.frankfurter.dev/v1/latest`) over Ktor (OkHttp on Android, CIO on JVM), responses decode via
  `parseFxRatesResponse` (kotlinx.serialization). Rates refresh only when the cached rates are older than 24 hours
  (`App.kt` `LaunchedEffect` → `ratesViewModel.refreshRatesIfStale`), cached in
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
- Ledger has three view modes (`LedgerViewMode`: Entries/Accounts/Categories) with period navigation, search and
  type filters; "nothing in the future" and "balance at period end" rules are enforced in `LedgerViewModel`.

## Conventions

- Conventional commits
- Version catalog at `gradle/libs.versions.toml`
- Kotlin 2.4.10, Compose Multiplatform 1.12.0 (material3 pinned separately at `1.12.0-alpha03`), AGP 9.3.2,
  SQLDelight 2.3.2, Ktor 3.5.2, Kotest 6.2.4, Kover 0.9.9, Detekt 2.0.0-alpha.6 (Gradle wrapper 9.7.1;
  compileSdk/targetSdk 37, minSdk 24, JVM target 11)
- Tests: Kotest (`StringSpec`, `shouldBe`), JUnit Platform, Compose UI tests via `runComposeUiTest` (v2 API).
  `createTestSqlDriver()` has a JVM-only actual, so the suite runs via `:shared:jvmTest`. Reuse the commonTest helpers
  instead of wiring up in-memory DBs per test: `lindenDatabase()` / `createTestSqlDriver()` live in
  `data/TestSqlUtils.kt`; Compose harnesses (`onTestMain`, `withApp`, `withViewModel`, `withAccountViewModel`,
  `withSettingsViewModel`, `withRatesViewModel`, `withEntryPoint`, `withLedgerViewModel`) live in `ui/Utils.kt`.
  JVM tests pin `user.language=en` / `user.country=US` so `formatAmount` assertions are locale-deterministic,
  and run test classes in parallel across up to 4 forked JVMs (`maxParallelForks` in `shared/build.gradle.kts`).
- Configuration cache + build cache enabled (`gradle.properties`).
- Each Composable goes in its own file (e.g. `DayHeader` lives in `DayHeader.kt`, not inside a screen file).
- Comment minimally: only add comments for obscure code which should be very rare.
- After each piece of work is done, scan through for omitted tests.
