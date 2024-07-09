package ru.itis.nightphotoapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

//TODO:
@Composable
fun ScrollWheel(
    modifier: Modifier,
    minValue: Int = 3,
    maxValue: Int = 9,
    onValueChange: (Int) -> Unit
) {
    // Значение, выбранное пользователем
    val selectedValue = remember { mutableStateOf(minValue) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.rotationZ = 180f
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { /* Обработка нажатия */ },
                    onTap = { /* Обработка касания */ }
                )
            }
        ) {
            // Рисование колеса
            val wheelRadius = size.minDimension / 4
            val center = Offset(size.width - wheelRadius, size.height - wheelRadius)
            drawCircle(Color.Blue, radius = 200.dp.toPx())
            val lineStart = Offset(center.x - wheelRadius, center.y)
            val lineEnd = Offset(center.x + wheelRadius, center.y)
            drawLine(Color.Red, start = lineStart, end = lineEnd, strokeWidth = 4.dp.toPx())
        }
    }
}