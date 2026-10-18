# Privacy Policy

**Last updated:** October 18, 2026

## Overview

Linden is a local-first expenses tracker. Your entries, accounts, categories and settings stay on your device, or in your own Android backup when device backup is enabled. Anonymous usage analytics and crash reports help improve the app.

## Data Collection

- **Usage analytics (Firebase Analytics).** Linden collects anonymous usage data — such as which screens are used, session information, app version and device type — together with a device/advertising identifier, to understand how the app is used and improve it. This data is not used to show ads, and it never includes your entries, accounts, categories, amounts or notification content.
- **Crash reports (Firebase Crashlytics).** If Linden crashes, a technical report (stack trace, app version, device model, OS version) is sent to help fix bugs.
- **No ads.** Linden shows no advertising.
- **Exchange rates.** Currency conversion requests current rates from the [Frankfurter API](https://www.frankfurter.app/); only a currency code is sent.
- **Notifications (optional, Android).** If you explicitly grant Notification access and enable Payment capture, Linden processes notifications from the apps you allowlist to suggest a payment entry. This happens on your device and is described under [Notification Access](#notification-access-android-optional).

## Notification Access (Android, optional)

Payment capture is off by default. Linden can read notifications only if you explicitly grant Notification access in Android settings and enable **Settings → Payment capture** in the app. Android then delivers notifications to Linden, but Linden processes and stores only notifications from the apps you add to the allowlist, in order to suggest a payment entry for you to review. Notification content is processed on your device: Linden never uploads or shares it with Firebase, Frankfurter or any other third party. Captured payment data and learned templates are stored locally; if you enable Android's auto backup, they can be included in your Google Drive backup like the rest of Linden's data. You can revoke Notification access at any time in Android settings or disable Payment capture in Linden.

## Data Storage

All financial data (entries, accounts, categories, settings) is stored locally in an SQLite database on your device and is never uploaded by Linden. The optional captured-payment inbox and learned capture rules are stored locally in the app's settings and are included in your own Linden backups like the rest of your data.

If Android's auto backup service is enabled on your device, the operating system may upload Linden's local database — including the captured-payment inbox and learned capture rules — to your Google Drive. That backup is controlled by you and your device settings, not by Linden.

## Data Sharing

Usage analytics and crash reports are processed by Google Firebase on our behalf. No other third party receives data from Linden. Notification content is never shared by Linden; it is processed on your device only. Android auto backup, when enabled, is a device-level backup controlled by you, not an upload by Linden.

## Children's Privacy

Linden does not knowingly collect any data from anyone, including children.

## Changes to This Policy

If this policy changes, the update will be reflected in this file with a revised date.

## Terms of Use

Linden is provided "as is", without warranty of any kind; see [Terms of Use](terms.md) for the full warranty and liability terms.

## Contact

For questions about this policy, open an issue on [GitHub](https://github.com/sjbtimdan/linden).
