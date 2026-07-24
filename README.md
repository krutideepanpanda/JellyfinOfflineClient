# Jellyfin Offline Client

A native Android Jellyfin client optimized for the latest devices, featuring **Smart Offline Sync**. This app is designed to provide a seamless offline viewing experience by intelligently managing your local storage.

## 🚀 Core Features

### 🧠 Smart Offline Sync
The heart of the app is its intelligent synchronization engine:
- **Predictive Downloading**: When you start watching a series, the app automatically queues and downloads the next 3 episodes in the background using `WorkManager`.
- **Automatic Cleanup**: Once you've watched 90% of an episode, it is automatically deleted from local storage to save space.
- **WiFi Only**: Downloads are restricted to unmetered (WiFi) networks by default to conserve mobile data.

### 🎬 High-Performance Playback
- Powered by **AndroidX Media3 (ExoPlayer)**.
- **Hybrid Playback Logic**: The app automatically detects if an episode is available offline and plays the local file for zero-latency startup. If not downloaded, it falls back to high-quality streaming from the Jellyfin server.

### 📱 Modern UI/UX
- Built entirely with **Jetpack Compose** and **Material 3**.
- Optimized for a clean, content-first experience.
- Supports secure authentication via Server URL, Username, and Password.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Media**: AndroidX Media3 (ExoPlayer)
- **Database**: Room (Offline state tracking)
- **Background Tasks**: WorkManager
- **Networking**: Official Jellyfin Kotlin SDK
- **Architecture**: MVVM (Model-View-ViewModel)

## 🛠 Setup & Installation

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/krutideepanpanda/JellyfinOfflineClient.git
    ```
2.  **Open in Android Studio**: Use Android Studio Jellyfish or newer.
3.  **Build**: Ensure you have **JDK 21** configured in your Gradle settings.
4.  **Run**: Deploy to your Android device or emulator (Android 8.0+ supported, optimized for Android 14+).

## 📂 Project Structure

- `ui/`: Compose screens and ViewModels for Home, Login, Details, and Player.
- `sync/`: The `SmartSyncManager` and `DownloadWorker` responsible for the offline engine.
- `data/`: Room database entities and DAOs for tracking offline content.
- `JellyfinClientManager`: Singleton managing the global Jellyfin API session.

---
*Built with ❤️ for the Jellyfin Community.*
