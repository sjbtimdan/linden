<!-- Context: project-intelligence/decisions | Priority: high | Version: 2.0 | Updated: 2026-08-29 -->

# Decisions Log

> Record major architectural and business decisions with full context. This prevents "why was this done?" debates.

## Quick Reference

- **Purpose**: Document decisions so future team members understand context
- **Format**: Each decision as a separate entry
- **Status**: Decided | Pending | Under Review | Deprecated

---

## Decision: Money as Integer Minor Units

**Date**: 2026-08-29
**Status**: Decided
**Owner**: Steve

### Context
Money needs to be stored and manipulated without floating-point rounding errors.

### Decision
Store all money as integer minor units (`Long`), never `Double`/`BigDecimal`. `450` = 4.50. All currencies (CHF/EUR/GBP/HKD/JPY/SGD/USD) use a 2-decimal minor unit. `amount` columns in `.sq` files are `INTEGER`.

### Rationale
Floating-point arithmetic introduces rounding errors that would corrupt financial data. Integer minor units are exact and simple.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| `Double` | Simple | Rounding errors | Corrupts financial data |
| `BigDecimal` | Exact | Verbose, not KMP-friendly | Overkill; integer minor units suffice |

### Impact
**Positive**: Exact money handling, no rounding surprises
**Negative**: Must remember to convert (minor units vs. display)
**Risk**: Forgetting to convert when formatting/parsing

### Related
- `technical-domain.md` - Money handling
- `ui/entry/MoneyFormat.kt` - `formatAmount`/`parseAmount`/`formatAmountCompact`

---

## Decision: Entry as Sealed Interface

**Date**: 2026-08-29
**Status**: Decided
**Owner**: Steve

### Context
Entries can be expenses, income, or transfers, each with different fields.

### Decision
`Entry` is a sealed interface (`ExpenseEntry`/`IncomeEntry`/`TransferEntry`) in `model/Entry.kt`. Transfers carry `toAccount`/`toAmount` (`toAmount` is null when both accounts share a currency). Entries carry no currency — it's defined by `account.currency` (`toAccount.currency` for transfers).

### Rationale
A sealed interface gives exhaustive `when` handling and type safety across the codebase.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| Single `Entry` data class with nullable fields | Simpler | Many null checks, no type safety | Sealed interface is cleaner |
| Separate tables per type | Clear | Complex queries, joins | Single `Entry` table with `type` column is simpler |

### Impact
**Positive**: Type-safe handling of each entry type
**Negative**: Adding a field touches all subclass branches plus `Entry.sq` insert/update and `EntryDao` mapping
**Risk**: Forgetting to update all branches when adding a field

### Related
- `model/Entry.kt` - Sealed interface
- `Entry.sq` - SQL schema
- `data/EntryDao.kt` - Mapping

---

## Decision: SQLDelight with Async API

**Date**: 2026-08-29
**Status**: Decided
**Owner**: Steve

### Context
Need a type-safe, KMP-compatible database layer.

### Decision
Use SQLDelight with `generateAsync = true`. Schema creation must be awaited (`createLindenDatabase(driver)` wraps `LindenDatabase.Schema.create(driver).await()`), DB ops are `suspend`, reactive reads use `.asFlow()`/`awaitAsList()`.

### Rationale
SQLDelight provides type-safe SQL with KMP support and an async API that fits the coroutine-based architecture.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| Room | Android-first | No KMP support | KMP is core to the project |
| Raw SQLite | Simple | No type safety, manual mapping | SQLDelight is safer |

### Impact
**Positive**: Type-safe SQL, async API, KMP support
**Negative**: Schema creation must be awaited; no auto-migrations
**Risk**: Editing `.sq` tables without adding `.sqm` migrations breaks the persisted desktop DB

### Related
- `data/DatabaseDriverFactory.kt` - `createLindenDatabase`
- `shared/build.gradle.kts` - SQLDelight config

---

## Decision: Composition Root (AppDependencies)

**Date**: 2026-08-29
**Status**: Decided
**Owner**: Steve

### Context
Need a single place to create and wire all long-lived dependencies.

### Decision
`AppDependencies` (in `AppDependencies.kt`) is the composition root. DAOs, repository and ViewModels are created eagerly; only `httpClient` is `by lazy` (its engine is expensive to build and tests injecting a fake FX source never trigger it). `createAppDependencies(driver)` is a suspend function that reads initial theme/currency/hideEntryTotal from `SettingsDao`.

### Rationale
Eager creation is cheap and `App` touches all ViewModels at first composition anyway. Lazy `httpClient` avoids building the expensive engine in tests.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| DI framework (Hilt/Koin) | Standard | Overkill for this scale | Manual composition root is simpler |
| Per-ViewModel creation | Simple | Duplicated wiring | Composition root centralizes it |

### Impact
**Positive**: Centralized wiring, testable (inject `FakeFxRatesSource`)
**Negative**: None significant
**Risk**: None

### Related
- `AppDependencies.kt` - Composition root
- `AppRoot.kt` - Async bootstrap

---

## Decision: Exact Calculator for Amount Entry

**Date**: 2026-08-29
**Status**: Decided
**Owner**: Steve

### Context
Amount entry needs to handle arithmetic chains like `100 / 3 * 3` exactly (100.00, not 99.99).

### Decision
Use `CalculatorModel`/`AmountCalculator` in `ui/entry`. Arithmetic runs on reduced fractions (Long numerator/denominator, never floats), evaluated left-to-right with no operator precedence, rounded to two decimals only for display and commit.

### Rationale
Fraction arithmetic keeps chains exact; left-to-right evaluation matches a simple calculator UX.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| Float arithmetic | Simple | Rounding errors | `100 / 3 * 3` = 99.99 |
| BigDecimal | Exact | Verbose | Fractions are cleaner |

### Impact
**Positive**: Exact arithmetic, no floating-point surprises
**Negative**: More complex implementation
**Risk**: Overflow (handled with safe arithmetic returning null)

### Related
- `ui/entry/CalculatorModel.kt` - Calculator state machine
- `ui/entry/AmountCalculator.kt` - Arithmetic

---

## Decision: Balance/Total Aggregation in SQL

**Date**: 2026-08-29
**Status**: Decided
**Owner**: Steve

### Context
Need efficient balance and total aggregation across accounts and categories.

### Decision
Push aggregation into SQL (`Entry.sq` `accountDeltas`/`categoryTotals` queries) and convert to the default currency once per currency group.

### Rationale
SQL aggregation is efficient and avoids loading all entries into memory.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| In-memory aggregation | Simple | Loads all entries | SQL is more efficient |

### Impact
**Positive**: Efficient aggregation
**Negative**: Currency conversion logic must be handled in Kotlin
**Risk**: None

### Related
- `Entry.sq` - `accountDeltas`/`categoryTotals` queries
- `data/EntryDao.kt` - DAO methods

---

## Deprecated Decisions

| Decision | Date | Replaced By | Why |
|----------|------|-------------|-----|
| (none) | - | - | - |

## Onboarding Checklist

- [x] Understand the philosophy behind major architectural choices
- [x] Know why certain technologies were chosen over alternatives
- [x] Understand trade-offs that were made
- [x] Know where to find decision context when questions arise
- [x] Understand what decisions are pending and why

## Related Files

- `technical-domain.md` - Technical implementation affected by these decisions
- `business-tech-bridge.md` - How decisions connect business and technical
- `living-notes.md` - Current open questions that may become decisions
