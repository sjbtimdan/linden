<!-- Context: project-intelligence/bridge | Priority: high | Version: 2.0 | Updated: 2026-08-30 -->

# Business ↔ Tech Bridge

> How business needs translate to technical solutions in Linden.

## Quick Reference

- **Purpose**: Show stakeholders technical choices serve business goals
- **Purpose**: Show developers business constraints drive architecture
- **Update When**: New features, refactoring, business pivot

## Core Mapping

| Business Need | Technical Solution | Why This Mapping | Business Value |
|---------------|-------------------|------------------|----------------|
| Run on Android + Desktop | KMP shared module (`:shared`) | Single codebase, both platforms in lockstep | Lower maintenance, consistent UX |
| Accurate money handling | Integer minor units (`Long`), exact calculator | No floating-point errors | Trustworthy balances |
| Multi-currency support | `Currency` enum, FX rates via Frankfurter, per-currency accounts | Cross-currency totals in default currency | Users can hold accounts in any currency |
| Fast entry | Predictions (`DescriptionPredictor`, `FieldPredictor`, `QuickEntryPredictor`) | Heuristic scoring of past entries | Less typing, fewer errors |
| Migrate from Ivy Wallet | `IvyImporter` (ZIP+JSON import) | Maps Ivy backup to Linden schema | Easy onboarding for existing users |
| Data safety | `LindenBackupManager` (JSON dump/restore) | Versioned, transactional restore | No data loss |
| Local-first | SQLDelight local DB, no server | All data on device | Privacy, offline, no accounts |
| Reconcile account balances | `LedgerViewModel.adjustBalance` + `BalanceAdjustment` helpers | Fresh income/expense entry per adjustment, no special marker | Ledger always matches the real bank balance |

## Feature Mapping Examples

### Feature: Entry Tracking

**Business Context**:
- User need: Record expenses, income, and transfers quickly
- Business goal: Core value proposition
- Priority: Highest (it's the primary screen)

**Technical Implementation**:
- Solution: `EntryPoint` screen with `EntryForm`, `EntryDraft`, `EntryPointViewModel`
- Architecture: MVVM — ViewModel exposes `StateFlow`s, screen collects with `collectAsState()`
- Trade-offs: Draft kept in ViewModel across config changes; exact calculator for amount entry

**Connection**:
Fast, accurate entry is the heart of the app. The calculator avoids floating-point surprises,
predictions reduce typing, and the draft survives configuration changes.

### Feature: Multi-Currency Ledger

**Business Context**:
- User need: See balances and totals across accounts in different currencies
- Business goal: Differentiate from single-currency trackers
- Priority: High

**Technical Implementation**:
- Solution: `LedgerViewModel` with `RatesFlowProvider`, `accountDeltas`/`categoryTotals` SQL queries
- Architecture: Aggregation pushed into SQL, converted to default currency once per currency group
- Trade-offs: FX rates cached 24h (Frankfurter API); missing rate → null total

**Connection**:
Users can hold accounts in any of 7 currencies and still see a single default-currency total.

### Feature: Ivy Wallet Import

**Business Context**:
- User need: Migrate from Ivy Wallet without re-entering history
- Business goal: Reduce onboarding friction
- Priority: Medium

**Technical Implementation**:
- Solution: `IvyImporter` reads ZIP+JSON, infers `CategoryType` from usage, maps balance titles to `initialBalance`
- Architecture: Replaces all rows in one transaction; currency-mismatched transactions routed to split accounts
- Trade-offs: `last_insert_rowid()` resolves auto-increment IDs; charset detection for UTF-16

**Connection**:
Existing users can switch to Linden without losing their financial history.

### Feature: Balance Reconciliation (Adjust Balance)

**Business Context**:
- User need: Bring a tracked account in line with its real bank balance (or a target), e.g. after a missed payment
- Business goal: Trustworthy "at a glance" balances
- Priority: Medium

**Technical Implementation**:
- Solution: `LedgerViewModel.adjustBalance` (accounts view of the ledger) computes `balanceAdjustment(current, target)`
  and turns the delta into a fresh entry via `adjustmentEntry` (`ui/accounts/BalanceAdjustment.kt`)
- Architecture: No special treatment — a positive delta becomes an income entry, a negative delta an expense entry,
  dated now with `description = null`; zero delta creates nothing
- Trade-offs: Each adjustment is an ordinary, visible, editable entry — no `is_adjustment` column, no hidden state

**Connection**:
The tracker heals drift without magic: reconciling just records income/expense like any other entry.

## Trade-off Decisions

| Situation | Business Priority | Technical Priority | Decision Made | Rationale |
|-----------|-------------------|-------------------|---------------|-----------|
| Money accuracy vs. simplicity | Accurate balances | Avoid float errors | Integer minor units + exact calculator | No floating-point surprises |
| Multi-currency vs. complexity | Cross-currency totals | Keep it simple | FX rates via Frankfurter, cached 24h | Live rates without a server |
| Local-first vs. cloud sync | Privacy, offline | No server to maintain | SQLDelight local DB | All data on device |
| KMP vs. native per platform | One codebase | Consistency | KMP shared module | Both platforms in lockstep |
| Adjustment clarity vs. history | Exact reconciliation | Don't pollute the ledger | Fresh entry per adjustment, no marker | Simple, inspectable history |
| Entry validation vs. flexible accounts | DB enforces valid entries | Allow liabilities | `amount >= 0` CHECKs; accounts may be negative | Correct data, flexible balances |

## Common Misalignments

| Misalignment | Warning Signs | Resolution Approach |
|--------------|---------------|---------------------|
| Money as `Double`/`BigDecimal` | Floating-point rounding in amounts | Always use integer minor units (`Long`) |
| `formatAmountCompact` in edit fields | `parseAmount` can't parse "1.25m" | Only use compact format for read-only displays |
| Editing `.sq` tables without migration | Desktop DB at `~/.linden/linden.db` not migrated | Add a `.sqm` migration or delete the local DB |
| Negative entry amounts | Entry `amount` < 0 | Entries carry a type; amount is always >= 0 (CHECK-enforced) |
| Currency change on account with entries | `accountsWithEntries` blocks it | Respect the business rule |

## Stakeholder Communication

This file helps translate between worlds:

**For Business Stakeholders**:
- Shows that technical investments serve business goals
- Provides context for why certain choices were made
- Demonstrates ROI of technical decisions

**For Technical Stakeholders**:
- Provides business context for architectural decisions
- Shows the "why" behind constraints and requirements
- Helps prioritize technical debt with business impact

## Onboarding Checklist

- [x] Understand the core business needs this project addresses
- [x] See how each major feature maps to business value
- [x] Know the key trade-offs and why decisions were made
- [x] Be able to explain to stakeholders why technical choices matter
- [x] Be able to explain to developers why business constraints exist

## Related Files

- `business-domain.md` - Business needs in detail
- `technical-domain.md` - Technical implementation in detail
- `decisions-log.md` - Decisions made with full context
- `living-notes.md` - Current open questions and issues
