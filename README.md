# FPKGi Manager — Android

<p align="center">
  <img src="assets/banner.png" alt="FPKGi Manager" width="600"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-6.5.2-blue?style=flat-square"/>
  <img src="https://img.shields.io/badge/platform-Android%2010%2B-green?style=flat-square"/>
  <img src="https://img.shields.io/badge/language-Kotlin-orange?style=flat-square"/>
  <img src="https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square"/>
</p>

> PS4 FPKG game manager for Android. Browse your library, verify availability, download packages, send them to your PS4 via FTP, and check for patch updates from OrbisPatches — all without leaving the app.

---

## 📋 Table of Contents

- [Features](#-features)
- [Screenshots](#-screenshots)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Usage](#-usage)
- [Project Structure](#-project-structure)
- [Building](#-building)
- [Changelog](#-changelog)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

| Feature | Details |
|---|---|
| 📚 **JSON Library** | Supports FPKGi dict format and PS4PKGInstaller list format |
| 🔍 **Search & Sort** | Filter by name, Title ID or region; sort by any column |
| ✅ **Availability Check** | HTTP HEAD verification of PKG download URLs |
| ⬇️ **Download Manager** | Background downloads with pause/resume support |
| 📡 **FTP to PS4** | Send PKGs directly to your console via FTP (ftpd required) |
| 📂 **Local PKG Browser** | Scan device storage for `.pkg` files and upload via FTP |
| 🔄 **OrbisPatches** | Patch info per game: version, required FW, size, date, notes |
| 🌐 **6 Languages** | Spanish, English, German, French, Italian, Japanese |
| 🆕 **Auto-Update** | Detects new app versions from GitHub Releases and installs in-app |
| 🎨 **Dark Theme** | Navy/cyan/gold colour palette optimised for OLED |

---

## 📱 Screenshots

> *(Add your own screenshots in `assets/` and link them here)*

---

## 📋 Requirements

- Android **10 (API 29)** or higher
- A JSON file exported from FPKGi or PS4PKGInstaller
- For FTP upload: a PS4 running a compatible FTP server (e.g. `ftpd`, `PS4FTP`, `goldhen ftpd`)
- For in-app install: allow installation from unknown sources when prompted

---

## 📥 Installation

### From GitHub Releases *(recommended)*

1. Go to [Releases](https://github.com/RastaFairy/FPKGi-A-/releases/latest)
2. Download the latest `.apk`
3. Install it on your Android device
4. Allow installation from unknown sources if prompted

### Build from source

See [Building](#-building) below.

---

## 🚀 Usage

### Loading your library

1. Open the app
2. Tap the **folder icon** in the top bar
3. Select your `games.json` file

The app supports both FPKGi format (dict with `DATA` key) and PS4PKGInstaller format (flat list of packages).

### Checking a game

Tap any game to open the detail screen. From there you can:

- **Verify availability** — checks if the PKG URL responds
- **Download PKG** — starts a background download, optionally sent directly via FTP
- **View OrbisPatches** — shows all known patches with version, required firmware, size, creation date and patch notes

### FTP setup

1. Go to **Settings → FTP Configuration**
2. Enter your PS4's IP, port (default 2121) and remote path (default `/data/pkg`)
3. Tap **Test connection**
4. Enable FTP — from now on all downloads go directly to your PS4

### Auto-update

When a new version is available on GitHub, a dialog appears on launch showing the changelog. Tap **"View on GitHub"** and the app downloads and installs the new APK automatically — no browser required.

---

## 🗂 Project Structure

```
app/src/main/java/com/fpkgi/manager/
├── MainActivity.kt              # Navigation host, update dialog
├── MainViewModel.kt             # State, downloads, OrbisPatches, update checker
├── data/
│   ├── model/Models.kt          # Game, OrbisPatch, DownloadItem, FtpConfig…
│   └── repository/
│       └── SettingsRepository.kt
├── i18n/
│   └── StringResources.kt       # 6-language string definitions
├── network/
│   ├── FtpDownloadService.kt    # Foreground service: download + FTP upload
│   ├── OrbisClient.kt           # 2-tier scraper (JSON API + WebView DOM)
│   ├── OrbisWebViewClient.kt    # Chromium WebView + DOM polling
│   └── UpdateChecker.kt         # GitHub Releases API client
├── ui/
│   ├── screens/
│   │   ├── GameListScreen.kt
│   │   ├── GameDetailScreen.kt
│   │   ├── DownloadsScreen.kt
│   │   ├── LocalPkgBrowserScreen.kt
│   │   └── SettingsScreen.kt
│   └── theme/                   # Colours, typography, shapes
└── utils/
    └── JsonParser.kt            # Dual-format JSON parser
```

---

## 🔨 Building

```bash
# Clone the repo
git clone https://github.com/RastaFairy/FPKGi-A-.git
cd FPKGi-A-

# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

**Requirements:** Android Studio Hedgehog or later, JDK 21, Android SDK 36.

---

## 📜 Changelog

See [CHANGELOG.md](CHANGELOG.md) for the full version history.

---

## 🤝 Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

---

## 📄 License

[MIT](LICENSE) — see the LICENSE file for details.

---

<p align="center">
  Original concept by <strong>Bucanero</strong> (PSP) and <strong>ItsJokerZz</strong> (PS4/PS5) · Android port by <strong>RastaFairy</strong>
</p>
