package ru.itis.nightphotoapp.ui.screens.camera

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel

@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = koinViewModel()
) {
    CameraContent()
}

@Composable
fun CameraContent(){
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Camera")
    }
}