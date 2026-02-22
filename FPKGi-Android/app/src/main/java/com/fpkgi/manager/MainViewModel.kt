package com.fpkgi.manager

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import com.fpkgi.manager.data.model.*
import com.fpkgi.manager.data.repository.SettingsRepository
import com.fpkgi.manager.network.FtpDownloadService
import com.fpkgi.manager.network.OrbisClient
import com.fpkgi.manager.utils.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val settings = SettingsRepository(application)

    // ─── Language ──────────────────────────────────────────────────────────
    val currentLanguage: StateFlow<String> = settings.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "es")

    fun setLanguage(code: String) {
        viewModelScope.launch { settings.saveLanguage(code) }
    }

    // ─── Games ────────────────────────────────────────────────────────────
    private val _allGames    = MutableStateFlow<List<Game>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _sortColumn  = MutableStateFlow("name")
    private val _sortAsc     = MutableStateFlow(true)
    private val _loadedFile  = MutableStateFlow("")

    val games: StateFlow<List<Game>> = combine(
        _allGames, _searchQuery, _sortColumn, _sortAsc
    ) { all, query, col, asc ->
        val filtered = if (query.isBlank()) all
        else all.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.titleId.contains(query, ignoreCase = true) ||
            it.region.contains(query, ignoreCase = true)
        }
        filtered.sortedWith(compareBy {
            when (col) {
                "name"    -> it.name.lowercase()
                "titleId" -> it.titleId
                "version" -> it.version
                "size"    -> it.size
                "region"  -> it.region
                "minFw"   -> it.minFw
                else      -> it.name.lowercase()
            }
        }).let { if (asc) it else it.reversed() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery    = _searchQuery.asStateFlow()
    val loadedFileName = _loadedFile.asStateFlow()

    // ─── Download service ─────────────────────────────────────────────────
    private var downloadService: FtpDownloadService? = null
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads = _downloads.asStateFlow()

    private val serviceConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = (binder as? FtpDownloadService.LocalBinder)?.getService()
            downloadService = svc
            svc?.let { viewModelScope.launch { it.downloads.collect { list -> _downloads.value = list } } }
        }
        override fun onServiceDisconnected(name: ComponentName?) { downloadService = null }
    }

    init {
        val app    = getApplication<Application>()
        val intent = Intent(app, FtpDownloadService::class.java)
        app.startForegroundService(intent)
        app.bindService(intent, serviceConn, Context.BIND_AUTO_CREATE)
    }

    // ─── JSON loading ──────────────────────────────────────────────────────
    fun loadJsonFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx     = getApplication<Application>()
                val content = ctx.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: return@launch
                val parsed = JsonParser.parseGames(content)
                _allGames.value = parsed
                _loadedFile.value = uri.lastPathSegment ?: "games.json"
            } catch (_: Exception) { }
        }
    }

    fun loadJsonFromFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsed = JsonParser.parseGames(file.readText())
                _allGames.value = parsed
                _loadedFile.value = file.name
            } catch (_: Exception) { }
        }
    }

    // ─── Search & sort ─────────────────────────────────────────────────────
    fun setSearch(query: String) { _searchQuery.value = query }
    fun setSort(col: String) {
        if (_sortColumn.value == col) _sortAsc.value = !_sortAsc.value
        else { _sortColumn.value = col; _sortAsc.value = true }
    }

    // ─── Availability check ────────────────────────────────────────────────
    fun checkAvailability(game: Game) {
        val idx = _allGames.value.indexOfFirst { it.titleId == game.titleId }
        if (idx < 0) return
        val updated = _allGames.value.toMutableList()
        updated[idx] = updated[idx].copy(availStatus = AvailStatus.CHECKING)
        _allGames.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            val available = OrbisClient.checkAvailability(game.pkgUrl)
            val list = _allGames.value.toMutableList()
            val i    = list.indexOfFirst { it.titleId == game.titleId }
            if (i >= 0) {
                list[i] = list[i].copy(
                    availStatus = if (available) AvailStatus.AVAILABLE else AvailStatus.UNAVAILABLE
                )
                _allGames.value = list
            }
        }
    }

    // ─── Downloads ────────────────────────────────────────────────────────
    fun startDownload(game: Game, pkgUrl: String? = null) {
        viewModelScope.launch {
            val config       = settings.ftpConfig.first()
            val url          = pkgUrl ?: game.pkgUrl
            val downloadPath = settings.downloadPath.first()
            val item = DownloadItem(
                id          = "${game.titleId}_${System.currentTimeMillis()}",
                game        = game,
                pkgUrl      = url,
                isFtp       = config.enabled,
                destPath    = downloadPath
            )
            downloadService?.startDownload(item, config)
                ?: startServiceAndDownload(item, config)
        }
    }

    fun pauseDownload(id: String) {
        downloadService?.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        downloadService?.resumeDownload(id)
    }

    fun cancelDownload(id: String) {
        val intent = Intent(getApplication(), FtpDownloadService::class.java).apply {
            action = FtpDownloadService.ACTION_CANCEL
            putExtra("download_id", id)
        }
        getApplication<Application>().startService(intent)
    }

    /** Upload a local .pkg file to the PS4 via FTP */
    fun uploadLocalPkg(localFilePath: String, fileName: String) {
        viewModelScope.launch {
            val config = settings.ftpConfig.first()
            if (!config.enabled) return@launch
            downloadService?.uploadLocalPkg(localFilePath, fileName, config)
        }
    }

    private fun startServiceAndDownload(item: DownloadItem, config: FtpConfig) {
        val app    = getApplication<Application>()
        val intent = Intent(app, FtpDownloadService::class.java)
        app.startForegroundService(intent)
        app.bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val svc = (binder as? FtpDownloadService.LocalBinder)?.getService() ?: return
                downloadService = svc
                svc.startDownload(item, config)
                viewModelScope.launch { svc.downloads.collect { _downloads.value = it } }
            }
            override fun onServiceDisconnected(name: ComponentName?) {}
        }, Context.BIND_AUTO_CREATE)
    }

    // ─── OrbisPatches ─────────────────────────────────────────────────────
    private val _orbisResult  = MutableStateFlow<OrbisResult?>(null)
    private val _orbisLoading = MutableStateFlow(false)
    val orbisResult  = _orbisResult.asStateFlow()
    val orbisLoading = _orbisLoading.asStateFlow()

    fun fetchOrbisPatches(titleId: String) {
        _orbisResult.value  = null
        _orbisLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = OrbisClient.fetchPatches(titleId)
            withContext(Dispatchers.Main) {
                _orbisResult.value  = result
                _orbisLoading.value = false
            }
        }
    }

    fun clearOrbisResult() { _orbisResult.value = null }

    // ─── FTP config ───────────────────────────────────────────────────────
    val ftpConfig = settings.ftpConfig
    suspend fun saveFtpConfig(config: FtpConfig) = settings.saveFtpConfig(config)

    // ─── Icon cache ───────────────────────────────────────────────────────
    private val _iconCacheMsg = MutableStateFlow("")
    val iconCacheMsg = _iconCacheMsg.asStateFlow()

    fun clearIconCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                Coil.imageLoader(ctx).diskCache?.clear()
                Coil.imageLoader(ctx).memoryCache?.clear()
                withContext(Dispatchers.Main) { _iconCacheMsg.value = "ok" }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _iconCacheMsg.value = "err:${e.message}" }
            }
        }
    }

    fun getIconCacheSize(): String {
        return try {
            val ctx   = getApplication<Application>()
            val bytes = Coil.imageLoader(ctx).diskCache?.size ?: 0L
            JsonParser.bytesToHuman(bytes)
        } catch (_: Exception) { "?" }
    }

    fun consumeIconCacheMsg() { _iconCacheMsg.value = "" }

    fun clearDoneDownloads() {
        downloadService?.clearDoneDownloads()
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────
    override fun onCleared() {
        runCatching { getApplication<Application>().unbindService(serviceConn) }
        super.onCleared()
    }
}
