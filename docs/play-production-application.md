# Play production access application

Notes for applying for production access after closed testing (personal developer
accounts created after Nov 13, 2023). Google requires **at least 12 testers opted in
continuously for 14 days** (any opt-out resets the clock), and review usually takes
seven days or less.

Google verifies tester engagement, so every answer must be **specific, truthful and
consistent with the Console data**. Using a paid/third-party testing provider is not
prohibited — the Console question even names it as an example — but fake engagement
is a policy violation. The typical rejection is "insufficient tester engagement".

## How did you recruit users for your closed test?

Draft (adapt the brackets; keep it concrete):

> I recruited testers through [provider/community name], a third-party closed-testing
> community, supplemented by [friends/colleagues]. Each tester opted in with their own
> Google account via the closed-testing link and had Linden installed for the full
> 14-day period. Testers were asked to use it like a real user: adding expenses, income
> and transfers; managing accounts and categories; browsing the ledger views; checking
> multi-currency balances and FX rates; backup/restore; and switching language and
> theme. Feedback was collected via [provider's channel / Google Form / email] and
> through Play Console's testing feedback.

## Other questions, same form

| Question | What to answer |
|---|---|
| How easy was it to recruit testers? | Pick what is true. A paid provider usually makes this "easy". |
| Engagement: which features did testers use? Did usage match expected production users? | Name the features actually used. Do **not** claim coverage the opt-in/usage data does not show — this is checked. |
| Feedback: summary and how it was collected | Take the real themes from **Monitor and improve → Ratings and reviews → Testing feedback**. Name the channel(s). |
| Target audience | Adults managing personal finances (18+), matching the content-rating choice. |
| Value proposition | Local-first expense tracker: no ads, multi-currency, fast entry, private by design. |
| Estimated first-year installs | Choose a modest, realistic range. |
| What changed from the closed test / how you decided it was ready | Name fixes with version codes. If the only feedback was cosmetic, say so honestly and describe what you verified instead. |

## Before submitting

- Console shows ≥ 12 testers opted in for 14 continuous days.
- All App content rows green — see the checklist in `docs/store-assets/README.md`.
- Privacy policy (and optionally Terms) hosted at public URLs; `1.0.0` build uploaded.
- The Play "missing native debug symbols" warning is advisory — see `RELEASING.md`.

## Avoid

- Vague "friends and family" with no detail, or inflated claims.
- Claiming testers exercised features they did not — low engagement means "continue
  testing", and overclaiming makes that worse.
- Describing bot/opt-in-only activity as real usage; extend the test with real usage
  instead, then reapply (another clean 12/14 cycle).

## Sources

- App testing requirements (12 testers / 14 days) — https://support.google.com/googleplay/android-developer/answer/14151465
- Closed test setup and feedback — https://support.google.com/googleplay/android-developer/answer/9845334
- Fake engagement policy — https://support.google.com/googleplay/android-developer/answer/9898684
- Publishing pitfalls / resubmitting — https://support.google.com/googleplay/android-developer/answer/15191715
