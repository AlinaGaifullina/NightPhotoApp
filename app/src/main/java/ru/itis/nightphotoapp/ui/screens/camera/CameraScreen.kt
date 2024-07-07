package ru.itis.nightphotoapp.ui.screens.camera

import android.Manifest
import android.app.Activity
import android.app.PendingIntent.getActivity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
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
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.impl.utils.CompareSizesByArea
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableIntStateOf
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
import java.util.Collections
import kotlin.math.roundToInt

class CompareSizesByArea : Comparator<Size> {
    override fun compare(size1: Size, size2: Size): Int {
        // Вычисляем площадь размера
        val area1 = size1.width.toLong() * size1.height
        val area2 = size2.width.toLong() * size2.height
        // Используем Long.compare для сравнения значений площади
        return java.lang.Long.compare(area1, area2)
    }
}

class AutoFitTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    private var ratioWidth = 0
    private var ratioHeight = 0

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        ratioWidth = width
        ratioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth)
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height)
            }
        }
    }

}

val isoValues: List<Int> = listOf(100, 200, 400, 800, 1600, 3200)
val shutterSpeedValues: List<Long> = listOf(100000, 200000, 500000, 1000000, 2000000, 4000000, 8000000, 15000000, 30000000, 60000000, 120000000, 250000000, 500000000, 1000000000, 2000000000, 4000000000, 8000000000, 16000000000, 32000000000)
val isoMaxIndex = isoValues.size - 1
val shutterSpeedMaxIndex = shutterSpeedValues.size - 1

fun formatShutterSpeed(shutterSpeedInNano: Long): String {
    val denominator = 1_000_000_000.0 / shutterSpeedInNano
    return if (denominator < 1) {
        "${shutterSpeedInNano / 1_000_000_000.0} сек"
    } else {
        "1/${denominator.toInt()}"
    }
}

@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = koinViewModel(),
    applicationContext: Context,
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val eventHandler = viewModel::event
    val action by viewModel.action.collectAsStateWithLifecycle(null)

    fun getScreenSize(windowManager: WindowManager): Point {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size
    }

    val cameraManager: CameraManager = applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    var previewSize by remember {
        mutableStateOf(Size(1080, 1920))
    }
    var height by remember {
        mutableIntStateOf(99)
    }

    var matrix by remember {
        mutableStateOf(Matrix())
    }
    var aspectRatio by remember {
        mutableStateOf(Size(9, 16))
    }

    val displaySize = getScreenSize(windowManager) //1080 2220

//    DisposableEffect(Unit) {
//        //        fun stopBackgroundThread() {
////            backgroundHandlerThread.quitSafely()
////            backgroundHandlerThread.join()
////        }
//    }


    LaunchedEffect(action) {
        when (action) {
            null -> Unit
            is CameraSideEffect.NavigateToSettingsScreen -> {
                navController.navigate(RootGraph.Settings.route)
            }
        }
    }


    val cameraId = cameraManager.cameraIdList[0]

    fun openCamera(){
        cameraManager.openCamera(cameraId, state.cameraStateCallback!!, state.handler)
    }

    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    val streamConfigurationMap = characteristics.get(
        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
    val previewSizes = streamConfigurationMap?.getOutputSizes(SurfaceTexture::class.java)

    println("aaaaaaaaaaaaaaaaa " + characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL))


    fun chooseOptimalSize(choices: Array<Size>, textureViewWidth: Int, textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size): Size {
        // Допуск в соотношении сторон
        val aspectTolerance = 700
        // Предпочтительные размеры
        val bigEnough = ArrayList<Size>()
        val notBigEnough = ArrayList<Size>()
        val w = aspectRatio.width.toFloat()
        val h = aspectRatio.height.toFloat()

        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight) {
                val ratio = option.width.toFloat() / option.height.toFloat()
                if (Math.abs(ratio - w / h) < aspectTolerance) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }
        }

        // Возвращаем подходящий размер
        return when {
            bigEnough.isNotEmpty() -> Collections.min(bigEnough, ru.itis.nightphotoapp.ui.screens.camera.CompareSizesByArea())
            notBigEnough.isNotEmpty() -> Collections.max(notBigEnough, ru.itis.nightphotoapp.ui.screens.camera.CompareSizesByArea())
            else -> {
                Log.e(TAG, "Couldn't find any suitable preview size")
                choices[13] // Возвращаем первый доступный размер как запасной вариант
            }
        }
    }


    fun configureTransform(viewWidth: Int, viewHeight: Int, windowManager: WindowManager, previewSize: Size?, textureView: TextureView): Matrix {
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = previewSize?.height?.let {
            RectF(
                0f,
                0f,
                it.toFloat(),
                previewSize.width.toFloat()
            )
        }
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            if (bufferRect != null) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            }
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / (previewSize?.height ?: 1080),
                viewWidth.toFloat() / (previewSize?.width ?: 1080)
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }

