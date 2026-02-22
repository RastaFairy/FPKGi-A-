package com.fpkgi.manager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.fpkgi.manager.data.model.Game
import com.fpkgi.manager.i18n.LocalAppStrings
import com.fpkgi.manager.i18n.appStringsForLanguage
import com.fpkgi.manager.ui.screens.*
import com.fpkgi.manager.ui.theme.DarkBackground
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
                onBack = { navController.popBackStack() })
        }
    }
}
