package ru.itis.nightphotoapp.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import ru.itis.nightphotoapp.R
import ru.itis.nightphotoapp.ui.navigation.RootGraph
import ru.itis.nightphotoapp.ui.screens.camera.CameraSideEffect

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
){
    val state by viewModel.state.collectAsState()
    val eventHandler = viewModel::event
    val action by viewModel.action.collectAsState(null)

    LaunchedEffect(action) {
        when (action) {
            null -> Unit
            is SettingsSideEffect.NavigateBack -> {
                navController.navigate(RootGraph.Camera.route)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onPrimary),
    ) {
        TopBar(
            onBackButtonClick = {
                eventHandler.invoke(
                    SettingsEvent.OnBackButtonClick
                )
            }
        )
        Text(text = state.isSaveSeries.toString(),
            color = MaterialTheme.colorScheme.primary)

        SettingsContent(
            isCheckboxClick = state.isSaveSeries,
            onCheckboxClick = { it ->
                eventHandler.invoke(
                    SettingsEvent.OnCheckboxClick(it)
                )
            }
        )
        Text(text = state.isSaveSeries.toString(),
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun TopBar(
    onBackButtonClick: () -> Unit
){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .alpha(0.85f)
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.primary)
    ){
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ){
            IconButton(
                modifier = Modifier
                    .padding(start = 14.dp),
                onClick = { onBackButtonClick() }
            ) {
                Icon(
                    painterResource(R.drawable.ic_left),
                    modifier = Modifier.size(40.dp),
                    contentDescription = "icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )

            }

            Text(
                text = "Настройки",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

}

@Composable
fun SettingsContent(
    isCheckboxClick: Boolean,
    onCheckboxClick: (status: Boolean) -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .alpha(0.6f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(18.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Папка для сохранения",
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "/storage/01234567/DSIM/Camera",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

        }

        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(100.dp)
                .alpha(0.6f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(18.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Сохранять серию снимков",
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
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "Будет сохранена вся серия снимков",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

        }
    }

}