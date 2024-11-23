package ru.itis.nightphotoapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.Size
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core.NORM_L2
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.SIFT
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo
import java.nio.ByteBuffer

class CameraParameters {

    companion object{
        val isoValues: List<Int> = listOf(100, 200, 400, 800, 1600, 3200)
        val shutterSpeedValues: List<Long> = listOf(100000, 200000, 500000, 1000000, 2000000, 4000000, 8000000, 15000000, 30000000, 60000000, 120000000, 250000000, 500000000, 1000000000, 2000000000, 4000000000, 8000000000, 16000000000, 32000000000)
        val isoMaxIndex = isoValues.size - 1
        val shutterSpeedMaxIndex = shutterSpeedValues.size - 1

        fun formatShutterSpeed(shutterSpeedInNano: Long): String {
            val denominator = 1_000_000_000.0 / shutterSpeedInNano
            return if (denominator < 1) {
                "${shutterSpeedInNano / 1_000_000_000.0}"
            } else {
                "1/${denominator.toInt()}"
            }
        }

        fun convertToFocusPoint(x: Float, y: Float, previewSize: Size, sensorArraySize: Rect): PointF {
            val focusX = (x / previewSize.width.toFloat()) * sensorArraySize.width()
            val focusY = (y / previewSize.height.toFloat()) * sensorArraySize.height()

            // Перевод координат в систему координат, используемую API камеры
            val resultX = sensorArraySize.left + focusX.toInt()
            val resultY = sensorArraySize.top + focusY.toInt()

            return PointF(resultX.toFloat(), resultY.toFloat())
        }

        fun getEvCompensationRange(context: Context, cameraId: String): Range<Int>? {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            return characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
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

        fun bitmapToMat(bitmap: Bitmap): Mat {
            // Проверяем, что входной Bitmap не пустой
            if (bitmap.isRecycled) {
                println("Ошибка: Входной Bitmap пустой или переработанный.")
                return Mat()
            }

            // Если Bitmap не в формате ARGB_8888, создаем новый Bitmap с нужным форматом
            val rgbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).apply {
                    val canvas = Canvas(this)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                }
            } else {
                bitmap
            }

            // Преобразуем Bitmap в Mat
            val mat = Mat()
            Utils.bitmapToMat(rgbBitmap, mat)

            // Проверяем, что преобразование прошло успешно
            if (mat.empty()) {
                println("Ошибка: Не удалось преобразовать Bitmap в Mat.")
                return Mat()
            }

            // Преобразуем Mat в RGB, если это необходимо
            if (mat.channels() == 4) {
                val rgbMat = Mat()
                Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_RGBA2RGB)
                return rgbMat
            } else if (mat.channels() != 3) {
                println("Ошибка: Неверное количество каналов: ${mat.channels()}. Ожидается 3 канала.")
                return Mat()
            }

