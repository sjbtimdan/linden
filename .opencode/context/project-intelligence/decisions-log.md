<!-- Context: project-intelligence/decisions | Priority: high | Version: 2.3 | Updated: 2026-11-11 -->

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
Store all money as integer minor units (`Long`), never `Double`/`BigDecimal`. `450` = 4.50. Every currency (CHF/CNY/EUR/GBP/HKD/IDR/INR/JPY/SGD/USD) is stored at a fixed 2-decimal scale; `Currency.decimalDigits` is the display/input precision (0 for zero-decimal IDR/JPY, 2 for the rest), so zero-decimal amounts render and parse as whole units while totals and FX math stay exact. `amount` columns in `.sq` files are `INTEGER`.

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
**Negative**: Schema creation must be awaited; DDL changes require deleting the local DB (schema v1, no migrations)
**Risk**: Editing `.sq` tables without deleting the persisted desktop DB breaks it

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
- `ui/entry/AmountCalculator.kt` - Keypad shell and calculator UI

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

## Decision: Balance Adjustments Create Fresh Entries

**Date**: 2026-08-30
**Status**: Decided
**Owner**: Steve

### Context
The accounts view offers "adjust balance" to reconcile a tracked account with its real bank balance.

### Decision
Each adjustment creates a **fresh, ordinary entry**: `LedgerViewModel.adjustBalance` computes
`balanceAdjustment(current, target)` and `adjustmentEntry` builds the delta as an income entry (delta > 0) or
expense entry (delta < 0), dated now, `description = null`, amount = absolute delta. Zero delta creates nothing.
There is no `is_adjustment` column, no per-month update logic, and no "Balance adjustment" description string —
an adjustment is just an entry.

### Rationale
A reconciliation is a real financial event; modeling it as an ordinary entry keeps the history simple, inspectable
and editable, with no hidden state to maintain or migrate.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| One adjustment entry per month, updated in place | Single entry per account | Needs a marker query + update-on-edit logic | Fresh entry per adjustment is simpler ("a balance adjustment just creates an entry") |
| `is_adjustment` column / hidden marker | Filters could exclude adjustments | Column, migration, all branches | No special treatment wanted |
| `description = "Balance adjustment"` | Human-readable in ledger | String-matching hacks to find adjustments | Entries already look fine without it; description stays null |

### Impact
**Positive**: Simple, immutable history; no hidden state; adjustments show up (and can be deleted) like any entry
**Negative**: `description = null` shows a blank description in the ledger for adjustment entries
**Risk**: None significant

### Related
- `ui/ledger/LedgerViewModel.kt` - `adjustBalance`
- `ui/accounts/BalanceAdjustment.kt` - `balanceAdjustment`/`adjustmentEntry`

---

## Decision: Accounts May Be Negative; Entries Never Are

**Date**: 2026-08-30
**Status**: Decided
**Owner**: Steve

### Context
Accounts can represent liabilities (credit cards, loans), so balances — including `initial_balance` — must be
allowed to go below zero. But an individual entry always has a type (expense vs income), so its amount is never negative.

### Decision
`parseAmount` accepts a leading `-` (returns negative minor units) so account dialogs can enter negative
initial balances; the account dialog warns instead of silently truncating an invalid value. `Account.sq`
`initial_balance` has no CHECK. Entry amounts are enforced non-negative in the DB: `Entry.sq` declares
`amount INTEGER NOT NULL CHECK (amount >= 0)` and `to_amount` `CHECK (to_amount IS NULL OR to_amount >= 0)`,
declared directly in `Entry.sq` (SQLite cannot add a CHECK in place, so the constraint lives in the
schema from the start).

### Rationale
Liabilities are first-class data (negative balances must be representable), while an entry's sign lives in its
`type`, so a negative entry amount would be meaningless and should be impossible.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| Reject negative input everywhere | Simple | Can't model liabilities | Liabilities are real |
| Allow negative entry `amount` | Flexible | Ambiguity with `type`, corrupts totals | Sign belongs in `type`, not amount |

