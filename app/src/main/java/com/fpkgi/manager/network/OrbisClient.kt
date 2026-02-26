package com.fpkgi.manager.network

import android.content.Context
import com.fpkgi.manager.data.model.OrbisPatch
import com.fpkgi.manager.data.model.OrbisResult
import com.fpkgi.manager.utils.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Fetches patch information from OrbisPatches.com.
 *
 * Pipeline de 2 tiers (alineado con el parser Python):
 *
 *  Tier 1 — JSON API directa
 *    GET https://orbispatches.com/api/patch.php?titleid=XXXX
 *    Rápida, sin JS, sin Cloudflare normalmente.
 *
 *  Tier 2 — WebView (Chromium real)
 *    Renderiza la página completa y extrae parches por selectores DOM:
 *      .patch-wrapper / a.patch-link[data-contentver]
 *      .col-auto.text-end  (size, fw, date)
 *      a.changeinfo-preview[data-patchnotes-charcount]
 *    Equivale exactamente al parser Python _parse_orbis_html().
 *    NO usa window.__NUXT__ (contamina con versiones npm).
 */
object OrbisClient {

    // Cache de sesión: evita relanzar WebView al volver al mismo juego.
    // Se limpia al reiniciar la app (no persistente).
    private val sessionCache = mutableMapOf<String, OrbisResult>()

    private val HEADERS = mapOf(
        "User-Agent"      to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept"          to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.9,es;q=0.8",
        "Accept-Encoding" to "gzip, deflate",
        "Connection"      to "keep-alive",
        "Cache-Control"   to "no-cache"
    )

    private const val API_JSON = "https://orbispatches.com/api/patch.php"

    // ─── Punto de entrada ────────────────────────────────────────────────

    suspend fun fetchPatches(titleId: String, context: Context? = null): OrbisResult =
        withContext(Dispatchers.IO) {
            val tid = titleId.trim().uppercase()

            // Cache hit: resultado inmediato sin red
            sessionCache[tid]?.let { return@withContext it }

            try {
                // Tier 1: JSON API (rápida, sin Cloudflare)
                val apiResult = tryJsonApi(tid)
                if (apiResult != null && apiResult.patches.isNotEmpty()) {
                    sessionCache[tid] = apiResult
                    return@withContext apiResult
                }

                // Tier 2: WebView DOM (igual que el parser Python con Playwright)
                if (context != null) {
                    val webResult = tryWebView(context, tid)
                    if (webResult != null) {
                        sessionCache[tid] = webResult
                        return@withContext webResult
                    }
                }

                OrbisResult(error = "Sin datos para $tid en OrbisPatches")

            } catch (e: Exception) {
                OrbisResult(error = "Error: ${e.message}")
            }
        }

    /** Invalida la entrada de caché para un juego (pull-to-refresh futuro). */
    fun invalidateCache(titleId: String) { sessionCache.remove(titleId.uppercase()) }

    // ─── Tier 1: JSON API ────────────────────────────────────────────────

    private fun tryJsonApi(tid: String): OrbisResult? {
        return try {
            val body = httpGet("$API_JSON?titleid=$tid") ?: return null
            val trimmed = body.trim()
            when {
                trimmed.startsWith("[") -> {
                    val arr = JSONArray(trimmed)
                    if (arr.length() == 0) null
                    else OrbisResult(patches = buildPatchesFromArray(arr))
                }
                trimmed.startsWith("{") -> {
                    val obj = JSONObject(trimmed)
                    if (obj.optBoolean("cloudflare")) return OrbisResult(blocked = true)
                    val arr = obj.optJSONArray("patches")
                        ?: obj.optJSONArray("patch")
                        ?: obj.optJSONArray("data")
                        ?: return null
                    val patches = buildPatchesFromArray(arr)
                    if (patches.isEmpty()) null
                    else OrbisResult(
                        patches  = patches,
                        gameName = obj.optString("name", tid)
                    )
                }
                else -> null
            }
        } catch (_: Exception) { null }
    }

    // ─── Tier 2: WebView DOM ─────────────────────────────────────────────

    private suspend fun tryWebView(context: Context, tid: String): OrbisResult? {
        return try {
            val raw = OrbisWebViewClient.extractPatchJson(context, tid) ?: return null
            parseDomResult(raw, tid)
        } catch (_: Exception) { null }
    }

