package com.fpkgi.manager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.fpkgi.manager.data.model.FtpConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fpkgi_prefs")

class SettingsRepository(private val context: Context) {

    companion object {
        private val FTP_ENABLED      = booleanPreferencesKey("ftp_enabled")
        private val FTP_HOST         = stringPreferencesKey("ftp_host")
        private val FTP_PORT         = intPreferencesKey("ftp_port")
        private val FTP_USER         = stringPreferencesKey("ftp_user")
        private val FTP_PASSWORD     = stringPreferencesKey("ftp_password")
        private val FTP_REMOTE_PATH  = stringPreferencesKey("ftp_remote_path")
        private val FTP_PASSIVE_MODE = booleanPreferencesKey("ftp_passive_mode")
        private val FTP_TIMEOUT      = intPreferencesKey("ftp_timeout")
        private val APP_LANGUAGE     = stringPreferencesKey("app_language")
        private val DOWNLOAD_PATH    = stringPreferencesKey("download_path")
    }

    val ftpConfig: Flow<FtpConfig> = context.dataStore.data.map { prefs ->
        FtpConfig(
            enabled     = prefs[FTP_ENABLED]      ?: false,
            host        = prefs[FTP_HOST]          ?: "192.168.1.210",
            port        = prefs[FTP_PORT]          ?: 2121,
            user        = prefs[FTP_USER]          ?: "anonymous",
            password    = prefs[FTP_PASSWORD]      ?: "",
            remotePath  = prefs[FTP_REMOTE_PATH]   ?: "/data/pkg",
            passiveMode = prefs[FTP_PASSIVE_MODE]  ?: true,
            timeout     = prefs[FTP_TIMEOUT]       ?: 30
        )
    }

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[APP_LANGUAGE] ?: "en"
    }

    val downloadPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DOWNLOAD_PATH] ?: ""
    }

    suspend fun saveFtpConfig(config: FtpConfig) {
        context.dataStore.edit { prefs ->
            prefs[FTP_ENABLED]      = config.enabled
            prefs[FTP_HOST]         = config.host
            prefs[FTP_PORT]         = config.port
            prefs[FTP_USER]         = config.user
            prefs[FTP_PASSWORD]     = config.password
            prefs[FTP_REMOTE_PATH]  = config.remotePath
            prefs[FTP_PASSIVE_MODE] = config.passiveMode
            prefs[FTP_TIMEOUT]      = config.timeout
        }
    }

    suspend fun saveLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_LANGUAGE] = lang
        }
    }

    suspend fun saveDownloadPath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[DOWNLOAD_PATH] = path
        }
    }
}
