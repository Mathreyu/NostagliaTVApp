# NostalgiaTVApp

A Google TV app that creates virtual channels with scheduled-style programming and launches content via deep links to streaming providers.

<img width="812" height="460" alt="image" src="https://github.com/user-attachments/assets/055c116b-48b0-4e76-bebd-77b37bfa511d" />


## What It Does

- Users tune into a channel instead of choosing episodes.
- The app decides “what’s on now” using scheduling rules.
- Playback is handed off to the provider app (Netflix, Disney+, etc.).

## MVP Features

- Virtual channels with rules-based scheduling
- Episode rotation with no-repeat windows
- Provider deep linking with availability checks
- Autoplay/resume flow after returning from playback
- Kid-safe mode with PIN exit
- Local Room storage

## Tech Stack

- Kotlin + Jetpack Compose for TV
- Room (local database)
- Android TV 12+ target

## Build

Set Java 17 and build:

```
./gradlew assembleDebug
```

## Notes

This app does not host or stream content. It only deep links to provider apps. Users must have their own subscriptions.
