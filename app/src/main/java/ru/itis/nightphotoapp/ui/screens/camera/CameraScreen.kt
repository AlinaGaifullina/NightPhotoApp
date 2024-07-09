package ru.itis.nightphotoapp.ui.screens.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ExifInterface
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import ru.itis.nightphotoapp.R
import ru.itis.nightphotoapp.ui.components.IsoSlider
import ru.itis.nightphotoapp.ui.components.SeriesSizesBox
import ru.itis.nightphotoapp.ui.components.ShutterSpeedSlider
import ru.itis.nightphotoapp.ui.navigation.RootGraph
import ru.itis.nightphotoapp.utils.CameraParameters
import ru.itis.nightphotoapp.utils.DisplayParameters
import ru.itis.nightphotoapp.utils.SliderStatus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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

@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = koinViewModel(),
    applicationContext: Context,
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val eventHandler = viewModel::event
    val action by viewModel.action.collectAsStateWithLifecycle(null)


    val cameraManager: CameraManager = applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val cameraId = cameraManager.cameraIdList[0]
    val displaySize = DisplayParameters.getScreenSize(windowManager) //1080 2220
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    val appContext = applicationContext
    val evRange = CameraParameters.getEvCompensationRange(appContext, cameraId)

    var matrix by remember {
        mutableStateOf(Matrix())
    }

    LaunchedEffect(action) {
        when (action) {
            null -> Unit
            is CameraSideEffect.NavigateToSettingsScreen -> {
                navController.navigate(RootGraph.Settings.route)
            }
        }
    }

    // подумать как сделать:
    DisposableEffect(Unit) {
        onDispose {
            //viewModel.releaseCameraResources()
        }
    }

    fun openCamera(){
        cameraManager.openCamera(cameraId, state.cameraStateCallback!!, state.handler)
    }

    val textureView = AutoFitTextureView(applicationContext).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        //setAspectRatio(3, 4)
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {

                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                // Освободите ресурсы камеры здесь
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // Обновите ваше изображение здесь
            }
        }
    }
    // Функция для получения размера превью
    fun getPreviewSize(characteristics: CameraCharacteristics): Size {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        // Вы можете изменить это, чтобы получить разные размеры в зависимости от вашего использования
        return map?.getOutputSizes(SurfaceTexture::class.java)?.firstOrNull() ?: Size(0, 0)
    }

    // Функция для получения размера активной области датчика
    fun getSensorArraySize(characteristics: CameraCharacteristics): Rect {
        return characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: Rect(0, 0, 0, 0)
    }


    val previewSize = getPreviewSize(characteristics)
    val sensorArraySize = getSensorArraySize(characteristics)

    // не работает, надо чинить
    textureView.setOnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            // Передайте координаты касания для настройки фокуса
            eventHandler(
                CameraEvent.OnSetFocus(x, y, previewSize, sensorArraySize)
            )
            v.performClick()
        }
        true
    }

    eventHandler.invoke(CameraEvent.OnTextureViewChanged(textureView))

    matrix.setScale(1.2f, 1.0f)
    state.textureView?.setTransform(matrix)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SettingsBar(
                modifier = Modifier,
                state = state,
                isCapturing = state.isCapturing,
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
                            isoMaxIndex = CameraParameters.isoMaxIndex,
                            onValueChanged = { it ->
                                state.capReq?.set(CaptureRequest.SENSOR_SENSITIVITY, CameraParameters.isoValues[it])
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
                            shutterSpeedMaxIndex = CameraParameters.shutterSpeedMaxIndex,
                            onValueChanged = { index ->
                                state.capReq?.set(CaptureRequest.SENSOR_EXPOSURE_TIME, CameraParameters.shutterSpeedValues[index])
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
                    SliderStatus.HIDE -> Text(text = "")
                }

            }

            eventHandler.invoke(CameraEvent.OnTextureViewChanged(textureView))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val compensationStep = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
            val compensationRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            Text(text = compensationRange.toString(), color = MaterialTheme.colorScheme.onPrimary)
            Text(text = compensationStep.toString(), color = MaterialTheme.colorScheme.onPrimary)

            if(state.isShowSeriesSizes) {
                SeriesSizesBox(
                    modifier = Modifier,
                    seriesSize = state.seriesSize,
                    isCheckboxClick = state.photoWithFlash,
                    onCheckboxClick = { isChecked ->
                        eventHandler(
                            CameraEvent.OnCheckboxClick(isChecked)
                        )
                    },
                    onChangeSeries = { size ->
                        eventHandler.invoke(
                            CameraEvent.OnSeriesSizeChanged(size)
                        )
                    }

                )
            }
            BottomBar(
                modifier = Modifier,
                photoWithFlash = state.photoWithFlash,
                photosNumber = state.seriesSize,
                isCapturing = state.isCapturing,
                onTakePhoto = {
                    state.imageReader?.setOnImageAvailableListener({ p0 ->
                        val image = p0?.acquireLatestImage()
                        val buffer = image!!.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        saveImageToGallery(appContext, bytes)

                        image.close()
                    }, state.handler)

                    eventHandler.invoke(
                        CameraEvent.OnTakePhoto(evRange)
                    )
                },
                onSeriesButtonClick = {
                    eventHandler.invoke(
                        CameraEvent.OnSeriesButtonClick
                    )
                }
            )
        }
    }
}

