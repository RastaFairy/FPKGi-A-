# Changelog

All notable changes to FPKGi Manager Android are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).  
Versioning follows `MAJOR.MINOR.PATCH` — see [versioning policy](#versioning-policy).

---

## [6.5.2] — 2026-02-26

### Added
- **In-app APK installation** — when a new version is detected, the app downloads the `.apk` to its internal cache and launches the system installer on top of the current screen. No browser required, no leaving the app.
- `FileProvider` declaration in `AndroidManifest.xml` to securely serve the downloaded APK to the system installer.
- `REQUEST_INSTALL_PACKAGES` permission.
- Download progress bar inside the update dialog (`LinearProgressIndicator`, 0–100%).
- Error state in the update dialog if the download fails.
- Disabled "Update" button while download is in progress to prevent double-taps.
- Fallback to browser if the GitHub release has no `.apk` asset attached.

### Changed
- Update dialog now distinguishes three states: *idle*, *downloading*, *error*.
- "View on GitHub" button triggers in-app download instead of opening the browser (when an APK asset exists).
- `StringResources`: added `updateDownloading` and `updateError` in all 6 languages.

### Fixed
- `android.net.Uri` and `java.io.File` imported twice in `MainViewModel.kt`.

---

## [6.5.1] — 2026-02-26

### Added
- **DOM polling** in `OrbisWebViewClient` — instead of a fixed 3,500 ms delay, the script is evaluated every 400 ms as soon as `onPageFinished` fires. Stops as soon as `.patch-wrapper` elements appear. Typical load time drops from ~4–5 s to ~1–2 s.
- **Session cache** in `OrbisClient` — `sessionCache: Map<String, OrbisResult>` stores results in memory for the lifetime of the app process. Navigating back to the same game is instantaneous. Exposed `invalidateCache()` for future pull-to-refresh support.

### Changed
- `OrbisWebViewClient`: initial wait after `onPageFinished` reduced from 3,500 ms to 600 ms.
- Maximum polling attempts set to 25 (10 s ceiling).

---

## [6.5] — 2026-02-26

### Added
- **App update checker** (`UpdateChecker.kt`) — queries `https://api.github.com/repos/RastaFairy/FPKGi-A-/releases/latest` on startup. Compares version segments numerically (not lexicographically), so `6.10 > 6.9` works correctly. Text suffixes (`-ftp`, `-beta`) are ignored during comparison.
- Update dialog with release changelog (up to 600 characters).
- `updateInfo`, `updateProgress` `StateFlow`s in `MainViewModel`.
- `checkForAppUpdate()` and `dismissUpdate()` in `MainViewModel`.
- Update strings in all 6 languages: `updateTitle`, `updateMessage`, `updateChangelog`, `updateConfirm`, `updateLater`.

### Changed
- `versionCode` bumped to `650`, `versionName` to `"6.5"`.

---

## [6.4.3] — 2026-02-26

### Fixed
- OrbisPatches showing identical patch lists across different games (e.g. both *A Hat in Time* and *A Monster's Expedition* showing the same v05.05, v12.19… list).
  - **Root cause 1:** `OrbisWebViewClient` was not clearing its WebView cache between requests. Fixed with `clearCache(true)`, `clearHistory()`, `WebStorage.deleteAllData()` and `cacheMode = LOAD_NO_CACHE`.
  - **Root cause 2:** The JS extraction script was reading `window.__NUXT__` globally, which contains npm/webpack dependency versions (e.g. `"tailwindcss": "14.09"`) that matched the version regex. Fixed by switching to pure DOM selectors.
  - **Root cause 3:** `\d{2}\.\d{2}` regex captured any two-digit version string. Added `isValidPs4Version()` validator restricting the major component to `01–20`.
- `OrbisResult` from the previous game persisting when navigating to a new game. Fixed with `DisposableEffect { onDispose { viewModel.clearOrbisResult() } }` in `GameDetailScreen`.

---

## [6.4.2] — 2026-02-26

### Changed
- Removed **"Download this patch"** button from every `PatchCard`. The section was informational only — download of individual patch PKGs was out of scope.
- `PatchCard` parameter `onDownload` removed entirely; `btnDownloadPatch` string replaced by `patchNoNotes` in all 6 languages.
- Latest patch card now **expands by default**.
- FW compatibility indicator now shows 🟢 / 🔴 emoji prefix matching the Python reference app behaviour.
- "No patch notes" shown in italic when `notes` is blank (previously nothing was shown).
- Version badge comparison made more robust: strips leading zeros and `v` prefixes before comparing.

---

## [6.4.1] — 2026-02-26

### Fixed
- `OrbisClient.kt` line 178: `return null` inside expression body (`= try { ... }`). Kotlin forbids bare `return` in expression bodies. All three affected functions (`tryJsonApi`, `tryHttpPage`, `httpGet`) converted to block bodies.
- `LocalPkgBrowserScreen.kt` lines 388 and 473: `NavyLight` colour reference unresolved. Replaced with `NavyMid`.
- `parseDomPatches` function: same expression-body + `return` pattern fixed.

---

## [6.4] — 2026-02-24

### Added
- **OrbisPatches WebView tier** (`OrbisWebViewClient.kt`): Android WebView renders the full page (Chromium, bypasses Cloudflare), then a JS script extracts patch data using the exact same CSS selectors as the Python reference parser (`_parse_orbis_html`):
  - `.patch-wrapper` — patch container
  - `a.patch-link[data-contentver]` — version
  - `.patch-container.latest` — latest badge
  - `.col-auto.text-end` [0, 1, 2] — size, required FW, creation date
  - `a.changeinfo-preview[data-patchnotes-charcount > 0]` — patch notes
- Cache cleared before each request (`clearCache`, `clearHistory`, `LOAD_NO_CACHE`) to guarantee fresh data.
- **Local PKG Browser** (`LocalPkgBrowserScreen.kt`): scans `Downloads/`, app external dirs, `PKG/`, `PS4/` and `FPKGI/` folders. Displays name, human-readable size and modification date. Per-file FTP upload button.
- `LocalPkgFile` data class with computed `humanSize` and `humanDate`.
- 9 new i18n strings for the PKG browser in all 6 languages.
- `uploadLocalPkg()` method in `MainViewModel`.

### Changed
- `OrbisClient` pipeline simplified to 2 tiers: JSON API (fast path) → WebView DOM.
- HTTP-only tier removed (Cloudflare blocked it reliably; WebView replaces it completely).
- `SettingsScreen`: "Send local PKG" replaced with navigation button to the new PKG browser screen.

---

## [6.3] — 2026-02-23

### Added
- **6-language i18n** (`StringResources.kt`): Spanish, English, German, French, Italian, Japanese. Language selection persisted in `DataStore`.
- **Pause/resume downloads** in `FtpDownloadService`.
- **FTP upload** of locally stored `.pkg` files to the PS4.
- Icon cache management in Settings (clear disk + memory cache, show cache size).
- `OrbisClient` tier 1: JSON API at `orbispatches.com/api/patch.php`.
- `OrbisClient` tier 2: HTTP page scraping with full Chrome headers.

### Changed
- `MainViewModel` now passes `Application` context to `OrbisClient` for WebView support.
- Navigation updated: `pkgbrowser` route added to `FPKGiNavHost`.

---

## [6.2] — 2026-02-23

### Added
- `FtpDownloadService` foreground service for background downloads and direct FTP transfer to PS4.
- FTP configuration screen (host, port, user, password, remote path, passive mode, timeout, test connection).
- `DownloadsScreen` with per-item progress, pause, resume and cancel.
- `DownloadItem` and `FtpConfig` data models.
- `SettingsRepository` using `DataStore` for persistent preferences.

---

## [6.1] — 2026-02-21

### Added
- `GameDetailScreen`: availability check, PKG download button, OrbisPatches section with expandable `PatchCard` per version.
- `PatchCard`: version, FW compatibility colour chip, size, creation date, expandable notes section.
- `OrbisClient`: basic HTTP scraper with `User-Agent` spoofing.
- `OrbisResult` / `OrbisPatch` data models.
- Availability status (`CHECKING`, `AVAILABLE`, `UNAVAILABLE`) in `Game` model.
- Coil-based icon loading from PKG URL thumbnails with disk cache.

### Fixed
- Material3 theme parent causing AAPT resource linking failure.
- Type inference errors in Kotlin 2.1.0 for `OrbisClient`.
- Missing `LocalUriHandler` import in `GameDetailScreen`.

---

## [6.0] — 2026-02-21

### Added
- Initial Android port of FPKGi Manager (previously Python/Tkinter desktop app).
- `GameListScreen` with search, multi-column sort and game count.
- Dual JSON format support: FPKGi dict (`DATA` key) and PS4PKGInstaller flat list.
- `JsonParser` with automatic format detection and human-readable size formatting.
- Navy/cyan/gold dark `FPKGiTheme` (Jetpack Compose Material3).
- `NavHost` navigation with slide animations.
- Edge-to-edge display support.
- `ACTION_VIEW` intent filter for opening `.json` files directly from the file manager.

---

## Versioning Policy

| Change scope | Version bump |
|---|---|
| Bug fix, minor optimisation | `+0.0.1` |
| New feature or significant improvement | `+0.1` |
| Major feature block or redesign | `+1.0` |
