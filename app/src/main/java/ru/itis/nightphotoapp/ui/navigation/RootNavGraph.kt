package ru.itis.nightphotoapp.ui.navigation

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.itis.nightphotoapp.ui.screens.camera.CameraScreen
import ru.itis.nightphotoapp.ui.screens.settings.SettingsScreen

@Composable
fun RootNavGraph(navController: NavHostController, applicationContext: Context) {
    NavHost(
        navController = navController,
        route = Graph.ROOT,
        startDestination = RootGraph.Camera.route,
    ) {

        composable(route = RootGraph.Camera.route) {
            CameraScreen(navController, applicationContext = applicationContext)
        }
        composable(route = RootGraph.Settings.route) {
            SettingsScreen(navController)
        }
    }
}

sealed class RootGraph(val route: String) {
    data object Camera : RootGraph(route = "camera")
    data object Settings : RootGraph(route = "settings")
}

object Graph {
    const val ROOT = "root_graph"
}
