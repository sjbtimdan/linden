<!-- Context: project-intelligence/business | Priority: high | Version: 2.0 | Updated: 2026-08-29 -->

# Business Domain

> The business context, problems solved, and value created by Linden.

## Quick Reference

- **Purpose**: Understand why this project exists
- **Update When**: Business direction changes, new features shipped, pivot
- **Audience**: Developers needing context, stakeholders, product team

## Project Identity

```
Project Name: Linden
Tagline: An expenses tracker
Problem Statement: Track personal expenses and income across multiple accounts and currencies
Solution: A KMP app (Android + Desktop) with entry tracking, ledger views, FX-rate-aware totals, and Ivy Wallet import
```

## Target Users

| User Segment | Who They Are | What They Need | Pain Points |
|--------------|--------------|----------------|-------------|
| Primary | Individuals tracking personal finances | Track expenses/income, see balances, multi-currency support | Manual spreadsheets, no cross-currency totals |
| Secondary | Users migrating from Ivy Wallet | Import existing data | Manual re-entry of historical transactions |

## Value Proposition

**For Users**:
- Track expenses, income, and transfers across multiple accounts
- Multi-currency support with live FX rates (Frankfurter API)
- Fast entry with predictions (description/account/category suggestions, quick entries)
- Exact calculator for amount entry (no floating-point surprises)
- Ledger with period navigation, search, and filters
- Backup/restore and Ivy Wallet import

**For Business**:
- Single KMP codebase for Android + Desktop (maintenance efficiency)
- Local-first (no server, no accounts, no cloud)

## Success Metrics

| Metric | Definition | Target | Current |
|--------|------------|--------|---------|
| Test coverage | Kover JVM variant | ≥ 50% | Enforced via `koverVerifyJvm` in `check` |

## Business Model (if applicable)

```
Revenue Model: N/A (personal project)
Pricing Strategy: N/A
Unit Economics: N/A
Market Position: Personal expenses tracker (local-first, multi-currency)
```

## Key Stakeholders

| Role | Name | Responsibility | Contact |
|------|------|----------------|---------|
| Maintainer | Steve | All development | N/A |

## Roadmap Context

**Current Focus**: Core expense tracking (entry, ledger, accounts, categories, rates, settings)
**Next Milestone**: N/A
**Long-term Vision**: N/A

## Business Constraints

- Local-first: no server, no accounts, no cloud sync
- Multi-currency: users may hold accounts in CHF/EUR/GBP/HKD/JPY/SGD/USD
- Cross-currency totals require FX rates (fetched from Frankfurter, cached 24h)

## Onboarding Checklist

- [x] Understand the problem statement (personal expenses tracker)
- [x] Identify target users and their needs (individuals, Ivy Wallet migrants)
- [x] Know the key value proposition (multi-currency, fast entry, local-first)
- [x] Understand success metrics (test coverage gate)
- [x] Know who the stakeholders are (single maintainer)
- [x] Understand current business constraints (local-first, multi-currency)

## Related Files

- `technical-domain.md` - How this business need is solved technically
- `business-tech-bridge.md` - Mapping between business and technical
- `decisions-log.md` - Business decisions with context
