# 🎮 FPKGi Manager — Android

<p align="center">
  <img src="https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-1.7-4285F4?logo=jetpackcompose" />
  <img src="https://img.shields.io/badge/license-MIT-blue" />
  <img src="https://img.shields.io/badge/version-6.0-00D4FF" />
</p>

<p align="center">
  Native Android manager for PS4 Fake-PKG libraries.<br>
  Browse your catalogue, check patch history via OrbisPatches,<br>
  and send PKGs directly to your PS4 over Wi-Fi with no PC required.
</p>

> 🖥️ Looking for the **desktop (Python) app**? → [FPKGi-for-PY](https://github.com/RastaFairy/FPKGi-for-PY)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 📂 **Dual JSON format** | FPKGi dict format + PS4PKGInstaller list format |
| 📡 **FTP transfer** | Send PKGs directly to PS4 over local Wi-Fi — no PC needed |
| ⬇️ **Local download** | Save PKGs to device storage |
| ✅ **Availability check** | HTTP HEAD verification per game — live green/red status |
| 🔍 **OrbisPatches** | Browse patch history and release notes per title |
| 🌍 **Multilingual UI** | Spanish and English included |
| 🎨 **PS4 dark theme** | Material 3 design inspired by the PS4 XMB interface |
| ⚙️ **Persistent settings** | FTP host, port, credentials and remote path saved via DataStore |

---

## 📋 Requirements

### Device
- Android 10+ (API 29) — compatible with any Android device since 2020

### For FTP transfer
- PS4 with an active FTP server:
  - **GoldHEN FTP Server** *(recommended)*
  - PS4FTP Homebrew
  - Any FTP server running on port 2121
- PS4 and Android device on the **same Wi-Fi network**

### Build
- Android Studio Hedgehog (2023.1) or newer
- JDK 17
- Android SDK: API 29 (min) – API 35 (compile)
- Kotlin 2.0
- Jetpack Compose 1.7

---

## 🚀 Build & Install

### From Android Studio

1. Clone the repository:

```bash
git clone https://github.com/RastaFairy/FPKGi-A-.git
cd FPKGi-A-
```

2. Open the project in Android Studio.
3. Sync Gradle, then **Run → Run 'app'**.

### From command line

```bash
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

### Release build

```bash
./gradlew assembleRelease
# Sign with your keystore before distributing
```

---

## 📡 FTP Configuration

Open **Settings** in the app and fill in:

| Field | Default | Description |
|-------|---------|-------------|
| Host | `192.168.1.XXX` | Your PS4's local IP address |
| Port | `2121` | FTP server port |
| Username | `anonymous` | Leave blank for GoldHEN |
| Password | *(empty)* | Leave blank for GoldHEN |
| Remote path | `/data/pkg` | Destination folder on the PS4 |
| Passive mode | ✓ enabled | Required for most home networks |

---

## 📁 Project Structure

```
FPKGi-A-/
├── app/
│   └── src/main/
│       ├── java/com/fpkgi/manager/
│       │   ├── MainActivity.kt          # Entry point
│       │   ├── MainViewModel.kt         # MVVM state + business logic
│       │   ├── data/
│       │   │   ├── model/Models.kt      # Data classes (Game, Patch, Config)
│       │   │   └── repository/
│       │   │       └── SettingsRepository.kt   # DataStore persistence
│       │   ├── i18n/
│       │   │   └── StringResources.kt   # Multilingual string system
│       │   ├── network/
│       │   │   ├── FtpDownloadService.kt  # Foreground FTP transfer service
│       │   │   └── OrbisClient.kt         # OrbisPatches HTTP client
│       │   └── ui/
│       │       ├── components/
│       │       │   └── Components.kt    # Reusable Compose components
│       │       ├── screens/
│       │       │   ├── GameListScreen.kt
│       │       │   ├── GameDetailScreen.kt
│       │       │   ├── DownloadsScreen.kt
│       │       │   └── SettingsScreen.kt
│       │       └── theme/Theme.kt       # Material 3 PS4-inspired dark theme
│       └── utils/
│           └── JsonParser.kt            # Dual JSON format parser
├── gradle/libs.versions.toml            # Version catalog
└── .github/
    ├── workflows/
    │   ├── ci.yml                       # Debug APK on every push to main
    │   └── release.yml                  # Release APK on tag v*
    └── ISSUE_TEMPLATE/
        ├── bug_report.yml
        └── feature_request.yml
```

---

## 🏗️ Architecture

The app follows the **MVVM** pattern with a unidirectional data flow:

```
UI Screens (Compose)
       ↕
MainViewModel (StateFlow)
       ↕
Repositories / Network clients
       ↕
DataStore / FTP / HTTP
```

- **`MainViewModel`** — single source of truth for game list, download states, patch data
- **`FtpDownloadService`** — Android foreground service ensuring transfers survive screen-off
- **`OrbisClient`** — coroutine-based HTTP client for OrbisPatches
- **`JsonParser`** — parses both JSON catalogue formats into unified `Game` model

---

# Changelog — FPKGi Manager (Android)

All notable changes are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

Repository: [github.com/RastaFairy/FPKGi-A-](https://github.com/RastaFairy/FPKGi-A-)

> The Android app is a port of the Python desktop edition.
> For the full history of the original project see:
> [FPKGi-for-PY / CHANGELOG.md](https://github.com/RastaFairy/FPKGi-for-PY/blob/main/CHANGELOG.md)

---

## [v6.0] — Initial Android release

### Added — Complete native Android application

**Platform & architecture**
- Minimum target: **Android 10 (API 29)** — covers all Android devices since 2020
- Written in **Kotlin 2.0** with **Jetpack Compose 1.7**
- **MVVM architecture** with `StateFlow` for unidirectional data flow
- Gradle **Kotlin DSL** (`build.gradle.kts`) with `libs.versions.toml` version catalog
- ProGuard rules for release builds
- Network security config — cleartext traffic allowed for local FTP/HTTP connections

**UI**
- `GameListScreen` — searchable, sortable game list loaded from JSON catalogue
- `GameDetailScreen` — cover art, full game metadata, patch history, download/FTP action buttons
- `DownloadsScreen` — active and queued transfer tracking with progress indicators
- `SettingsScreen` — FTP host, port, credentials, remote path, passive mode toggle
- Material 3 dark theme inspired by the PS4 XMB interface (`ui/theme/Theme.kt`)
- Reusable Compose components (`ui/components/Components.kt`)

**Core features**
- `JsonParser` — parses both FPKGi dict format and PS4PKGInstaller list format into a unified `Game` model (same dual-format support as the Python edition)
- `OrbisClient` — coroutine-based HTTP client for OrbisPatches patch data and release notes
- `FtpDownloadService` — Android **foreground service** for reliable background FTP transfers; transfers survive screen-off and app backgrounding
- `SettingsRepository` — persistent settings via **DataStore Preferences**

**Internationalisation**
- `StringResources.kt` + `i18n` module — type-safe multilingual string system
- Spanish and English included at launch
- Architecture supports adding further languages without structural changes

**Data model** (`data/model/Models.kt`)
- `Game` — unified game representation with title ID, name, version, region, size, firmware, PKG URL, cover URL
- `Patch` — patch entry with version, firmware, size, date, release notes
- `FtpConfig` — FTP connection parameters

**CI/CD** (`.github/workflows/`)
- `ci.yml` — builds debug APK on every push to `main` and on pull requests
- `release.yml` — builds and signs release APK, uploads to GitHub Releases on `v*` tags

**Issue templates** (`.github/ISSUE_TEMPLATE/`)
- `bug_report.yml` — structured bug report with reproduction steps, environment
- `feature_request.yml` — feature proposal with use-case description

---

## Roadmap / Planned

- Additional UI languages (DE, FR, IT, JA, KO, RU, ZH)
- AI patch note translation via Anthropic API (matching Python v5.11 feature)
- Animated PS4-style background (matching Python v5.12 feature)
- Availability batch-check for entire catalogue
- Dark/light theme toggle

---

## 🤝 Contributing

1. Fork the repository and create a feature branch.
2. Ensure the project builds with `./gradlew assembleDebug`.
3. Open a Pull Request against `main`.

Use the **Bug Report** or **Feature Request** issue templates.

---

## 📜 Credits

| Role | Name |
|------|------|
| Original concept (PSP Homebrew) | [Bucanero](https://github.com/bucanero) |
| PS4 / PS5 port | ItsJokerZz |
| Python edition | RastaFairy |
| Android port | RastaFairy |

---

## 📄 License

[MIT](LICENSE) © RastaFairy
