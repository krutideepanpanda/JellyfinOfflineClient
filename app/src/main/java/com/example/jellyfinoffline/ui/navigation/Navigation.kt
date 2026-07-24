package com.example.jellyfinoffline.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jellyfinoffline.ui.login.LoginScreen
import com.example.jellyfinoffline.ui.home.HomeScreen
import com.example.jellyfinoffline.ui.details.ShowDetailsScreen
import com.example.jellyfinoffline.ui.player.PlayerScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { api ->
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            HomeScreen(
                onShowClick = { showId ->
                    navController.navigate("details/$showId")
                }
            )
        }
        
        composable(
            "details/{showId}",
            arguments = listOf(navArgument("showId") { type = NavType.StringType })
        ) { backStackEntry ->
            val showId = backStackEntry.arguments?.getString("showId") ?: return@composable
            ShowDetailsScreen(
                showId = showId,
                onEpisodeClick = { episodeId ->
                    navController.navigate("player/$showId/$episodeId")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            "player/{showId}/{episodeId}",
            arguments = listOf(
                navArgument("showId") { type = NavType.StringType },
                navArgument("episodeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val showId = backStackEntry.arguments?.getString("showId") ?: return@composable
            val episodeId = backStackEntry.arguments?.getString("episodeId") ?: return@composable
            PlayerScreen(
                showId = showId,
                episodeId = episodeId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
