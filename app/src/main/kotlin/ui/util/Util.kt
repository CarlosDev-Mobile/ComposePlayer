package com.carlosdev.player.ui.util

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@SuppressLint("NewApi")
fun ComponentActivity.enableFullyEdgeToEdge() {
    enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
    if (Build.VERSION.SDK_INT >= 29) {
        window.isNavigationBarContrastEnforced = false
    }
}

fun Modifier.myLaunchedEffect(
    coroutineScope: CoroutineScope,
    block: suspend CoroutineScope.(Modifier) -> Modifier
): Modifier = composed {
    var modifier by remember { mutableStateOf<Modifier>(Modifier) }

    LaunchedEffect(key1 = Unit) {
        coroutineScope.launch {
            modifier = block(modifier)
        }
    }

    modifier
}