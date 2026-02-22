package com.fpkgi.manager.network

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

object OrbisClient {

    // Desktop Chrome UA for better Cloudflare acceptance
    private val HEADERS = mapOf(
        "User-Agent"      to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept"          to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.9,es;q=0.8",
        "Accept-Encoding" to "gzip, deflate",
        "Connection"      to "keep-alive",
        "Upgrade-Insecure-Requests" to "1"
    )

    private const val API_JSON = "https://orbispatches.com/api/patch.php"
    private const val API_HTML = "https://orbispatches.com"

    suspend fun fetchPatches(titleId: String): OrbisResult = withContext(Dispatchers.IO) {
        val tid = titleId.trim().uppercase()
        try {
            // 1. Try JSON API
            val jsonResult = tryJsonApi(tid)
            if (jsonResult != null && (jsonResult.patches.isNotEmpty() || jsonResult.blocked)) {
                return@withContext jsonResult
            }
            // 2. Try HTML page parsing as fallback
            val htmlResult = tryHtmlPage(tid)
            if (htmlResult != null) return@withContext htmlResult

            OrbisResult(error = "Sin datos para $tid en OrbisPatches")
        } catch (e: Exception) {
            OrbisResult(error = "Error de red: ${e.message}")
        }
    }

    // ─── JSON API ──────────────────────────────────────────────────────────
    private fun tryJsonApi(tid: String): OrbisResult? {
        return try {
            val body = httpGet("$API_JSON?titleid=$tid") ?: return null
            parseJsonBody(body, tid)
        } catch (_: Exception) { null }
    }

