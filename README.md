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