//        val aspectRatio = (previewSize?.width?.toFloat() ?: 9f) / (previewSize?.height?.toFloat()
//            ?: 16f)
//        val newWidth: Int
//        val newHeight: Int
//        if (viewHeight > viewWidth * aspectRatio) {
//            newWidth = viewWidth
//            newHeight = (viewWidth / aspectRatio).toInt()
//        } else {
//            newWidth = (viewHeight * aspectRatio).toInt()
//            newHeight = viewHeight
//        }
//
//        val dx = (viewWidth - newWidth) / 2
//        val dy = (viewHeight - newHeight) / 2
//
//        matrix.postScale(newWidth.toFloat() / viewWidth, newHeight.toFloat() / viewHeight, centerX, centerY)
//        matrix.postTranslate(dx.toFloat(), dy.toFloat())
        return matrix
    }




    val textureView = AutoFitTextureView(applicationContext).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        setAspectRatio(3, 4)
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {

                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

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

    eventHandler.invoke(CameraEvent.OnTextureViewChanged(textureView))


    //height = textureView.height
// textureView.post {
//        aspectRatio = Size(9, 16)
//
//        // Здесь вы можете определить maxWidth и maxHeight в зависимости от требований вашего приложения
//        val maxWidth = displaySize.x
//        val maxHeight = displaySize.y
//        previewSize = previewSizes?.let { chooseOptimalSize(it, state.textureView!!.width, state.textureView!!.height, maxWidth, maxHeight, aspectRatio) }!!
//        matrix = state.textureView?.let {
//            configureTransform(
//                state.textureView!!.width, state.textureView!!.height, windowManager, previewSize,
//                it
//            )
//        }!!

        //matrix?.setScale(1.5f, 0.7f)
        //state.textureView?.setTransform(matrix)
//    }

    matrix.setScale(1.2f, 1.0f)
    state.textureView?.setTransform(matrix)

    //eventHandler.invoke(CameraEvent.OnTextureViewChanged(textureView))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
//        Column {
//            if (previewSizes != null) {
//                for( i in previewSizes){
//                    Text(text = i.toString())
//                }
//            }
//        }


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SettingsBar(
                modifier = Modifier,
                state = state,
                isoValues = isoValues,
                onSettingsClick = {
                    eventHandler.invoke(
                        CameraEvent.OnSettingsClick
                    )
                },
                onAutoExpClick = {
                    eventHandler.invoke(
                        CameraEvent.OnChangeModeClick
                    )
                },
                onIsoClick = {
                    eventHandler.invoke(
                        CameraEvent.OnIsoButtonClick
                    )
                },
                onShutterSpeedClick = {
                    eventHandler.invoke(
                        CameraEvent.OnShutterSpeedButtonClick
                    )
                }
            )

            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .aspectRatio(3f / 4f) ,
                    factory = { textureView }
                )
                when(state.sliderStatus){
                    SliderStatus.ISO -> {
                        IsoSlider(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp),
                            isoIndex = state.isoIndex,
                            isoMaxIndex = isoMaxIndex,
                            onValueChanged = { it ->
                                state.capReq?.set(CaptureRequest.SENSOR_SENSITIVITY, isoValues[it])
                                eventHandler.invoke(
                                    CameraEvent.OnIsoIndexChanged(it)
                                )
                                try {
                                    state.cameraCaptureSession?.setRepeatingRequest(
                                        state.capReq!!.build(),
                                        null,
                                        state.handler
                                    )
                                } catch (e: CameraAccessException) {
                                    Log.e("CameraApp", "Не удалось обновить ISO на лету", e)
                                }
                            }
                        )
                    }
                    SliderStatus.SHUTTER_SPEED -> {
                        ShutterSpeedSlider(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp),
                            shutterSpeedIndex = state.shutterSpeedIndex,
                            shutterSpeedMaxIndex = shutterSpeedMaxIndex,
                            onValueChanged = { index ->
                                state.capReq?.set(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterSpeedValues[index])
                                eventHandler.invoke(
                                    CameraEvent.OnShutterSpeedIndexChanged(index)
                                )
                                try {
                                    state.cameraCaptureSession?.setRepeatingRequest(
                                        state.capReq!!.build(),
                                        null,
                                        state.handler
                                    )
                                } catch (e: CameraAccessException) {
                                    Log.e("CameraApp", "Не удалось обновить выдержку на лету", e)
                                }
                            }
                        )
                    }
                    SliderStatus.HIDE -> Text(text = "Hide")
                }

            }

            eventHandler.invoke(CameraEvent.OnTextureViewChanged(textureView))
        }
        BottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            photoWithFlash = state.photoWithFlash,
            photosNumber = state.photosNumber
        )
    }
}

