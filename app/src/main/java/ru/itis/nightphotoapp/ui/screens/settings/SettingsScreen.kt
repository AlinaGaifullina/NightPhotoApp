package ru.itis.nightphotoapp.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
){

    SettingsContent()
}

@Composable
fun SettingsContent(){

}