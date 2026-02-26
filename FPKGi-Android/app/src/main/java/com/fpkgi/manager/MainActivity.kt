package com.fpkgi.manager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.fpkgi.manager.data.model.Game
import com.fpkgi.manager.i18n.LocalAppStrings
import com.fpkgi.manager.i18n.appStringsForLanguage
import com.fpkgi.manager.ui.screens.*
import com.fpkgi.manager.ui.theme.DarkBackground
import com.fpkgi.manager.BuildConfig
import com.fpkgi.manager.ui.theme.FPKGiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            // Observe language preference and recompose with right strings
            val langCode by viewModel.currentLanguage.collectAsState()
            val strings   = remember(langCode) { appStringsForLanguage(langCode) }

            CompositionLocalProvider(LocalAppStrings provides strings) {
                FPKGiTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize().background(DarkBackground)
                    ) {
                        FPKGiNavHost(viewModel)
                        // Comprobar actualizaciones al arrancar (una vez por sesión)
                        LaunchedEffect(Unit) {
                            viewModel.checkForAppUpdate(BuildConfig.VERSION_NAME)
                        }
                        // Diálogo de actualización
                        val updateInfo     by viewModel.updateInfo.collectAsState()
                        val updateProgress by viewModel.updateProgress.collectAsState()
                        updateInfo?.let { info ->
                            UpdateDialog(
                                info       = info,
                                progress   = updateProgress,
                                s          = strings,
                                onInstall  = { viewModel.downloadAndInstall(info.apkUrl) },
                                onDismiss  = { viewModel.dismissUpdate() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri -> viewModel.loadJsonFromUri(uri) }
        }
    }
}

// ─── Navigation ───────────────────────────────────────────────────────────
@Composable
fun FPKGiNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()
    var selectedGame  by remember { mutableStateOf<Game?>(null) }

    NavHost(
        navController      = navController,
        startDestination   = "games",
        enterTransition    = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left,  tween(250)) },
        exitTransition     = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(250)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(250)) },
        popExitTransition  = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(250)) }
    ) {
        composable("games") {
            GameListScreen(
                viewModel          = viewModel,
                onGameDetail       = { game -> selectedGame = game; navController.navigate("detail") },
                onNavigateDownloads = { navController.navigate("downloads") },
                onNavigateSettings  = { navController.navigate("settings") }
            )
        }
        composable("detail") {
            val game = selectedGame
            if (game != null) {
                GameDetailScreen(game = game, viewModel = viewModel,
                    onBack = { navController.popBackStack() })
            } else {
                navController.popBackStack()
            }
        }
        composable("downloads") {
            DownloadsScreen(viewModel = viewModel,
                onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigatePkgBrowser = { navController.navigate("pkgbrowser") })
        }
        composable("pkgbrowser") {
            LocalPkgBrowserScreen(viewModel = viewModel,
                onBack = { navController.popBackStack() })
        }
    }
}

// ─── Update dialog ────────────────────────────────────────────────────────
@Composable
private fun UpdateDialog(
    info:      com.fpkgi.manager.network.UpdateChecker.UpdateInfo,
    progress:  Float,   // -2=inactivo | -1=error | 0..1=descargando | 1=listo
    s:         com.fpkgi.manager.i18n.AppStrings,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    // Determinar estado actual
    val isDownloading = progress in 0f..0.9999f
    val isError       = progress == -1f
    val hasApk        = info.apkUrl.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Text(s.updateTitle,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    s.updateMessage.replace("{version}", info.latestVersion),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize   = 13.sp
                )
                if (info.changelog.isNotBlank()) {
                    Text(s.updateChangelog,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize   = 11.sp)
                    Text(info.changelog,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize   = 11.sp, lineHeight = 15.sp,
                        color      = com.fpkgi.manager.ui.theme.TextSecondary)
                }
                // Barra de progreso durante la descarga
                when {
                    isDownloading -> {
                        val pct = (progress * 100).toInt()
                        Text(s.updateDownloading.replace("{pct}", pct.toString()),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize   = 11.sp,
                            color      = com.fpkgi.manager.ui.theme.NeonGreen)
                        LinearProgressIndicator(
                            progress   = { progress },
                            modifier   = androidx.compose.ui.Modifier.fillMaxWidth()
                        )
                    }
                    isError -> Text(s.updateError,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize   = 11.sp,
                        color      = com.fpkgi.manager.ui.theme.ErrorRed)
                }
            }
        },
        confirmButton = {
            when {
                // Descargando: botón deshabilitado
                isDownloading -> TextButton(onClick = {}, enabled = false) {
                    Text(s.updateConfirm,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                // APK disponible: descargar e instalar sin salir de la app
                hasApk && !isError -> TextButton(onClick = onInstall) {
                    Text(s.updateConfirm,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                // Sin APK o error: abrir GitHub en el navegador
                else -> TextButton(onClick = {
                    uriHandler.openUri(info.releaseUrl)
                    onDismiss()
                }) {
                    Text(s.updateConfirm,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text(s.updateLater,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
    )
}
