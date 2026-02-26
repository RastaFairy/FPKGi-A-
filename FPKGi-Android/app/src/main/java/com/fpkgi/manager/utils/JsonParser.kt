package com.fpkgi.manager.utils

import com.fpkgi.manager.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.DecimalFormat

object JsonParser {

    private val gson = Gson()

    fun parseGames(jsonString: String): List<Game> {
        return try {
            val root = JsonParser.parseString(jsonString)
            when {
                root.isJsonObject -> {
                    val obj = root.asJsonObject
                    when {
                        obj.has("DATA") -> parseFpkgiDict(obj)
                        obj.has("packages") -> parsePackagesList(obj.getAsJsonArray("packages"))
                        else -> emptyList()
                    }
                }
                root.isJsonArray -> parsePackagesList(root.asJsonArray)
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseFpkgiDict(root: JsonObject): List<Game> {
        val data = root.getAsJsonObject("DATA") ?: return emptyList()
        val games = mutableListOf<Game>()
        for ((pkgUrl, entry) in data.entrySet()) {
            val e = entry.asJsonObject
            games.add(
                Game(
                    titleId = e.getString("title_id") ?: "?",
                    name = e.getString("name") ?: "?",
                    version = parseVersion(e.get("version")?.asString),
                    region = e.getString("region") ?: "?",
                    size = parseSize(e.get("size")),
                    minFw = e.get("min_fw")?.asString
                        ?: e.get("required_fw")?.asString
                        ?: e.get("fw")?.asString ?: "?",
                    coverUrl = e.getString("cover_url") ?: e.getString("icon_url") ?: "",
                    pkgUrl = pkgUrl.trim().trim('_')
                )
            )
        }
        return games
    }

    private fun parsePackagesList(array: JsonArray): List<Game> {
        val games = mutableListOf<Game>()
        for (item in array) {
            val e = item.asJsonObject
            games.add(
                Game(
                    titleId = e.getString("title_id") ?: "?",
                    name = e.getString("name") ?: "?",
                    version = parseVersion(e.get("version")?.asString),
                    region = e.getString("region") ?: "?",
                    size = parseSize(e.get("size")),
                    minFw = e.get("system_version")?.asString
                        ?: e.get("min_fw")?.asString
                        ?: e.get("required_fw")?.asString
                        ?: e.get("fw")?.asString ?: "?",
                    coverUrl = e.getString("icon_url") ?: e.getString("cover_url") ?: "",
                    pkgUrl = (e.getString("pkg_url") ?: "").trim().trim('_')
                )
            )
        }
        return games
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private fun JsonObject.getString(key: String): String? =
        try { get(key)?.takeIf { !it.isJsonNull }?.asString } catch (_: Exception) { null }

    fun parseVersion(ver: String?): String {
        if (ver == null) return "?"
        val s = ver.trim().trimStart('v')
        val parts = s.split(".")
        return try {
            val major = parts.getOrNull(0)?.toInt() ?: 0
            val minor = parts.getOrNull(1)?.toInt() ?: 0
            "%02d.%02d".format(major, minor)
        } catch (_: Exception) { s }
    }

    private fun parseSize(element: com.google.gson.JsonElement?): String {
        if (element == null || element.isJsonNull) return "?"
        return try {
            val str = element.asString
            // If already human-readable
            if (str.contains("GB", ignoreCase = true) ||
                str.contains("MB", ignoreCase = true) ||
                str.contains("KB", ignoreCase = true)) return str
            // Convert bytes
            bytesToHuman(str.replace(",", "").toLong())
        } catch (_: Exception) {
            element.asString ?: "?"
        }
    }

    fun bytesToHuman(bytes: Long): String {
        val df = DecimalFormat("#.##")
        return when {
            bytes >= 1_073_741_824L -> "${df.format(bytes / 1_073_741_824.0)} GB"
            bytes >= 1_048_576L     -> "${df.format(bytes / 1_048_576.0)} MB"
            bytes >= 1_024L         -> "${df.format(bytes / 1_024.0)} KB"
            else                    -> "$bytes B"
        }
    }
}