@Composable
fun SettingsBar(
    modifier: Modifier,
    state: CameraState,
    isoValues: List<Int>,
    onSettingsClick: () -> Unit,
    onAutoExpClick: () -> Unit,
    onIsoClick: () -> Unit,
    onShutterSpeedClick: () -> Unit
){
    Box(
        modifier = Modifier
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp)
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
                    onClick = { onAutoExpClick() }
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
                            text = state.isoValue.toString(),
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
                            text = formatShutterSpeed(state.shutterSpeedValue),
                            color = MaterialTheme.colorScheme.onPrimary,
                            //fontSize = 7.sp
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

        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp)
                //.background(MaterialTheme.colorScheme.primary),
            //horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            if (state.isAutoMode){
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )
                for(i in 1..2){
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .alpha(0.3f)
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clickable { onIsoClick() }
                        .fillMaxHeight()
                        .weight(1f)

                )
                Box(
                    modifier = Modifier
                        .clickable { onShutterSpeedClick() }
                        .fillMaxHeight()
                        .weight(1f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
fun IsoSlider(
    modifier: Modifier,
    isoIndex: Int,
    isoMaxIndex: Int,
    onValueChanged: (newValue: Int) -> Unit
){
    Slider(
        modifier = modifier.padding(horizontal = 16.dp),
        value = isoIndex.toFloat(),
        onValueChange = { newValue ->
            onValueChanged(newValue.roundToInt())
        },
        valueRange = 0f..isoMaxIndex.toFloat(),
        steps = isoMaxIndex - 1
    )
}

@Composable
fun ShutterSpeedSlider(
    modifier: Modifier,
    shutterSpeedIndex: Int,
    shutterSpeedMaxIndex: Int,
    onValueChanged: (newValue: Int) -> Unit
){
    Slider(
        modifier = modifier.padding(horizontal = 16.dp),
        value = shutterSpeedIndex.toFloat(),
        onValueChange = { newValue ->
            onValueChanged(newValue.roundToInt())
        },
        valueRange = 0f..shutterSpeedMaxIndex.toFloat(),
        steps = shutterSpeedMaxIndex - 1
    )
}

@Composable
fun BottomBar(
    modifier: Modifier,
    photoWithFlash: Boolean,
    photosNumber: Int
){

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ){
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    text = "Ночное фото",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 26.dp, vertical = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .width(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.onPrimary)
                ) {

                }

                Icon(
                    painterResource(R.drawable.ic_take_photo),
                    modifier = Modifier
                        .clickable { }
                        .size(80.dp),
                    contentDescription = "icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )

                Box(
                    modifier = Modifier
                        .clickable { }
                ){
                    Icon(
                        painterResource(
                            if(photoWithFlash) R.drawable.ic_rec_flash else R.drawable.ic_rec
                        ),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(60.dp),
                        contentDescription = "icon",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )

                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = photosNumber.toString(),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

