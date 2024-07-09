package ru.itis.nightphotoapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.itis.nightphotoapp.utils.CameraParameters
import kotlin.math.roundToInt

@Composable
fun ShutterSpeedSlider(
    modifier: Modifier,
    shutterSpeedIndex: Int,
    shutterSpeedMaxIndex: Int,
    onValueChanged: (newValue: Int) -> Unit
){
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = shutterSpeedIndex.toFloat(),
            onValueChange = { newValue ->
                onValueChanged(newValue.roundToInt())
            },
            valueRange = 0f..shutterSpeedMaxIndex.toFloat(),
            steps = shutterSpeedMaxIndex - 1
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            var indexCounter = 0
            //1/10000 1/16
            for(i in 0..6){
                Box(
                    modifier = Modifier.width(42.dp)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = CameraParameters.formatShutterSpeed(CameraParameters.shutterSpeedValues[indexCounter]),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                indexCounter += 3
            }
        }
    }
}