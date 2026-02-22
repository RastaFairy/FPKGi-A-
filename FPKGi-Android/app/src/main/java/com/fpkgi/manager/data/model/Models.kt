package com.fpkgi.manager.data.model

import com.google.gson.annotations.SerializedName

// ─── Modelos de juego PS4 ───────────────────────────────────────────────────

data class Game(
    val titleId: String,
    val name: String,
    val version: String,
    val region: String,
    val size: String,
    val minFw: String,
    val coverUrl: String,
    val pkgUrl: String,
    var availStatus: AvailStatus = AvailStatus.UNCHECKED
)

enum class AvailStatus {
    UNCHECKED, CHECKING, AVAILABLE, UNAVAILABLE
}

// ─── Formato JSON FPKGi (dict) ────────────────────────────────────────────
data class FpkgiJson(
    @SerializedName("DATA") val data: Map<String, FpkgiEntry>?
)

data class FpkgiEntry(
    @SerializedName("title_id")    val titleId: String?,
    @SerializedName("name")        val name: String?,
    @SerializedName("version")     val version: Any?,
    @SerializedName("region")      val region: String?,
    @SerializedName("size")        val size: Any?,
    @SerializedName("min_fw")      val minFw: Any?,
    @SerializedName("required_fw") val requiredFw: Any?,
    @SerializedName("fw")          val fw: Any?,
    @SerializedName("cover_url")   val coverUrl: String?,
    @SerializedName("icon_url")    val iconUrl: String?
)

// ─── Formato JSON PS4PKGInstaller (lista) ─────────────────────────────────
data class PackagesJson(
    @SerializedName("packages") val packages: List<PackageEntry>?
)

data class PackageEntry(
    @SerializedName("title_id")      val titleId: String?,
    @SerializedName("name")          val name: String?,
    @SerializedName("version")       val version: Any?,
    @SerializedName("region")        val region: String?,
    @SerializedName("size")          val size: Any?,
    @SerializedName("system_version") val systemVersion: Any?,
    @SerializedName("min_fw")        val minFw: Any?,
    @SerializedName("required_fw")   val requiredFw: Any?,
    @SerializedName("fw")            val fw: Any?,
    @SerializedName("icon_url")      val iconUrl: String?,
    @SerializedName("cover_url")     val coverUrl: String?,
    @SerializedName("pkg_url")       val pkgUrl: String?
)

// ─── Descarga activa ──────────────────────────────────────────────────────
data class DownloadItem(
    val id: String,
    val game: Game,
    val pkgUrl: String,
    val isFtp: Boolean,
    var progress: Float = 0f,
    var downloaded: Long = 0L,
    var total: Long = 0L,
    var status: DownloadStatus = DownloadStatus.QUEUED,
    var errorMsg: String = "",
    var destPath: String = "",
    // Display name override (used for local PKG FTP uploads)
    var displayName: String = ""
)

enum class DownloadStatus {
    QUEUED, RUNNING, PAUSED, DONE, ERROR, CANCELLED
}

// ─── Configuración FTP ────────────────────────────────────────────────────
data class FtpConfig(
    val enabled: Boolean = false,
    val host: String = "192.168.1.210",
    val port: Int = 2121,
    val user: String = "anonymous",
    val password: String = "",
    val remotePath: String = "/data/pkg",
    val passiveMode: Boolean = true,
    val timeout: Int = 30
)

// ─── Parches de OrbisPatches ──────────────────────────────────────────────
data class OrbisResult(
    val patches: List<OrbisPatch> = emptyList(),
    val gameName: String = "",
    val blocked: Boolean = false,
    val error: String = ""
)

data class OrbisPatch(
    val version: String,
    val firmware: String,
    val size: String,
    val creationDate: String,
    val notes: String,
    val isLatest: Boolean,
    val patchKey: String
)