fun generateEvSeries(numberOfShots: Int, minEv: Double, maxEv: Double): List<Double> {
    if (numberOfShots !in 1..9) {
        throw IllegalArgumentException("Количество снимков должно быть от 1 до 9")
    }

    val referenceEv = minOf(Math.abs(minEv), Math.abs(maxEv))
    val evStep = (referenceEv * 2) / (numberOfShots - 1)
    val middleIndex = (numberOfShots - 1) / 2

    return List(numberOfShots) { index ->
        -referenceEv + (index * evStep)
    }
}

// Пример использования функции:
val evList = generateEvSeries(9, -24.0, 12.0)

fun saveImageToGallery(context: Context, bytes: ByteArray) {
    // Создаем Bitmap из массива байтов
    val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    // Получаем ориентацию из ExifInterface
    val exifInterface = ExifInterface(ByteArrayInputStream(bytes))
    val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    println(ExifInterface.ORIENTATION_ROTATE_90)

    // Поворачиваем изображение, если это необходимо
    val matrix = Matrix()
    // если что:
    when (orientation) {
        ExifInterface.ORIENTATION_NORMAL -> matrix.postRotate(90f)
//        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
//        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
//        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    }
    val rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)

    // Сохраняем изображение
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_$timeStamp.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        // Добавьте больше метаданных при необходимости
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        context.contentResolver.openOutputStream(it).use { outputStream ->
            if (outputStream != null) {
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            outputStream?.flush()
            outputStream?.close()
        }
    }

    // Освобождаем ресурсы
    originalBitmap.recycle()
    rotatedBitmap.recycle()
}

// Вспомогательная функция для конвертации Bitmap в массив байтов
fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 100, stream)
    return stream.toByteArray()
}

@Composable
fun SettingsBar(
    modifier: Modifier,
    state: CameraState,
    isCapturing: Boolean,
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
                    onClick = { if (!isCapturing) { onAutoExpClick() } }
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
                        .align(Alignment.Center),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.iso),
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
                        .align(Alignment.Center),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.shutter_speed),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = CameraParameters.formatShutterSpeed(state.shutterSpeedValue),
                            color = MaterialTheme.colorScheme.onPrimary,
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
                    onClick = { if (!isCapturing) { onSettingsClick() } }
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
                        .clickable { if (!isCapturing) { onIsoClick() } }
                        .fillMaxHeight()
                        .weight(1f)

                )
                Box(
                    modifier = Modifier
                        .clickable { if (!isCapturing) { onShutterSpeedClick() } }
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
fun BottomBar(
    modifier: Modifier,
    photoWithFlash: Boolean,
    photosNumber: Int,
    isCapturing: Boolean,
    onTakePhoto: () -> Unit,
    onSeriesButtonClick: () -> Unit
){

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(MaterialTheme.colorScheme.primary)
    ) {
        if(isCapturing){
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.wait_photos),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                CircularProgressIndicator(
                    modifier = Modifier,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
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
                        text = stringResource(id = R.string.night_photo),
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
                    )

                    Icon(
                        painterResource(R.drawable.ic_take_photo),
                        modifier = Modifier
                            .clickable { onTakePhoto() }
                            .size(80.dp),
                        contentDescription = "icon",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )

                    Box(
                        modifier = Modifier
                            .clickable { onSeriesButtonClick() }
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
}

