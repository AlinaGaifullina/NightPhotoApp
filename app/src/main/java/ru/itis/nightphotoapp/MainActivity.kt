package ru.itis.nightphotoapp

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
import android.hardware.camera2.CameraMetadata.CONTROL_MODE_OFF
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.Surface
import android.view.TextureView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.Flow
import ru.itis.nightphotoapp.ui.navigation.RootNavGraph
import ru.itis.nightphotoapp.ui.theme.NightPhotoAppTheme
import java.util.prefs.Preferences
import androidx.camera.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        fun arePermissionsGranted(): Boolean {
            return CAMERA_PERMISSION.all { perssion ->
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    perssion
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        if (!arePermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, CAMERA_PERMISSION, 100
            )
        }




//        fun stopBackgroundThread() {
//            backgroundHandlerThread.quitSafely()
//            backgroundHandlerThread.join()
//        }



        setContent {
            NightPhotoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val controller = remember {
                        LifecycleCameraController(
                            applicationContext
                        ).apply {
                            setEnabledUseCases(
                                CameraController.IMAGE_CAPTURE or
                                        CameraController.VIDEO_CAPTURE
                            )
                        }
                    }


                    val navController = rememberNavController()
                    RootNavGraph(navController = navController, this.applicationContext)
                }
            }
        }
    }

    companion object {
        val CAMERA_PERMISSION = arrayOf(
            android.Manifest.permission.CAMERA
        )
    }
}

fun getCameraCharacteristics(context: Context): String {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
        var text = "12345"
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val cameraDirection = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_BACK -> "Back"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }

            val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) as Range<Int>?
            val minIso = isoRange?.lower ?: "Unknown"
            val maxIso = isoRange?.upper ?: "Unknown"

            val exposureTimeRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) as Range<Long>?
            val minExposureTime = exposureTimeRange?.lower ?: "Unknown"
            val maxExposureTime = exposureTimeRange?.upper ?: "Unknown"

            if(cameraDirection == "Back")
            text = "Camera ID: $cameraId Direction: $cameraDirection ISO Range: $minIso - $maxIso Exposure Time Range: $minExposureTime - $maxExposureTime"
            // Добавьте здесь другие характеристики, которые вас интересуют
        }
        return text
    } catch (e: Exception) {
        return e.printStackTrace().toString()
    }
}

