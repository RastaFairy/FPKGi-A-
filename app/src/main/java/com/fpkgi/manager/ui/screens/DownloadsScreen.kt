package com.fpkgi.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpkgi.manager.MainViewModel
import com.fpkgi.manager.data.model.DownloadItem
import com.fpkgi.manager.data.model.DownloadStatus
import com.fpkgi.manager.i18n.LocalAppStrings
import com.fpkgi.manager.ui.theme.*
import com.fpkgi.manager.utils.JsonParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val s         = LocalAppStrings.current
    val downloads by viewModel.downloads.collectAsState()
    val active    = downloads.count {
        it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.PAUSED
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.downloadsTitle,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, color = CyberBlue)
                        Text("$active activa(s) / ${downloads.size} total",
                            color = TextSecondary, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = CyberBlue)
                    }
                },
                actions = {
                    // Clear completed
                    if (downloads.any { it.status == DownloadStatus.DONE ||
                                       it.status == DownloadStatus.CANCELLED ||
                                       it.status == DownloadStatus.ERROR }) {
                        TextButton(onClick = { viewModel.clearDoneDownloads() }) {
                            Text(s.btnClearDone, color = TextSecondary,
                                fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyMid)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("📭", fontSize = 56.sp)
                    Text(s.noDownloads, color = TextSecondary,
                        fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Text(s.noDownloadsHint, color = TextMuted,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(downloads, key = { it.id }) { item ->
                    DownloadCard(
                        item      = item,
                        onPause   = { viewModel.pauseDownload(item.id) },
                        onResume  = { viewModel.resumeDownload(item.id) },
                        onCancel  = { viewModel.cancelDownload(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val s           = LocalAppStrings.current
    val borderColor = when (item.status) {
        DownloadStatus.DONE      -> NeonGreen
        DownloadStatus.ERROR     -> ErrorRed
        DownloadStatus.CANCELLED -> TextMuted
        DownloadStatus.PAUSED    -> GoldYellow
        DownloadStatus.RUNNING   -> CyberBlue
        else                     -> NavyDeep
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape    = RoundedCornerShape(8.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (item.status) {
                    DownloadStatus.DONE  -> Icon(Icons.Default.CheckCircle, null,
                        tint = NeonGreen, modifier = Modifier.size(16.dp))
                    DownloadStatus.ERROR -> Icon(Icons.Default.Error, null,
                        tint = ErrorRed, modifier = Modifier.size(16.dp))
                    DownloadStatus.PAUSED -> Icon(Icons.Default.Pause, null,
                        tint = GoldYellow, modifier = Modifier.size(16.dp))
                    else -> {}
                }
                Spacer(Modifier.width(6.dp))
                // Show displayName for local uploads, else game name
                val displayText = if (item.displayName.isNotBlank()) item.displayName
                                  else item.game.name
                Text(displayText,
                    color = TextPrimary, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                // FTP / Local badge
                Box(
                    Modifier.clip(RoundedCornerShape(4.dp))
                        .background(if (item.isFtp) NeonGreen.copy(0.12f) else NavyDeep)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(if (item.isFtp) s.labelFtp else s.labelLocal,
                        color = if (item.isFtp) NeonGreen else TextSecondary,
                        fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Destination path
            if (item.destPath.isNotBlank()) {
                Text(item.destPath, color = TextMuted, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }

            // Progress section
            when (item.status) {
                DownloadStatus.RUNNING, DownloadStatus.QUEUED, DownloadStatus.PAUSED -> {
                    LinearProgressIndicator(
                        progress    = { item.progress },
                        modifier    = Modifier.fillMaxWidth(),
                        color       = when (item.status) {
                            DownloadStatus.PAUSED -> GoldYellow
                            else -> if (item.isFtp) NeonGreen else CyberBlue
                        },
                        trackColor  = NavyDeep
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when {
                                item.status == DownloadStatus.PAUSED ->
                                    s.labelPaused
                                item.status == DownloadStatus.QUEUED ->
                                    s.labelQueued
                                item.total > 0 ->
                                    "${JsonParser.bytesToHuman(item.downloaded)} / ${JsonParser.bytesToHuman(item.total)}"
                                else -> s.labelStarting
                            },
                            color = when (item.status) {
                                DownloadStatus.PAUSED -> GoldYellow
                                else -> TextSecondary
                            },
                            fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.total > 0 && item.status != DownloadStatus.PAUSED) {
                            Text("${(item.progress * 100).toInt()}%",
                                color = if (item.isFtp) NeonGreen else CyberBlue,
                                fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    // Control buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Pause / Resume (only for local HTTP downloads)
                        if (!item.isFtp) {
                            if (item.status == DownloadStatus.RUNNING) {
                                SmallActionButton(s.btnPause, GoldYellow, onPause)
                            } else if (item.status == DownloadStatus.PAUSED) {
                                SmallActionButton(s.btnResume, NeonGreen, onResume)
                            }
                        }
                        // Cancel always available
                        if (item.status != DownloadStatus.CANCELLED) {
                            SmallActionButton(s.btnCancel, ErrorRed, onCancel)
                        }
                    }
                }

                DownloadStatus.DONE -> {
                    LinearProgressIndicator(
                        progress   = { 1f },
                        modifier   = Modifier.fillMaxWidth(),
                        color      = NeonGreen, trackColor = NavyDeep
                    )
                    Text(s.labelCompleted,
                        color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }

                DownloadStatus.ERROR -> {
                    Text("${s.labelError}: ${item.errorMsg}",
                        color = ErrorRed, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }

                DownloadStatus.CANCELLED -> {
                    Text(s.labelCancelled, color = TextMuted,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SmallActionButton(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(28.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = color)
    }
}

// Extension on ViewModel to clear done downloads