    /**
     * Parsea el JSON devuelto por el script DOM de OrbisWebViewClient.
     * Estructura esperada:
     *   { "patches": [ { version, firmware, size, creation_date, notes, is_latest } ], "gameName": "..." }
     * o bien:
     *   { "cloudflare": true }
     *   { "error": "..." }
     *   { "patches": [], "empty": true }
     */
    private fun parseDomResult(raw: String, tid: String): OrbisResult? {
        return try {
            val obj = JSONObject(raw)

            if (obj.optBoolean("cloudflare")) {
                return OrbisResult(
                    blocked = true,
                    error   = "Cloudflare activo. Usa 🌐 para abrir en navegador."
                )
            }

            val errMsg = obj.optString("error", "")
            if (errMsg.isNotBlank()) {
                return OrbisResult(error = "Error JS: $errMsg")
            }

            if (obj.optBoolean("empty")) {
                return OrbisResult(
                    gameName = obj.optString("gameName", tid),
                    error    = "Este título no tiene parches en OrbisPatches."
                )
            }

            val arr = obj.optJSONArray("patches") ?: return null
            val patches = buildPatchesFromDom(arr)
            val gameName = obj.optString("gameName", tid)

            if (patches.isEmpty()) null
            else OrbisResult(patches = patches, gameName = gameName)

        } catch (_: Exception) { null }
    }

    // ─── Construcción de OrbisPatch desde DOM JSON ───────────────────────
    //
    // El JSON del script DOM usa exactamente estos campos (igual que el Python):
    //   version, firmware, size, creation_date, notes, is_latest

    private fun buildPatchesFromDom(arr: JSONArray): List<OrbisPatch> {
        val patches = (0 until arr.length()).mapNotNull { i ->
            try {
                val p = arr.getJSONObject(i)
                val ver = p.optString("version", "").trim()
                if (ver.isBlank()) return@mapNotNull null

                OrbisPatch(
                    version      = JsonParser.parseVersion(ver),
                    firmware     = p.optString("firmware", "?").trim().ifBlank { "?" },
                    size         = p.optString("size",     "?").trim().ifBlank { "?" },
                    creationDate = p.optString("creation_date", "").trim(),
                    notes        = p.optString("notes", "").trim(),
                    isLatest     = p.optBoolean("is_latest", false),
                    patchKey     = ""
                )
            } catch (_: Exception) { null }
        }
        // El JS ya ordena por versión, pero re-asegurar isLatest
        return if (patches.isEmpty()) patches
        else {
            val alreadyMarked = patches.any { it.isLatest }
            if (alreadyMarked) patches
            else listOf(patches[0].copy(isLatest = true)) + patches.drop(1)
        }
    }

    // ─── Construcción de OrbisPatch desde API JSON ───────────────────────
    //
    // La API usa campos: contentver/version, requiredFw/firmware, size, createdAt/date,
    // changeInfo/notes/description, contentid/patchKey

    private fun buildPatchesFromArray(arr: JSONArray): List<OrbisPatch> {
        val patches = (0 until arr.length()).mapNotNull { i ->
            try {
                val p = arr.getJSONObject(i)
                val ver = JsonParser.parseVersion(
                    p.optString("version").ifBlank {
                        p.optString("contentver").ifBlank { p.optString("packageVersion") }
                    }
                )
                if (ver.isBlank()) return@mapNotNull null
                OrbisPatch(
                    version      = ver,
                    firmware     = p.optString("requiredFw").ifBlank { p.optString("firmware", "?") },
                    size         = p.optString("size", "?"),
                    creationDate = p.optString("createdAt").ifBlank { p.optString("date", "") },
                    notes        = p.optString("changeInfo").ifBlank {
                        p.optString("notes").ifBlank { p.optString("description", "") }
                    }.trim(),
                    isLatest     = false,
                    patchKey     = p.optString("contentid", "").ifBlank { p.optString("patchKey", "") }
                )
            } catch (_: Exception) { null }
        }.sortedByDescending { normalizeVer(it.version) }

        return if (patches.isEmpty()) patches
        else listOf(patches[0].copy(isLatest = true)) + patches.drop(1)
    }

    // ─── HTTP helper ─────────────────────────────────────────────────────

    private fun httpGet(url: String): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            HEADERS.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.connectTimeout = 15_000
            conn.readTimeout    = 20_000
            conn.instanceFollowRedirects = true
            conn.connect()
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }
            val encoding = conn.contentEncoding ?: ""
            val stream   = if (encoding.equals("gzip", ignoreCase = true))
                GZIPInputStream(conn.inputStream) else conn.inputStream
            val body = stream.bufferedReader(Charsets.UTF_8).readText()
            conn.disconnect()
            body
        } catch (_: Exception) { null }
    }

    private fun normalizeVer(v: String): String =
        v.split(".").joinToString(".") {
            it.trim().toIntOrNull()?.toString()?.padStart(10, '0') ?: "0000000000"
        }

    // ─── Disponibilidad de URL ────────────────────────────────────────────

    suspend fun checkAvailability(url: String): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext false
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            HEADERS.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.connectTimeout = 8_000
            conn.readTimeout    = 8_000
            conn.instanceFollowRedirects = true
            val code = conn.responseCode
            conn.disconnect()
            code in listOf(200, 206, 301, 302, 303, 307, 308)
        } catch (_: Exception) { false }
    }
}
