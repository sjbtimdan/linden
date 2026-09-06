<!-- Context: project-intelligence/notes | Priority: high | Version: 2.0 | Updated: 2026-09-05 -->

# Living Notes

> Active issues, technical debt, open questions, and insights that don't fit elsewhere. Keep this alive.

## Quick Reference

- **Purpose**: Capture current state, problems, and open questions
- **Update**: Weekly or when status changes
- **Archive**: Move resolved items to bottom with status

## Technical Debt

| Item | Impact | Priority | Mitigation |
|------|--------|----------|------------|
| Every `.sq` DDL change needs a new `.sqm` migration | Schema is v5 on `main` (`1.sqm` entry CHECKs, `2.sqm` UNIQUE names, `3.sqm` budgets, `4.sqm` `AccountEntity.hidden`). Forgetting a migration breaks the persisted desktop DB at `~/.linden/linden.db` | Medium | Add a `.sqm` migration for table changes |
| Android DB with a newer schema opened by an older app | `DatabaseDriverFactory` (androidMain) overrides `onDowngrade` to no-op, so the DB opens but keeps its newer columns, re-stamped to the older `user_version`. Moving back to the newer app replays the re-added migration (`4.sqm` = `ADD COLUMN hidden`) on a table that already has the column → `duplicate column name: hidden` on that device | Low | Reinstall the debug app, or drop/tolerate the leftover column (see Known Issues) |
| `:shared` Android compiles via `compileAndroidMain`, not `compileDebugKotlin` | Confusing for new devs | Low | Documented in AGENTS.md |
| Kover coverage only on JVM variant | Android target runs device tests only | Low | `koverVerifyJvm` (50% min) runs as part of `check` |

### Technical Debt Details

**SQLDelight migrations are manual**  
*Priority*: Medium  
*Impact*: Editing an `.sq` table without a matching `.sqm` breaks the persisted desktop DB at `~/.linden/linden.db`  
*Root Cause*: Migrations exist (v5 on `main`, 1–4.sqm) but must be authored by hand  
*Proposed Solution*: Keep adding `.sqm` files whenever a table changes  
*Effort*: Small  
*Status*: Acknowledged

**Android opens newer DBs via no-op `onDowngrade` (2026-09-04)**  
*Priority*: Low  
*Impact*: A DB stamped by a newer app version no longer crashes startup ("Can't downgrade database from version X to Y") — data is preserved and schema changes here are additive-only, so extra columns are ignored safely.  
*Root Cause*: `AndroidSqliteDriver` hands `Schema.version` to SQLiteOpenHelper; its default `onDowngrade` (androidx `SupportSQLiteOpenHelper.Callback`) always throws.  
*Caveat*: After the no-op downgrade the framework re-stamps the DB to `user_version = Schema.version`, but **the table keeps its newer columns**. If a newer app (schema v5, `4.sqm` = `ALTER TABLE AccountEntity ADD COLUMN hidden …`) is reinstalled over such a DB, the migration replays on a table that already has `hidden` and fails with `duplicate column name: hidden`.  
*Status*: Acknowledged

## Open Questions

| Question | Stakeholders | Status | Next Action |
|----------|--------------|--------|-------------|
| (none) | - | - | - |

## Known Issues

| Issue | Severity | Workaround | Status |
|-------|----------|------------|--------|
| Device that ran a v5 app then an older one keeps the `hidden` column re-stamped to `user_version=4`; reinstalling v5 replays `4.sqm` (`ADD COLUMN hidden`) on that DB and errors `duplicate column name: hidden` | Low | Reinstall the debug app (fresh v5 DB migrates cleanly) or make `4.sqm` tolerate the existing column | Known |

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
- **Manual SQLDelight migrations** - Schema v5 on `main` but every `.sq` DDL change still needs a hand-written `.sqm`
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
- **Each new class ships with a unit test** - Unless it is pure configuration (constants/values with no behavior), a new class is not done without its test
- **Each Composable in its own file** - e.g. `DayHeader` lives in `DayHeader.kt`, not inside a screen file
- **Comment minimally** - Only add comments for obscure code
- **Conventional commits** - Standard commit message format
- **Version catalog** - `gradle/libs.versions.toml`
- **Kotlin source files use PascalCase** - e.g. `EntryDao.kt`, not `entry_dao.kt`

