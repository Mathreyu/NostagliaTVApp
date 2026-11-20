# NostalgiaTV

A Google TV app that creates virtual channels with scheduled-style programming and launches content via deep links to streaming providers.

<img width="812" height="460" alt="image" src="https://github.com/user-attachments/assets/055c116b-48b0-4e76-bebd-77b37bfa511d" />


## What It Does

- Users tune into a channel instead of choosing episodes.
- The app decides “what’s on now” using scheduling rules.
- Playback is handed off to the provider app (Netflix, Disney+, etc.).

## Features

- Virtual channels with channel numbers and accent colours, tuned like a cable box (CH up/down)
- Live guide: what is on now and coming up on every channel
- Rules-based, deterministic scheduling with no-repeat rotation
- Show library and lineup editor: create, edit and delete shows, and choose which shows air on each channel
- Provider deep linking with availability checks; the provider app is launched, never embedded
- Honest links: when a provider gives us no way to open a single episode, the app opens the show's title page and says so on the channel screen ("pick S1E3")
- Autoplay/resume flow after returning from playback
- Parental lock: kid-safe mode with a parent-set PIN, stored only as a salted PBKDF2 hash, locked for 60 s after 5 wrong attempts and kept on across restarts. Editors (channels, rules, lineups, shows) are refused in kid-safe mode unless a parent unlocks them with the PIN
- Local Room storage; play history is pruned to 30 days / 500 entries on start

## Tech Stack

- Kotlin + Jetpack Compose for TV
- Room (local database, schema v4 with migrations; exported schemas live in `app/schemas`)
- Android TV 12+ target

## Build

Requires JDK 17 (the Gradle toolchain is set to 17).

```
./gradlew assembleDebug testDebugUnitTest lintDebug
```

## Architecture

- `data`: Room entities, DAOs, `TvRepository` and seed data. `Provider` holds per-provider link templates.
- `engine`: `ProgrammingEngine` is a pure function of channel, shows, rules and the clock. Nothing is stored to decide what is on; play history is only a log.
- `resolver`: turns a program into a provider deep link (`LinkSelector` picks an episode link or falls back to the title page) and checks the provider is installed.
- `playback`: hands off to the provider app and remembers when to resume.
- `security`: kid-safe policy, PIN hashing and lockout.
- `ui`: `MainViewModel` owns a single `MainUiState` and an explicit back stack; screens and the cable-box design system (`ui/components`) are Compose for TV.

## Notes

This app does not host or stream content. It only deep links to provider apps. Users must have their own subscriptions.

The shows and content IDs in the seed data are samples for a first run, not verified catalogue IDs. Set real IDs in the show editor.
