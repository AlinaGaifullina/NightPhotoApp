package ru.itis.nightphotoapp.ui.screens.camera

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import ru.itis.nightphotoapp.R
import ru.itis.nightphotoapp.ui.navigation.RootGraph
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = koinViewModel(),
    applicationContext: Context,
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val eventHandler = viewModel::event
    val action by viewModel.action.collectAsStateWithLifecycle(null)

    var isoValue by remember { mutableStateOf(100f) }

    LaunchedEffect(action) {
        when (action) {
            null -> Unit
            is CameraSideEffect.NavigateToSettingsScreen -> {
                navController.navigate(RootGraph.Settings.route)
            }
        }
    }


    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    //lateinit var captureRequest: CaptureRequest
    lateinit var handlerThread: HandlerThread
    lateinit var handler: Handler
    lateinit var capReq: CaptureRequest.Builder

//    DisposableEffect(Unit) {
//        onDispose {
//            handler.removeCallbacksAndMessages(null)
//            handlerThread.quitSafely()
//            try {
//                handlerThread.join()
//            } catch (e: InterruptedException) {
//                Log.e("CameraApp", "Ошибка остановки фонового потока", e)
//            }
//        }
//    }

    cameraManager = applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    handlerThread = HandlerThread("CameraVideoThread")
    handlerThread.start()
    handler = Handler(handlerThread.looper)

    textureView = TextureView(applicationContext)

    val cameraStateCallback = object : CameraDevice.StateCallback() {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            val surface = Surface(textureView.surfaceTexture)
            capReq.addTarget(surface)
            capReq.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF)
            capReq.set(CaptureRequest.SENSOR_SENSITIVITY, 3200)

            cameraDevice.createCaptureSession(listOf(surface), object:
                CameraCaptureSession.StateCallback() {
                override fun onConfigured(p0: CameraCaptureSession) {
                    cameraCaptureSession = p0
                    cameraCaptureSession.setRepeatingRequest(capReq.build(), null, null)
                    eventHandler.invoke(CameraEvent.OnCaptureCreate(capReq))
                    eventHandler.invoke(CameraEvent.OnCameraCaptureSession(cameraCaptureSession))
                }

                override fun onConfigureFailed(p0: CameraCaptureSession) {
                    TODO("Not yet implemented")
                }

            }, handler)

        }
        override fun onDisconnected(cameraDevice: CameraDevice) {

        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
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
    val cameraId = cameraManager.cameraIdList[0]

    fun openCamera(){
        cameraManager.openCamera(cameraId, cameraStateCallback, handler)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                textureView.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            openCamera()
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                            // Реагируйте на изменение размеров здесь
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            // Освободите ресурсы камеры здесь
                            return false
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                            // Обновите ваше изображение здесь
                        }
                    }
                }

            }
        )
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        IconButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = {
                val newIsoValue = 400
                state.capReq?.set(CaptureRequest.SENSOR_SENSITIVITY, newIsoValue)
                try {
                    state.cameraCaptureSession?.setRepeatingRequest(state.capReq!!.build(), null, handler)
                    cameraCaptureSession = state.cameraCaptureSession!!
                    capReq = state.capReq!!
                } catch (e: CameraAccessException) {
                    Log.e("CameraApp", "Не удалось обновить ISO на лету", e)
                }
            }
        ) {
            Icon(
                painterResource(
                    R.drawable.ic_auto_exp
                ),
                modifier = Modifier.size(50.dp),
                contentDescription = "icon",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        IconButton(
            modifier = Modifier.align(Alignment.TopCenter),
            onClick = {
                val newIsoValue = 3200
                state.capReq?.set(CaptureRequest.SENSOR_SENSITIVITY, newIsoValue)
                try {
                    state.cameraCaptureSession?.setRepeatingRequest(state.capReq!!.build(), null, handler)
                    cameraCaptureSession = state.cameraCaptureSession!!
                    capReq = state.capReq!!
                } catch (e: CameraAccessException) {
                    Log.e("CameraApp", "Не удалось обновить ISO на лету", e)
                }
            }
        ) {
            Icon(
                painterResource(
                    R.drawable.ic_auto_exp
                ),
                modifier = Modifier.size(50.dp),
                contentDescription = "icon",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        val isoValues: List<Int> = listOf(100, 200, 400, 800, 1600, 3200)
        var sliderPosition by remember { mutableStateOf(0f) }
        val maxIndex = isoValues.size - 1
        var index by remember { mutableStateOf(0) }

        Slider(
            modifier = Modifier.align(Alignment.BottomCenter),
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
                index = newValue.roundToInt()
                state.capReq?.set(CaptureRequest.SENSOR_SENSITIVITY, isoValues[index])
                try {
                    state.cameraCaptureSession?.setRepeatingRequest(state.capReq!!.build(), null, handler)
                    cameraCaptureSession = state.cameraCaptureSession!!
                    capReq = state.capReq!!
                } catch (e: CameraAccessException) {
                    Log.e("CameraApp", "Не удалось обновить ISO на лету", e)
                }
            },
            valueRange = 0f..maxIndex.toFloat(),
            steps = maxIndex - 1
        )

//        Slider(
//            value = isoValue,
//            onValueChange = {
//                isoValue = it
//                val newIsoValue = it
//                capReq.set(CaptureRequest.SENSOR_SENSITIVITY, newIsoValue.toInt())
//                try {
//                    cameraCaptureSession?.setRepeatingRequest(capReq.build(), null, handler)
//                } catch (e: CameraAccessException) {
//                    Log.e("CameraApp", "Не удалось обновить ISO на лету", e)
//                }
//                            },
//            valueRange = 100f..3200f,
//            onValueChangeFinished = {
////                val newIsoValue = 400
////                capReq.set(CaptureRequest.SENSOR_SENSITIVITY, newIsoValue.toInt())
////                try {
////                    cameraCaptureSession?.setRepeatingRequest(capReq.build(), null, handler)
////                } catch (e: CameraAccessException) {
////                    Log.e("CameraApp", "Не удалось обновить ISO на лету", e)
////                }
//            }
//        )


    }


    //Text(text = cameraInfo.exposureState.exposureCompensationRange.toString())
//                        var isoValue by remember { mutableStateOf(100f) }
//                        Slider(
//                            modifier = Modifier.align(Alignment.TopCenter),
//                            value = isoValue,
//                            onValueChange = { newValue ->
//                                isoValue = newValue
//                                            },
//                            valueRange = -24f..12f,
//                            onValueChangeFinished = {
//                                val isoRange = cameraInfo.exposureState.exposureCompensationRange
//                                val scaledISO = (isoRange.lower ?: 0) + (isoValue * ((isoRange.upper ?: 0) - (isoRange.lower ?: 0))).toInt()
//                                cameraControl.setExposureCompensationIndex(scaledISO)
//                                println("Выбранное значение ISO: ${isoValue.toInt()}")
//
//                            }
//                        )

//                        AndroidView(
//                            modifier = Modifier.fillMaxSize(),
//                            factory = {
//                                PreviewView(it).apply {
//                                    this.controller = controller
//                                    controller.bindToLifecycle(lifecycleOwner)
//                                }
//                            }
//                        )
    //getCameraCharacteristics(context = applicationContext)

//                        Text(text = getCameraCharacteristics(context = applicationContext),
//                            modifier = Modifier.align(Alignment.Center),
//                            color = MaterialTheme.colorScheme.primary)

//
//                        Slider(
//                            value = isoValue,
//                            onValueChange = { isoValue = it },
//                            valueRange = 100f..3200f,
//                            onValueChangeFinished = {
//                                // Здесь можно вызвать функцию, которая применит значение ISO к камере
//                                println("Выбранное значение ISO: ${isoValue.toInt()}")
//                            }
//                        )
//                    }




//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black)
//    ) {
//
//        val lifecycleOwner = LocalLifecycleOwner.current
//        AndroidView(
//            modifier = Modifier.fillMaxSize(),
//            factory = {
//                PreviewView(it).apply {
//                    this.controller = controller
//                    controller.bindToLifecycle(lifecycleOwner)
//                }
//            }
//        )
//
//        SettingsBar(
//            modifier = Modifier
//                .align(Alignment.TopCenter),
//            state = state,
//            onSettingsClick = {
//                eventHandler.invoke(
//                    CameraEvent.OnSettingsClick
//                )
//            }
//        )
//
//
////        Slider(
////            value = isoValue,
////            onValueChange = { newValue ->
////                isoValue = newValue
////                val iso = (((newValue * controller.cameraInfo?.exposureState?.exposureCompensationRange)
////                    ?: 100f)).toInt() // Преобразование значения ползунка в значение ISO
////                // Здесь вы можете вызвать метод для изменения ISO камеры
////                controller.cameraControl?.setExposureCompensationIndex(iso)
////            },
////            valueRange = 100f..3200f, // Диапазон значений ISO
////            onValueChangeFinished = {
////                eventHandler.invoke(
////                    CameraEvent.OnIsoChanged(isoValue.toInt())
////                )
////                // Вызывается, когда пользователь отпускает ползунок
////            }
////        )
//        Text(text = controller.cameraInfo?.exposureState?.isExposureCompensationSupported.toString(),
//            modifier = Modifier.align(Alignment.Center),
//            color = MaterialTheme.colorScheme.primary)
//
//        BottomBar(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//        )
//    }
}

@Composable
fun SettingsBar(
    modifier: Modifier,
    state: CameraState,
    onSettingsClick: () -> Unit
){
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .alpha(0.85f)
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.primary),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for(i in 1..3){
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.onPrimary)
            )
        }
    }

    Row(
        modifier = modifier
            //.padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(70.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {

        // режим
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(70.dp)
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.Center),
                onClick = { /*TODO*/ }
            ) {
                Icon(
                    painterResource(
                        if(state.isAutoMode) R.drawable.ic_auto_exp else R.drawable.ic_not_auto_exp
                    ),
                    modifier = Modifier.size(50.dp),
                    contentDescription = "icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // исо
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(70.dp)
        ) {
            Box(
                modifier = Modifier
                    .clickable { }
                    .align(Alignment.Center),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ISO",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = state.iso.toString(),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // выдержка
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(70.dp)
        ) {
            Box(
                modifier = Modifier
                    .clickable { }
                    .align(Alignment.Center),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Выдержка",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = state.shutterSpeed,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 7.sp
                    )
                }
            }
        }

        // настройки
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(70.dp)
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.Center),
                onClick = {
                    onSettingsClick()
                }
            ) {
                Icon(
                    painterResource(R.drawable.ic_settings),
                    modifier = Modifier.size(35.dp),
                    contentDescription = "icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun BottomBar(
    modifier: Modifier
){

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .alpha(0.85f)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 26.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {


            }

        }
    }
}

