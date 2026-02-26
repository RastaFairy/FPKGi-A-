# Frequently Asked Questions

## General

**Q: What is FPKGi Manager?**  
A: An Android app to browse, verify and download PS4 FPKG packages. It also shows patch information from OrbisPatches and sends files directly to your PS4 via FTP.

**Q: Does this work with PS5 packages?**  
A: No. The app is designed exclusively for PS4 FPKG files.

**Q: Is it safe to use?**  
A: The app only reads local JSON files and makes network requests to the URLs already present in your JSON. It does not modify any files on your device outside its own internal cache.

---

## JSON library

**Q: Where do I get the JSON file?**  
A: Export it from the desktop FPKGi Manager or PS4PKGInstaller. The format is documented in [INSTALL.md](INSTALL.md).

**Q: The app shows 0 games after loading the JSON.**  
A: Check that the JSON matches one of the two supported formats. Open the file in a text editor and verify it has either a `DATA` key (FPKGi format) or a `packages` array (PS4PKGInstaller format).

**Q: Can I open the JSON directly from my file manager?**  
A: Yes. The app registers itself as a handler for `.json` files. Tap the file in any file manager and choose FPKGi Manager.

---

## Downloads and FTP

**Q: The download starts but nothing arrives on the PS4.**  
A: Check that FTP is enabled in Settings, the IP is correct, and the PS4 FTP server is running. Use the **Test connection** button to verify before downloading.

**Q: What FTP server should I use on the PS4?**  
A: Any homebrew FTP server works. The most common are GoldHEN's built-in ftpd (port 2121) and the standalone ftpd homebrew.

**Q: Can I pause and resume a download?**  
A: Yes. Go to the Downloads screen and use the pause/resume buttons per item.

**Q: Where are files saved if FTP is disabled?**  
A: In your device's `Downloads/FPKGi/` folder, organised by Title ID.

---

## OrbisPatches

**Q: The patch list is empty or shows "no results".**  
A: The title may not have any patches listed on OrbisPatches, or Cloudflare is blocking the request. Try tapping the 🌐 button to open the page in your browser and check manually.

**Q: The patches load slowly the first time.**  
A: The first load uses the embedded Chromium WebView to render the page and bypass Cloudflare. This takes 1–3 seconds. Subsequent visits to the same game in the same session are instant (session cache).

**Q: The patch versions look wrong (e.g. v45.73, v14.09).**  
A: This was a bug in versions prior to 6.4.3 where JavaScript library versions from the page's Nuxt state were being captured instead of actual PS4 patch versions. Update to 6.4.3 or later.

---

## Updates

**Q: How does the in-app update work?**  
A: On launch the app queries the GitHub Releases API. If a newer version exists it shows a dialog with the changelog. Tapping "View on GitHub" downloads the APK to the app's internal cache and launches the system installer on top of the current screen.

**Q: I dismissed the update dialog. How do I get it back?**  
A: Restart the app. The check runs once per session on launch.

**Q: The update dialog says "Download error".**  
A: The GitHub release may not have an APK asset attached, or your connection dropped during the download. The dialog falls back to opening the release page in the browser.
