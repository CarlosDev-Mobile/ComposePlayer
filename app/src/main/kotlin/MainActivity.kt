package com.carlosdev.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION
import androidx.media3.common.Player.EVENT_MEDIA_METADATA_CHANGED
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.EVENT_TRACKS_CHANGED
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.carlosdev.player.logic.audio.media.AudioController
import com.carlosdev.player.logic.audio.media.MediaKit
import com.carlosdev.player.logic.audio.media.RememberState
import com.carlosdev.player.logic.audio.media.Utils.updateLibraryWithInCoroutine
import com.carlosdev.player.logic.audio.service.AudioService
import com.carlosdev.player.logic.screen.AlbumPager
import com.carlosdev.player.logic.screen.ArtistPager
import com.carlosdev.player.logic.screen.PreviewSong
import com.carlosdev.player.logic.screen.SongPager
import com.carlosdev.player.ui.theme.ComposePlayerTheme
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

var audioController: AudioController? = null
private lateinit var player: MediaController
private lateinit var rememberState: RememberState
private lateinit var controllerFuture: ListenableFuture<MediaController>

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposePlayerTheme {
                var showPreviewSong by remember { mutableStateOf(false) }
                App(onClick = {showPreviewSong = true})
                if (showPreviewSong) {
                    PreviewSong(onDismiss = { showPreviewSong = false })
                }
            }
        }
    }

    override fun onDestroy() {
        releaseController()
        super.onDestroy()
    }
}

private suspend fun initializeController(context: Context) {
    controllerFuture =
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, AudioService::class.java))
        ).buildAsync()

    updateMediaMetadataUI()
    setController(context)
}

private fun releaseController() {
    MediaController.releaseFuture(controllerFuture)
}

private suspend fun setController(context: Context) {
    try {
        player = controllerFuture.await()
        audioController = AudioController(player)
    } catch (t: Throwable) {
        Log.w("MainAct", "Failed to connect to MediaController", t)
        return
    }

    rememberState = RememberState(context, player)

    updateCurrentPlaylistUI()
    updateMediaMetadataUI()
    restoreApp()

    player.addListener(
        object : Player.Listener {

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                rememberState.save()
//                    binding.playerPause.toggle(isPlaying)
//                    binding.miniPlayerPause.toggle(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {}

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(EVENT_TRACKS_CHANGED)) {
                    rememberState.save()
                }
                if (events.contains(EVENT_TIMELINE_CHANGED)) {
                    updateCurrentPlaylistUI()
                }
                if (events.contains(EVENT_MEDIA_METADATA_CHANGED)) {
                    rememberState.save()
                    updateMediaMetadataUI()
                }
                if (events.contains(EVENT_MEDIA_ITEM_TRANSITION)) {
                    rememberState.save()
                }
            }
        }
    )
}

private fun restoreApp() {

}

private fun updateMediaMetadataUI() {
    if (!::player.isInitialized || player.mediaItemCount == 0) {
        // sugestao de texto carregando..
        return
    }
    //songPlayer?.updateUIWithMediaController(player)
}

private fun updateCurrentPlaylistUI() {
    if (!::player.isInitialized) {
        return
    }
    Log.d("updateCurrentPlaylistUI", "updateCurrentPlaylistUI: ${player.currentPosition}")
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(onClick: () -> Unit) {
    val libraryViewModel = MediaKit()
    val context = LocalContext.current
    val tabs = listOf("Music", "Albums", "Artists", "Playlists")
    val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = 0)

    val coroutineScope = rememberCoroutineScope().apply {

        launch {
            updateLibraryWithInCoroutine(libraryViewModel, context)
            pagerState.animateScrollToPage(0)
            try {
                initializeController(context)
                awaitCancellation()
            } finally {
                releaseController()
            }
        }
    }

    val listState = rememberLazyListState()
    var showTopAppBar by remember { mutableStateOf(true) }
    LaunchedEffect(
        key1 = remember { derivedStateOf { listState.firstVisibleItemIndex } },
        key2 = remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }) {
        val previousIndex = listState.firstVisibleItemIndex
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { currentIndex ->
                // Verifica se a lista está rolando para cima ou para baixo
                showTopAppBar = currentIndex <= previousIndex
            }
    }
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = PartiallyExpanded,
            skipHiddenState = true
        )
    )

    BottomSheetScaffold(
        sheetShape = BottomSheetDefaults.HiddenShape,
        sheetPeekHeight = 70.dp,
        scaffoldState = bottomSheetScaffoldState,
        sheetContent = {
            Column(
                modifier = when(bottomSheetScaffoldState.bottomSheetState.currentValue) {
                    Expanded -> Modifier
                        .fillMaxSize()
                        .absoluteOffset(y = (-140).dp)
                        .background(Color.Blue)
                    Hidden -> {
                        Modifier
                    }
                    PartiallyExpanded -> Modifier
                        .fillMaxSize()
                        .absoluteOffset(y = (-70).dp)
                        .background(Color.Blue)
                }
            ) {
                Text("Sheet Content")
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .absoluteOffset(y = 140.dp)
                        .background(Color.White)
                ) {
                    Text("Conteúdo do BottomSheet", modifier = Modifier.padding(16.dp))
                }
            }


        },
        topBar = {
            AnimatedVisibility(
                visible = showTopAppBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {

            }

            TopAppBar(
                title = { Text(LocalContext.current.getString(R.string.app_name)) },
                actions = {
                    Icon(
                        modifier = Modifier.padding(end = 15.dp),
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground
                    )

                    Icon(
                        modifier = Modifier.padding(end = 15.dp),
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp, bottom = 10.dp),
                selectedTabIndex = 0,
                indicator = {}, divider = {}
            ) {
                tabs.forEachIndexed { index, title ->

                    Tab(
                        modifier = if (pagerState.currentPage == index) {
                            Modifier
                                .wrapContentSize()
                                .width(90.dp)
                                .height(35.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            Modifier
                                .wrapContentSize()
                                .width(90.dp)
                                .height(35.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(MaterialTheme.colorScheme.background)
                        },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title, color = MaterialTheme.colorScheme.onBackground) }
                    )
                }
            }

            HorizontalPager(
                pageContent = {
                    when (tabs[it]) {
                        "Music" -> {
                            SongPager(libraryViewModel, lazyState = listState)
                        }

                        "Albums" -> AlbumPager(libraryViewModel, lazyState = listState)
                        "Artists" -> ArtistPager(onClick, libraryViewModel, lazyState = listState)
                        "Playlists" -> {

                        }
                    }
                },
                state = pagerState,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun GreetingPreview() {
    ComposePlayerTheme {
        App(onClick = {})
    }
}