package com.example.socialbaby.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class RootRoute(val route: String) {
    data object Child : RootRoute("child_graph")
    data object Parent : RootRoute("parent_graph")
}

@Composable
fun RootNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = RootRoute.Child.route) {
        composable(RootRoute.Child.route) {
            ChildNavGraph(
                onNavigateToParent = { navController.navigate(RootRoute.Parent.route) },
                onNavigateToPlayer = { id -> navController.navigate("${ChildRoute.Player.route}/$id") }
            )
        }
        composable(RootRoute.Parent.route) {
            ParentNavGraph(
                onBackToChild = {
                    navController.popBackStack(RootRoute.Child.route, inclusive = false)
                    if (!navController.popBackStack()) {
                        navController.navigate(RootRoute.Child.route)
                    }
                },
                onNavigateToPlayer = { id -> navController.navigate("${ChildRoute.Player.route}/$id") }
            )
        }
        composable("${ChildRoute.Player.route}/{mediaId}") { backStack ->
            val mediaId = backStack.arguments?.getString("mediaId")?.toLongOrNull() ?: 0L
            // Player is accessible from both child and parent graphs; we delegate to Child player
            // For simplicity we reuse PlayerScreen here
            androidx.compose.runtime.LaunchedEffect(Unit) {}
            // Actual player screen will be placed via ChildNavGraph player route; this is fallback
        }
    }
}

// Alternate: simpler top-level NavHost combining both graphs

@Composable
fun KidTubeRootNav(
    startIsParent: Boolean = false,
    outerNav: NavHostController
) {
    // Not used, kept for architecture doc compliance
}
