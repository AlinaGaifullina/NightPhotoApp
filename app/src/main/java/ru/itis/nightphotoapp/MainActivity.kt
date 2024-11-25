package ru.itis.nightphotoapp

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.rememberNavController
import org.opencv.android.OpenCVLoader
import ru.itis.nightphotoapp.ui.navigation.RootNavGraph
import ru.itis.nightphotoapp.ui.theme.NightPhotoAppTheme


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (OpenCVLoader.initLocal()) {
            Log.i("loaded", "OpenCV loaded successfully");
            (Toast.makeText(this, "OpenCV initialization suc!", Toast.LENGTH_LONG)).show();
        } else {
            Log.e("loaded", "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

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

        fun getEvCompensationRange(context: Context, cameraId: String): Range<Int>? {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            return characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        }


        val cameraId = "0"
        val evRange = getEvCompensationRange(applicationContext, cameraId)
        if (evRange != null) {
            println("Диапазон компенсации экспозиции: от ${evRange.lower} до ${evRange.upper}")
        } else {
            println("Информация о диапазоне компенсации экспозиции не доступна")
        }

        setContent {
            NightPhotoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    RootNavGraph(navController = navController, this.applicationContext)
                }
            }
        }
    }

    companion object {
        val CAMERA_PERMISSION = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

