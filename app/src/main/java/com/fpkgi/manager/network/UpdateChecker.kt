package com.fpkgi.manager.network

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val GITHUB_RELEASES_URL =
        "https://api.github.com/repos/RastaFairy/FPKGi-A-/releases/latest"

    data class UpdateInfo(
        val latestVersion: String,
        val releaseUrl:    String,
        val apkUrl:        String,  // URL directa al .apk del release asset
        val changelog:     String
    )

    /** Devuelve UpdateInfo si hay versión más reciente, null si no. */
    fun checkForUpdate(currentVersion: String): UpdateInfo? {
        return try {
            val conn = URL(GITHUB_RELEASES_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "FPKGi-Android-Updater")
            conn.connectTimeout = 8_000
            conn.readTimeout    = 8_000
            conn.instanceFollowRedirects = true

            if (conn.responseCode != 200) { conn.disconnect(); return null }
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json      = JSONObject(body)
            val tagName   = json.optString("tag_name", "")
            val htmlUrl   = json.optString("html_url", "")
            val changelog = json.optString("body", "").take(600).trim()
            val remoteVer = tagName.trimStart('v', 'V').trim()

            if (remoteVer.isBlank() || !isNewer(remoteVer, currentVersion)) return null

            // Buscar el primer .apk entre los assets del release
            val apkUrl = json.optJSONArray("assets")?.let { assets ->
                (0 until assets.length()).firstNotNullOfOrNull { i ->
                    val url = assets.getJSONObject(i)
                        .optString("browser_download_url", "")
                    if (url.endsWith(".apk", ignoreCase = true)) url else null
                }
            } ?: ""

            UpdateInfo(remoteVer, htmlUrl, apkUrl, changelog)
        } catch (_: Exception) { null }
    }

    /** true si remote > current comparando segmentos numéricos. */
    private fun isNewer(remote: String, current: String): Boolean {
        val r = segments(remote)
        val c = segments(current)
        repeat(maxOf(r.size, c.size)) { i ->
            val diff = (r.getOrElse(i) { 0 }) - (c.getOrElse(i) { 0 })
            if (diff != 0) return diff > 0
        }
        return false
    }

    private fun segments(v: String): List<Int> =
        v.split(Regex("[^\\d]+")).filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
}
