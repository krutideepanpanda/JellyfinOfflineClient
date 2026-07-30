package com.example.jellyfinoffline.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jellyfinoffline.ui.login.LoginScreen
import com.example.jellyfinoffline.ui.home.HomeScreen
import com.example.jellyfinoffline.ui.library.LibraryScreen
import com.example.jellyfinoffline.ui.details.ShowDetailsScreen
import com.example.jellyfinoffline.ui.player.PlayerScreen
import com.example.jellyfinoffline.ui.downloads.DownloadsScreen
import com.example.jellyfinoffline.ui.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            AppBottomNav(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { _ ->
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                )
            }
            
            composable("home") {
                HomeScreen(
                    onShowClick = { showId ->
                        navController.navigate("details/$showId")
                    },
                    onMovieClick = { movieId ->
                        navController.navigate("player/$movieId/$movieId")
                    },
                    onLibraryClick = { libId, libName ->
                        navController.navigate("library/$libId/$libName")
                    }
                )
            }
            
            composable(
                "library/{libraryId}/{libraryName}",
                arguments = listOf(
                    navArgument("libraryId") { type = NavType.StringType },
                    navArgument("libraryName") { type = NavType.StringType }
                ),
            ) { backStackEntry ->
                val libId = backStackEntry.arguments?.getString("libraryId") ?: return@composable
                val libName = backStackEntry.arguments?.getString("libraryName") ?: return@composable
                LibraryScreen(
                    libraryId = libId,
                    libraryName = libName,
                    onShowClick = { showId -> navController.navigate("details/$showId") },
                    onMovieClick = { movieId -> navController.navigate("player/$movieId/$movieId") },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("downloads") {
                DownloadsScreen(
                    onPlayClick = { showId, episodeId ->
                        navController.navigate("player/$showId/$episodeId")
                    }
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(
                "details/{showId}",
                arguments = listOf(navArgument("showId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val showId = backStackEntry.arguments?.getString("showId") ?: return@composable
                ShowDetailsScreen(
                    showId = showId,
                    onEpisodeClick = { episodeId ->
                        navController.navigate("player/$showId/$episodeId")
                    },
                ) { navController.popBackStack() }
            }
            
            composable(
                "player/{showId}/{episodeId}",
                arguments = listOf(
                    navArgument("showId") { type = NavType.StringType },
                    navArgument("episodeId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val showId = backStackEntry.arguments?.getString("showId") ?: return@composable
                val episodeId = backStackEntry.arguments?.getString("episodeId") ?: return@composable
                PlayerScreen(
                    showId = showId,
                    episodeId = episodeId,
                ) { navController.popBackStack() }
            }
        }
    }
}
