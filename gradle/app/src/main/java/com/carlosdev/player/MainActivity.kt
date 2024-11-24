package com.carlosdev.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION
import androidx.media3.common.Player.EVENT_MEDIA_METADATA_CHANGED
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.EVENT_TRACKS_CHANGED
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.rememberAsyncImagePainter
import com.carlosdev.player.logic.audio.media.AudioController
import com.carlosdev.player.logic.audio.media.MediaKit
import com.carlosdev.player.logic.audio.media.RememberState
import com.carlosdev.player.logic.audio.media.Utils.updateLibraryWithInCoroutine
import com.carlosdev.player.logic.audio.service.AudioService
import com.carlosdev.player.ui.icons.AlbumVector
import com.carlosdev.player.ui.theme.ComposePlayerTheme
import com.carlosdev.player.ui.util.enableFullyEdgeToEdge
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    lateinit var audioController: AudioController
    private lateinit var player: MediaController
    private lateinit var remenberState: RememberState
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    val libraryViewModel: MediaKit by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableFullyEdgeToEdge()
        setContent {
            ComposePlayerTheme {
                App()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            updateLibraryWithInCoroutine(libraryViewModel, applicationContext)
        }

        lifecycleScope.launch {

            try {
                initializeController()
                awaitCancellation()
            } finally {
                releaseController()
            }
        }
    }
    
    private suspend fun initializeController() {
        controllerFuture =
            MediaController.Builder(
                this,
                SessionToken(this, ComponentName(this, AudioService::class.java))
            ).buildAsync()

        updateMediaMetadataUI()
        setController()
    }

    private fun releaseController() {
        MediaController.releaseFuture(controllerFuture)
    }
    
    private suspend fun setController() {
        try {
            player = controllerFuture.await()
            audioController = AudioController(player)
        } catch (t: Throwable) {
            Log.w("MainAct", "Failed to connect to MediaController", t)
            return
        }

        remenberState = RememberState(applicationContext, player)

        updateCurrentPlaylistUI()
        updateMediaMetadataUI()
        restoreApp()

        player.addListener(
            object : Player.Listener {

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    remenberState.save()
//                    binding.playerPause.toggle(isPlaying)
//                    binding.miniPlayerPause.toggle(isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {}

                override fun onEvents(player: Player, events: Player.Events) {
                    if (events.contains(EVENT_TRACKS_CHANGED)) {
                        remenberState.save()
                    }
                    if (events.contains(EVENT_TIMELINE_CHANGED)) {
                        updateCurrentPlaylistUI()
                    }
                    if (events.contains(EVENT_MEDIA_METADATA_CHANGED)) {
                        remenberState.save()
                        updateMediaMetadataUI()
                    }
                    if (events.contains(EVENT_MEDIA_ITEM_TRANSITION)) {
                        remenberState.save()
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

    override fun onDestroy() {
        releaseController()
        super.onDestroy()
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun App() {
    val tabs = listOf("Music", "Albums", "Artist", "Playlists")
    val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = 0)

    val selectedTabIndex by remember { derivedStateOf { pagerState.currentPage } }
    val coroutineScope = rememberCoroutineScope().apply {
        launch {
            pagerState.animateScrollToPage(0)
        }
    }
    Scaffold(
        topBar = {
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
                    val tabColor = if (selectedTabIndex == index) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.background
                    }

                    Tab(
                        modifier = Modifier
                            .wrapContentSize()
                            .width(90.dp)
                            .height(35.dp)
                            .drawBehind {
                                drawRoundRect(
                                    cornerRadius = CornerRadius(10.dp.toPx()),
                                    color = tabColor
                                )
                            },
                        selected = selectedTabIndex == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title, color = MaterialTheme.colorScheme.onBackground) }
                    )
                }
            }
            val act = MainActivity()
            val (isPagerVisible, setPagerVisible) = remember { mutableStateOf(false) }

            act.libraryViewModel.loadList {
                setPagerVisible(true)
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->

                when (page) {
                    0 -> {
                        MusicPager()
                    }
                    1 -> Text(text = "Page $selectedTabIndex")
                    2 -> Text(text = "Page $selectedTabIndex")
                    3 -> Text(text = "Page $selectedTabIndex")
                }
            }

        }
    }
}

@Composable
fun MusicPager() {
    val act = LocalContext.current as MainActivity
    val mediaList by act.libraryViewModel.mediaItemList.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.padding(horizontal = 0.dp)) {
            items(mediaList.size) {
                val item = mediaList[it]
                ItemMusic(
                    imageVector = item.mediaMetadata.artworkUri!!,
                    title = item.mediaMetadata.title.toString(),
                    subtitle = item.mediaMetadata.artist.toString(),
                    onClick = {
                        act.audioController.setMediaItem(mediaList)
                        act.audioController.playItemAtIndex(it)
                    }
                )
            }
        }
    }
}

@Composable
fun ItemMusic(onClick: () -> Unit ,imageVector: Uri, title: String, subtitle: String) {
    val iconBackground = MaterialTheme.colorScheme.secondaryContainer
    Column (Modifier.clickable {
        onClick.invoke()
    }){
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = {
                Image(
                    painter = rememberAsyncImagePainter(model = imageVector),
                    contentDescription = "Localized description",
                    Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .drawBehind {
                            drawRoundRect(
                                cornerRadius = CornerRadius(10.dp.toPx()),
                                color = iconBackground
                            )
                        }
                )
            }
        )
        HorizontalDivider()
    }
}

@Preview(showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun GreetingPreview() {
    ComposePlayerTheme {
        App()
    }
}