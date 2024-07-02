package ru.itis.nightphotoapp.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.itis.nightphotoapp.ui.screens.camera.CameraViewModel
import ru.itis.nightphotoapp.ui.screens.settings.SettingsViewModel

val uiModule = module {

    viewModel {
        CameraViewModel()
    }
    viewModel {
        SettingsViewModel()
    }
}