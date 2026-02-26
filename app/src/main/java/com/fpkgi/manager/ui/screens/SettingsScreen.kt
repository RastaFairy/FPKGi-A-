package com.fpkgi.manager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpkgi.manager.MainViewModel
import com.fpkgi.manager.data.model.FtpConfig
import com.fpkgi.manager.i18n.AVAILABLE_LANGUAGES
import com.fpkgi.manager.i18n.LocalAppStrings
import com.fpkgi.manager.ui.components.FpkgiButton
import com.fpkgi.manager.ui.components.SectionHeader
import com.fpkgi.manager.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit, onNavigatePkgBrowser: () -> Unit = {}) {
    val s             = LocalAppStrings.current
    val scope         = rememberCoroutineScope()
    val ctx           = LocalContext.current
    val currentConfig by viewModel.ftpConfig.collectAsState(FtpConfig())
    val currentLang   by viewModel.currentLanguage.collectAsState()
    val iconCacheMsg  by viewModel.iconCacheMsg.collectAsState()

    // FTP fields
    var enabled     by remember(currentConfig) { mutableStateOf(currentConfig.enabled) }
    var host        by remember(currentConfig) { mutableStateOf(currentConfig.host) }
    var port        by remember(currentConfig) { mutableStateOf(currentConfig.port.toString()) }
    var user        by remember(currentConfig) { mutableStateOf(currentConfig.user) }
    var password    by remember(currentConfig) { mutableStateOf(currentConfig.password) }
    var remotePath  by remember(currentConfig) { mutableStateOf(currentConfig.remotePath) }
    var passiveMode by remember(currentConfig) { mutableStateOf(currentConfig.passiveMode) }
    var timeout     by remember(currentConfig) { mutableStateOf(currentConfig.timeout.toString()) }

    var testStatus    by remember { mutableStateOf("") }
    var showPassword  by remember { mutableStateOf(false) }
    var saved         by remember { mutableStateOf(false) }
    var cacheSize     by remember { mutableStateOf("") }

    // Local PKG upload state
    var uploadStatus  by remember { mutableStateOf("") }
    var uploadPicking by remember { mutableStateOf(false) }

    // File picker for local PKG
    val pkgFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uploadPicking = false
        if (uri == null) { uploadStatus = ""; return@rememberLauncherForActivityResult }
        val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
            ?: "upload.pkg"
        val realPath = uri.path ?: run {
            uploadStatus = s.sendLocalError.replace("{msg}", "No se pudo obtener la ruta del archivo")
            return@rememberLauncherForActivityResult
        }
        if (!currentConfig.enabled) {
            uploadStatus = s.sendLocalNoFtp
            return@rememberLauncherForActivityResult
        }
        uploadStatus = s.sendLocalUploading
        scope.launch(Dispatchers.IO) {
            try {
                // Copy file to cache first to get a real file path
                val cacheFile = java.io.File(ctx.cacheDir, fileName)
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    cacheFile.outputStream().use { output -> input.copyTo(output) }
                }
                withContext(Dispatchers.Main) {
                    viewModel.uploadLocalPkg(cacheFile.absolutePath, fileName)
                    uploadStatus = s.sendLocalSuccess.replace("{dest}",
                        "${currentConfig.host}:${currentConfig.port}${currentConfig.remotePath}/$fileName")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uploadStatus = s.sendLocalError.replace("{msg}", e.message ?: "Error desconocido")
                }
            }
        }
    }

    // Load cache size on entry
    LaunchedEffect(Unit) {
        cacheSize = viewModel.getIconCacheSize()
    }

    // Handle icon cache clear message
    LaunchedEffect(iconCacheMsg) {
        if (iconCacheMsg == "ok") {
            cacheSize = viewModel.getIconCacheSize()
            viewModel.consumeIconCacheMsg()
        }
    }

    fun saveConfig() {
        val portInt    = port.trim().toIntOrNull() ?: 2121
        val timeoutInt = timeout.trim().toIntOrNull() ?: 30
        val config = FtpConfig(
            enabled = enabled, host = host.trim(),
            port = portInt, user = user.trim(),
            password = password, remotePath = remotePath.trim(),
            passiveMode = passiveMode, timeout = timeoutInt
        )
        scope.launch { viewModel.saveFtpConfig(config); saved = true }
    }

    fun testConnection() {
        testStatus = "🔄 Probando..."
        scope.launch(Dispatchers.IO) {
            try {
                val ftp = FTPClient()
                ftp.defaultTimeout = 10_000; ftp.connectTimeout = 10_000
                ftp.connect(host.trim(), port.trim().toIntOrNull() ?: 2121)
                ftp.login(user.trim(), password)
                if (passiveMode) ftp.enterLocalPassiveMode()
                val status = try {
                    ftp.changeWorkingDirectory(remotePath.trim())
                    "✅ Conexión OK"
                } catch (_: Exception) { "⚠️ Conectado (directorio no existe)" }
                ftp.logout(); ftp.disconnect()
                withContext(Dispatchers.Main) { testStatus = status }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    testStatus = "❌ Error: ${e.message?.take(60)}"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(s.settingsTitle, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, color = CyberBlue)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = CyberBlue)
                    }
                },
                actions = {
                    IconButton(onClick = { saveConfig(); onBack() }) {
                        Icon(Icons.Default.Save, "Guardar", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyMid)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ─── Language ─────────────────────────────────────────────────
            SectionHeader(s.languageSection)
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyMid),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AVAILABLE_LANGUAGES.forEach { (code, nativeName) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (code == currentLang) NavyDeep else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { viewModel.setLanguage(code) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (code == currentLang) "✓  $nativeName" else "   $nativeName",
                                color = if (code == currentLang) CyberBlue else TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (code == currentLang) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── Icon Cache ───────────────────────────────────────────────
            SectionHeader(s.iconCacheSection)
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyMid),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (cacheSize.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.iconCacheSizeLabel, color = TextSecondary,
                                fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(s.iconCacheSize.replace("{size}", cacheSize),
                                color = GoldYellow, fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    FpkgiButton(
                        s.iconCacheClearBtn,
                        onClick = { viewModel.clearIconCache() },
                        color   = NeonOrange
                    )
                    if (iconCacheMsg == "ok") {
                        Text(s.iconCacheCleared, color = NeonGreen,
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── Send local PKG via FTP ───────────────────────────────────
            SectionHeader(s.sendLocalSection)
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyMid),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.sendLocalHint, color = TextSecondary,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 16.sp)
                    // Navigate to the dedicated PKG browser screen
                    FpkgiButton(
                        text    = s.pkgBrowserTitle,
                        onClick = onNavigatePkgBrowser,
                        color   = NeonOrange
                    )
                    if (!currentConfig.enabled) {
                        Text(s.sendLocalNoFtpHint, color = TextMuted,
                            fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── FTP Config ───────────────────────────────────────────────
            SectionHeader(s.ftpSection)
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyMid),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.ftpEnable, color = TextPrimary,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            Text(s.ftpEnableDesc, color = TextSecondary,
                                fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Switch(
                            checked       = enabled,
                            onCheckedChange = { enabled = it },
                            colors        = SwitchDefaults.colors(
                                checkedThumbColor = NavyDark, checkedTrackColor = NeonGreen)
                        )
                    }
                    HorizontalDivider(color = NavyDeep)
                    val fe = enabled
                    FtpField(s.ftpHost, host, { host = it }, "192.168.1.210", fe, KeyboardType.Uri)
                    FtpField(s.ftpPort, port, { port = it }, "2121", fe, KeyboardType.Number)
                    FtpField(s.ftpUser, user, { user = it }, "anonymous", fe)
                    FtpField(
                        s.ftpPassword, password, { password = it },
                        "(vacío si no tiene)", fe,
                        isPassword      = true,
                        showPassword    = showPassword,
                        onTogglePassword = { showPassword = !showPassword }
                    )
                    FtpField(s.ftpRemotePath, remotePath, { remotePath = it }, "/data/pkg", fe)
                    FtpField(s.ftpTimeout, timeout, { timeout = it }, "30", fe, KeyboardType.Number)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.ftpPassive,
                            color = if (fe) TextPrimary else TextMuted,
                            fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                            modifier = Modifier.weight(1f))
                        Switch(
                            checked         = passiveMode,
                            onCheckedChange = { passiveMode = it },
                            enabled         = fe,
                            colors          = SwitchDefaults.colors(
                                checkedThumbColor = NavyDark, checkedTrackColor = CyberBlue)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FpkgiButton(s.ftpTestBtn, onClick = { testConnection() },
                    color = CyberBlue, enabled = enabled)
                if (testStatus.isNotBlank()) {
                    val color = when {
                        testStatus.startsWith("✅") -> NeonGreen
                        testStatus.startsWith("⚠")  -> NeonOrange
                        testStatus.startsWith("❌") -> ErrorRed
                        else -> GoldYellow
                    }
                    Text(testStatus, color = color, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                FpkgiButton(s.ftpSaveBtn, onClick = { saveConfig() }, color = NeonGreen)
                if (saved) Text(s.ftpSaved, color = NeonGreen,
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))

            // ─── FTP Guide ────────────────────────────────────────────────
            SectionHeader(s.ftpGuideTitle)
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDeep),
                shape  = RoundedCornerShape(8.dp)
            ) {
                Text(s.ftpGuideText, color = TextSecondary,
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 17.sp,
                    modifier = Modifier.padding(14.dp))
            }

            Spacer(Modifier.height(16.dp))

            // ─── About ────────────────────────────────────────────────────
            SectionHeader(s.aboutSection)
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDeep),
                shape  = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.aboutVersion, color = CyberBlue,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(s.aboutOriginal, color = TextSecondary,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(s.aboutPs4Port, color = TextSecondary,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(s.aboutPython, color = TextSecondary,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(s.aboutAndroid, color = TextSecondary,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(s.aboutLicense, color = TextMuted,
                        fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FtpField(
    label: String, value: String, onValueChange: (String) -> Unit,
    placeholder: String, enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = if (enabled) CyberBlue else TextMuted,
            fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier.fillMaxWidth(),
            enabled       = enabled,
            singleLine    = true,
            placeholder   = {
                Text(placeholder, color = TextMuted,
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            },
            trailingIcon = if (isPassword) {
                { IconButton(onClick = { onTogglePassword?.invoke() }) {
                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = TextSecondary)
                }}
            } else null,
            visualTransformation = if (isPassword && !showPassword)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = CyberBlue,
                unfocusedBorderColor  = NavyDeep,
                focusedContainerColor = SurfaceInput,
                unfocusedContainerColor = SurfaceInput,
                disabledContainerColor  = SurfaceInput.copy(alpha = 0.5f),
                disabledBorderColor     = NavyDeep.copy(alpha = 0.5f),
                cursorColor             = CyberBlue,
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
                disabledTextColor       = TextMuted
            ),
            shape = RoundedCornerShape(6.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        )
    }
}
