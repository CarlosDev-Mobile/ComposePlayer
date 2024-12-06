package com.carlosdev.player.logic.screen

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carlosdev.player.audioController
import com.carlosdev.player.logic.audio.media.MediaKit
import com.carlosdev.player.logic.screen.item.ItemAlbum
import com.carlosdev.player.logic.screen.item.ItemMusic
import com.carlosdev.player.ui.theme.ComposePlayerTheme

@Composable
fun AlbumPager(
    viewModel: MediaKit = viewModel(),
    lazyState: LazyListState = rememberLazyListState()
) {
    val items by viewModel.albumItemList.collectAsStateWithLifecycle(emptyList())
    if (items.isEmpty()) {
        CircularProgressIndicator()
    } else {
        val i = items.sortedBy { it.title.toString() }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            LazyVerticalGrid (
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize()
            ) {
                items(i.size) {
                    val item = i[it]
                    ItemAlbum(
                        imageVector = item.artUri,
                        title = item.title.toString(),
                        subtitle = item.artist.toString(),
                        onClick = {

                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MyFragmentLayoutCompose(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Hello from Fragment Layout (Compose)"
        )
        Spacer(modifier = Modifier.height(16.dp))
        // ... outros componentes do layout
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    showSystemUi = true
)
@Composable
fun GreetingPreview() {
    ComposePlayerTheme {
        AlbumPager()
    }
}