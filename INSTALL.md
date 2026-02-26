# Installation & Setup Guide

## Installing the APK

### Prerequisites

- Android device running **Android 10 (API 29)** or higher
- Installation from unknown sources must be allowed (one-time prompt)

### Steps

1. Download the latest `FPKGi-Manager-X.X.X.apk` from the [Releases page](https://github.com/RastaFairy/FPKGi-A-/releases/latest).
2. Open the downloaded file on your Android device.
3. If prompted with *"Install unknown apps"*, tap **Settings → Allow from this source**, then go back and tap **Install**.
4. The app appears in your launcher as **FPKGi Manager**.

---

## Loading your game library

The app reads JSON files in two formats:

### FPKGi format (dict)

```json
{
  "DATA": {
    "CUSA07995": {
      "title_id": "CUSA07995",
      "name": "A Way Out",
      "version": "01.01",
      "region": "USA",
      "min_fw": "9.00",
      "size": "16.21 GB",
      "pkg_url": "https://example.com/CUSA07995.pkg",
      "cover_url": "https://example.com/CUSA07995.jpg"
    }
  }
}
```

### PS4PKGInstaller format (list)

```json
{
  "packages": [
    {
      "title_id": "CUSA07995",
      "name": "A Way Out",
      "version": "1.01",
      "region": "USA",
      "min_fw": "9.00",
      "size": "16.21 GB",
      "pkg_url": "https://example.com/CUSA07995.pkg"
    }
  ]
}
```

**To load:** tap the folder icon in the top bar and select your `.json` file. The app also opens `.json` files directly from the system file manager.

---

## FTP setup (direct PS4 transfer)

### On your PS4

Enable an FTP server. Common options:

| Homebrew | Default port |
|---|---|
| GoldHEN built-in ftpd | 2121 |
| PS4FTP (by Aldo Vargas) | 2121 |
| ftpd (standalone) | 21 |

Note the **IP address** shown by the FTP server (e.g. `192.168.1.210`).

### In FPKGi Manager

1. Go to **Settings → FTP Configuration**.
2. Fill in:
   - **Host:** your PS4's IP address
   - **Port:** 2121 (or whatever your server uses)
   - **Remote path:** `/data/pkg` (or your preferred folder on the PS4)
   - **Passive mode:** on (recommended for home networks)
3. Tap **Test connection** — you should see ✅ *Connection OK*.
4. Toggle **Enable FTP** to on.

From now on, all downloads bypass local storage and go directly to your PS4.

---

## OrbisPatches integration

No configuration needed. When you open a game's detail screen, the app automatically queries [orbispatches.com](https://orbispatches.com) for available patches.

- **Tier 1 (fast):** JSON API — instant if the API is reachable.
- **Tier 2 (fallback):** embedded Chromium WebView renders the page and extracts data from the DOM, bypassing Cloudflare. This takes 1–3 seconds.
- **Session cache:** once a game's patches are loaded, they are cached for the remainder of the session. Navigating back is instant.

---

## Auto-update

On each launch the app silently checks GitHub for a newer release. If one exists, a dialog appears with the changelog. Tap **"View on GitHub"** to download and install the new APK without leaving the app.

To disable: there is currently no setting to disable the update check. It makes one read-only HTTPS request to `api.github.com` and does nothing else if no update is found.
