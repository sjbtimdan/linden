<!-- Context: project-intelligence/notes | Priority: high | Version: 2.0 | Updated: 2026-08-30 -->

# Living Notes

> Active issues, technical debt, open questions, and insights that don't fit elsewhere. Keep this alive.

## Quick Reference

- **Purpose**: Capture current state, problems, and open questions
- **Update**: Weekly or when status changes
- **Archive**: Move resolved items to bottom with status

## Technical Debt

| Item | Impact | Priority | Mitigation |
|------|--------|----------|------------|
| Every `.sq` DDL change needs a new `.sqm` migration | Schema is now v2 (`migrations/1.sqm`: entry amount CHECKs); forgetting a migration breaks the persisted desktop DB at `~/.linden/linden.db` | Medium | Add a `.sqm` migration for table changes |
| `:shared` Android compiles via `compileAndroidMain`, not `compileDebugKotlin` | Confusing for new devs | Low | Documented in AGENTS.md |
| Kover coverage only on JVM variant | Android target runs device tests only | Low | `koverVerifyJvm` (50% min) runs as part of `check` |

### Technical Debt Details

**SQLDelight migrations are manual**  
*Priority*: Medium  
*Impact*: Editing an `.sq` table without a matching `.sqm` breaks the persisted desktop DB at `~/.linden/linden.db`  
*Root Cause*: Migrations exist (schema v2) but must be authored by hand  
*Proposed Solution*: Keep adding `.sqm` files whenever a table changes  
*Effort*: Small  
*Status*: Acknowledged

## Open Questions

| Question | Stakeholders | Status | Next Action |
|----------|--------------|--------|-------------|
| (none) | - | - | - |

## Known Issues

| Issue | Severity | Workaround | Status |
|-------|----------|------------|--------|
| (none) | - | - | - |

## Insights & Lessons Learned

### What Works Well
- **Integer minor units for money** - Exact, no floating-point surprises
- **Sealed `Entry` interface** - Type-safe handling of expense/income/transfer
- **SQLDelight async API** - Fits the coroutine-based architecture
- **Composition root (`AppDependencies`)** - Centralized wiring, testable
- **Exact calculator** - `100 / 3 * 3` = 100.00, not 99.99
- **Predictions** - Heuristic scoring of past entries, pure functions, no DB
- **Entry amount CHECK constraints** - DB-level guarantee that entries are never negative
- **Adjust balance as ordinary entries** - Reconciliation just creates income/expense, no hidden state

### What Could Be Better
- **Manual SQLDelight migrations** - Schema v2 exists but every `.sq` DDL change needs a hand-written `.sqm`
- **`formatAmountCompact` not parseable** - Can't pre-fill edit fields with it

### Lessons Learned
- **Money is always integer minor units** - Never `Double`/`BigDecimal`
- **`formatAmountCompact` is display-only** - `parseAmount` can't parse "1.25m"
- **`compileAndroidMain` for `:shared`** - `compileDebugKotlin` only exists on `:androidApp`
- **Tests inject `FakeFxRatesSource`** - Never hit the real Frankfurter API in tests
- **`parseAmount` accepts negatives** - A leading `-` parses to negative minor units (liabilities); entry amounts are never negative
- **No FK constraints (deliberate)** - Relationships are enforced in app code (`accountsWithEntries` guard, `requireNotNull` in `toEntry`); SQLite FKs are off by default and would need a rebuild migration

## Patterns & Conventions

### Code Patterns Worth Preserving
- **Each Composable in its own file** - e.g. `DayHeader` lives in `DayHeader.kt`, not inside a screen file
- **Comment minimally** - Only add comments for obscure code
- **Conventional commits** - Standard commit message format
- **Version catalog** - `gradle/libs.versions.toml`
- **Kotlin source files use PascalCase** - e.g. `EntryDao.kt`, not `entry_dao.kt`

### Gotchas for Maintainers
- **`formatAmountCompact`** - Never use it to pre-fill edit fields; `parseAmount` can't parse the suffix
- **`accountsWithEntries`** - Blocks changing the currency of an account that has entries
- **`Entry` sealed interface** - Adding a field touches all subclass branches plus `Entry.sq` insert/update and `EntryDao` mapping
- **Adjust balance entries have `description = null`** - Reconciliation entries are ordinary entries with no marker, so they appear in the ledger without a description
- **SQLite FK enforcement is off by default** - If FKs are ever added, `PRAGMA foreign_keys = ON` must be set in both `DatabaseDriverFactory` actuals (Android + JVM) or the constraints are decorative
- **Accounts can be negative, entries never** - `parseAmount` handles `-` (liabilities/negative balances); entry `amount >= 0` is CHECK-enforced (schema v2)
- **SQLDelight async** - Schema creation must be awaited; DB ops are `suspend`
- **No pre-commit hook** - Run `./gradlew detekt --auto-correct` after edits
- **JVM tests pin locale** - `user.language=en` / `user.country=US` so `formatAmount` assertions are deterministic

## Active Projects

| Project | Goal | Owner | Timeline |
|---------|------|-------|----------|
| (none) | - | - | - |

## Archive (Resolved Items)

Moved here for historical reference. Current team should refer to current notes above.

### Resolved: No SQLDelight migrations existed (schema version 1)
- **Resolved**: 2026-08-30
- **Resolution**: Added `sqldelight/migrations/1.sqm` (v1→v2), rebuilding `EntryEntity` with CHECK constraints on `amount >= 0` and `to_amount` (NULL-or-`>= 0`)
- **Learnings**: SQLite can't add a CHECK in place — a table rebuild (rename → create → copy → drop) is the migration pattern to follow

## Onboarding Checklist

- [x] Review known technical debt and understand impact
- [x] Know what open questions exist and who's involved
- [x] Understand current issues and workarounds
- [x] Be aware of patterns and gotchas
- [x] Know active projects and timelines
- [x] Understand the team's priorities

## Related Files

- `decisions-log.md` - Past decisions that inform current state
- `business-domain.md` - Business context for current priorities
- `technical-domain.md` - Technical context for current state
- `business-tech-bridge.md` - Context for current trade-offs
