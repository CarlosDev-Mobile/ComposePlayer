package com.carlosdev.player.logic.screen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.carlosdev.player.audioController
import com.carlosdev.player.ui.theme.ComposePlayerTheme

@Composable
fun PreviewSong(onDismiss: () -> Unit) {
    val mediaItem = audioController?.player()?.currentMediaItem
    // ... Seu layout de sobreposição aqui ...
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row {

        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    showSystemUi = true, device = "spec:width=1080px,height=2340px,dpi=440",
    wallpaper = Wallpapers.NONE
)
@Composable
private fun Preview() {
    ComposePlayerTheme {
        PreviewSong(onDismiss = {})
    }
}