### Impact
**Positive**: DB-enforced invariant (entries never negative); accounts can model debts
**Negative**: App/UI layer still guards against logical misuse
**Risk**: Editing `Entry.sq` amounts later requires deleting the local DB (no migrations)

### Related
- `ui/entry/MoneyFormat.kt` - `parseAmount` (accepts `-`)
- `Entry.sq` - CHECK constraints
- `model/Entry.kt` - sealed `ExpenseEntry`/`IncomeEntry`/`TransferEntry`

---

## Decision: Bottom Navigation Order (Ledger | Entry | Settings)

**Date**: 2026-08-30
**Status**: Decided
**Owner**: Steve

### Context
The bottom navigation bar has three destinations (Entry, Ledger, Settings). Entry is the most-used view —
the primary screen and the one most likely to be used single-handedly on a phone.

### Decision
Order the bottom navigation as **Ledger | Entry | Settings**: Ledger on the left, Entry in the center,
Settings on the right. The app still starts on Entry.

### Rationale
The center slot is the easiest to reach with a thumb when holding the phone one-handed, so the most-used
view (Entry) sits there. Ledger takes the left slot, Settings the right.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| Keep Entry first (Entry \| Ledger \| Settings) | Matches startup screen | Left edge is the hardest to reach one-handed | Center is the most thumb-friendly slot |
| Entry center, Ledger right | Entry centered | Ledger is a frequent destination; right reads less naturally for browsing | Ledger on the left is the better "review" position |

### Impact
**Positive**: One-handed thumb reach for the most-used view
**Negative**: None significant
**Risk**: None

### Related
- `App.kt` - `NavigationBar` item order

---

## Decision: No Foreign Key Constraints in the Database

**Date**: 2026-09-02
**Status**: Decided
**Owner**: Steve

### Context
The schema has three inter-table references — `EntryEntity.account_id` and `EntryEntity.to_account_id` →
`AccountEntity.id`, and `EntryEntity.category_id` → `CategoryEntity.id` — but no FK constraints. Referential
integrity is enforced entirely in application code.

### Decision
Do **not** add foreign key constraints. Keep enforcing relationships in the ViewModel/DAO layer:
`accountsWithEntries` blocks deleting an account that has entries (and changing its currency); categories are
never individually deleted (no `deleteById` in `Category.sq`); `EntryDao.toEntry` fails loudly via
`requireNotNull` if a dangling reference is ever read.

### Rationale
- SQLite does **not** enforce FKs unless `PRAGMA foreign_keys = ON` is set per connection; SQLDelight doesn't
  enable it by default, so constraints would be decorative unless both `DatabaseDriverFactory` actuals
  (Android + JVM) are updated — a classic false-confidence trap.
