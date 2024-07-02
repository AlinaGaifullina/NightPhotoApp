package ru.itis.nightphotoapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.itis.nightphotoapp.ui.screens.camera.CameraScreen
import ru.itis.nightphotoapp.ui.screens.settings.SettingsScreen

@Composable
fun RootNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        route = Graph.ROOT,
        startDestination = RootGraph.camera.route,
    ) {

        composable(route = RootGraph.camera.route) {
            CameraScreen(navController)
        }
        composable(route = RootGraph.settings.route) {
            SettingsScreen(navController)
        }
    }
}

sealed class RootGraph(val route: String) {
    object camera : RootGraph(route = "camera")
    object settings : RootGraph(route = "settings")
}

object Graph {
    const val ROOT = "root_graph"
}
