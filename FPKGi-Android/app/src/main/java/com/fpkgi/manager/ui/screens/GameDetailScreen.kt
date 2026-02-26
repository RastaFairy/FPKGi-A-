package com.fpkgi.manager.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fpkgi.manager.MainViewModel
import com.fpkgi.manager.data.model.*
import com.fpkgi.manager.i18n.LocalAppStrings
import com.fpkgi.manager.ui.components.*
import com.fpkgi.manager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    game: Game,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val s             = LocalAppStrings.current
    val orbisResult  by viewModel.orbisResult.collectAsState()
    val orbisLoading by viewModel.orbisLoading.collectAsState()
    val ftpConfig    by viewModel.ftpConfig.collectAsState(FtpConfig())
    val uriHandler    = LocalUriHandler.current
    var showDlConfirm by remember { mutableStateOf(false) }

    // Cargar parches al entrar. DisposableEffect limpia el resultado al salir,
    // evitando que el juego siguiente muestre datos del anterior.
    LaunchedEffect(game.titleId) { viewModel.fetchOrbisPatches(game.titleId) }
    DisposableEffect(game.titleId) {
        onDispose { viewModel.clearOrbisResult() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(game.name, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, color = TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = CyberBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyMid),
                actions = {
                    IconButton(onClick = {
                        uriHandler.openUri("https://orbispatches.com/${game.titleId}")
                    }) {
                        Icon(Icons.Default.OpenInBrowser, "OrbisPatches", tint = TextSecondary)
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ─── Header ───────────────────────────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth().background(NavyMid).padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        Modifier.size(100.dp).clip(RoundedCornerShape(10.dp)).background(NavyDeep),
                        contentAlignment = Alignment.Center
                    ) {
                        if (game.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(game.coverUrl).crossfade(true).build(),
                                contentDescription = game.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("🎮", fontSize = 42.sp)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(game.name, color = CyberBlue, fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        MonoLabel(s.detailTitleId, game.titleId)
                        MonoLabel(s.detailVersion, "v${game.version}")
                        MonoLabel(s.detailRegion, game.region)
                        MonoLabel(s.detailFw, game.minFw)
                        MonoLabel(s.detailSize, game.size)
                        Spacer(Modifier.height(8.dp))
                        StatusBadge(game.availStatus)
                    }
                }
            }

            // ─── Actions ──────────────────────────────────────────────────
            item {
                Column(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ftpLabel = if (ftpConfig.enabled)
                        "${s.btnDownloadPkg}  →  FTP ${ftpConfig.host}"
                    else s.btnDownloadPkg

                    FpkgiButton(
                        text    = ftpLabel,
                        onClick = { showDlConfirm = true },
                        color   = if (game.pkgUrl.isBlank()) TextMuted else CyberBlue,
                        enabled = game.pkgUrl.isNotBlank()
                    )
                    FpkgiButton(
                        s.btnCheckAvail,
                        onClick = { viewModel.checkAvailability(game) },
                        color   = NeonGreen
                    )
                }
            }

            // ─── Updates section ──────────────────────────────────────────
            item { SectionHeader(s.updatesSection) }

            if (orbisLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = CyberBlue)
                            Text(s.consultingOrbis, color = TextSecondary,
                                fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                val result = orbisResult
                when {
                    result == null -> { /* still loading */ }
                    result.blocked -> {
                        item { OrbisBlockedCard(game.titleId, uriHandler, s) }
                    }
                    result.patches.isEmpty() -> {
                        item {
                            Box(Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("📭", fontSize = 36.sp)
                                    Text(result.error.ifBlank { s.noPatches },
                                        color = TextSecondary, fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    else -> {
                        item {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                Text(s.patchesFound.replace("{n}", result.patches.size.toString()),
                                    color = NeonGreen, fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp)
                            }
                        }
                        items(result.patches) { patch ->
                            PatchCard(patch = patch, game = game, s = s)
                        }
                        item { WarningCard(s) }
                    }
                }
            }
        }
    }

    // Download confirmation dialog
    if (showDlConfirm) {
        AlertDialog(
            onDismissRequest = { showDlConfirm = false },
            containerColor   = NavyMid,
            title = { Text(s.confirmTitle, color = CyberBlue,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(game.name, color = TextPrimary, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold)
                    Text("${s.detailVersion}: v${game.version}  |  ${game.size}",
                        color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    if (ftpConfig.enabled) {
                        Text(s.destFtp
                            .replace("{host}", ftpConfig.host)
                            .replace("{port}", ftpConfig.port.toString())
                            .replace("{path}", ftpConfig.remotePath),
                            color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    } else {
                        Text(s.destLocal, color = TextSecondary,
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDlConfirm = false; viewModel.startDownload(game)
                }) {
                    Text(s.confirmBtn, color = CyberBlue, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDlConfirm = false }) {
                    Text(s.cancelBtn, color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
private fun PatchCard(
    patch: OrbisPatch,
    game: Game,
    s: com.fpkgi.manager.i18n.AppStrings
) {
    var expanded by remember { mutableStateOf(patch.isLatest) }
    val isYours = try {
        patch.version.trim() == game.version.trim() ||
        patch.version.trimStart('0', 'v').trim() == game.version.trimStart('0', 'v').trim()
    } catch (_: Exception) { false }

    // FW compatibility: 🟢 si el parche requiere FW <= FW del juego (compatible)
    //                   🔴 si requiere FW más alto (necesita actualizar la consola)
    val fwCompat: Boolean? = try {
        if (patch.firmware.isNotBlank() && patch.firmware != "?" &&
            game.minFw.isNotBlank() && game.minFw != "?") {
            patch.firmware.toFloat() <= game.minFw.toFloat()
        } else null
    } catch (_: Exception) { null }

    val borderColor = when {
        patch.isLatest -> GoldYellow
        isYours        -> NeonGreen
        else           -> NavyDeep
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (patch.isLatest) Color(0xFF1A2A3A) else SurfaceCard),
        shape  = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(12.dp)) {

            // ── Fila 1: versión + badges + toggle ──────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Patch v${patch.version}",
                    color = if (patch.isLatest) GoldYellow else CyberBlue,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(8.dp))
                if (patch.isLatest) InfoChip(s.patchLatest, GoldYellow)
                if (isYours)        InfoChip(s.patchYours,  NeonGreen)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { expanded = !expanded }, Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = TextSecondary
                    )
                }
            }

            // ── Fila 2: FW 🟢/🔴, tamaño, fecha ──────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (patch.firmware.isNotBlank() && patch.firmware != "?") {
                    val fwIcon  = when (fwCompat) { true -> "🟢 "; false -> "🔴 "; null -> "" }
                    val fwColor = when (fwCompat) { true -> NeonGreen; false -> ErrorRed; null -> TextSecondary }
                    InfoChip(
                        text  = "${fwIcon}${s.patchFwLabel} ${patch.firmware}",
                        color = fwColor
                    )
                }
                if (patch.size.isNotBlank() && patch.size != "?")
                    InfoChip(patch.size)
                if (patch.creationDate.isNotBlank())
                    InfoChip(patch.creationDate, TextMuted)
            }

            // ── Notas (expandible) ─────────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = NavyDeep)
                    Spacer(Modifier.height(8.dp))
                    if (patch.notes.isNotBlank()) {
                        Text(
                            s.patchNotesTitle,
                            color      = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkBackground)
                                .padding(10.dp)
                        ) {
                            Text(
                                patch.notes,
                                color      = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize   = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        Text(
                            s.patchNoNotes,
                            color      = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 11.sp,
                            fontStyle  = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrbisBlockedCard(
    titleId: String,
    uriHandler: UriHandler,
    s: com.fpkgi.manager.i18n.AppStrings
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF2A1800)),
        border   = BorderStroke(1.dp, NeonOrange)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.orbisBlocked, color = NeonOrange, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold)
            Text(s.orbisBlockedHint, color = TextSecondary,
                fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            TextButton(onClick = {
                uriHandler.openUri("https://orbispatches.com/$titleId")
            }) {
                Text(s.openOrbis, color = CyberBlue, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun WarningCard(s: com.fpkgi.manager.i18n.AppStrings) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF3B1800)),
        border   = BorderStroke(1.dp, NeonOrange.copy(alpha = 0.4f))
    ) {
        Text(s.warningBackport, color = NeonOrange, fontFamily = FontFamily.Monospace,
            fontSize = 11.sp, modifier = Modifier.padding(12.dp), lineHeight = 16.sp)
    }
}
