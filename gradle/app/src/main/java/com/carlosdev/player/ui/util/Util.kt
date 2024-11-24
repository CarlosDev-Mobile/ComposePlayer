package com.carlosdev.player.ui.util

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

@SuppressLint("NewApi")
fun ComponentActivity.enableFullyEdgeToEdge() {
    enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
    if (Build.VERSION.SDK_INT >= 29) {
        window.isNavigationBarContrastEnforced = false
    }
}