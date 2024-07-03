package ru.itis.nightphotoapp.ui.screens.camera

import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.itis.nightphotoapp.domain.repository.CameraRepository

data class CameraState(
    val photosNumber: Int = 1,
    val photoWithFlash: Boolean = true,
    val iso: Int = 100,
    val shutterSpeed: String = "",
    val isAutoMode: Boolean = true,
    val blockedSettings: List<Int> = emptyList(),
)

sealed interface CameraSideEffect {
    object NavigateToSettingsScreen : CameraSideEffect
}

sealed interface CameraEvent {
    object OnChangeModeClick : CameraEvent
    object OnIsoButtonClick : CameraEvent
    object OnShutterSpeedButtonClick : CameraEvent
    object OnSettingsClick : CameraEvent
    data class OnIsoChanged(val iso: Int) : CameraEvent
    data class OnShutterSpeedChanged(val shutterSpeed: String) : CameraEvent
}


class CameraViewModel(
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _state: MutableStateFlow<CameraState> = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state

    private val _action = MutableSharedFlow<CameraSideEffect?>()
    val action: SharedFlow<CameraSideEffect?>
        get() = _action.asSharedFlow()

    fun event(cameraEvent: CameraEvent) {
        when (cameraEvent) {
            CameraEvent.OnChangeModeClick -> onChangeModeClick()
            CameraEvent.OnIsoButtonClick -> onIsoButtonClick()
            CameraEvent.OnShutterSpeedButtonClick -> onShutterSpeedButtonClick()
            CameraEvent.OnSettingsClick -> onSettingsClick()
            is CameraEvent.OnIsoChanged -> onIsoChanged(cameraEvent.iso)
            is CameraEvent.OnShutterSpeedChanged -> onShutterSpeedChanged(cameraEvent.shutterSpeed)

        }
    }


    private fun onSettingsClick() {
        viewModelScope.launch {
            _action.emit(
                CameraSideEffect.NavigateToSettingsScreen
            )
        }
    }

    private fun onChangeModeClick() {
        val isAutoMode = _state.value.isAutoMode
        _state.tryEmit(
            _state.value.copy(
                isAutoMode = isAutoMode
            )
        )
    }

    private fun onShutterSpeedButtonClick() {
        _state.tryEmit(
            _state.value.copy(

            )
        )
    }

    private fun onIsoButtonClick() {
        _state.tryEmit(
            _state.value.copy(

            )
        )
    }

    private fun onIsoChanged(iso: Int) {
        _state.tryEmit(
            _state.value.copy(
                iso = iso
            )
        )
    }

    private fun onShutterSpeedChanged(shutterSpeed: String) {
        _state.tryEmit(
            _state.value.copy(
                shutterSpeed = shutterSpeed
            )
        )
    }

    fun onTakePhoto(
        controller: LifecycleCameraController
    ) {
        viewModelScope.launch {
            cameraRepository.takePhoto(controller)
        }
    }

}