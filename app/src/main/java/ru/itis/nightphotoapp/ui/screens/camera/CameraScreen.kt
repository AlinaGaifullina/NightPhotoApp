package ru.itis.nightphotoapp.ui.screens.camera

import android.app.Activity
import android.content.Context
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import ru.itis.nightphotoapp.R
import ru.itis.nightphotoapp.ui.navigation.RootGraph

@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = koinViewModel(),
    applicationContext: Context
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val eventHandler = viewModel::event
    val action by viewModel.action.collectAsStateWithLifecycle(null)

    LaunchedEffect(action) {
        when (action) {
            null -> Unit
            is CameraSideEffect.NavigateToSettingsScreen -> {
                navController.navigate(RootGraph.Settings.route)
            }
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        val lifecycleOwner = LocalLifecycleOwner.current
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(it).apply {
                    this.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
                }
            }
        )

        SettingsBar(
            modifier = Modifier
                .align(Alignment.TopCenter),
            state = state,
            onSettingsClick = {
                eventHandler.invoke(
                    CameraEvent.OnSettingsClick
                )
            }
        )

        BottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun SettingsBar(
    modifier: Modifier,
    state: CameraState,
    onSettingsClick: () -> Unit
){
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .alpha(0.85f)
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
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
            //.padding(horizontal = 20.dp)
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
                onClick = { /*TODO*/ }
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
                    .clickable {  }
                    .align(Alignment.Center),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ISO",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = state.iso.toString(),
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
                    .clickable {  }
                    .align(Alignment.Center),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Выдержка",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = state.shutterSpeed,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 7.sp
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
                onClick = {
                    onSettingsClick()
                }
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
}

@Composable
fun BottomBar(
    modifier: Modifier
){

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .alpha(0.85f)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 26.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {


            }

        }
    }
}

