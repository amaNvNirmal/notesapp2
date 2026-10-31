# TuneNotes

Notes app for Android (Kotlin + Jetpack Compose). Every note has a music section: paste a
YouTube link, it plays right there, and you write below it.

## Run it

1. Install **Android Studio** (Koala 2024.1 or newer).
2. `File > Open` and pick this `TuneNotes` folder. Let Gradle sync (needs internet the first time).
3. Plug in a phone (USB debugging on) or start an emulator, then press **Run**.

To get an installable APK: `Build > Build APK(s)` (debug), or `./gradlew assembleRelease`
for the smaller, minified build (signed with the debug key, fine for personal use).

## How it works

- `MusicSection.kt`: paste field, then a player with custom play/pause, seek bar and ±10s.
  Uses the official YouTube IFrame player via the `android-youtube-player` library.
- `YouTube.kt`: link parsing (watch, youtu.be, shorts, music.youtube.com...) and title lookup.
- `NoteStore.kt`: notes saved as a JSON file on the device. No database, instant start-up.
- `NotesListScreen.kt` / `EditorScreen.kt`: the two screens. Long-press a card to pin it.

## Good to know

- Some videos block embedding (common for label-owned music videos). The card then shows
  "Open in YouTube".
- The video stays visible (you can shrink it) because YouTube's embed terms don't allow hiding it.
- Audio keeps playing while you are in the app and usually when you switch apps briefly,
  but Android may pause it when the screen is off. True lock-screen playback would need a
  foreground service, which is the natural next feature to add.
