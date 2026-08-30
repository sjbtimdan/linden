<!-- Context: project-intelligence/technical | Priority: high | Version: 2.0 | Updated: 2026-08-30 -->

# Technical Domain

> The technical foundation, architecture, and key decisions of the Linden expenses tracker.

## Quick Reference

- **Purpose**: Understand how the project works technically
- **Update When**: New features, refactoring, tech stack changes
- **Audience**: Developers, DevOps, technical stakeholders

## Primary Stack

| Layer | Technology | Version | Rationale |
|-------|-----------|---------|-----------|
| Language | Kotlin (KMP) | 2.4.10 | Single codebase for Android + Desktop |
| UI | Compose Multiplatform | 1.12.0 (material3 `1.12.0-alpha03`) | Shared UI across platforms |
| Database | SQLDelight | 2.3.2 | Type-safe SQL, async API, KMP support |
| Networking | Ktor | 3.5.2 | FX rates fetch (OkHttp on Android, CIO on JVM) |
| Serialization | kotlinx.serialization | (via Ktor) | JSON for FX rates, backups, Ivy import |
| Testing | Kotest | 6.2.4 | `StringSpec`, `shouldBe`, JUnit Platform |
| Coverage | Kover | 0.9.9 | 50% min gate on JVM variant |
| Linting | Detekt | 2.0.0-alpha.6 | ktlint-wrapper ruleset, formatting only |
| Build | Gradle | 9.7.1 (wrapper) | AGP 9.3.2, KMP plugin |
| Android | AGP | 9.3.2 | compileSdk/targetSdk 37, minSdk 24, JVM target 11 |

## Architecture Pattern

```
Type: KMP shared library + platform entry points
Pattern: MVVM with unidirectional data flow
```

- **`:shared`** — KMP library (targets `jvm()` + `android`) containing ALL UI and business logic.
- **`:androidApp`** — Android entry (`MainActivity`, appId `org.sjbtimdan.linden`).
- **`:desktopApp`** — Desktop/JVM entry (`Main.kt` → Compose `Window`, main class `org.sjbtimdan.linden.MainKt`).

### Why This Architecture?

KMP lets the entire app (UI + business logic) live in one shared module, so Android and Desktop
stay in lockstep. The composition root (`AppDependencies`) creates all DAOs, repository and
ViewModels eagerly; only `httpClient` is `by lazy` (its engine is expensive and tests injecting a
fake FX source never trigger it). ViewModels expose `StateFlow`s; screens collect them with
`collectAsState()`.

## Project Structure

```
linden/
├── shared/                    # KMP library — ALL UI + business logic
│   └── src/
│       ├── commonMain/        # Shared code (Kotlin + .sq files)
│       │   ├── kotlin/org/sjbtimdan/linden/
│   │   │   ├── App.kt             # Screen navigation (sealed Screen; bottom nav: Ledger | Entry | Settings)
│       │   │   ├── AppDependencies.kt # Composition root
│       │   │   ├── AppRoot.kt         # Async bootstrap + loading/error
│       │   │   ├── model/             # Account, Category, Currency, Entry, FxRate, ThemeMode
│       │   │   ├── data/              # DAOs, FxRates, DatabaseDriverFactory
│       │   │   ├── backup/            # LindenBackupManager (JSON dump/restore)
│       │   │   ├── imports/           # IvyImporter (Ivy Wallet backup import)
│       │   │   ├── predictions/       # DescriptionPredictor, FieldPredictor, QuickEntryPredictor
│       │   │   └── ui/                # Screens + ViewModels by feature
│       │   │       ├── entry/         # Entry form, calculator, suggestions
│       │   │       ├── ledger/        # Ledger (entries/accounts/categories views, adjust balance)
│       │   │       ├── accounts/      # Account list, BalanceAdjustment helpers
│       │   │       ├── categories/    # Category list
│       │   │       ├── rates/         # FX rates
│       │   │       ├── settings/      # Settings
│       │   │       └── theme/         # Colors, Theme
│       │   └── sqldelight/            # org/sjbtimdan/linden/*.sq + migrations/ (schema v2;
│       │                              #   1.sqm adds entry amount CHECK constraints)
│       ├── androidMain/       # Android actuals (DatabaseDriverFactory, BackHandler, file pickers)
│       ├── jvmMain/           # JVM actuals (DatabaseDriverFactory, BackHandler, file pickers)
│       ├── commonTest/        # Shared tests (Kotest)
│       └── jvmTest/           # JVM tests (locale-pinned, parallel forks)
├── androidApp/                # Android entry point
├── desktopApp/                # Desktop entry point
├── gradle/libs.versions.toml  # Version catalog
├── detekt.yml                 # Detekt config (ktlint-wrapper)
└── check.sh                   # Runs everything
```

**Key Directories**:
- `shared/src/commonMain/kotlin/org/sjbtimdan/linden/` - All shared code, organized by concern (model/data/backup/imports/predictions/ui)
- `shared/src/commonMain/sqldelight/org/sjbtimdan/linden/` - SQLDelight `.sq` files
- `shared/src/commonMain/sqldelight/migrations/` - `.sqm` migrations (schema v2: `1.sqm` adds entry amount CHECKs)
- `shared/src/commonTest/` - Shared tests (run via `:shared:jvmTest`)