            println("Успешно преобразовано Bitmap в Mat.")
            return mat
        }

        fun matToBitmap(mat: Mat): Bitmap? {
            // Проверяем, что входной Mat не пустой
            if (mat.empty()) {
                println("Ошибка: Входной Mat пустой.")
                return null
            }

            // Приводим Mat к нужному типу
            val convertedMat: Mat = when (mat.type()) {
                CvType.CV_8UC1, CvType.CV_8UC3, CvType.CV_8UC4 -> mat
                CvType.CV_16UC1 -> {
                    // Преобразуем 16-битное одноканальное изображение в 8-битное
                    val mat8U = Mat()
                    mat.convertTo(mat8U, CvType.CV_8UC1, 1.0 / 256.0) // Делим на 256, чтобы получить 8-битное значение
                    mat8U
                }
                CvType.CV_16UC3 -> {
                    // Преобразуем 16-битное трехканальное изображение в 8-битное
                    val mat8U = Mat()
                    mat.convertTo(mat8U, CvType.CV_8UC3, 1.0 / 256.0) // Делим на 256, чтобы получить 8-битное значение
                    mat8U
                }
                CvType.CV_32FC3 -> {
                    // Преобразуем 16-битное четырехканальное изображение в 8-битное
                    val mat8U = Mat()
                    mat.convertTo(mat8U, CvType.CV_8UC3, 255.0) // Делим на 256, чтобы получить 8-битное значение
                    mat8U
                }
                else -> {
                    println("Ошибка: Неверный тип Mat: ${mat.type()}. Ожидается CV_8UC1, CV_8UC3 или CV_8UC4.")
                    return null
                }
            }

            // Создаем Bitmap и преобразуем Mat в Bitmap
            val bitmap = Bitmap.createBitmap(convertedMat.cols(), convertedMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(convertedMat, bitmap)

            return bitmap
        }

        fun fuseImages(images: List<Bitmap>): Bitmap? {

            val mats = images.map { bitmapToMat(it) }

            if (mats.size < 2) {
                println("Недостаточно изображений для слияния.")
                return null
            }

            val mergeExposures = Photo.createMergeMertens()

            if (mergeExposures == null) {
                println("Ошибка: Не удалось создать объект MergeMertens.")
                return null
            }

            val fusedImage = Mat.zeros(mats[0].size(), mats[0].type())

            try {
                mergeExposures.process(mats, fusedImage)
            } catch (e: Exception) {
                println("Ошибка при слиянии изображений: ${e.message}")
                return null
            }

            return matToBitmap(fusedImage)
        }

        fun bitmapToMat2(bitmap: Bitmap): Mat {
            val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
            val bytes = ByteArray(bitmap.byteCount)
            bitmap.copyPixelsToBuffer(ByteBuffer.wrap(bytes))
            mat.put(0, 0, bytes)
            return mat
        }

        fun matToBitmap2(mat: Mat): Bitmap {
            val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
            val bytes = ByteArray(mat.cols() * mat.rows() * 4)
            mat.get(0, 0, bytes)
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
            return bitmap
        }

        fun stabilizeImages(images: List<Bitmap>): List<Bitmap> {
            // Convert Bitmaps to Mats
            val mats = images.map { bitmapToMat2(it) }

// Detect SIFT features in each image
            val sift = SIFT.create()

            val keypoints = mutableListOf<MatOfKeyPoint>()
            mats.forEachIndexed { i, m ->
                val kp = MatOfKeyPoint()
                sift.detect(m, kp)
                keypoints.add(kp)
                println("Количество ключевых точек в изображении $i: ${kp.toList().size}") // Отладка
            }

            val descriptors = mats.indices.map { i ->
                val descriptor = Mat()
                sift.compute(mats[i], keypoints[i], descriptor)
                descriptor
            }

// Find the central image
            val centralIndex = mats.size / 2
            val centralImage = mats[centralIndex]

// Compute the homography matrix for each image relative to the central image
            val homographies = mats.indices.map { i ->
                if (i == centralIndex) {
                    Mat.eye(3, 3, CvType.CV_64FC1)
                } else {
                    // Создание экземпляра BFMatcher
                    val matcher = BFMatcher.create(NORM_L2, true)

                    // Создание MatOfDMatch для хранения совпадений
                    val matches = MatOfDMatch()

                    // Выполнение сопоставления
                    matcher.match(descriptors[i], descriptors[centralIndex], matches)

                    // Преобразование MatOfDMatch в список DMatch
                    val matchesList = matches.toList()

                    // Фильтруем хорошие совпадения
                    val goodMatches = matchesList.filter { it.distance < 70.0 } // Уменьшите порог

                    // Извлекаем исходные и целевые точки
                    val srcPointsList = goodMatches.map { keypoints[i].toList()[it.queryIdx].pt }
                    val dstPointsList = goodMatches.map { keypoints[centralIndex].toList()[it.trainIdx].pt }

                    // Вывод количества точек для отладки
                    println("Количество хороших совпадений: ${goodMatches.size}")
                    println("Количество исходных точек: ${srcPointsList.size}")
                    println("Количество целевых точек: ${dstPointsList.size}")

                    // Проверка на количество точек
                    if (srcPointsList.size < 4 || dstPointsList.size < 4) {
                        throw IllegalArgumentException("Необходимо минимум 4 точки для вычисления гомографии.")
                    }

                    // Преобразование списков точек в MatOfPoint2f
                    val srcPoints = MatOfPoint2f(*srcPointsList.toTypedArray())
                    val dstPoints = MatOfPoint2f(*dstPointsList.toTypedArray())

                    // Создание маски
                    val mask = Mat()

                    // Находим гомографию
                    val homography = Calib3d.findHomography(srcPoints, dstPoints, Calib3d.RANSAC, 3.0, mask, 2000)

                    // Используйте полученную гомографию (например, для трансформации изображений)
                    println("Гомография: $homography")
                    homography
                }
            }

            // Warp each image to the central image
            val stabilizedImages = mats.mapIndexed { i, mat ->
                val warpedMat = Mat()
                Imgproc.warpPerspective(mat, warpedMat, homographies[i], mat.size())
                matToBitmap2(warpedMat)
            }

            return stabilizedImages
        }
    }
}