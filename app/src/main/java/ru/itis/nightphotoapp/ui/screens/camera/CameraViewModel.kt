package ru.itis.nightphotoapp.ui.screens.camera

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.StateCallback
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.itis.nightphotoapp.domain.repository.CameraRepository
import ru.itis.nightphotoapp.utils.CameraParameters
import ru.itis.nightphotoapp.utils.SliderStatus


data class CameraState(
    val seriesSize: Int = 3,
    val isCapturing: Boolean = false,
    val isGenerating: Boolean = false,
    val photoWithFlash: Boolean = false,
    val isShowSeriesSizes: Boolean = false,
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
    val cameraDevice: CameraDevice? = null,
    val imageReader: ImageReader? = null,
    val bitmapList: List<Bitmap> = emptyList(),
    val generatedImage: Bitmap? = null,
    val generatedImageIcon: Bitmap? = null
)

sealed interface CameraSideEffect {
    object NavigateToSettingsScreen : CameraSideEffect
}

sealed interface CameraEvent {
    data class OnTakePhoto(val evRange: Range<Int>?) : CameraEvent
    object OnClearGeneratedImage : CameraEvent
    object OnChangeModeClick : CameraEvent
    object OnIsoButtonClick : CameraEvent
    object OnShutterSpeedButtonClick : CameraEvent
    object OnSettingsClick : CameraEvent
    object OnSeriesButtonClick : CameraEvent
    data class OnSeriesSizeChanged(val seriesSize: Int) : CameraEvent
    data class OnCheckboxClick(val isChecked: Boolean) : CameraEvent
    data class OnSetFocus(val x: Float, val y: Float, val previewSize: Size, val sensorArraySize: Rect) : CameraEvent
    data class OnIsoIndexChanged(val isoIndex: Int) : CameraEvent
    data class OnShutterSpeedIndexChanged(val shutterSpeedIndex: Int) : CameraEvent
//    data class OnIsoValueChanged(val isoValue: Int) : CameraEvent
//    data class OnShutterSpeedValueChanged(val shutterSpeedValue: String) : CameraEvent
    data class OnTextureViewChanged(val textureView: AutoFitTextureView?) : CameraEvent
    data class OnCaptureRequestChanged(val capReq: CaptureRequest.Builder) : CameraEvent
    data class OnCameraCaptureSessionChanged(val cameraCaptureSession: CameraCaptureSession) : CameraEvent
    data class OnSliderStatusChanged(val sliderStatus: SliderStatus) : CameraEvent
    data class OnBitmapListChanged(val newBitmap: Bitmap) : CameraEvent
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
                    handler = Handler(_state.value.handlerThread.looper),
                    imageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG,  1)
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

                            _state.value.cameraDevice?.createCaptureSession(listOf(_state.value.surface, _state.value.imageReader?.surface), object:
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
            is CameraEvent.OnTakePhoto -> onTakePhoto(cameraEvent.evRange)
            CameraEvent.OnClearGeneratedImage -> onClearGeneratedImage()
            CameraEvent.OnChangeModeClick -> onChangeModeClick()
            CameraEvent.OnIsoButtonClick -> onIsoButtonClick()
            CameraEvent.OnShutterSpeedButtonClick -> onShutterSpeedButtonClick()
            CameraEvent.OnSettingsClick -> onSettingsClick()
            CameraEvent.OnSeriesButtonClick -> onSeriesButtonClick()
            is CameraEvent.OnIsoIndexChanged -> onIsoIndexChanged(cameraEvent.isoIndex)
            is CameraEvent.OnCheckboxClick -> onCheckboxClick(cameraEvent.isChecked)
            is CameraEvent.OnSetFocus -> onSetFocus(cameraEvent.x, cameraEvent.y, cameraEvent.previewSize, cameraEvent.sensorArraySize)
            is CameraEvent.OnSeriesSizeChanged -> onSeriesSizeChanged(cameraEvent.seriesSize)
            is CameraEvent.OnShutterSpeedIndexChanged -> onShutterSpeedIndexChanged(cameraEvent.shutterSpeedIndex)
//            is CameraEvent.OnIsoValueChanged -> onIsoValueChanged(cameraEvent.isoValue)
//            is CameraEvent.OnShutterSpeedValueChanged -> onShutterSpeedValueChanged(cameraEvent.shutterSpeedValue)
            is CameraEvent.OnTextureViewChanged -> onTextureViewChanged(cameraEvent.textureView)
            is CameraEvent.OnCaptureRequestChanged -> onCaptureRequestChanged(cameraEvent.capReq)
            is CameraEvent.OnCameraCaptureSessionChanged -> onCameraCaptureSessionChanged(cameraEvent.cameraCaptureSession)
            is CameraEvent.OnSliderStatusChanged -> onSliderStatusChanged(cameraEvent.sliderStatus)
            is CameraEvent.OnBitmapListChanged -> onBitmapListChanged(cameraEvent.newBitmap)

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

    private fun onClearGeneratedImage() {
        val generatedImage = _state.value.generatedImage
        _state.tryEmit(
            _state.value.copy(
                generatedImage = null,
                generatedImageIcon = generatedImage
            )
        )
    }

    private fun onSeriesButtonClick() {
        val isShowSeriesSizes = _state.value.isShowSeriesSizes
        _state.tryEmit(
            _state.value.copy(
                isShowSeriesSizes = !isShowSeriesSizes
            )
        )
    }

    private fun onCheckboxClick(isChecked: Boolean) {
        val i = state.value.photoWithFlash
        _state.tryEmit(
            _state.value.copy(
                photoWithFlash = !isChecked
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

    private fun onBitmapListChanged(newBitmap: Bitmap) {
        val bitmapList = _state.value.bitmapList
        val newList = bitmapList.toMutableList()
        newList.add(newBitmap)
        _state.tryEmit(
            _state.value.copy(
                bitmapList = newList
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

    private fun onSeriesSizeChanged(seriesSize: Int) {
        _state.tryEmit(
            _state.value.copy(
                seriesSize = seriesSize
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
        with(_state.value){
            if(_state.value.isAutoMode){
                capReq?.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_ON)
                capReq?.set(CaptureRequest.SENSOR_SENSITIVITY, 800)

                cameraCaptureSession?.setRepeatingRequest(capReq!!.build(), null, handler)
            } else {
                _state.tryEmit(
                    _state.value.copy(
                        isoValue = 400,
                        shutterSpeedValue = 30000000,
                        isoIndex = CameraParameters.isoValues.indexOf(400),
                        shutterSpeedIndex = CameraParameters.shutterSpeedValues.indexOf(30000000)
                    )
                )
                capReq?.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF)
                capReq?.set(CaptureRequest.SENSOR_SENSITIVITY, 400)
                capReq?.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 30000000)
                cameraCaptureSession?.setRepeatingRequest(capReq!!.build(), null, handler)
            }
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
                shutterSpeedValue = CameraParameters.shutterSpeedValues[shutterSpeedIndex]
            )
        )
    }

    private fun onIsoIndexChanged(isoIndex: Int) {
        _state.tryEmit(
            _state.value.copy(
                isoIndex = isoIndex,
                isoValue = CameraParameters.isoValues[isoIndex]
            )
        )
    }

    // не работает, надо чинить
    private fun onSetFocus(x: Float, y: Float, previewSize: Size, sensorArraySize: Rect) {
        val focusPoint = CameraParameters.convertToFocusPoint(x, y, previewSize, sensorArraySize)

        val singleFocusReq = _state.value.cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        with(_state.value){
            singleFocusReq?.addTarget(surface!!)

            val meteringAreaSize = 200
            val meteringRectangle = MeteringRectangle(focusPoint.x.toInt(),
                focusPoint.y.toInt(), meteringAreaSize, meteringAreaSize, MeteringRectangle.METERING_WEIGHT_MAX)
            singleFocusReq?.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(meteringRectangle))

            singleFocusReq?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            singleFocusReq?.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)

            // Отправка запроса на камеру
            //cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, null)

            cameraCaptureSession?.capture(singleFocusReq!!.build(), null, null)
        }
    }

    // посмотреть внимательно еще раз
    private fun onTakePhoto(evRange: Range<Int>?) {
        viewModelScope.launch {
            if(_state.value.seriesSize == 1){
                _state.tryEmit(
                    _state.value.copy(
                        isCapturing = true
                    )
                )
                val singlePhotoReq = _state.value.cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                with(_state.value) {
                    singlePhotoReq?.addTarget(imageReader!!.surface)
                    singlePhotoReq?.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        if (isAutoMode) CONTROL_AE_MODE_ON else CONTROL_AE_MODE_OFF
                    )
                    singlePhotoReq?.set(CaptureRequest.SENSOR_SENSITIVITY, isoValue)
                    singlePhotoReq?.set(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterSpeedValue)

                    cameraCaptureSession?.capture(singlePhotoReq!!.build(), null, null)
                }

                _state.tryEmit(
                    _state.value.copy(
                        isCapturing = false
                    )
                )
            } else {
                _state.tryEmit(
                    _state.value.copy(
                        isCapturing = true
                    )
                )
                with(_state.value){
                    val listOfEv = generateEvSeries(seriesSize, evRange!!.lower.toDouble(), evRange.upper.toDouble())
                    for(ev in listOfEv){

                        capReq?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                        capReq?.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ev.toInt())
                        cameraCaptureSession?.setRepeatingRequest(capReq!!.build(), null, handler)

                        delay(1000) // костыль или не костыль???
                        val singlePhotoReq = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        singlePhotoReq?.addTarget(imageReader!!.surface)
                        singlePhotoReq?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                        singlePhotoReq?.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ev.toInt())

                        cameraCaptureSession?.capture(singlePhotoReq!!.build(), null, null)
                    }


                    if(photoWithFlash){
                        capReq?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                        capReq?.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
                        //capReq?.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)

                        cameraCaptureSession?.setRepeatingRequest(capReq!!.build(), null, handler)

                        delay(2000) // костыль или не костыль???
                        val singlePhotoReq = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)?.apply {
                            addTarget(imageReader!!.surface)
                            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                            set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
                            set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)

                            set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
                        }
                        cameraCaptureSession?.capture(singlePhotoReq!!.build(), null, handler)
                    }
                    capReq?.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_ON)
                    capReq?.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
                    cameraCaptureSession?.setRepeatingRequest(capReq!!.build(), null, handler)
                }

                val bitmapList = state.value.bitmapList
                val newBitmapList = CameraParameters.stabilizeImages(bitmapList)

                _state.tryEmit(
                    _state.value.copy(
                        bitmapList = newBitmapList,
                        isGenerating = true
                    )
                )

                val generatedImage = CameraParameters.fuseImages(state.value.bitmapList)
                delay(4000)

                _state.tryEmit(
                    _state.value.copy(
                        generatedImage = generatedImage,
                        isGenerating = false,
                        bitmapList = emptyList()
                    )
                )

                _state.tryEmit(
                    _state.value.copy(
                        isCapturing = false,
                    )
                )
            }
        }
    }

// подумать как сделать:
//    fun releaseCameraResources() {
//        with(_state.value){
//            cameraCaptureSession?.close()
//            cameraDevice?.close()
//            imageReader?.close()
//            handlerThread.quitSafely()
//            try {
//                handlerThread.join()
//            } catch (e: InterruptedException) {
//                Log.e("CameraRelease", "Error on closing handlerThread", e)
//            }
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        releaseCameraResources()
//    }
}