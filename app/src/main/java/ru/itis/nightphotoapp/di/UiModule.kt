package ru.itis.nightphotoapp.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.itis.nightphotoapp.data.repositoryimpl.CameraRepositoryImpl
import ru.itis.nightphotoapp.domain.repository.CameraRepository
import ru.itis.nightphotoapp.ui.screens.camera.CameraViewModel
import ru.itis.nightphotoapp.ui.screens.settings.SettingsViewModel

val uiModule = module {

    single<Application> { androidContext().applicationContext as Application }

    single<CameraRepository> { CameraRepositoryImpl(get()) }

    viewModel {
        CameraViewModel(get())
    }
    viewModel {
        SettingsViewModel()
    }
}