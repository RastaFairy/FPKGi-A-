# Contributing to FPKGi Manager Android

Thank you for considering contributing. The following guidelines keep the process clean and predictable.

---

## Table of Contents

- [Reporting bugs](#reporting-bugs)
- [Suggesting features](#suggesting-features)
- [Development setup](#development-setup)
- [Pull request process](#pull-request-process)
- [Code style](#code-style)
- [Versioning policy](#versioning-policy)
- [Commit messages](#commit-messages)

---

## Reporting bugs

Before opening an issue:

1. Search [existing issues](https://github.com/RastaFairy/FPKGi-A-/issues) to avoid duplicates.
2. Reproduce the bug on the latest release.

When opening an issue, include:

- App version (shown in Settings)
- Android version and device model
- Steps to reproduce
- Expected behaviour vs actual behaviour
- Logcat output if available (`adb logcat | grep fpkgi`)

---

## Suggesting features

Open a GitHub Issue with the label `enhancement`. Describe:

- The problem it solves
- The proposed behaviour
- Any relevant examples from the Python reference app (`fpkgi_manager_with_ftp.py`)

---

## Development setup

```bash
# 1. Fork and clone
git clone https://github.com/YOUR_USERNAME/FPKGi-A-.git
cd FPKGi-A-

# 2. Open in Android Studio (Hedgehog or later)
# 3. Sync Gradle — all dependencies download automatically

# 4. Build debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

**Requirements:**

| Tool | Minimum version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) |
| JDK | 21 |
| Android SDK | API 36 |
| Kotlin | 2.1.0 |
| Gradle | 8.14 |

---

## Pull request process

1. **Branch naming:** `feature/short-description`, `fix/short-description`, `chore/short-description`
2. One logical change per PR — avoid mixing features and fixes.
3. Update `CHANGELOG.md` and `CHANGELOG_ES.md` under an `[Unreleased]` section.
4. Update `README.md` / `README_ES.md` if the change affects documented behaviour.
5. Bump `versionName` and `versionCode` in `app/build.gradle.kts` following the [versioning policy](#versioning-policy).
6. All existing code must compile without errors (`./gradlew assembleDebug`).
7. The PR description must reference the related issue (`Fixes #123`).

---

## Code style

- **Language:** Kotlin only — no Java.
- **Formatting:** follow the existing file style; 4-space indentation, no trailing whitespace.
- **Imports:** no wildcard imports except for Compose (`import androidx.compose.material3.*` is acceptable).
- **Coroutines:** all network calls must run on `Dispatchers.IO`; all UI state updates on `Dispatchers.Main`.
- **No expression-body + bare return:** Kotlin forbids `return` inside `= expression` bodies. Always use block bodies `{ ... }` when the function contains `return` statements.
- **Duplicate imports:** each import must appear exactly once.
- **i18n:** any user-visible string must be added to `StringResources.kt` in **all 6 languages** (Spanish, English, German, French, Italian, Japanese). No hardcoded strings in composables.

---

## Versioning policy

| Scope of change | Bump |
|---|---|
| Bug fix or minor optimisation | `+0.0.1` |
| New feature or significant improvement | `+0.1` |
| Major feature block or redesign | `+1.0` |

`versionCode` mirrors `versionName` digits: version `6.5.2` → `versionCode = 652`.

---

## Commit messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add pull-to-refresh on OrbisPatches section
fix: remove duplicate Uri import in MainViewModel
chore: bump version to 6.5.3
docs: update CHANGELOG for 6.5.2
```

Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `perf`, `test`.
