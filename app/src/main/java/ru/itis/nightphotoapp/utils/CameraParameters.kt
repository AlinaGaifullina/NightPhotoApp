package ru.itis.nightphotoapp.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range

class CameraParameters {

    companion object{
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
    }
}