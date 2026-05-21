# Mini Video Journal

Mini Video Journal is a small Android app for recording short video clips, saving them locally with an optional description, and browsing them in a vertical feed.

The app is intentionally focused on the core flow: record, review, save, browse, play inline, delete, and share. The UI is simple, but the structure is meant to show clean boundaries and testable logic.

## What is included

- Camera recording with CameraX
- Inline playback with Media3 ExoPlayer
- Vertical snapping feed with latest clips first
- Optional text description for each clip
- Local persistence with SQLDelight
- App-private file storage for videos and thumbnails
- Thumbnail generation for the feed
- Share action through Android Intent and FileProvider
- Koin dependency injection
- Jetpack Compose UI
- Basic Material theming
- GitHub Actions CI for tests, lint, and debug build

## Setup

Requirements:

- Android Studio
- Android SDK 36 installed
- JDK 21 recommended, matching the CI setup

Run locally:

```powershell
git clone <repo-url>
cd VideoJournal
```

Open the project in Android Studio, let Gradle sync, select the `app` run configuration, and run it on a device or emulator.

The camera flow is best checked on a real device. It can work on an emulator too, but the preview depends on the emulator camera configuration.

## Useful commands

Run unit tests on Windows:

```powershell
.\gradlew.bat testDebugUnitTest
```

Run unit tests on macOS/Linux:

```bash
./gradlew testDebugUnitTest
```

Build debug APK:

```powershell
.\gradlew.bat assembleDebug
```

Run lint:

```powershell
.\gradlew.bat lintDebug
```

## Architecture

The project uses a single Android module, but the code is split into Clean Architecture style packages:

```text
presentation -> domain <- data
app/di -> wires everything together
```

I kept it single-module on purpose. For a time-boxed home assignment it is easier to review, easier to run, and avoids spending time on Gradle/module setup instead of the actual product flow. The important part is still there: the domain layer is clean, data implements domain ports, presentation talks to use cases, and Koin is used at the application boundary.

Package responsibilities:

- `domain` contains models, repository/media interfaces, use cases, and dispatcher abstractions.
- `data` contains SQLDelight repository implementation, file storage, and thumbnail generation.
- `presentation` contains Compose screens, ViewModels, CameraX and ExoPlayer adapters.
- `app` contains the Android application, navigation, and dependency graph.

## A few decisions

I used CameraX instead of launching an external camera intent because the task is about an in-app recording experience. It gives better control over permissions, recording state, duration, and the review screen after recording.

Videos are stored in app-private storage, so the app does not request broad storage permissions. This is safer and simpler for this kind of journal app. Recorded videos live under the app files directory, temp recordings live under cache, and startup cleanup removes abandoned temp recordings.

The feed uses one shared Media3 ExoPlayer instance instead of creating one player per feed item. Only the active item can own playback, and swiping away pauses the previous video. This keeps memory and decoder usage under control.

Thumbnails are treated as a nice UX improvement, not as critical data. If thumbnail generation fails, the video can still be saved and shown with a fallback placeholder.

Backups are disabled for the app because the content is private video journal data. I would rather avoid accidental cloud backup of personal recordings by default.

## Testing strategy

I focused tests on logic that is worth protecting:

- use cases for save/delete behavior and cleanup
- ViewModels for UI state transitions
- SQLDelight repository behavior with an in-memory driver
- storage logic around temp/final files
- mapper tests for database/domain conversion

I did not aim for 100% coverage. The goal was to show how I split responsibilities and where I would place tests in a real project.

## What was challenging and interesting

The biggest challenge for me was working with Media3 ExoPlayer and CameraX. I had never used them before, so a lot of the time went into understanding lifecycle, player ownership, camera callbacks, recording finalization, and how to keep all of that aligned with Compose state.

At the same time, that was also the most interesting part of the assignment.

I also spent some time cleaning up the app flow so that it feels predictable: record, review, save, return to the newest video in the feed, tap to play, tap again to pause.

## What I would improve with more time

- Add Compose UI tests for the feed and recording flows.
- Add editing descriptions after saving.
- Add search/filter by description or date.
- Add a calendar/timeline view for older clips.
- Add simple video trimming before saving.
- Add pagination or lazy loading if the local library grows a lot.
- Add better error messages for low storage or camera initialization failures.
- Add preloading for the next video in the feed.
