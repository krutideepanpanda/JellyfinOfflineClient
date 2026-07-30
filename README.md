# Jellyfin Offline Client

Jellyfin Offline Client is a specialized Android application designed for high-performance media consumption with an emphasis on automated offline synchronization. The application is built using modern Android development standards to ensure reliability, security, and a superior user experience.

## Core Functionalities

### Smart Offline Synchronization
The application features a proprietary synchronization engine designed to optimize local storage while ensuring content availability:
- **Predictive Background Ingestion**: Utilizing Android WorkManager, the system automatically identifies and downloads the subsequent three episodes of an active series.
- **Automated Lifecycle Management**: Content is automatically purged from local storage once a 90% completion threshold is reached, maintaining storage efficiency.
- **Network Constraint Compliance**: Data transfers are restricted to unmetered networks to prevent unauthorized mobile data consumption.

### High-Performance Media Playback
- **Media3 Integration**: Leverages the AndroidX Media3 (ExoPlayer) framework for robust codec support and adaptive bitrate handling.
- **Hybrid Source Resolution**: The playback engine dynamically evaluates content availability. It prioritizes local assets for instantaneous playback and seamlessly transitions to remote streaming endpoints when necessary.

### User Interface and Experience
- **Declarative UI**: Developed using Jetpack Compose and Material Design 3 guidelines for a consistent and responsive interface.
- **Secure Authentication**: Implements standard Jellyfin authentication protocols for secure session management.

## Technical Specifications

- **Programming Language**: Kotlin 1.9.23
- **UI Framework**: Jetpack Compose
- **Media Engine**: AndroidX Media3
- **Persistence Layer**: Room Persistence Library
- **Background Processing**: WorkManager
- **API Integration**: Official Jellyfin Kotlin SDK
- **Design Pattern**: Model-View-ViewModel (MVVM)

## Installation and Configuration

### Prerequisites
- Android Studio Jellyfish (or more recent versions)
- Java Development Kit (JDK) 21

### Deployment Process
1.  **Source Retrieval**:
    ```bash
    git clone https://github.com/krutideepanpanda/Antigravity-jellyfin-client.git
    ```
2.  **Project Initialization**: Open the project directory within Android Studio.
3.  **Build Execution**: Perform a Gradle build ensuring the environment is configured for JDK 21.
4.  **Device Deployment**: Deploy the compiled APK to an Android device (Minimum SDK 26, Optimized for SDK 34+).

## Architectural Overview

- `ui`: Contains presentation logic and Compose-based screen definitions.
- `sync`: Orchestrates the Smart Sync engine and background workers.
- `data`: Manages the Room database schema and data access objects.
- `JellyfinClientManager`: Singleton facilitating the global API session state.

---
© 2026 Kruti Deepan Panda. This is an unofficial third-party client and is not affiliated with or endorsed by the Jellyfin project.
