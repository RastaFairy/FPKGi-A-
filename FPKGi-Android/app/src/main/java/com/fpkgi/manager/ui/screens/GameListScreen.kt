package com.fpkgi.manager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
fun GameListScreen(
    viewModel: MainViewModel,
    onGameDetail: (Game) -> Unit,
    onNavigateDownloads: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val s               = LocalAppStrings.current
    val games          by viewModel.games.collectAsState()
    val searchQuery    by viewModel.searchQuery.collectAsState()
    val loadedFile     by viewModel.loadedFileName.collectAsState()
    val downloads      by viewModel.downloads.collectAsState()
    val activeDownloads = downloads.count {
        it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.PAUSED
    }

    val jsonPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.loadJsonFromUri(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.appTitle, fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, color = CyberBlue, fontSize = 18.sp)
                        if (loadedFile.isNotBlank()) {
                            Text(loadedFile, color = TextSecondary,
                                fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyMid),
                actions = {
                    if (activeDownloads > 0) {
                        BadgedBox(badge = {
                            Badge(containerColor = NeonOrange) {
                                Text("$activeDownloads", color = NavyDark)
                            }
                        }) {
                            IconButton(onClick = onNavigateDownloads) {
                                Icon(Icons.Default.Download, "Descargas", tint = NeonOrange)
                            }
                        }
                    } else {
                        IconButton(onClick = onNavigateDownloads) {
                            Icon(Icons.Default.Download, "Descargas", tint = TextSecondary)
                        }
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, "Ajustes", tint = TextSecondary)
                    }
                }
            )
        },
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { jsonPicker.launch("application/json") },
                containerColor = NavyDeep, contentColor = CyberBlue
            ) { Icon(Icons.Default.FolderOpen, s.openJson) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = viewModel::setSearch,
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder   = {
                    Text(s.searchPlaceholder, color = TextMuted,
                        fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                },
                leadingIcon  = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearch("") }) {
                            Icon(Icons.Default.Clear, null, tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = CyberBlue, unfocusedBorderColor = NavyDeep,
                    focusedContainerColor = NavyMid, unfocusedContainerColor = NavyMid,
                    cursorColor          = CyberBlue,
                    focusedTextColor     = TextPrimary, unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            )

            // Stats + sort chips
            if (games.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(s.nGames.replace("{n}", games.size.toString()),
                        color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.weight(1f))
                    SortChip(s.sortName)   { viewModel.setSort("name") }
                    Spacer(Modifier.width(4.dp))
                    SortChip(s.sortRegion) { viewModel.setSort("region") }
                    Spacer(Modifier.width(4.dp))
                    SortChip(s.sortFw)     { viewModel.setSort("minFw") }
                }
            }

            if (games.isEmpty()) {
                EmptyState(s = s, onOpen = { jsonPicker.launch("application/json") })
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(games, key = { "${it.titleId}_${it.pkgUrl.takeLast(8)}" }) { game ->
                        GameCard(
                            game          = game,
                            s             = s,
                            onClick       = { onGameDetail(game) },
                            onDownload    = { viewModel.startDownload(game) },
                            onCheckAvail  = { viewModel.checkAvailability(game) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(4.dp))
            .background(NavyDeep)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun GameCard(
    game: Game,
    s: com.fpkgi.manager.i18n.AppStrings,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onCheckAvail: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape    = RoundedCornerShape(8.dp),
        border   = BorderStroke(1.dp, NavyDeep)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)).background(NavyDeep),
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
                    Text("🎮", fontSize = 26.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(game.name, color = TextPrimary, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip(game.titleId, CyberBlue)
                    InfoChip("v${game.version}")
                    InfoChip(game.region)
                    InfoChip("FW ${game.minFw}")
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(game.availStatus, s)
                    Text(game.size, color = TextMuted, fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(NavyMid)
                ) {
                    DropdownMenuItem(
                        text    = { Text(s.ctxDownload, color = CyberBlue,
                            fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                        onClick = { showMenu = false; onDownload() }
                    )
                    DropdownMenuItem(
                        text    = { Text(s.ctxCheckAvail, color = TextPrimary,
                            fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                        onClick = { showMenu = false; onCheckAvail() }
                    )
                    DropdownMenuItem(
                        text    = { Text(s.ctxDetails, color = TextPrimary,
                            fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                        onClick = { showMenu = false; onClick() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(s: com.fpkgi.manager.i18n.AppStrings, onOpen: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🎮", fontSize = 72.sp)
            Text(s.emptyTitle, color = CyberBlue, fontSize = 22.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("PS4 FPKG Package Manager", color = TextSecondary,
                fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            Text(s.emptyHint, color = TextMuted, fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onOpen,
                colors  = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                shape   = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.FolderOpen, null, tint = CyberBlue)
                Spacer(Modifier.width(8.dp))
                Text(s.openJson, color = CyberBlue,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Text(s.emptyFormats, color = TextMuted, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace)
        }
    }
}
