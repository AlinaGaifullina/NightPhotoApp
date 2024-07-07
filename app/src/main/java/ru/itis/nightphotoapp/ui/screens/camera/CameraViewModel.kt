package ru.itis.nightphotoapp.ui.screens.camera

import android.content.ContentValues.TAG
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.StateCallback
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
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

enum class SliderStatus {
    HIDE,
    ISO,
    SHUTTER_SPEED
}

data class CameraState(
    val photosNumber: Int = 1,
    val photoWithFlash: Boolean = false,
    val isoIndex: Int = 0,
    val shutterSpeedIndex: Int = 0,
    val shutterSpeedValue: Long = 100000,
    val isoValue: Int = 100,
    val isAutoMode: Boolean = true,
    val sliderStatus: SliderStatus = SliderStatus.HIDE,
    val blockedSettings: List<Int> = emptyList(),
    val capReq: CaptureRequest.Builder? = null,
    val cameraCaptureSession: CameraCaptureSession? = null,
    val cameraStateCallback: StateCallback? = null,
    val textureView: AutoFitTextureView? = null,
    val surface: Surface? = null,
    val handler: Handler? = null,
    val handlerThread: HandlerThread = HandlerThread("CameraVideoThread" ),
    val cameraDevice: CameraDevice? = null
)

sealed interface CameraSideEffect {
    object NavigateToSettingsScreen : CameraSideEffect
}

sealed interface CameraEvent {
    object OnChangeModeClick : CameraEvent
    object OnIsoButtonClick : CameraEvent
    object OnShutterSpeedButtonClick : CameraEvent
    object OnSettingsClick : CameraEvent
    data class OnIsoIndexChanged(val isoIndex: Int) : CameraEvent
    data class OnShutterSpeedIndexChanged(val shutterSpeedIndex: Int) : CameraEvent
//    data class OnIsoValueChanged(val isoValue: Int) : CameraEvent
//    data class OnShutterSpeedValueChanged(val shutterSpeedValue: String) : CameraEvent
    data class OnTextureViewChanged(val textureView: AutoFitTextureView?) : CameraEvent
    data class OnCaptureRequestChanged(val capReq: CaptureRequest.Builder) : CameraEvent
    data class OnCameraCaptureSessionChanged(val cameraCaptureSession: CameraCaptureSession) : CameraEvent
    data class OnSliderStatusChanged(val sliderStatus: SliderStatus) : CameraEvent
}

