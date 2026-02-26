package com.fpkgi.manager.ui.screens

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpkgi.manager.MainViewModel
import com.fpkgi.manager.data.model.LocalPkgFile
import com.fpkgi.manager.i18n.LocalAppStrings
import com.fpkgi.manager.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPkgBrowserScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val s           = LocalAppStrings.current
    val ctx         = LocalContext.current
    val scope       = rememberCoroutineScope()
    val ftpConfig   by viewModel.ftpConfig.collectAsState(initial = null)

    var pkgFiles    by remember { mutableStateOf<List<LocalPkgFile>>(emptyList()) }
    var isScanning  by remember { mutableStateOf(false) }
    var scanDirs    by remember { mutableStateOf("") }
    var selectedPkg by remember { mutableStateOf<LocalPkgFile?>(null) }
    var sendStatus  by remember { mutableStateOf("") }
    var isSending   by remember { mutableStateOf(false) }
    var uploadingId by remember { mutableStateOf<String?>(null) }

    // File picker fallback — select ANY .pkg from storage
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val cfg = ftpConfig
        if (cfg == null || !cfg.enabled) {
            sendStatus = s.sendLocalNoFtp; return@rememberLauncherForActivityResult
        }
        val fileName = uri.lastPathSegment?.substringAfterLast("/")
            ?.substringAfterLast("%2F") ?: "file.pkg"
        sendStatus = s.sendLocalUploading
        isSending = true
        scope.launch(Dispatchers.IO) {
            try {
                val cacheFile = File(ctx.cacheDir, fileName)
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    cacheFile.outputStream().use { it2 -> input.copyTo(it2) }
                }
                withContext(Dispatchers.Main) {
                    viewModel.uploadLocalPkg(cacheFile.absolutePath, fileName)
                    sendStatus = s.sendLocalSuccess.replace(
                        "{dest}", "${cfg.host}:${cfg.port}${cfg.remotePath}/$fileName"
                    )
                    isSending = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sendStatus = s.sendLocalError.replace("{msg}", e.message ?: "?")
                    isSending = false
                }
            }
        }
    }

    // Scan for .pkg files on launch
    LaunchedEffect(Unit) {
        isScanning = true
        val found = mutableListOf<LocalPkgFile>()
        val searchDirs = buildList {
            // 1. Public Downloads folder
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            // 2. App external files dir
            ctx.getExternalFilesDir(null)?.let { add(it) }
            ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { add(it) }
            // 3. App internal cache
            add(ctx.cacheDir)
            // 4. External storage root (if accessible)
            Environment.getExternalStorageDirectory()?.let { root ->
                File(root, "PKG").takeIf { it.exists() }?.let { add(it) }
                File(root, "PS4").takeIf { it.exists() }?.let { add(it) }
                File(root, "FPKGI").takeIf { it.exists() }?.let { add(it) }
            }
        }.distinct()
        scanDirs = searchDirs.joinToString(", ") { it.name }
        withContext(Dispatchers.IO) {
            for (dir in searchDirs) {
                scanDirForPkgs(dir, found)
            }
            found.sortByDescending { it.modifiedAt }
        }
        pkgFiles = found
        isScanning = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        s.pkgBrowserTitle,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberBlue
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = CyberBlue)
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = {
                        scope.launch {
                            isScanning = true
                            val found = mutableListOf<LocalPkgFile>()
                            withContext(Dispatchers.IO) {
                                val dirs = buildList {
                                    add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                                    ctx.getExternalFilesDir(null)?.let { add(it) }
                                    add(ctx.cacheDir)
                                    Environment.getExternalStorageDirectory()?.let { root ->
                                        listOf("PKG","PS4","FPKGI").forEach { sub ->
                                            File(root, sub).takeIf { it.exists() }?.let { add(it) }
                                        }
                                    }
                                }.distinct()
                                dirs.forEach { scanDirForPkgs(it, found) }
                                found.sortByDescending { it.modifiedAt }
                            }
                            pkgFiles = found
                            isScanning = false
                        }
                    }) {
                        Icon(Icons.Default.Refresh, s.refresh, tint = TextSecondary)
                    }
                    // Manual file picker
                    IconButton(onClick = { filePicker.launch("*/*") }) {
                        Icon(Icons.Default.FolderOpen, s.pkgBrowserPickFile, tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyMid)
            )
        },
        containerColor = DarkBackground
    ) { padding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── FTP status banner ─────────────────────────────────────────
            item {
                val cfg = ftpConfig
                val ftpOk = cfg != null && cfg.enabled && cfg.host.isNotBlank()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (ftpOk) NeonGreen.copy(alpha = 0.08f) else ErrorRed.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (ftpOk) Icons.Default.Wifi else Icons.Default.WifiOff,
                        null,
                        tint = if (ftpOk) NeonGreen else ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        if (ftpOk) "FTP → ${cfg!!.host}:${cfg.port}" else s.sendLocalNoFtp,
                        color = if (ftpOk) NeonGreen else ErrorRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            // ── Send status ───────────────────────────────────────────────
            if (sendStatus.isNotBlank()) {
                item {
                    AnimatedVisibility(visible = true) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavyMid)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberBlue
                                )
                            }
                            Text(
                                sendStatus,
                                color = when {
                                    sendStatus.startsWith("✅") -> NeonGreen
                                    sendStatus.startsWith("❌") -> ErrorRed
                                    else -> CyberBlue
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Info / scanning ───────────────────────────────────────────
            if (isScanning) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = CyberBlue)
                            Text(
                                s.pkgBrowserScanning,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            if (pkgFiles.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📂", fontSize = 48.sp)
                        Text(
                            s.pkgBrowserEmpty,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                        Text(
                            s.pkgBrowserEmptyHint,
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { filePicker.launch("*/*") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(s.pkgBrowserPickFile, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                return@LazyColumn
            }

            // ── Header count ──────────────────────────────────────────────
            item {
                Text(
                    "${pkgFiles.size} ${s.pkgBrowserFound}",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ── PKG file list ─────────────────────────────────────────────
            items(pkgFiles, key = { it.path }) { pkg ->
                PkgFileCard(
                    pkg           = pkg,
                    isSelected    = selectedPkg?.path == pkg.path,
                    isUploading   = uploadingId == pkg.path,
                    ftpEnabled    = ftpConfig?.enabled == true,
                    strings       = s,
                    onClick       = { selectedPkg = if (selectedPkg?.path == pkg.path) null else pkg },
                    onSend        = {
                        val cfg = ftpConfig
                        if (cfg == null || !cfg.enabled) {
                            sendStatus = s.sendLocalNoFtp; return@PkgFileCard
                        }
                        uploadingId = pkg.path
                        sendStatus  = s.sendLocalUploading
                        isSending   = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                withContext(Dispatchers.Main) {
                                    viewModel.uploadLocalPkg(pkg.path, pkg.name)
                                    sendStatus = s.sendLocalSuccess.replace(
                                        "{dest}", "${cfg.host}:${cfg.port}${cfg.remotePath}/${pkg.name}"
                                    )
                                    uploadingId = null
                                    isSending   = false
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    sendStatus  = s.sendLocalError.replace("{msg}", e.message ?: "?")
                                    uploadingId = null
                                    isSending   = false
                                }
                            }
                        }
                    }
                )
            }

            // ── Bottom: pick from anywhere ────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, TextSecondary.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        s.pkgBrowserPickFile,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PkgFileCard(
    pkg: LocalPkgFile,
    isSelected: Boolean,
    isUploading: Boolean,
    ftpEnabled: Boolean,
    strings: com.fpkgi.manager.i18n.AppStrings,
    onClick: () -> Unit,
    onSend: () -> Unit
) {
    val borderColor = when {
        isUploading -> CyberBlue
        isSelected  -> NeonGreen.copy(alpha = 0.6f)
        else        -> NavyMid.copy(alpha = 0.3f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(NavyMid)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icon
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavyDeep),
                contentAlignment = Alignment.Center
            ) {
                Text("📦", fontSize = 20.sp)
            }

            // Name + meta
            Column(Modifier.weight(1f)) {
                Text(
                    pkg.name,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        pkg.humanSize,
                        color = CyberBlue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text("·", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        pkg.humanDate,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            // Send button
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = CyberBlue
                )
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = ftpEnabled,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Upload,
                        strings.pkgBrowserSend,
                        tint = if (ftpEnabled) NeonGreen else TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Expanded path
        AnimatedVisibility(visible = isSelected) {
            Column(Modifier.padding(top = 8.dp)) {
                HorizontalDivider(color = NavyMid.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Text(
                    pkg.path,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onSend,
                    enabled = ftpEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)
                ) {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        strings.pkgBrowserSendFull,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun scanDirForPkgs(dir: File, result: MutableList<LocalPkgFile>) {
    if (!dir.exists() || !dir.isDirectory) return
    try {
        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> scanDirForPkgs(file, result) // recurse 1 level
                file.extension.equals("pkg", ignoreCase = true) -> {
                    result.add(
                        LocalPkgFile(
                            name       = file.name,
                            path       = file.absolutePath,
                            sizeBytes  = file.length(),
                            modifiedAt = file.lastModified()
                        )
                    )
                }
            }
        }
    } catch (_: SecurityException) { /* no permission for this dir */ }
}