## Key Technical Decisions

| Decision | Rationale | Impact |
|----------|-----------|--------|
| Money as integer minor units (`Long`) | Avoids floating-point errors; `450` = 4.50 | All currencies use 2-decimal minor units; `amount` columns are `INTEGER` |
| `Entry` as sealed interface | `ExpenseEntry`/`IncomeEntry`/`TransferEntry`; transfers carry `toAccount`/`toAmount` | Adding a field touches all subclass branches + `Entry.sq` + `EntryDao` mapping |
| SQLDelight `generateAsync = true` | Async API: schema creation awaited, DB ops `suspend`, reactive reads via `.asFlow()`/`awaitAsList()` | `createLindenDatabase(driver)` wraps `Schema.create(driver).await()` |
| Entry amount CHECK constraints | `Entry.sq`: `amount >= 0`, `to_amount` NULL-or-`>= 0`; `migrations/1.sqm` rebuilds the table (schema v2) | Entries are never negative; DB enforces the invariant (SQLite can't add a CHECK in place) |
| Balance adjustment = fresh entry | `adjustBalance` computes `balanceAdjustment(current, target)`; `adjustmentEntry` builds income/expense dated now, `description = null` | No `is_adjustment` column or per-month update — adjustments are ordinary, visible entries |
| `expect`/`actual` for platform code | `DatabaseDriverFactory`, `BackHandler`, `formatAmount`, file pickers | Per-source-set actuals in `androidMain`/`jvmMain` |
| Exact calculator for amount entry | `CalculatorModel`/`AmountCalculator` on reduced fractions (Long num/den), left-to-right, no precedence | `100 / 3 * 3` = 100.00 exactly; rounded only for display/commit |
| Balance/total aggregation in SQL | `Entry.sq` `accountDeltas`/`categoryTotals` queries | Converted to default currency once per currency group |
| Composition root `AppDependencies` | DAOs/repository/ViewModels created eagerly; `httpClient` lazy | Tests inject `FakeFxRatesSource`, never hit real API |
| Startup async via `AppRoot` | `createAppDependencies(driver)` hops to `Dispatchers.IO` | Shows `CircularProgressIndicator` (`testTag("loading")`) until ready |
| Bottom nav order: Ledger \| Entry \| Settings | Entry is the most-used view; the center slot is easiest to reach with a thumb one-handed | `App.kt` `NavigationBar` item order (app still starts on Entry) |

See `decisions-log.md` for full decision history with alternatives.

## Integration Points

| System | Purpose | Protocol | Direction |
|--------|---------|----------|-----------|
| Frankfurter API (`api.frankfurter.dev/v1/latest`) | FX rates | HTTPS GET over Ktor | Outbound |
| SQLDelight DB | Persistence | SQL (async) | Internal |
| Ivy Wallet backup (ZIP+JSON) | Import | File read | Inbound |
| Linden backup (JSON) | Backup/restore | File read/write | Inbound/Outbound |

## Technical Constraints

| Constraint | Origin | Impact |
|------------|--------|--------|
| Editing an `.sq` table requires a new `.sqm` migration | Tech | Schema is now v2 (`migrations/1.sqm`: entry amount CHECKs); missing migrations break the persisted desktop DB at `~/.linden/linden.db` |
| Accounts may be negative; entries never | Domain invariant | `initial_balance` has no CHECK (liabilities allowed); entry `amount >= 0` is CHECK-enforced since v2 |
| `:shared` Android compiles via `compileAndroidMain`, not `compileDebugKotlin` | AGP 9 KMP plugin | `compileDebugKotlin` only exists on `:androidApp` |
| Kover coverage only on JVM variant | Android runs device tests only | `koverVerifyJvm` (50% min) runs as part of `check` |
| `formatAmountCompact` not parseable by `parseAmount` | Design | Never use it to pre-fill edit fields |
| `accountsWithEntries` blocks currency change | Business rule | Can't change currency of an account with entries |

## Development Environment

```
Setup: ./gradlew build
Requirements: JDK 11+, Android SDK (compileSdk 37)
Local Dev: ./gradlew :desktopApp:run
Testing: ./gradlew :shared:jvmTest (or ./check.sh for everything)
```

## Deployment

```
Environment: Local (no CI/CD deployment)
Platform: Android (assembleDebug) + Desktop (run)
CI/CD: .github/workflows/check.yml runs ./gradlew check
Monitoring: N/A (local app)
```

## Onboarding Checklist

- [x] Know the primary tech stack (KMP, Compose Multiplatform, SQLDelight, Ktor)
- [x] Understand the architecture pattern (MVVM, sealed Screen navigation, composition root)
- [x] Know the key project directories and their purpose
- [x] Understand major technical decisions and rationale (money as minor units, sealed Entry, async SQLDelight)
- [x] Know integration points and dependencies (Frankfurter API, SQLDelight DB)
- [x] Be able to set up local development environment
- [x] Know how to run tests and deploy

## Related Files

- `business-domain.md` - Why this technical foundation exists
- `business-tech-bridge.md` - How business needs map to technical solutions
- `decisions-log.md` - Full decision history with context
- `living-notes.md` - Current gotchas, patterns, and technical debt