class CameraViewModel(
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _state: MutableStateFlow<CameraState> = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state

    private val _action = MutableSharedFlow<CameraSideEffect?>()
    val action: SharedFlow<CameraSideEffect?>
        get() = _action.asSharedFlow()

    init {

        viewModelScope.launch {
            _state.value.handlerThread.start()
            _state.tryEmit(
                _state.value.copy(
                    handler = Handler(_state.value.handlerThread.looper)
                )
            )
        }

        viewModelScope.launch {
            _state.tryEmit(
                _state.value.copy(
                    cameraStateCallback = object : CameraDevice.StateCallback() {
                        @RequiresApi(Build.VERSION_CODES.P)
                        override fun onOpened(camera: CameraDevice) {
                            _state.tryEmit(
                                _state.value.copy(
                                    cameraDevice = camera
                                )
                            )
                            _state.tryEmit(
                                _state.value.copy(
                                    capReq = _state.value.cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW),
                                    surface = Surface(_state.value.textureView?.surfaceTexture)
                                )
                            )

                            _state.value.surface?.let { _state.value.capReq?.addTarget(it) }

                            _state.value.cameraDevice?.createCaptureSession(listOf(_state.value.surface), object:
                                CameraCaptureSession.StateCallback() {
                                override fun onConfigured(p0: CameraCaptureSession) {
                                    _state.tryEmit(
                                        _state.value.copy(
                                            cameraCaptureSession = p0
                                        )
                                    )
                                    _state.value.cameraCaptureSession?.setRepeatingRequest(_state.value.capReq!!.build(), null, null)
                                }

                                override fun onConfigureFailed(p0: CameraCaptureSession) {
                                    TODO("Not yet implemented")
                                }

                            }, _state.value.handler)

                        }
                        override fun onDisconnected(cameraDevice: CameraDevice) {
                            _state.value.cameraDevice?.close()
                        }

                        override fun onError(cameraDevice: CameraDevice, error: Int) {
                            _state.value.cameraDevice?.close()
                            _state.tryEmit(
                                _state.value.copy(
                                    cameraDevice = null
                                )
                            )
                            val errorMsg = when(error) {
                                ERROR_CAMERA_DEVICE -> "Fatal (device)"
                                ERROR_CAMERA_DISABLED -> "Device policy"
                                ERROR_CAMERA_IN_USE -> "Camera in use"
                                ERROR_CAMERA_SERVICE -> "Fatal (service)"
                                ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                                else -> "Unknown"
                            }
                            Log.e(TAG, "Error when trying to connect camera $errorMsg")
                        }
                    }
                )
            )
        }
    }


    fun event(cameraEvent: CameraEvent) {
        when (cameraEvent) {
            CameraEvent.OnChangeModeClick -> onChangeModeClick()
            CameraEvent.OnIsoButtonClick -> onIsoButtonClick()
            CameraEvent.OnShutterSpeedButtonClick -> onShutterSpeedButtonClick()
            CameraEvent.OnSettingsClick -> onSettingsClick()
            is CameraEvent.OnIsoIndexChanged -> onIsoIndexChanged(cameraEvent.isoIndex)
            is CameraEvent.OnShutterSpeedIndexChanged -> onShutterSpeedIndexChanged(cameraEvent.shutterSpeedIndex)
//            is CameraEvent.OnIsoValueChanged -> onIsoValueChanged(cameraEvent.isoValue)
//            is CameraEvent.OnShutterSpeedValueChanged -> onShutterSpeedValueChanged(cameraEvent.shutterSpeedValue)
            is CameraEvent.OnTextureViewChanged -> onTextureViewChanged(cameraEvent.textureView)
            is CameraEvent.OnCaptureRequestChanged -> onCaptureRequestChanged(cameraEvent.capReq)
            is CameraEvent.OnCameraCaptureSessionChanged -> onCameraCaptureSessionChanged(cameraEvent.cameraCaptureSession)
            is CameraEvent.OnSliderStatusChanged -> onSliderStatusChanged(cameraEvent.sliderStatus)

        }
    }


    private fun onSettingsClick() {
        viewModelScope.launch {
            _action.emit(
                CameraSideEffect.NavigateToSettingsScreen
            )
        }
    }

    private fun onCaptureRequestChanged(capReq: CaptureRequest.Builder) {
        _state.tryEmit(
            _state.value.copy(
                capReq = capReq
            )
        )
    }

    private fun onTextureViewChanged(textureView: AutoFitTextureView?) {
        _state.tryEmit(
            _state.value.copy(
                textureView = textureView
            )
        )
    }

    private fun onHandlerThreadChanged(handlerThread: HandlerThread) {
        _state.tryEmit(
            _state.value.copy(
                handlerThread = handlerThread
            )
        )
    }

    private fun onHandlerChanged(handler: Handler) {
        _state.tryEmit(
            _state.value.copy(
                handler = handler
            )
        )
    }

    private fun onCameraCaptureSessionChanged(cameraCaptureSession: CameraCaptureSession) {
        _state.tryEmit(
            _state.value.copy(
                cameraCaptureSession = cameraCaptureSession
            )
        )
    }

    private fun onSliderStatusChanged(sliderStatus: SliderStatus) {
        _state.tryEmit(
            _state.value.copy(
                sliderStatus = sliderStatus
            )
        )
    }

    private fun onChangeModeClick() {
        val isAutoMode = _state.value.isAutoMode
        _state.tryEmit(
            _state.value.copy(
                isAutoMode = !isAutoMode,
                sliderStatus = SliderStatus.HIDE
            )
        )
        if(_state.value.isAutoMode){
            _state.value.capReq?.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_ON)
            _state.value.capReq?.set(CaptureRequest.SENSOR_SENSITIVITY, 800)
            _state.value.cameraCaptureSession?.setRepeatingRequest(_state.value.capReq!!.build(), null, _state.value.handler)
        } else {
            _state.value.capReq?.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF)
        }
    }

    private fun onShutterSpeedButtonClick() {
        _state.tryEmit(
            _state.value.copy(
                sliderStatus = SliderStatus.SHUTTER_SPEED
            )
        )
    }

    private fun onIsoButtonClick() {
        _state.tryEmit(
            _state.value.copy(
                sliderStatus = SliderStatus.ISO
            )
        )
    }

    private fun onShutterSpeedIndexChanged(shutterSpeedIndex: Int) {
        _state.tryEmit(
            _state.value.copy(
                shutterSpeedIndex = shutterSpeedIndex,
                shutterSpeedValue = shutterSpeedValues[shutterSpeedIndex]
            )
        )
    }

    private fun onIsoIndexChanged(isoIndex: Int) {
        _state.tryEmit(
            _state.value.copy(
                isoIndex = isoIndex,
                isoValue = isoValues[isoIndex]
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