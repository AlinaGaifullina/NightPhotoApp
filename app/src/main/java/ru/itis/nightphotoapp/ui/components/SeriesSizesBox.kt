package ru.itis.nightphotoapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.itis.nightphotoapp.R

@Composable
fun SeriesSizesBox(
    modifier: Modifier,
    seriesSize: Int,
    isCheckboxClick: Boolean,
    onCheckboxClick: (Boolean) -> Unit,
    onChangeSeries: (Int) -> Unit,
){
    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(0.8f)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.primary)
    ){
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                text = stringResource(id = R.string.choose_series_size),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for(i in 1..9) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (seriesSize == i) MaterialTheme.colorScheme.onPrimary else Color.Transparent)
                            .clickable { onChangeSeries(i) }

                    ) {
                        Text(
                            modifier = Modifier.padding(4.dp),
                            text = i.toString(),
                            color = if(seriesSize == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.add_photo_with_flash),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Box(
                    modifier = Modifier
                        .clickable { onCheckboxClick(isCheckboxClick) }
                ){
                    Icon(
                        painterResource(
                            if(isCheckboxClick) R.drawable.ic_checkbox_true else R.drawable.ic_checkbox_false
                        ),
                        modifier = Modifier.size(25.dp),
                        contentDescription = "icon",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}