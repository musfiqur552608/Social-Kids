package com.example.socialbaby.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.socialbaby.ui.parent.addcontent.AddContentScreen
import com.example.socialbaby.ui.parent.auth.ParentUnlockScreen
import com.example.socialbaby.ui.parent.dashboard.ParentDashboardScreen
import com.example.socialbaby.ui.parent.history.WatchHistoryScreen
import com.example.socialbaby.ui.parent.manageshelf.ManageShelfScreen
import com.example.socialbaby.ui.parent.settings.ParentSettingsScreen

sealed class ParentRoute(val route: String) {
    data object Unlock : ParentRoute("parent_unlock")
    data object Dashboard : ParentRoute("parent_dashboard")
    data object AddContent : ParentRoute("parent_add")
    data object ManageShelf : ParentRoute("parent_shelf")
    data object Settings : ParentRoute("parent_settings")
    data object History : ParentRoute("parent_history")
}

@Composable
fun ParentNavGraph(
    navController: NavHostController = rememberNavController(),
    onBackToChild: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit
) {
    NavHost(navController = navController, startDestination = ParentRoute.Unlock.route) {
        composable(ParentRoute.Unlock.route) {
            ParentUnlockScreen(
                onUnlocked = { navController.navigate(ParentRoute.Dashboard.route) { popUpTo(ParentRoute.Unlock.route) { inclusive = true } } },
                onBackToChild = onBackToChild
            )
        }
        composable(ParentRoute.Dashboard.route) {
            ParentDashboardScreen(
                onBackToChild = onBackToChild,
                onNavigateToAdd = { navController.navigate(ParentRoute.AddContent.route) },
                onNavigateToShelf = { navController.navigate(ParentRoute.ManageShelf.route) },
                onNavigateToSettings = { navController.navigate(ParentRoute.Settings.route) },
                onNavigateToHistory = { navController.navigate(ParentRoute.History.route) },
                onPreviewItem = onNavigateToPlayer
            )
        }
        composable(ParentRoute.AddContent.route) {
            AddContentScreen(onBack = { navController.popBackStack() })
        }
        composable(ParentRoute.ManageShelf.route) {
            ManageShelfScreen(onBack = { navController.popBackStack() })
        }
        composable(ParentRoute.Settings.route) {
            ParentSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(ParentRoute.History.route) {
            WatchHistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
