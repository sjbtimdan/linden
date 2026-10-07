# Store assets

Google Play listing assets for Linden. Committed here so they are versioned and
regenerable.

## Contents

| File | Purpose | Spec |
|---|---|---|
| `play-icon-512.png` | Play Store app icon | 512 × 512 px, 32-bit PNG with alpha |
| `play-feature-graphic-1024x500.png` | Play Store feature graphic | 1024 × 500 px, 24-bit PNG, no alpha |

The feature graphic must stay free of transparency and keep key content away from
the outer edges, since Play overlays/crops it in some placements.

## Regenerating

Both assets are rendered from the same Java2D source as the launcher icon
(`desktopApp/.../IconRenderer.kt`, design mirrored from
`desktopApp/src/main/composeResources/drawable/linden_icon.svg`):

```bash
./gradlew :desktopApp:renderIcon
```

This also refreshes the master icon at `build/icon-render/linden-icon-1024.png`.

## Listing checklist

- [x] Short description (< 80 chars) — `docs/store-listings/`
- [x] Full description (< 4000 chars) — `docs/store-listings/`
- [x] Localized listings (it/fr/de/hi/id/zh-CN/zh-HK) — AI first pass in `docs/store-listings/`, native review pending
- [x] App icon 512 × 512 — `play-icon-512.png`
- [x] Feature graphic 1024 × 500 — `play-feature-graphic-1024x500.png`
- [x] Phone screenshots (2–8) — capture from a device/emulator, 16:9–9:16
- [ ] 7" tablet screenshots (optional, up to 8)
- [ ] 10" tablet screenshots (optional, up to 8)
- [ ] Promo video (optional) — YouTube URL
- [ ] Privacy policy URL — host `docs/privacy-policy.md` at a public URL
- [ ] Terms of Use URL (optional) — host `docs/terms.md` at a public URL
- [ ] Production access application (personal accounts, 12 testers / 14 days) — see `docs/play-production-application.md`
- [ ] Content rating questionnaire completed in Play Console
- [ ] Data safety form completed (local financial data; Firebase Analytics usage + Crashlytics diagnostics; advertising ID declared)

## Screenshots

The committed set (`screenshots/`, 1080 × 1920, 24-bit PNG without alpha). Each
carries its caption as an overlay:

1. `screen-1-ledger-entries.png` — Ledger entries — "All your money, one timeline"
2. `screen-2-entry-calculator.png` — amount calculator — "Add entries in seconds"
3. `screen-3-ledger-accounts.png` — accounts and currencies — "Track accounts in any currency"
4. `screen-4-ledger-categories.png` — category totals — "Know where it goes"
5. `screen-5-insights.png` — 12-month trend — "12 months at a glance"

Regenerate: seed the demo database with `scripts/linden-seed-demo.py` (writes
`~/.linden/linden.db` — back up the real one first), install the debug APK on a
16:9 emulator, then capture with `adb exec-out screencap -p > shot.png`.
