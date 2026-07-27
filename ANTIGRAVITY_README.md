# 🌌 Antigravity IDE Project Summary: Jellyfin Offline Client

**Repository**: [JellyfinOfflineClient](https://github.com/krutideepanpanda/JellyfinOfflineClient)  
**Current Release Tag**: `v0.2.0-alpha` (Alpha 2 - Official Feature Parity)  
**Target Architecture**: Native Android 15 (API 35), Optimized for Google Pixel 10  
**Core Framework**: Kotlin 1.9.23, Jetpack Compose (Material 3), AndroidX Media3 (ExoPlayer), Room SQLite, WorkManager  

---

## 🎯 1. Project Overview & Mission
This application is a specialized, high-performance, native Android client for Jellyfin media servers, engineered specifically for **TV Shows, Anime, and Movies**. It differentiates itself from standard web wrappers by implementing a **smart predictive offline caching engine** and zero-latency local disk playback while maintaining 100% login and browsing feature parity with the official Jellyfin client.

---

## 🚀 2. Alpha 2 Feature Parity (`v0.2.0-alpha`)

### A. Authentication & Server Discovery (`LoginScreen.kt` & `ServerDiscoveryManager.kt`)
*   **UDP / SSDP Auto-Discovery**: Broadcasts UDP packets on port `7359`. Discovered local servers appear as interactive 1-click Material 3 selection chips, eliminating manual URL configuration.
*   **Quick Connect PIN Login**: Interfaces with `api.quickConnectApi` to generate a 6-digit authorization PIN and polls server state every 3 seconds until verified by a TV or web browser.

### B. Multi-Tab Navigation Architecture (`BottomNav.kt` & `Navigation.kt`)
*   **🏠 Home Tab**: Utilizes `userLibraryApi` to fetch user libraries, applying strict filters to separate **TV Shows** and **Movies**. Renders interactive **Continue Watching** and **Next Up** carousels loaded asynchronously via **Coil (`AsyncImage`)**.
*   **📥 Downloads Tab**: Real-time download management console showing active worker tasks, byte-stream completion percentage bars, local device storage usage, and 1-click playback triggers for downloaded media.
*   **⚙️ Settings Tab**: Server connection details, streaming quality selector, Smart Sync automation rules, and account logout.

### C. Smart Sync Engine (`SmartSyncManager.kt`, `DownloadWorker.kt`, `OfflineEpisodeDao.kt`)
*   **Predictive Ingestion**: Automatically enqueues the next **3 sequential episodes** of an active series into Android `WorkManager` for background downloading over unmetered Wi-Fi.
*   **Automated Storage Purging**: When playback completion reaches **`>= 90%`**, `onEpisodeProgress()` automatically deletes the local `.mp4` file from `Downloads/JellyfinOffline` and purges the Room database record.
*   **Real-time Byte Tracking**: Calculates live download percentages and persists them to Room SQLite (`progressPercentage: Float`) so UI cards reflect live download status across app restarts.

### D. Advanced Media Player (`PlayerScreen.kt`)
*   **Hybrid Stream Resolution**: Evaluates `OfflineEpisodeDao`. If media exists on disk, it initializes `ExoPlayer` with `MediaItem.fromUri(Uri.fromFile(File(path)))` for zero-latency offline viewing; otherwise, it resolves remote network streaming URLs.
*   **On-Screen Overlay Controls**: Includes custom floating buttons for ⏪ **-10s Fast Rewind**, ⏩ **+10s Fast Forward**, and a **Playback Speed Selector** (`1.0x -> 1.25x -> 1.5x -> 2.0x`).

---

## 🛠️ 3. Antigravity IDE & Windows CLI Build Protocols

### The Windows File Lock Challenge
When compiling via Gradle CLI on Windows, lingering Java daemons or open Android Studio instances frequently hold file locks on `app\build\kotlin\compileDebugKotlin\cacheable`, causing fatal `java.io.IOException: Unable to delete directory` errors.

### The Authoritative Clean Build Command
All Antigravity agents and developers **must** use the following PowerShell command sequence to terminate orphaned Java daemons, release file locks, and compile in CLI mode:

```powershell
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force; Start-Sleep -Seconds 2; Remove-Item -Recurse -Force app\build -ErrorAction SilentlyContinue; .\gradlew.bat assembleDebug --no-daemon
```

*   **Compiled APK Location**: `app\build\outputs\apk\debug\app-debug.apk`

---

## 🤖 4. Configured Antigravity Ecosystem

This project contains an embedded Antigravity IDE customization suite:

### Subagents (`define_subagent`)
1.  **`android-build-resolver`**: Dedicated Gradle build doctor. Diagnoses compilation errors, resolves KSP Room warnings, executes the Windows daemon termination protocol, and builds debug APKs via wrapper.
2.  **`jellyfin-compose-auditor`**: Dedicated UI and offline architecture auditor. Verifies Material 3 compliance, responsive scaling for Pixel 10 viewports, Coil image integration, and offline SQLite synchronization logic.

### Custom Workflow Skills
*   **`.skills/jellyfin-android-workflow/SKILL.md`**: Codifies the Windows CLI build protocol, Room database percentage schemas, and ExoPlayer hybrid playback rules.
*   **`.gitignore`**: Configured to ignore local AI workspaces (`.antigravity/`, `.gemini/`, `.agents/`, `.skills/`) while tracking official Gradle wrapper executables (`gradlew`, `gradlew.bat`).

---
*Generated by Antigravity AI for Google DeepMind Advanced Agentic Coding.*
