# Linden

An expenses tracker written in KMP for Android and Desktop.

## Codebase Mental Map

Read the project-intelligence files first to understand the codebase without re-reading all the source:

- `.opencode/context/project-intelligence/navigation.md` — overview of the mental map
- `.opencode/context/project-intelligence/technical-domain.md` — stack, architecture, structure, decisions
- `.opencode/context/project-intelligence/business-domain.md` — why the project exists
- `.opencode/context/project-intelligence/business-tech-bridge.md` — business → technical mapping
- `.opencode/context/project-intelligence/decisions-log.md` — key architectural decisions
- `.opencode/context/project-intelligence/living-notes.md` — gotchas, patterns, technical debt (most current; check it before large changes)

`RELEASING.md` covers signing, Play App Signing, and R8 minification.

## Skills

Read `../compose-kotlin-agent-skills/SKILL.md` first, then load the matching reference file
from `references/` for the topic (e.g. `08-kmp-cmp.md`, `01-architecture.md`, `02-compose-ui.md`, `11-testing.md`).
`context7` and `task-management` skills are installed under `.opencode/skills/`; the OAC context system
lives in `.opencode/context/` (standards/workflows under `core/`, loaded via ContextScout).

## MCP

Check if IntelliJ MCP is available (configured in `opencode.json`).
Use it for searching files, compiling and testing as it is faster.
When running tests: if a test configuration is not available (e.g. for jvmTests) then create it.

## Tests

Always run tests at the end of each change. Use `./check.sh` as that runs everything (`./gradlew check`).
During development, prefer `./gradlew fastCheck` (runs `jvmTest` without the Kover gate) or IntelliJ MCP.
Run a single test class with `./gradlew :shared:jvmTest --tests "org.sjbtimdan.linden.ui.entry.EntryPointTest"`.

## Modules

- `:shared` — shared UI and business logic (KMP library, Compose Multiplatform; targets `jvm()` + `android`).
  All UI lives here. `App.kt` owns screen navigation via a sealed `Screen` class with a stable `key`
  (`Entry`/`Ledger`/`Settings`/`CategoryList`/`AccountList`/`Rates`/`Budgets`/`Insights`) in a
  `NavigationBar` scaffold (bottom nav order: Ledger | Entry | Settings; app starts on Entry).
- `:androidApp` — Android entry (`MainActivity`, appId `org.sjbtimdan.linden`, minSdk 24, targetSdk 37, edge-to-edge).
  Firebase/Crashlytics + Analytics are wired here only (`google-services.json`, BoM, plugins applied on the module —
  not `apply false`). Desktop has no crash reporting.
- `:desktopApp` — Desktop/JVM entry (`Main.kt` → Compose `Window`, main class `org.sjbtimdan.linden.MainKt`).

## Commands

```
./gradlew :shared:compileKotlinMetadata     # quick verify shared compiles
./gradlew :shared:compileKotlinJvm          # verify shared JVM target
./gradlew :shared:compileAndroidMain        # verify shared Android target
./gradlew :androidApp:compileDebugKotlin    # verify Android app
./gradlew :desktopApp:compileKotlin         # verify Desktop app
./gradlew :shared:jvmTest                   # run Kotest suite (commonTest + jvmTest)
./gradlew fastCheck                         # jvmTest only, no Kover gate — fast dev loop
./gradlew :androidApp:assembleDebug         # full Android debug build
./gradlew :desktopApp:run                   # run Desktop app
./gradlew check                             # full check — CI runs this too (.github/workflows/check.yml)
./gradlew detekt                            # formatting check (ktlint ruleset, all modules)
./gradlew detekt --auto-correct             # auto-fix formatting violations
./gradlew :desktopApp:renderIcon            # regenerate master icon (build/icon-render/) + Play assets (docs/store-assets/)
./gradlew dependencyUpdates -Drevision=release  # check newer dependency versions (report in build/)
```