### UI strings (i18n, since 2026-09-06)
- All UI copy lives in `shared/src/commonMain/composeResources/values/strings.xml` (English = default). Per-locale variants come later as `values-<lang>/` dirs (see `.tmp/INTERNATIONALIZATION.md`).
- In composables: `stringResource(Res.string.key)`; plurals: `pluralStringResource(Res.plurals.key, quantity, quantity)` — **the quantity must be repeated as the `%1$d` format arg** (CMP selects the plural category from the first quantity but does not substitute placeholders automatically).
- The generated accessors are **top-level extension properties on `Res.string`/`Res.plurals`**: every key needs its own explicit import (`import org.sjbtimdan.linden.resources.<key>`; no wildcard imports — detekt `NoWildcardImports`). `Res` itself is `internal` in package `org.sjbtimdan.linden.resources` (set via `compose.resources { packageOfResClass }` in `shared/build.gradle.kts`).
- Key naming: `screen_purpose` (e.g. `ledger_empty_no_match`); verbs/actions shared across screens are `common_*` (`common_save`, `common_cancel`…). `testTag` strings and user data are never resources; Ivy importer keyword tables and `"initial balance"`/`"adjust balance"` matching must NOT be translated.
- Copy produced by pure helpers is modeled as pure *enums* (e.g. `MissingRequirement` in `ui/entry`) whose localized wording is resolved by a `@Composable` extension (`text()`) mapping to resources — the "each new class ships with a test" rule applies to those mappings (`MissingRequirementTest` locks enum → English copy).
- Never call `stringResource` from non-composable lambdas (validation `onSave` handlers etc.): resolve the string into a `val` earlier in the composable and capture it.
- **Dates are language-driven, never raw-locale** (`DateLanguage` in `ui/entry/DateLanguage.kt`, since 2026-09-06): `formatDate`/period labels render from per-language month tables and layouts (`English` US-styled "Aug 13, 2026" / `Italian` "13 ago 2026" / `Chinese` "2026年8月13日"; unsupported locales fall back to English), so numeric formats like `08/23/2026` can never appear. Resolve the language via `dateLanguage(platformLanguageCode())` (pure, language-explicit overloads exist for tests — never mutate the JVM locale); the in-app override will feed the same enum. `formatTime` stays `HH:mm`.
- **App language** (`AppLanguage` in `model/AppLanguage.kt`, since 2026-09-06): Settings → Language offers the four concrete languages (English / Italiano / 简体中文 / 繁體中文（香港）) — there is **no "System" option**; the chip that shows selected is the effective language, which resolves from the platform tag (`AppLanguage.fromSystemLanguage`; it/zh systems map to their own language — zh split by script/region, Hant or HK/MO/TW → zh-HK — and every other system language falls back to English). Pinning writes a BCP-47 tag under `SettingsDao` `language`; nothing is written at startup (absent = `SYSTEM` sentinel = follow/resolve system). `App` collects it and calls `ApplyLanguageOverride(language)` (expect/actual in `ui/AppLocale.*`) during composition before the subtree, wrapped in `key(language)` so everything re-composes under the new `Locale.setDefault` — but only a **pin** swaps the platform locale: unpinned apps keep the system locale (money/region formatting untouched) while dates/strings resolve via `dateLanguage(platformLocaleTag())`. Picker labels are language autonyms; keep them distinct from other "System" copy. The Android actual of `ApplyLanguageOverride` patches the app context's configuration (`Configuration.setLocale` via `updateConfiguration`, deprecated but observed by the CMP resource library) using `LocalContext.current`, so a **pin** switches strings there too — not just `Locale.getDefault()`-driven dates/money. Verified by compile + JVM suite only; a device/emulator smoke (pin a language with a differing OS locale) is still on the QA list.
- **Translations ship as locale resource dirs** (since 2026-09-06): `values-it`, `values-zh-rCN` (Simplified), `values-zh-rHK` (Traditional/HK), and `values-zh` (script-less fallback mirroring zh-rCN). System/pinned languages resolve them automatically; unsupported locales fall back to the English default. **`LocaleFileParityTest` (jvmTest) gates them**: every locale must define exactly the default's keys with matching placeholder counts — a missing key or dropped `%N$` argument fails CI. zh plurals only carry `other` (CLDR). Caveats: `values-zh-rHK` currently uses 戶口 for "account" (HK usage) while 記帳/帳目 etc. follow HK conventions; copy is an AI first pass — **native-speaker review pending** before marketing; Android still needs the configuration-locale update in `ApplyLanguageOverride` if strings must switch for a *pinned* language there (system-driven switching works already). Locale generation script: `.tmp/gen_it.py` / `.tmp/gen_zh.py` (regenerate + re-check parity after touching values/strings.xml).


### Gotchas for Maintainers
- **`formatAmountCompact`** - Never use it to pre-fill edit fields; `parseAmount` can't parse the suffix
- **`accountsWithEntries`** - Blocks changing the currency of an account that has entries
- **`Entry` sealed interface** - Adding a field touches all subclass branches plus `Entry.sq` insert/update and `EntryDao` mapping
- **Adjust balance entries have `description = null`** - Reconciliation entries are ordinary entries with no marker, so they appear in the ledger without a description
- **SQLite FK enforcement is off by default** - If FKs are ever added, `PRAGMA foreign_keys = ON` must be set in both `DatabaseDriverFactory` actuals (Android + JVM) or the constraints are decorative
- **Accounts can be negative, entries never** - `parseAmount` handles `-` (liabilities/negative balances); entry `amount >= 0` is CHECK-enforced (since `1.sqm`)
- **Android tolerates newer DBs** - `onDowngrade` is a no-op in `DatabaseDriverFactory` (androidMain); the JVM driver never version-checks, so a DB stamped by newer code opens everywhere as long as changes stay additive
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
