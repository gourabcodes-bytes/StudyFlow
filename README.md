# StudyFlow

A study planner built with **Kotlin Multiplatform** and **Compose Multiplatform**.
One shared codebase runs on **Android** and **Desktop (Windows / macOS / Linux)**, and the two
versions keep each other in sync over your local Wi-Fi: no account, no cloud.

## Features

- **Timetable**: weekly classes with room, colour tag and clash warnings
- **Deadlines**: assignments grouped into Overdue / Next 7 days / Later, priorities, subjects, notes
- **Focus timer**: Pomodoro-style focus and break phases, saved sessions, 7-day focus chart
- **Today**: what is next, what is due, how much you have focused
- **Offline first**: everything is stored on the device and works without internet
- **Phone ⇄ desktop sync**: the desktop app can act as a sync hub; the phone merges with it
  (newest edit wins, deletions sync too, pairing code protects your data)

## Run it

You need **Android Studio** (or IntelliJ IDEA) with the Kotlin Multiplatform plugin and **JDK 17+**.

1. Open this folder as a project and let Gradle sync.
2. **Desktop:** `./gradlew :composeApp:run`  (Windows: `gradlew.bat :composeApp:run`)
3. **Android:** pick the `composeApp` run configuration and press Run on an emulator or a phone.
4. **Tests:** `./gradlew :composeApp:desktopTest`

## Try the sync

1. Start the desktop app, open the **Sync** tab and switch on **"This computer is the sync hub"**.
   It shows a 6-digit pairing code and an address such as `192.168.1.20:8765`.
2. On the phone (same Wi-Fi) open **Sync**, type that address and the code, press **Sync now**.
3. Add a deadline on either device and sync again, and it appears on both.

Tips: the Android emulator reaches your computer at `10.0.2.2:8765`. If the phone cannot connect,
allow Java through the Windows firewall for private networks (port 8765).

## How it is built

```
composeApp/src
├── commonMain      shared by every platform (~90 % of the code)
│   ├── model/      data classes (@Serializable) and the Syncable interface
│   ├── logic/      timetable and deadline rules (pure functions)
│   ├── focus/      TimerMachine (pure state machine) + FocusTimer (runs it on the clock)
│   ├── data/       StudyRepository (single source of truth), SettingsStore, Storage interface
│   ├── sync/       last-write-wins merge, SyncController (Ktor client), SyncHost interface
│   └── ui/         Compose Multiplatform screens
├── androidMain     Application, Activity, file storage, ringtone alert
└── desktopMain     window, file storage, embedded Ktor server (the sync hub)
```

Where Kotlin Multiplatform's `expect`/`actual` is used: `createStorage()`.
Platform-only code is kept tiny; the desktop-only sync hub plugs in through the shared `SyncHost` interface.

**Sync design.** Every item has an `id`, an `updatedAt` time and a `deleted` flag. Deleting marks the
item instead of removing it, so the deletion can travel to the other device. Merging keeps the newest
copy of each id, which makes syncing idempotent: syncing twice changes nothing.

**Tech:** Kotlin 2.1, Compose Multiplatform 1.8, Material 3, kotlinx-coroutines, kotlinx-serialization,
kotlinx-datetime, Ktor (client on both platforms, server on desktop).

## Known limitations / ideas

- Sync works on the same local network only (plain HTTP protected by a pairing code).
- No system notifications for deadlines yet (the focus timer plays a sound when a phase ends).
- iOS target not added yet: the shared code is ready for it, it needs a Mac with Xcode.
- Natural next steps: deadline notifications, a week-grid timetable view, cloud relay for sync outside Wi-Fi.