Formatting is enforced by Detekt (plugin `dev.detekt`) with the ktlint-wrapper ruleset — see
`detekt.yml` at the repo root and `.editorconfig` (max line 140, 4-space indent, trailing commas allowed).
`./gradlew check` runs the per-module `detekt` tasks. There is no pre-commit hook (`git config core.hooksPath`
still points at `.githooks`, but that directory doesn't exist), so run `./gradlew detekt --auto-correct` yourself after edits.

The `:shared` Android target compiles via `compileAndroidMain`, **not** `compileDebugKotlin`
(that task only exists on `:androidApp`). `:shared` uses the AGP 9 `com.android.kotlin.multiplatform.library` plugin
and also defines an instrumentation test builder (`androidx.test.runner.AndroidJUnitRunner`) — device-only tests.

`./gradlew check` also runs the Kover coverage gate (`koverVerifyJvm`, 50% min) on the shared JVM variant —
coverage is only enforced there because the Android target runs device tests only, which Kover doesn't support.

`shared/build.gradle.kts` has a `generateBuildInfo` task that emits `BuildInfo.kt` into commonMain (version +
git describe). It is generated code — never edit it.

## Architecture & Gotchas

- Package root `org.sjbtimdan.linden`. Models in `.model`, DAOs in `.data`, screens/ViewModels in `.ui.<feature>`,
  backup/restore in `.backup` (`LindenBackupManager` — zipped JSON dump of all tables, invoked from Settings),
  CSV export in `.export` (`CsvExporter`), import in `.imports` (`IvyImporter`), entry prediction in `.predictions`
  (`DescriptionPredictor`, `FieldPredictor`, `QuickEntryPredictor`, `RecurringDetector` — heuristic scoring of past
  entries, pure functions, no DB) consumed via `EntrySuggestionsProvider` in `ui/entry`. Kotlin source files use PascalCase.
- **Time is always injected, never read statically** (since 2026-09-12): new code that needs "now"/"today" takes
  `clock: AppClock = SystemClock` (`time/AppClock.kt`: `now(): Instant` + `todayIn(zone): LocalDate`) and must not call
  `Clock.System.now()`/`todayIn` directly. Tests inject `FakeClock` (mutable `now`/`today`, shared `TEST_NOW`).
  Zone reads (`TimeZone.currentSystemDefault()`) stay static — only *time* is injected.
- **State must survive configuration changes** (since 2026-09-10): `App` keeps the active `Screen` in
  `rememberSaveable` with `ScreenSaver`; `AppRootViewModel` owns the `AppDependencies` composition root for the host
  lifetime. `AppDependencies.close()` (cancel `appScope`, close the lazily-built `httpClient`) is called from
  `AppRootViewModel.onCleared`, **not** a `DisposableEffect` (which would fire on every rotation and hand the app a
  closed client). `MainActivity` passes `applicationContext` to `DatabaseDriverFactory` so the retained ViewModel never
  leaks the activity. A plain `remember` in either place resets navigation and loses the in-progress `EntryDraft`.
- **All UI copy lives in resources** (`shared/src/commonMain/composeResources/values/strings.xml`; per-locale dirs
  `values-it`, `values-zh`, `values-zh-rCN`, `values-zh-rHK`). Use `stringResource(Res.string.key)` /
  `pluralStringResource(Res.plurals.key, qty, qty)` — **repeat the quantity as the `%1$d` arg**. Accessors are
  top-level extensions on `Res.string`/`Res.plurals` and need explicit per-key imports (no wildcards). Never call
  `stringResource` from non-composable lambdas (resolve to a `val` first). `testTag`s and user data are never resources.
  `LocaleFileParityTest` (jvmTest) gates locale parity: every locale must define the default's keys with matching
  placeholder counts. Dates render from `DateLanguage` tables, not the raw locale; app language is `AppLanguage`
  (pin stored in `SettingsDao` under `language`, absent = follow system). See living-notes for the full i18n rules.
- **First-run flow**: `AppRoot`/`AppRootViewModel` start in `Loading`, then `FirstRun` (no default currency set) or
  `Ready`. `FirstRunScreen` prompts for a currency, then `completeFirstRun(currency)` writes it and triggers
  `DefaultDataSeeder` (default categories/accounts). Nothing is written at startup otherwise. `testTag("loading")`
  marks the spinner; startup failure shows `StartupError`.
- Money is stored as integer minor units (`Long`), never `Double`/`BigDecimal` — `450` = 4.50. All currencies
  (CHF/EUR/GBP/HKD/JPY/SGD/USD) use a 2-decimal minor unit. `formatAmount` in `ui/entry/MoneyFormat.kt` is an
  `expect`/`actual` using the platform locale (`java.text.NumberFormat`, thousands grouping); `parseAmount` is pure
  common code that accepts grouped input ("1,000", "1.000", "1 000"). `formatAmountCompact` (pure common code) shortens
  read-only displays of amounts ≥ 1,000,000.00 to "1.25m"/"1.235b" (fixed '.', trimmed zeros, half-up rounding);
  never use it to pre-fill edit fields — `parseAmount` can't parse the suffix. `amount` columns in `.sq` files are `INTEGER`.
  `parseAmount` accepts a leading `-` (negative balance/liability); accounts may be negative, entries never are (CHECK-enforced).
- Adjust Balance (accounts view of the ledger): `LedgerViewModel.adjustBalance` reconciles an account to a target
  balance by creating a fresh income/expense entry per adjustment (positive delta → income, negative delta → expense,
  dated now, `description = null`) — pure helpers in `ui/accounts/BalanceAdjustment.kt`. No `is_adjustment` column,
  no per-month update logic: an adjustment is just an ordinary entry.
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
- SQLDelight schema is version 1 with no migrations (pre-release migrations were collapsed). Editing an `.sq`
  table requires deleting the local DB (`~/.linden/linden.db` on desktop); there are no `.sqm` migrations to bump.
  SQLite FKs are deliberately **not** declared/enforced — relationships are guarded in app code (`accountsWithEntries`,
  `requireNotNull` in `toEntry`). If FKs are ever added, `PRAGMA foreign_keys = ON` must be set in both driver actuals.
- `AppDependencies` (from `AppDependencies.kt`) is the composition root: DAOs, repository and ViewModels are created
  eagerly, only `httpClient` is `by lazy` (its engine is expensive to build and tests injecting a fake FX source never
  trigger it). Theme + default currency are read from `SettingsDao` with fallbacks `ThemeMode.SYSTEM` / `Currency.CHF`.
- FX rates power cross-currency totals: `FxRatesFetcher` (in `.data`) calls Frankfurter
  (`api.frankfurter.dev/v1/latest`) over Ktor (OkHttp on Android, CIO on JVM), responses decode via
  `parseFxRatesResponse` (kotlinx.serialization). Rates refresh only when the cached rates are older than 24 hours
  (`App.kt` `LaunchedEffect` → `ratesViewModel.refreshRatesIfStale`), cached in
  `FxRateEntity` (`FxRateDao`), consumed via `FxRatesRepository`; `RatesFlowProvider` exposes default currency + rates
  as `StateFlow`s. Tests inject `FakeFxRatesSource` — never hit the real API.
- `DatabaseDriverFactory` is an `expect class` (shared enables `-Xexpect-actual-classes`), with per-source-set
  actuals at `shared/src/{androidMain,jvmMain}/.../DatabaseDriverFactory.kt`. Desktop persists to `~/.linden/linden.db`;
  Android uses `AndroidSqliteDriver` (its `onDowngrade` is a no-op, so a DB stamped by newer code opens instead of crashing).
- `ui/BackHandler` is a `@Composable expect`: the Android actual wires `androidx.activity.compose.BackHandler` so the
  system back cancels in-progress edits; the JVM actual is a no-op. `ui/AppLocale` (apply-language override) is also
  `expect`/`actual`.
- Balance/total aggregation is pushed into SQL (`Entry.sq` `accountDeltas` / `categoryTotals` queries) and converted
  to the default currency once per currency group. `accountsWithEntries` blocks changing the currency of an account
  that has entries.
- `IvyImporter` replaces all rows in one transaction, infers `CategoryType` (Expense/Income/Both) from usage, maps
  "initial balance"/"adjust balance" titles onto `Account.initialBalance`, and routes currency-mismatched transactions
  to split accounts named `IVY: <name> (<currency>)`.
- Ledger has three view modes (`LedgerViewMode`: Entries/Accounts/Categories) with period navigation, search and
  type filters; "nothing in the future" and "balance at period end" rules are enforced in `LedgerViewModel`. The
  show-future toggle/notice is window-relative: it appears only when the shown window still has days ahead of today
  (`windowStart <= today < windowEnd`, or unbounded `All`); a wholly past or wholly future window shows all rows
  with no toggle (`LedgerViewModel.hideFutureBound` is the single date cutoff).
- Insights (`ui/insights`) pages a 12-month expense/income trend chart and breaks a selected month down by category;
  Budgets live in `ui/budget` (`BudgetScreen`, `CategoryBudget`).

## Conventions

- Conventional commits
- Version catalog at `gradle/libs.versions.toml`
- Kotlin 2.4.20, Compose Multiplatform 1.12.0 (material3 pinned separately at `1.12.0-alpha03`), AGP 9.4.0,
  SQLDelight 2.3.2, Ktor 3.5.2, Kotest 6.2.4, Kover 0.9.9, Detekt 2.0.0-alpha.6, Firebase BoM 34.19.0
  (Gradle wrapper 9.7.1; compileSdk/targetSdk 37, minSdk 24, JVM target 11)
- **Every new class ships with a unit test**, unless it is pure configuration (constants/values with no behavior).
- Tests: Kotest (`StringSpec`, `shouldBe`), JUnit Platform, Compose UI tests via `runComposeUiTest` (v2 API).
  `createTestSqlDriver()` has a JVM-only actual, so the suite runs via `:shared:jvmTest`. Reuse the commonTest helpers
  instead of wiring up in-memory DBs per test: `lindenDatabase()` / `createTestSqlDriver()` live in
  `data/TestSqlUtils.kt`; Compose harnesses (`onTestMain`, `withApp`, `withViewModel`, `withAccountViewModel`,
  `withSettingsViewModel`, `withRatesViewModel`, `withEntryPoint`, `withLedgerViewModel`) live in `ui/Utils.kt`.
  JVM tests pin `user.language=en` / `user.country=US` so `formatAmount` assertions are locale-deterministic,
  and run test classes in parallel across `maxParallelForks = availableProcessors` (`shared/build.gradle.kts`).
- Configuration cache + build cache enabled (`gradle.properties`).
- Each Composable goes in its own file (e.g. `DayHeader` lives in `DayHeader.kt`, not inside a screen file).
- Comment minimally: only add comments for obscure code which should be very rare.
- After each piece of work is done, scan through for omitted tests.
