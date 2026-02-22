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
