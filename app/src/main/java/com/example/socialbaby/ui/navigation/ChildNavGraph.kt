package com.example.socialbaby.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.socialbaby.ui.child.home.HomeScreen
import com.example.socialbaby.ui.child.photos.PhotoGalleryScreen
import com.example.socialbaby.ui.child.player.PlayerScreen
import com.example.socialbaby.ui.child.shorts.ShortsFeedScreen

sealed class ChildRoute(val route: String) {
    data object Home : ChildRoute("child_home")
    data object Shorts : ChildRoute("child_shorts")
    data object Photos : ChildRoute("child_photos")
    data object Player : ChildRoute("child_player")
    data object TimeUp : ChildRoute("child_timeup")
}

@Composable
fun ChildNavGraph(
    navController: NavHostController = rememberNavController(),
    onNavigateToParent: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit
) {
    NavHost(navController = navController, startDestination = ChildRoute.Home.route) {
        composable(ChildRoute.Home.route) {
            HomeScreen(
                onNavigateToParent = onNavigateToParent,
                onNavigateToPlayer = { id -> navController.navigate("${ChildRoute.Player.route}/$id") },
                onNavigateToShorts = { navController.navigate(ChildRoute.Shorts.route) },
                onNavigateToPhotos = { navController.navigate(ChildRoute.Photos.route) }
            )
        }
        composable(ChildRoute.Shorts.route) {
            ShortsFeedScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { id -> navController.navigate("${ChildRoute.Player.route}/$id") }
            )
        }
        composable(ChildRoute.Photos.route) {
            PhotoGalleryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { id -> navController.navigate("${ChildRoute.Player.route}/$id") }
            )
        }
        composable("${ChildRoute.Player.route}/{mediaId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("mediaId")?.toLongOrNull() ?: 0L
            PlayerScreen(
                mediaId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
