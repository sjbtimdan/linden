<!-- Context: project-intelligence/notes | Priority: high | Version: 2.0 | Updated: 2026-08-29 -->

# Living Notes

> Active issues, technical debt, open questions, and insights that don't fit elsewhere. Keep this alive.

## Quick Reference

- **Purpose**: Capture current state, problems, and open questions
- **Update**: Weekly or when status changes
- **Archive**: Move resolved items to bottom with status

## Technical Debt

| Item | Impact | Priority | Mitigation |
|------|--------|----------|------------|
| No SQLDelight migrations (schema version 1) | Editing `.sq` tables won't auto-migrate the persisted desktop DB at `~/.linden/linden.db` | High | Add a `.sqm` migration, or delete the local DB |
| `:shared` Android compiles via `compileAndroidMain`, not `compileDebugKotlin` | Confusing for new devs | Low | Documented in AGENTS.md |
| Kover coverage only on JVM variant | Android target runs device tests only | Low | `koverVerifyJvm` (50% min) runs as part of `check` |

### Technical Debt Details

**No SQLDelight Migrations**  
*Priority*: High  
*Impact*: Editing an `.sq` table won't auto-migrate the persisted desktop DB at `~/.linden/linden.db`  
*Root Cause*: Schema version 1, no `.sqm` migration files  
*Proposed Solution*: Add `.sqm` migrations when schema changes  
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

### What Could Be Better
- **No SQLDelight migrations** - Schema changes require manual DB deletion
- **`formatAmountCompact` not parseable** - Can't pre-fill edit fields with it

### Lessons Learned
- **Money is always integer minor units** - Never `Double`/`BigDecimal`
- **`formatAmountCompact` is display-only** - `parseAmount` can't parse "1.25m"
- **`compileAndroidMain` for `:shared`** - `compileDebugKotlin` only exists on `:androidApp`
- **Tests inject `FakeFxRatesSource`** - Never hit the real Frankfurter API in tests

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
- **SQLDelight async** - Schema creation must be awaited; DB ops are `suspend`
- **No pre-commit hook** - Run `./gradlew detekt --auto-correct` after edits
- **JVM tests pin locale** - `user.language=en` / `user.country=US` so `formatAmount` assertions are deterministic

## Active Projects

| Project | Goal | Owner | Timeline |
|---------|------|-------|----------|
| (none) | - | - | - |

## Archive (Resolved Items)

Moved here for historical reference. Current team should refer to current notes above.

### Resolved: (none)
- **Resolved**: -
- **Resolution**: -
- **Learnings**: -

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
