package com.carlosdev.player.logic.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carlosdev.player.audioController
import com.carlosdev.player.logic.audio.media.MediaKit
import com.carlosdev.player.logic.screen.item.ItemMusic

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun SongPager(
    viewModel: MediaKit = viewModel(),
    lazyState: LazyListState = rememberLazyListState()
) {
    val items by viewModel.mediaItemList.collectAsStateWithLifecycle(emptyList())
    if (items.isEmpty()) {
        CircularProgressIndicator()
    } else {
        val i = items.sortedBy { it.mediaMetadata.title.toString() }
        println(i[2].mediaMetadata.artworkUri)
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 0.dp),
                state = lazyState
            ) {
                item { Text("Header") }
                items(i.size) {
                    val item = i[it]
                    ItemMusic(
                        imageVector = item.mediaMetadata.artworkUri,
                        title = item.mediaMetadata.title.toString(),
                        subtitle = item.mediaMetadata.artist.toString(),
                        onClick = {
                            audioController?.setMediaItem(i.toMutableList())
                            audioController?.playItemAtIndex(it)
                        }
                    )
                }
            }
        }
    }
}