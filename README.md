# StudyFlow 🎓

A modern, offline-first study companion built with **Kotlin Multiplatform** and **Compose
Multiplatform**. One shared codebase runs seamlessly on **Android** and **Desktop (macOS / Windows /
Linux)** with local peer-to-peer Wi-Fi synchronization, an AI study assistant, and gamification
rewards.

---

## 🌟 Key Features

- 📅 **Timetable & Schedule Management**:
    - Weekly class scheduling with room details, color tags, and automated time-clash detection.
- 🎯 **Deadline & Assignment Tracker**:
    - Organizes assignments by urgency (*Overdue*, *Next 7 Days*, *Later*).
    - Priority flags (High, Medium, Low), course association, exam countdowns, and notes.
- ⏱️ **Focus Timer (Pomodoro)**:
    - Customizable focus and break intervals powered by an internal deterministic state machine.
    - Historical session logging and 7-day focus analytics.
- 🤖 **AI Study Assistant**:
    - Integration with Anthropic's Claude API for instant tutoring, study plans, summary creation,
      and quiz generation.
    - Local study note archiving for saving useful AI answers offline.
- 🏆 **Gamification & Achievements**:
    - Earn XP, level up, unlock achievement badges, and track daily study streaks.
    - Idempotent XP tracking ensures rewards sync seamlessly across devices without double-counting.
- 🔄 **Local Peer-to-Peer Sync (No Cloud Required)**:
    - Phone ⇄ Desktop synchronization over local Wi-Fi via an embedded Ktor HTTP server and client.
    - Protected by a 6-digit pairing code header (`X-Sync-Code`) with Last-Write-Wins merge logic
      and tombstone deletions.
- 🎨 **Personalization & Theme Customization**:
    - Light, Dark, and System color modes with custom accent color palettes.
    - Adaptive layouts for mobile screens and multi-pane desktop windows.

---

## 🏗️ Architecture & Project Structure

The project follows clean architecture principles tailored for Kotlin Multiplatform (KMP) and
Compose Multiplatform:

```
StudyFlow/
├── build.gradle.kts                # Root build configuration
├── settings.gradle.kts             # Subproject inclusions and plugin management
├── gradle/libs.versions.toml       # Version catalog (Dependencies & SDK versions)
└── composeApp/
    ├── build.gradle.kts            # Compose Multiplatform build file
    └── src/
        ├── commonMain/              # ~90% of codebase shared across all targets
        │   └── kotlin/com/studyflow/
        │       ├── AppContainer.kt  # Root Dependency Injection container
        │       ├── model/           # Data models (@Serializable, Syncable interface)
        │       ├── logic/           # Domain logic (Timetable clash check, Schedule, Stats)
        │       ├── focus/           # FocusTimer & TimerMachine state engine
        │       ├── data/            # StudyRepository (single source of truth), SettingsStore
        │       ├── sync/            # Merge engine, SyncController (Ktor client)
        │       ├── ai/              # AssistantClient (Ktor HTTP client for Claude API)
        │       ├── util/            # Utilities (Format, TimeUtil, Pickers, Ids)
        │       └── ui/              # Compose Multiplatform UI (Screens, Cards, Components)
        │
        ├── androidMain/             # Android platform implementation
        │   ├── AndroidManifest.xml  # Manifest definition with network security config
        │   └── kotlin/com/studyflow/# MainActivity, StudyFlowApp, Storage.android, Notifier
        │
        ├── desktopMain/             # Desktop (JVM) platform implementation
        │   └── kotlin/com/studyflow/# Main window entry, DesktopSyncHost (Embedded Ktor Server)
        │
        ├── iosMain/                 # iOS (Native) platform implementation
        │   └── kotlin/com/studyflow/# MainViewController (Swift export), Storage.ios, PlatformActions
        │
        └── commonTest/              # Cross-platform unit test suite
            └── kotlin/com/studyflow/# State machine, merge engine, and logic tests
```

---