    private fun parseJsonBody(body: String, tid: String): OrbisResult? {
        val trimmed = body.trim()
        // Cloudflare challenge page check
        if (trimmed.startsWith("<!DOCTYPE") || trimmed.startsWith("<html")) {
            return if (isCloudflareBlock(body))
                OrbisResult(blocked = true)
            else
                null
        }
        return try {
            when {
                trimmed.startsWith("{") -> parseJsonObject(trimmed, tid)
                trimmed.startsWith("[") -> parseJsonArray(JSONArray(trimmed), tid)
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun parseJsonObject(body: String, tid: String): OrbisResult? {
        val obj = JSONObject(body)
        // { "patches": [...] } or { "patch": [...] }
        val arr = obj.optJSONArray("patches")
            ?: obj.optJSONArray("patch")
            ?: obj.optJSONArray("data")
        if (arr != null) {
            val patches = buildPatches(arr)
            return OrbisResult(patches = patches, gameName = obj.optString("name", tid))
        }
        // { "titleid": "...", "data": { "XX.XX": {...} } }
        val dataObj = obj.optJSONObject("data")
        if (dataObj != null) {
            val patches = buildPatchesFromObject(dataObj)
            return OrbisResult(patches = patches, gameName = obj.optString("name", tid))
        }
        return null
    }

    private fun parseJsonArray(arr: JSONArray, tid: String): OrbisResult {
        val patches = buildPatches(arr)
        return OrbisResult(patches = patches)
    }

    private fun buildPatches(arr: JSONArray): List<OrbisPatch> {
        val patches    = mutableListOf<OrbisPatch>()
        var latestSet  = false
        for (i in 0 until arr.length()) {
            val p   = arr.getJSONObject(i)
            val ver = JsonParser.parseVersion(
                p.optString("version").ifBlank {
                    p.optString("contentver").ifBlank {
                        p.optString("packageVersion")
                    }
                }
            )
            val isLatest = !latestSet
            latestSet = true
            patches.add(OrbisPatch(
                version      = ver,
                firmware     = p.optString("requiredFw").ifBlank { p.optString("firmware", "?") },
                size         = p.optString("size", "?"),
                creationDate = p.optString("createdAt").ifBlank { p.optString("date", "") },
                notes        = p.optString("changeInfo").ifBlank {
                    p.optString("notes").ifBlank { p.optString("description", "") }
                }.trim(),
                isLatest     = isLatest,
                patchKey     = p.optString("contentid", "").ifBlank { p.optString("patchKey", "") }
            ))
        }
        patches.sortByDescending { patch: OrbisPatch -> normalizeVer(patch.version) }
        return patches
    }

    private fun buildPatchesFromObject(dataObj: JSONObject): List<OrbisPatch> {
        val patches   = mutableListOf<OrbisPatch>()
        val keys      = dataObj.keys().asSequence().toList()
        val sorted    = keys.sortedByDescending { normalizeVer(it) }
        var latestSet = false
        for (ver in sorted) {
            val p = dataObj.optJSONObject(ver) ?: continue
            val isLatest = !latestSet; latestSet = true
            patches.add(OrbisPatch(
                version      = JsonParser.parseVersion(ver),
                firmware     = p.optString("requiredFw", "?"),
                size         = p.optString("size", "?"),
                creationDate = p.optString("createdAt", ""),
                notes        = p.optString("changeInfo", "").trim(),
                isLatest     = isLatest,
                patchKey     = p.optString("contentid", "")
            ))
        }
        return patches
    }

    // ─── HTML fallback ─────────────────────────────────────────────────────
    private fun tryHtmlPage(tid: String): OrbisResult? {
        return try {
            val body = httpGet("$API_HTML/$tid") ?: return null
            if (isCloudflareBlock(body)) return OrbisResult(blocked = true)
            parseHtmlPage(body, tid)
        } catch (_: Exception) { null }
    }

    private fun parseHtmlPage(html: String, tid: String): OrbisResult? {
        val patches = mutableListOf<OrbisPatch>()
        var latestSet = false

        // Look for JSON data embedded in script tags
        // Pattern: window.__NUXT__ or __NEXT_DATA__ or similar
        val jsonDataRegex = Regex("""window\.__(?:NUXT|NEXT_DATA|DATA)__\s*=\s*(\{.*?\});""", RegexOption.DOT_MATCHES_ALL)
        val scriptDataRegex = Regex("""<script[^>]*type="application/json"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        
        // Try to find patch rows via HTML patterns
        // OrbisPatches uses a card/table layout: look for version patterns
        val versionRegex = Regex("""(?:version|ver|patch)["\s:>]+(\d{2}\.\d{2})""", RegexOption.IGNORE_CASE)
        
        // Try JSON in script tags
        for (match in scriptDataRegex.findAll(html)) {
            try {
                val json   = match.groupValues[1].trim()
                val result = parseJsonBody(json, tid)
                if (result != null && result.patches.isNotEmpty()) return result
            } catch (_: Exception) { }
        }

        // Try __NUXT__ / __NEXT_DATA__
        for (match in jsonDataRegex.findAll(html)) {
            try {
                val json   = match.groupValues[1]
                val result = extractPatchesFromNestedJson(json, tid)
                if (result != null && result.patches.isNotEmpty()) return result
            } catch (_: Exception) { }
        }

        // Last resort: regex scan for version numbers in the HTML
        val versions = versionRegex.findAll(html).map { it.groupValues[1] }.distinct().toList()
        if (versions.isEmpty()) return null

        for ((idx, ver) in versions.withIndex()) {
            val isLatest = !latestSet; latestSet = true
            patches.add(OrbisPatch(
                version      = JsonParser.parseVersion(ver),
                firmware     = "?",
                size         = "?",
                creationDate = "",
                notes        = "",
                isLatest     = isLatest,
                patchKey     = ""
            ))
        }
        return if (patches.isNotEmpty()) OrbisResult(patches = patches) else null
    }

    private fun extractPatchesFromNestedJson(jsonStr: String, tid: String): OrbisResult? {
        return try {
            // Try to find patch arrays anywhere in the nested JSON
            fun findPatches(obj: JSONObject): JSONArray? {
                for (key in obj.keys()) {
                    val v = obj.opt(key) ?: continue
                    if (v is JSONArray && v.length() > 0) {
                        try {
                            val first = v.getJSONObject(0)
                            if (first.has("version") || first.has("contentver") || first.has("patchKey"))
                                return v
                        } catch (_: Exception) { }
                    }
                    if (v is JSONObject) {
                        val found = findPatches(v)
                        if (found != null) return found
                    }
                }
                return null
            }
            val root = JSONObject(jsonStr)
            val arr  = findPatches(root) ?: return null
            val p    = buildPatches(arr)
            if (p.isNotEmpty()) OrbisResult(patches = p) else null
        } catch (_: Exception) { null }
    }

    // ─── HTTP helpers ──────────────────────────────────────────────────────
    private fun httpGet(url: String): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                HEADERS.forEach { (k, v) -> setRequestProperty(k, v) }
                connectTimeout = 15_000
                readTimeout    = 20_000
                instanceFollowRedirects = true
                connect()
            }
            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                return null
            }
            val enc    = conn.contentEncoding ?: ""
            val stream = if (enc.equals("gzip", ignoreCase = true))
                GZIPInputStream(conn.inputStream)
            else conn.inputStream
            val body = stream.bufferedReader(Charsets.UTF_8).readText()
            conn.disconnect()
            body
        } catch (_: Exception) { null }
    }

    private fun isCloudflareBlock(html: String): Boolean {
        val lower = html.lowercase()
        return lower.contains("just a moment") ||
               lower.contains("cf-browser-verification") ||
               lower.contains("cloudflare") ||
               lower.contains("ddos-guard") ||
               (lower.contains("challenge") && lower.contains("security"))
    }

    private fun normalizeVer(v: String): String =
        v.split(".").joinToString(".") {
            it.trim().toIntOrNull()?.toString()?.padStart(10, '0') ?: "0000000000"
        }

    // ─── Availability ──────────────────────────────────────────────────────
    suspend fun checkAvailability(url: String): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext false
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "HEAD"
                HEADERS.forEach { (k, v) -> setRequestProperty(k, v) }
                connectTimeout = 8_000
                readTimeout    = 8_000
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            conn.disconnect()
            code in listOf(200, 206, 301, 302, 303, 307, 308)
        } catch (_: Exception) { false }
    }
}
