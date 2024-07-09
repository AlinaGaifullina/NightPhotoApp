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
fun IsoSlider(
    modifier: Modifier,
    isoIndex: Int,
    isoMaxIndex: Int,
    onValueChanged: (newValue: Int) -> Unit
){
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = isoIndex.toFloat(),
            onValueChange = { newValue ->
                onValueChanged(newValue.roundToInt())
            },
            valueRange = 0f..isoMaxIndex.toFloat(),
            steps = isoMaxIndex - 1
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for(i in 0..5){
                Box(
                    modifier = Modifier.width(30.dp)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = CameraParameters.isoValues[i].toString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}