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

- [x] Short description (< 80 chars) — `docs/store-listing.md`
- [x] Full description (< 4000 chars) — `docs/store-listing.md`
- [x] App icon 512 × 512 — `play-icon-512.png`
- [x] Feature graphic 1024 × 500 — `play-feature-graphic-1024x500.png`
- [ ] Phone screenshots (2–8) — capture from a device/emulator, 16:9–9:16
- [ ] 7" tablet screenshots (optional, up to 8)
- [ ] 10" tablet screenshots (optional, up to 8)
- [ ] Promo video (optional) — YouTube URL
- [ ] Privacy policy URL — see `docs/privacy-policy.md`
- [ ] Content rating questionnaire completed in Play Console
- [ ] Data safety form completed (app stores data locally, no collection)

## Screenshot guidance

Suggested shots (capture in this order so the strongest comes first):

1. Ledger — entries view for the current month
2. Add entry dialog with the amount calculator
3. Accounts view with multi-currency balances
4. Categories view with totals
5. Settings — backup / theme / default currency

Capture with `adb exec-out screencap -p > shot.png` (or the emulator camera icon).
Crop/letterbox to a consistent aspect ratio (16:9 or 9:16) before uploading.
