# KindleVibe Reader

An offline-first EPUB reader for Android with privacy guarantees.

## Build Commands

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Run tests
./gradlew test

# Clean build
./gradlew clean
```

## Project Structure

- **core/**: Core utilities and shared components
  - `App.kt`: Application class with dependency injection setup
  - `Dispatchers.kt`: Coroutine dispatchers wrapper
  - `Result.kt`: Sealed class for handling API/operation results

- **data/**: Data layer with database and repositories
  - `db/`: Room database setup with entities, DAOs, and migrations
  - `prefs/`: DataStore preferences wrapper
  - `repo/`: Repository pattern implementation for data access

- **reader/**: EPUB reading engine integration
  - `ReadiumInit.kt`: Readium toolkit initialization
  - `LocatorCodec.kt`: Location/position encoding/decoding
  - `PreferencesMapper.kt`: Reading preferences mapping

- **ui/**: User interface layer with Jetpack Compose
  - `theme/`: Material3 theme, colors, and typography
  - `nav/`: Navigation setup and route definitions
  - `screens/`: Main app screens (Library, Reader, Settings, About)
  - `components/`: Reusable UI components

- **viewmodel/**: ViewModels for state management
  - `LibraryViewModel.kt`: Library screen state and logic
  - `ReaderViewModel.kt`: Reader screen state and logic
  - `SettingsViewModel.kt`: Settings screen state and logic

- **MainActivity.kt**: Single-activity entry point hosting the navigation graph

## Features

- Offline-first EPUB reading
- Privacy-focused (no internet permissions)
- Material3 design with multiple reading themes
- Book library management
- Reading progress tracking
- Bookmark support (planned)

## Tech Stack

- **Kotlin**: 2.0.x
- **Jetpack Compose**: Modern UI toolkit
- **Room**: Local database
- **DataStore**: Preferences storage
- **Navigation Compose**: Screen navigation
- **Readium**: EPUB rendering engine
- **Coroutines**: Asynchronous programming
- **Material3**: Design system

## Privacy

This app is designed to be completely offline and does not require internet permissions. All reading data stays on your device.