## 🛠️ Tech Stack & Libraries

- **Language & Runtime**: Kotlin 2.1+, JDK 17+
- **UI Toolkit**: Compose Multiplatform 1.8+ (Material 3)
- **Asynchronous & Reactive Programming**: Kotlin Coroutines & `StateFlow`
- **Serialization**: `kotlinx.serialization` (JSON)
- **Date & Time**: `kotlinx-datetime`
- **Networking**: Ktor Client (OkHttp for Android, CIO for Desktop, Darwin for iOS) & Ktor Server (
  Desktop Sync Hub)
- **Testing**: `kotlin.test` framework

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (2024.1+ recommended) with the **Kotlin Multiplatform** plugin.
- **JDK 17** or higher.

### Running the App

1. **Desktop App (macOS / Windows / Linux)**:
   ```bash
   ./gradlew :composeApp:run
   ```
2. **Android App**:
    - Open the project in Android Studio.
    - Select the `composeApp` run configuration and select your connected Android device or
      emulator.
    - Click **Run**.

3. **Running Automated Tests**:
   ```bash
   ./gradlew :composeApp:desktopTest
   ```

---

## 🔁 How Local Peer-to-Peer Sync Works

StudyFlow allows your Android phone and Desktop computer to keep data in sync on the same Wi-Fi
network without relying on any cloud provider or account registration:

1. **Start the Desktop Sync Hub**:
    - Open StudyFlow on Desktop, navigate to the **Sync** tab, and toggle **"This computer is the
      sync hub"**.
    - The desktop displays an IP address (e.g., `192.168.1.20:8765`) and a randomly generated *
      *6-digit pairing code**.
2. **Connect from Android**:
    - Open StudyFlow on your phone (connected to the same Wi-Fi).
    - Go to **Sync**, enter the IP address and pairing code displayed on the desktop, and tap **Sync
      Now**.
3. **Synchronization Logic**:
    - Items use deterministic UUIDs and carry an `updatedAt` timestamp and a `deleted` flag (
      tombstone).
    - Sync merging follows **Last-Write-Wins (LWW)** semantics, making the sync operation idempotent
      and network fault-tolerant.

*Note for Android Emulator*: Use `10.0.2.2:8765` as the target host IP when testing sync against a
desktop hub running on the local host.

---

## 🤖 Configuring the AI Study Assistant

1. Obtain an API Key from [console.anthropic.com](https://console.anthropic.com/).
2. Open StudyFlow, navigate to **Settings**, and paste your API key under **AI Study Assistant**.
3. Select your preferred model (e.g., `claude-3-5-sonnet-20241022` or `claude-3-5-haiku-20241022`).
4. Access the **Assistant** tab to ask homework questions, request topic summaries, or generate
   practice quizzes. Useful answers can be saved directly into your local **Notes**.

---

## 🛡️ Source Control & Security Audit

A repository audit was performed to ensure code quality, security, and version control hygiene:

- ✅ **Secret Leak Inspection**: **Passed**. No API keys, passwords, or authentication tokens are
  hardcoded in the source code.
- ✅ **Malware / Command Execution Audit**: **Passed**. No malicious shell commands, dynamic code
  evaluations, or unauthorized remote invocations exist.
- ⚠️ **Git Hygiene Recommendations**:
    - `local.properties`: Contains developer-specific SDK paths; should be removed from git
      tracking (`git rm --cached local.properties`).
    - `.idea/` workspace files (e.g., `deviceStreaming.xml`) and `.kotlin/` sessions should be
      excluded via `.gitignore`.
- ⚠️ **Stray Artifact Cleanup**:
    - Unused zero-byte duplicate files in `src/commonMain/kotlin/com/studyflow/ui/` resulting from
      extracted patches should be pruned.
    - `personalization-update.zip` in `commonMain/kotlin/com/studyflow/ui/` should be removed from
      source control.

---

## 📄 License

Distributed under the MIT License. See `LICENCE` for details.
