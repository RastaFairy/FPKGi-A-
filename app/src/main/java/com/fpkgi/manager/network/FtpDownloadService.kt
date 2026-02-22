package com.fpkgi.manager.network

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.fpkgi.manager.MainActivity
import com.fpkgi.manager.data.model.DownloadItem
import com.fpkgi.manager.data.model.DownloadStatus
import com.fpkgi.manager.data.model.FtpConfig
import com.fpkgi.manager.data.model.Game
import com.fpkgi.manager.utils.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class FtpDownloadService : Service() {

    companion object {
        const val CHANNEL_ID    = "fpkgi_downloads"
        const val NOTIF_ID      = 1001
        const val ACTION_CANCEL = "com.fpkgi.manager.CANCEL_DOWNLOAD"
        const val ACTION_PAUSE  = "com.fpkgi.manager.PAUSE_DOWNLOAD"
        const val ACTION_RESUME = "com.fpkgi.manager.RESUME_DOWNLOAD"
    }

    inner class LocalBinder : Binder() {
        fun getService(): FtpDownloadService = this@FtpDownloadService
    }

    private val binder       = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    // Per-download pause flags (thread-safe via serviceScope)
    @Volatile private var pausedIds = setOf<String>()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> cancelDownload(intent.getStringExtra("download_id"))
            ACTION_PAUSE  -> pauseDownload(intent.getStringExtra("download_id") ?: "")
            ACTION_RESUME -> resumeDownload(intent.getStringExtra("download_id") ?: "")
        }
        startForeground(NOTIF_ID, buildNotification("FPKGi Manager", "Preparado"))
        return START_STICKY
    }

    // ─── Public API ────────────────────────────────────────────────────────

    fun startDownload(item: DownloadItem, config: FtpConfig) {
        _downloads.value = _downloads.value + item
        serviceScope.launch {
            if (config.enabled) downloadViaFtp(item, config)
            else downloadLocal(item)
        }
    }

    /** Upload a local .pkg file to the configured FTP server (PS4). */
    fun uploadLocalPkg(localFilePath: String, fileName: String, config: FtpConfig): String {
        val id        = "local_${System.currentTimeMillis()}"
        val fakeGame  = Game(
            titleId  = "LOCAL_UPLOAD",
            name     = fileName,
            version  = "?", region = "", size = "?",
            minFw    = "?", coverUrl = "", pkgUrl = ""
        )
        val item = DownloadItem(
            id          = id,
            game        = fakeGame,
            pkgUrl      = localFilePath,
            isFtp       = true,
            displayName = fileName
        )
        _downloads.value = _downloads.value + item
        serviceScope.launch { uploadFileViaFtp(item, localFilePath, fileName, config) }
        return id
    }

    fun pauseDownload(id: String) {
        if (id.isBlank()) return
        pausedIds = pausedIds + id
        _downloads.value = _downloads.value.map {
            if (it.id == id && it.status == DownloadStatus.RUNNING)
                it.copy(status = DownloadStatus.PAUSED)
            else it
        }
    }

    fun resumeDownload(id: String) {
        if (id.isBlank()) return
        pausedIds = pausedIds - id
        _downloads.value = _downloads.value.map {
            if (it.id == id && it.status == DownloadStatus.PAUSED)
                it.copy(status = DownloadStatus.RUNNING)
            else it
        }
    }

    private fun cancelDownload(id: String?) {
        id ?: return
        pausedIds = pausedIds - id
        _downloads.value = _downloads.value.map {
            if (it.id == id) it.copy(status = DownloadStatus.CANCELLED) else it
        }
    }

    // ─── HTTP local download ───────────────────────────────────────────────
    private suspend fun downloadLocal(item: DownloadItem) {
        updateStatus(item.id, DownloadStatus.RUNNING)
        try {
            val url  = URL(item.pkgUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36")
                connectTimeout = 30_000
                readTimeout    = 60_000
                connect()
            }
            val total    = conn.contentLengthLong
            val filename = item.pkgUrl.substringAfterLast('/').substringBefore('?')
                .ifBlank { "${item.game.titleId}.pkg" }
            val dest = File(item.destPath.ifBlank {
                getExternalFilesDir(null)?.absolutePath + "/downloads/${item.game.titleId}"
            })
            dest.mkdirs()
            val file = File(dest, filename)

            BufferedInputStream(conn.inputStream).use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(1024 * 64)
                    var done   = 0L
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        // Cancel check
                        if (getItem(item.id)?.status == DownloadStatus.CANCELLED) {
                            file.delete(); return
                        }
                        // Pause wait loop
                        while (item.id in pausedIds) {
                            delay(400)
                            if (getItem(item.id)?.status == DownloadStatus.CANCELLED) {
                                file.delete(); return
                            }
                        }
                        output.write(buffer, 0, n)
                        done += n
                        updateProgress(item.id, done, total)
                        updateNotification("⬇ ${item.game.name}",
                            "${JsonParser.bytesToHuman(done)} / ${JsonParser.bytesToHuman(total)}")
                    }
                }
            }
            updateStatus(item.id, DownloadStatus.DONE, destPath = file.absolutePath)
            updateNotification("✅ Completado", item.game.name)
        } catch (e: Exception) {
            if (getItem(item.id)?.status != DownloadStatus.CANCELLED) {
                updateStatus(item.id, DownloadStatus.ERROR, error = e.message ?: "Error desconocido")
                updateNotification("❌ Error", e.message ?: "")
            }
        }
        stopIfIdle()
    }

    // ─── HTTP → FTP streaming ─────────────────────────────────────────────
    private suspend fun downloadViaFtp(item: DownloadItem, config: FtpConfig) {
        updateStatus(item.id, DownloadStatus.RUNNING)
        val ftp = FTPClient()
        try {
            ftp.defaultTimeout = config.timeout * 1000
            ftp.connectTimeout = config.timeout * 1000
            ftp.connect(config.host, config.port)
            ftp.login(config.user, config.password)
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            if (config.passiveMode) ftp.enterLocalPassiveMode() else ftp.enterLocalActiveMode()

            ensureFtpDir(ftp, config.remotePath)
            ftp.changeWorkingDirectory(config.remotePath)

            val filename = item.pkgUrl.substringAfterLast('/').substringBefore('?')
                .ifBlank { "${item.game.titleId}.pkg" }

            val url  = URL(item.pkgUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36")
                connectTimeout = 30_000; readTimeout = 0; connect()
            }
            val total = conn.contentLengthLong

            val trackingStream = ProgressInputStream(conn.inputStream, total) { done ->
                if (getItem(item.id)?.status == DownloadStatus.CANCELLED) { ftp.abort(); return@ProgressInputStream }
                updateProgress(item.id, done, total)
                updateNotification("📡 FTP: ${item.game.name}",
                    "${JsonParser.bytesToHuman(done)} / ${JsonParser.bytesToHuman(total)}")
            }
            val success = ftp.storeFile(filename, trackingStream)
            trackingStream.close(); conn.disconnect()

            if (success) {
                val dest = "${config.host}:${config.port}${config.remotePath}/$filename"
                updateStatus(item.id, DownloadStatus.DONE, destPath = dest)
                updateNotification("✅ FTP OK", item.game.name)
            } else {
                updateStatus(item.id, DownloadStatus.ERROR, error = "FTP: transferencia fallida")
                updateNotification("❌ FTP Error", item.game.name)
            }
        } catch (e: Exception) {
            if (getItem(item.id)?.status != DownloadStatus.CANCELLED) {
                updateStatus(item.id, DownloadStatus.ERROR, error = "FTP: ${e.message}")
                updateNotification("❌ FTP Error", e.message ?: "")
            }
        } finally {
            runCatching { ftp.logout() }
            runCatching { ftp.disconnect() }
        }
        stopIfIdle()
    }

    // ─── Local file → FTP upload ───────────────────────────────────────────
    private suspend fun uploadFileViaFtp(
        item: DownloadItem, localPath: String, fileName: String, config: FtpConfig
    ) {
        updateStatus(item.id, DownloadStatus.RUNNING)
        val ftp = FTPClient()
        try {
            ftp.defaultTimeout = config.timeout * 1000
            ftp.connectTimeout = config.timeout * 1000
            ftp.connect(config.host, config.port)
            ftp.login(config.user, config.password)
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            if (config.passiveMode) ftp.enterLocalPassiveMode() else ftp.enterLocalActiveMode()

            ensureFtpDir(ftp, config.remotePath)
            ftp.changeWorkingDirectory(config.remotePath)

            val localFile = File(localPath)
            val total     = localFile.length()

            val trackingStream = ProgressInputStream(FileInputStream(localFile), total) { done ->
                if (getItem(item.id)?.status == DownloadStatus.CANCELLED) { ftp.abort(); return@ProgressInputStream }
                updateProgress(item.id, done, total)
                updateNotification("📤 Subiendo: $fileName",
                    "${JsonParser.bytesToHuman(done)} / ${JsonParser.bytesToHuman(total)}")
            }
            val success = ftp.storeFile(fileName, trackingStream)
            trackingStream.close()

            if (success) {
                val dest = "${config.host}:${config.port}${config.remotePath}/$fileName"
                updateStatus(item.id, DownloadStatus.DONE, destPath = dest)
                updateNotification("✅ Enviado", fileName)
            } else {
                updateStatus(item.id, DownloadStatus.ERROR, error = "FTP: transferencia fallida")
            }
        } catch (e: Exception) {
            if (getItem(item.id)?.status != DownloadStatus.CANCELLED) {
                updateStatus(item.id, DownloadStatus.ERROR, error = "FTP: ${e.message}")
                updateNotification("❌ Error FTP", e.message ?: "")
            }
        } finally {
            runCatching { ftp.logout() }
            runCatching { ftp.disconnect() }
        }
        stopIfIdle()
    }

    private fun ensureFtpDir(ftp: FTPClient, path: String) {
        var current = ""
        for (part in path.trim('/').split('/')) {
            current += "/$part"
            try { ftp.changeWorkingDirectory(current) }
            catch (_: Exception) {
                ftp.makeDirectory(current)
                runCatching { ftp.changeWorkingDirectory(current) }
            }
        }
    }

    // ─── State helpers ─────────────────────────────────────────────────────
    private fun getItem(id: String) = _downloads.value.find { it.id == id }

    private fun updateProgress(id: String, done: Long, total: Long) {
        val p = if (total > 0) done.toFloat() / total else 0f
        _downloads.value = _downloads.value.map {
            if (it.id == id) it.copy(downloaded = done, total = total, progress = p) else it
        }
    }

    private fun updateStatus(id: String, status: DownloadStatus, error: String = "", destPath: String = "") {
        _downloads.value = _downloads.value.map {
            if (it.id == id) it.copy(
                status   = status,
                errorMsg = error,
                destPath = destPath.ifBlank { it.destPath }
            ) else it
        }
    }

    private fun stopIfIdle() {
        val running = _downloads.value.count { it.status == DownloadStatus.RUNNING }
        if (running == 0) updateNotification("FPKGi Manager", "${_downloads.value.size} elementos")
    }

    // ─── Notification ──────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "FPKGi Descargas",
            NotificationManager.IMPORTANCE_LOW).apply { description = "Descargas PKG" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title).setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pi).setOngoing(true).build()
    }

    private fun updateNotification(title: String, content: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification(title, content))
    }

    private fun acquireWakeLock() {
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FPKGi::DownloadLock")
        wakeLock?.acquire(10 * 60 * 60 * 1000L)
    }

    fun clearDoneDownloads() {
        _downloads.value = _downloads.value.filter {
            it.status == DownloadStatus.RUNNING ||
            it.status == DownloadStatus.PAUSED ||
            it.status == DownloadStatus.QUEUED
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        wakeLock?.release()
        super.onDestroy()
    }
}

// ─── InputStream with progress callback ───────────────────────────────────
private class ProgressInputStream(
    private val wrapped: java.io.InputStream,
    @Suppress("UNUSED_PARAMETER") private val total: Long,
    private val onProgress: (Long) -> Unit
) : java.io.InputStream() {
    private var read = 0L
    override fun read(): Int {
        val b = wrapped.read()
        if (b != -1) { read++; onProgress(read) }
        return b
    }
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = wrapped.read(b, off, len)
        if (n > 0) { read += n; onProgress(read) }
        return n
    }
    override fun close() = wrapped.close()
}
