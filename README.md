# Zone for Android

A native Android port of the Zone personal workspace: a diary, lists, habits, notes, money, passbook, shared-expense split, mileage, watchlist, links, subscriptions, settings, and a local review desk.

Built with **Kotlin** and **Jetpack Compose** using a light, iOS-inspired theme: generous white space, rounded cards, soft separators, a restrained blue accent, and a five-item bottom tab bar.

## Features

- **Overview** — greeting, monthly spend, diary streak, open list items, average mileage, and a searchable tool grid.
- **Diary** — mood-tagged entries, search, month grouping, streaks, tags, copy, CSV export.
- **Lists** — nested items and steps, progress, open/done filters, templates, CSV export.
- **Habits** — weekly targets, day-by-day check-ins, streaks, week navigation, CSV export.
- **Notes** — colored notes, pinning, search, filters, Markdown export.
- **Money** — monthly ledger, income/expense filters, category filters, budget pace, CSV export.
- **Passbook** — multiple accounts, deposits/withdrawals, account balances, statement filters, CSV export.
- **Split** — shared tables, member balances, greedy settle-up plan, settlements, expense ledger.
- **Mileage** — fuel log, odometer chains, vehicle garage, average and best mileage gauge, CSV export.
- **Watchlist** — queued/watching/watched titles, series seasons, progress, ratings, CSV export.
- **Links** — URL shelf, tags, favorites, search, open/copy.
- **Subscriptions** — monthly burn, annual cost, renewals, pause/resume, CSV export.
- **Settings** — light/dark/system theme, display currency, backup export/import.
- **Review desk** — local audit trail, approve/reject/restore/reapply, auto-approve policy, report export.

All data is stored locally in `SharedPreferences` as JSON. Export/import uses the same section names as the web app (`diary`, `lists`, `habits`, `notes`, `money`, `passbook`, `split`, `trips`, `vehicles`, `watchlist`, `links`, `subscriptions`, `settings`, `admin`).

## Build

The repository includes a Gradle wrapper. To build locally:

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

`.github/workflows/android.yml` builds the debug APK on every push to `main` (and on manual dispatch). It does **not** upload a workflow artifact. Instead it publishes the APK directly to a GitHub **pre-release** named `debug-<run number>`, with `prerelease: true`.

No local build is required to produce the APK; the workflow handles it.