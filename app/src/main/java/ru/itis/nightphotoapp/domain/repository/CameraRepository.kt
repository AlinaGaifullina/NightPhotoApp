package ru.itis.nightphotoapp.domain.repository

import androidx.camera.view.LifecycleCameraController

interface CameraRepository {

    suspend fun takePhoto(
        controller: LifecycleCameraController
    )

}