- Adding FKs requires a table-rebuild migration (SQLite can't add an FK in place) — real work and risk for a safety net the code already provides.
- `ON DELETE CASCADE` would silently delete entries when an account is deleted, contradicting the deliberate
  "block deletion of accounts with entries" behavior.
- Backup restore and Ivy import already insert parents-before-children and delete children-before-parents, but
  with enforced FKs that ordering becomes a hard requirement rather than a convenience.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| FKs with `ON DELETE CASCADE` | DB-enforced integrity | Silently deletes entries on account delete | Contradicts the deliberate delete guard |
| FKs without cascade | Write-time safety net | Needs `PRAGMA foreign_keys = ON` in both drivers + rebuild migration + strict insert/delete ordering | Cost without behavior change; code already enforces the invariants |
| Code-level enforcement (chosen) | No migration risk, no pragma gotcha | Schema doesn't self-document relationships | Simplest; a future orphan bug would be caught at read time |

### Impact
**Positive**: No migration risk, no pragma gotcha, no ordering fragility in bulk paths
**Negative**: Schema doesn't self-document relationships; a future app-code bug could create orphans (caught at read time by `requireNotNull`)
**Risk**: If individual account/category deletion is ever added, revisit FKs (with `PRAGMA foreign_keys = ON` in both drivers + a rebuild migration + a test proving enforcement)

### Related
- `Entry.sq` / `Account.sq` / `Category.sq` - Schema (no FK declarations)
- `ui/accounts/AccountListViewModel.kt` - `accountsWithEntries` delete/currency guard
- `data/EntryDao.kt` - `toEntry` `requireNotNull` on dangling references
- `backup/LindenBackupManager.kt`, `imports/IvyImporter.kt` - Parent-before-child insert ordering

---

## Decision: Backup Strategy — Android Auto Backup + Manual Export

**Date**: 2026-10-01
**Status**: Superseded (2026-10-18)
**Owner**: Steve

> Superseded by **Android Auto Backup Disabled** below. Auto Backup was the right
> trade-off before payment capture; notification-derived data changes the privacy calculus.

### Context
Users need their data to survive device loss. The alternative considered was a built-in auto-backup that writes a
backup to a user-chosen external folder (e.g. Google Drive via SAF) on a daily schedule, with Always / Wi-Fi Only /
Never options and in-app error surfacing. The app already has manual backup/restore (zipped JSON dump of all tables)
and CSV export in Settings, and `androidApp` already declares `android:allowBackup="true"`.

### Decision
Rely on Android Auto Backup (already enabled by `android:allowBackup="true"`) instead of building a custom
external-folder auto-backup feature. Settings → Back up database / Restore / Export to CSV remain the only
user-visible, cross-platform backup paths.

### Rationale
- Auto Backup covers the whole app data set — `databases/linden.db` (entries, accounts, categories, settings, FX
  rates, budgets) plus shared preferences — with no app code.
- It runs automatically (≥24h cadence, idle + charging + Wi-Fi), and the system shuts the app down during backup,
  so there is no torn-database-snapshot risk to handle.
- Restore happens on (re)install — Play, adb, or device setup — before first launch, which the app's first-run
  currency gate already handles.
- A custom implementation would need SAF persisted tree permissions, a network policy, scheduling/idempotence,
  retention, and error UI — significant machinery for a worse outcome (silent, maintenance-free backup is exactly
  what Auto Backup provides).

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| Custom auto-backup to external folder (SAF / Drive) | Visible files, user-controlled destination, restorable on demand | SAF permissions, network gating, scheduling, retention, error UI, concurrency work | Auto Backup already provides the core value for free |
| Google Drive REST API + OAuth | Full control | OAuth setup, Play Console config, KMP-unfriendly SDK | Overkill; a SAF variant would still be needed for desktop |
| WorkManager background backup | Runs without app open | Headless DB + DI + same concurrency work | Unnecessary; device-loss recovery is covered |

### Impact
**Positive**: Zero code; silent, OS-maintained device-loss recovery; DB and prefs covered; no race conditions to
manage; manual export remains for cross-platform/manual use
**Negative**: No user-visible backup file; no error surfacing (failures are silent); restore is install-time only,
latest-backup-only, all-or-nothing; Android-only; backups are keyed to the signing certificate (Play App Signing
keeps release consistent; debug builds differ)
**Risk**: If a future requirement demands on-demand/visible restore or desktop coverage, the custom feature must be
built; revisit this decision then

### Related
- `androidApp/src/main/AndroidManifest.xml` - was `android:allowBackup="true"` (historical; now `false`)
- `backup/LindenBackupManager.kt` - Manual zipped-JSON backup/restore
- `ui/settings/SettingsScreen.kt` - Backup / CSV export actions
- [Auto Backup](https://developer.android.com/identity/data/autobackup) / [Testing](https://developer.android.com/identity/data/testingbackup)

---

## Decision: Android Auto Backup Disabled

**Date**: 2026-10-18
**Status**: Decided
**Owner**: Steve

### Context
Payment capture stores notification-derived data — the pending-capture inbox and learned templates — in the
same SQLite database as financial data. Android Auto Backup would upload that whole database to the user's Google
Drive whenever device backup is enabled. The privacy policy now states that Linden opts out of Android Auto Backup
and that notification content is not uploaded by Linden.

### Decision
Set `android:allowBackup="false"` in `androidApp/src/main/AndroidManifest.xml`. Linden data stays on-device;
users create explicit backups through **Settings → Back up database** (or CSV export) and choose where to save them.

### Rationale
- Auto Backup cannot selectively exclude the capture rows: the capture inbox and templates live in `linden.db`
  alongside the financial data, so the only file-level choices are "back up everything" or "back up nothing".
- Keeping `allowBackup="true"` would contradict the privacy policy's promise that Linden does not upload data to
  Google Drive.
- Manual backup/restore already exists, covers all tables, and works on both Android and desktop.
- This matches the privacy posture of peers: Ivy Wallet also sets `allowBackup="false"`; MoneyWallet allows
  Auto Backup only when client-side encryption is available.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| Encryption-gated Auto Backup (`dataExtractionRules` with `disableIfNoEncryptionCapabilities="true"` and `fullBackupContent` with `requireFlags="clientSideEncryption"`) | Keeps install-time recovery; avoids unencrypted cloud copies; used by MoneyWallet | Still uploads the whole DB (including notification data) to Google Drive when encryption is available; contradicts the current privacy policy; needs XML rules + testing | **Not now.** It would technically violate the privacy policy as written; revisit only after an explicit policy change |
| Keep Auto Backup and move capture data to an excluded store (SharedPreferences/file) | Financial data can still be auto-backed up without notification data | Refactor: capture keys move out of the settings table; backup/restore, tests and the listener all need updating | Deferred; larger change, not needed for 1.2.0 |
| Disable Auto Backup (chosen) | Strongest privacy; simple; policy and code agree | A lost phone without a manual export loses data; no automatic install-time restore | Accepted for 1.2.0 |
| Custom cloud sync | Visible, cross-platform, account-based | Server, accounts, conflict resolution, scope | Contradicts the local-first model |

### Impact
**Positive**: No automatic Google Drive copy; notification-derived data stays on-device; code and policy agree.
**Negative**: Android users lose install-time Auto Backup; the manual backup action is now the only recovery path.
**Risk**: Users who never make a backup can lose data on device loss. Keep **Settings → Back up database**
discoverable and consider prompting for a backup after significant changes.

### Related
- `androidApp/src/main/AndroidManifest.xml` - `android:allowBackup="false"`
- `docs/privacy-policy.md` - Notification Access / Data Storage / Data Sharing
- `backup/LindenBackupManager.kt` - Manual zipped-JSON backup/restore
- `ui/settings/SettingsScreen.kt` - Back up database / Restore / CSV export
- `Decision: Backup Strategy — Android Auto Backup + Manual Export` (superseded)

---

## Decision: Enforce Cheap Architectural Rules With Source-Scan Tests

**Date**: 2026-11-11
**Status**: Decided
**Owner**: Steve

### Context
`AGENTS.md` and `living-notes.md` carry 100+ dated rules. Prose does not fail the build, so a rule like "time is always injected" or "money is never floating point" erodes silently the first time someone writes `Clock.System.now()` or adds a `Double` amount.

### Decision
A rule that can be checked cheaply and without ambiguity becomes a test in `shared/src/jvmTest/kotlin/org/sjbtimdan/linden/ArchitectureInvariantsTest.kt`, which scans source text and reports `path:line: code`. Currently enforced: no `Clock.System` outside `time/AppClock.kt`, no `Double`/`Float`/`BigDecimal` in `model/`, no `REAL` column in any `.sq` (`FxRate` allowlisted — a rate is not money). A meta-test asserts the scans still see the expected number of files, so they cannot pass vacuously.

Rules already covered by behaviour tests (zero-decimal parsing, adjustment-entry shape, show-future window) are not duplicated. Rules that are not truly held by the codebase are not encoded: "one Composable per file" is already false (20 files declare two or more), so the docs were corrected to the practised rule instead.

### Rationale
A text scan is ~80 lines, needs no build plugin, and runs in the existing `jvmTest` task, so the rule fails on the same `./gradlew check` as everything else. A custom detekt rule needs its own module, rule jar and tests — more build surface than the checks it replaces.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| Custom detekt rule module | Integrated with existing static analysis | New module, rule jar on the detekt classpath, own tests | More build surface than the scans |
| Keep prose only, review manually | Zero build cost | Already failed in practice; rules drift silently | The whole point of the decision |
| Encode "one Composable per file" too | Would make the doc claim true | Requires an allowlist of 20 legitimate exceptions | That is a ledger of exceptions, not a rule |

### Impact
**Positive**: The two highest-value rules now fail the build with a file and line; docs point at the enforcing test
**Negative**: Text scans catch the obvious regression only (a fully-qualified `java.math.BigDecimal` or `Instant.parse` of a literal slips through)
**Risk**: If the scan's file layout assumptions move, the meta-test fails loudly rather than silently passing

### Related Files
- `shared/src/jvmTest/kotlin/org/sjbtimdan/linden/ArchitectureInvariantsTest.kt` - the scans
- `shared/src/jvmTest/kotlin/org/sjbtimdan/linden/LocaleFileParityTest.kt` - the same `File("src/...")` lookup precedent
- `AGENTS.md` - clock and money bullets name the test; composable rule corrected
- `.opencode/context/project-intelligence/living-notes.md` - "Executable invariants" table (rule → test)

---

## Decision: Historical FX Rates and Frankfurter v1 vs v2

**Date**: 2026-11-11
**Status**: Pending (deferred — stay on v1 for now)
**Owner**: Steve

### Context
Cross-currency totals convert at the *latest* stored rate, so a past period's foreign balances and
header total are converted at today's rate and drift every day. A user asked whether historical rates
are feasible. Findings below were verified against the live API on 2026-11-11, not taken from docs.

**The API is not the blocker.** Linden already calls v1 (`/v1/latest`), which also supports:
- `/v1/{yyyy-mm-dd}?base=…&symbols=…` — one historical date
- `/v1/{from}..{to}?base=…&symbols=…` — time series

**The schema is the blocker.** `FxRateEntity` is `PRIMARY KEY (baseCurrency, quoteCurrency)` and
`refreshRates` does `insertOrReplace`, so exactly one observation per pair survives; the stored `date`
is overwritten on every refresh. History needs `FxRateHistoryEntity(base, quote, date, rate)` (or the
rate denormalized onto the entry). Volume is trivial: ~9 pairs × ~250 business days ≈ 2,250 rows/year
per base. That is a schema change, so it needs the migrations prerequisite first (schema is v1, no `.sqm`).

### Verified API behaviour
| Finding | Evidence |
|---|---|
| All 10 Linden currencies covered | `CHF` base against `CNY,EUR,GBP,HKD,IDR,INR,JPY,SGD,USD` returned every one |
| Weekend/holiday lookups auto-settle on the last business day | `date=2026-11-08` (Sunday) → `date:"2026-11-06"`; trust the returned `date`, not the requested one |
| Time series skips non-business days | `2026-10-26..2026-11-04` has no 10-31 / 11-01 |
| Granularity is one observation per business day | ECB publishes ~16:00 CET; no intraday rate for an entry's `createdAt` |
| No quotas; local caching encouraged | FAQ: "storing daily observations locally … is encouraged" |
| No privacy-label disclosure needed | FAQ: anonymous queries, no request logs retained |

### v1 vs v2
v2 (`/v2/rates`) is current; v1 is "deprecated in favor of v2, but remains available indefinitely".
v2 uses `quotes` (not `symbols` — `symbols` returns **422 unknown parameter**) and returns a flat
array `[{date, base, quote, rate}]` instead of v1's `{base, date, rates:{…}}`, so it is a parser and
model change, not a URL change. It adds `group`, `providers`, `expand=providers`, CSV and NDJSON.

**v2 buys no coverage** for these 10 currencies. Its default feed is *blended* across 111 providers,
which is not reproducible:

| Date | Pair | v2 blended | v2 ECB / v1 |
|---|---|---|---|
| 2026-11-04 | CHF→USD | 1.2229 | 1.2263 |
| 2026-11-08 (Sun) | CHF→EUR | 1.0922 | no ECB observation |

The docs warn "the last decimal places may shift as new data comes in". Since Linden *stores* the rate
and renders totals from it, a shifting feed means a stored conversion can disagree with a re-fetch.

### Decision
**Stay on v1 for now; do not upgrade standalone.** Upgrade only when the historical-rates work happens
— the same file (`FxRates.kt`: model, parser, fetcher, URL) is rewritten either way — and then pin to
`/v2/providers/ecb/rates` so current and historical conversions stay reproducible. If history is built,
adopt **period-end rates** first (one rate per currency per period); entry-date rates are the most
accurate but break the SQL-side aggregation (`accountDeltas`/`categoryTotals`) and are a much larger
refactor.

### Rationale
A standalone v2 bump has no user-visible benefit (same currencies, fresher-by-one-day latest behind a
24h cache), introduces non-reproducible blended rates, and churns the exact file the history work
rewrites. v1 has no sunset date.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|---|---|---|---|
| Upgrade to v2 now | Current API; fresher blended latest | Parser/model rewrite for no feature; blended rates shift | Do it once, with history |
| v2 pinned to ECB provider | Reproducible + current API | Still needs array parser; must verify ECB coverage per currency | Correct target *when* upgrading |
| Period-end historical rates | Fixes visible drift; small | Not per-entry accurate | Recommended first step if history lands |
| Entry-date rates | Most accurate | Breaks SQL aggregation; per-entry conversion in Kotlin | Deferred; largest scope |

### Impact
**Positive**: Decision recorded with verified evidence; no risk taken on a working integration
**Negative**: Past-period totals keep converting at today's rate until this is picked up
**Risk**: If v1 is ever retired, the parser change lands at the worst possible time

### Revisit When
- Historical rates are built (then do v1→v2 ECB-pinned in the same change)
- v1 shows any sign of retirement
- Fresher-than-ECB "latest" becomes a requirement

### Related
- `data/FxRates.kt` - `FrankfurterRates`, `parseFxRatesResponse`, `FxRatesFetcher`, `FRANKFURTER_LATEST_URL`
- `data/FxRatesRepository.kt` / `data/RatesFlowProvider.kt` - 24h staleness, rate flows
- `FxRate.sq` - one-row-per-pair storage; `FxRate`/`FxRates` in `model/FxRate.kt`
- `ui/accounts/AccountBalance.kt`, `ui/ledger/PeriodTotal.kt` - conversion sites
- `docs/privacy-policy.md` - policy wording; needs "current and historical rates" if history lands
- `.opencode/context/project-intelligence/living-notes.md` - open question + debt row

---

## Deprecated Decisions

| Decision | Date | Replaced By | Why |
|----------|------|-------------|-----|
| Android Auto Backup + Manual Export | 2026-10-01 | Android Auto Backup disabled | The DB also holds notification-capture data; the privacy policy now promises no Auto Backup/Google Drive upload |
| One-per-month balance adjustment (updated in place) | 2026-08-30 | Fresh entry per adjustment | Hidden update logic; a reconciliation is just an entry |
| Special adjust-balance marker (`is_adjustment` column, "Balance adjustment" description) | 2026-08-30 | No marker — ordinary entries with `description = null` | "A balance adjustment just creates an entry, no need to mark it as special" |

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
