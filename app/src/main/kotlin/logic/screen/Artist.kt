package com.carlosdev.player.logic.screen

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carlosdev.player.MainActivity
import com.carlosdev.player.logic.audio.media.MediaKit
import com.carlosdev.player.logic.screen.item.ItemAlbum
import com.carlosdev.player.ui.theme.ComposePlayerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistPager(
    onClick: () -> Unit,
    viewModel: MediaKit = viewModel(),
    lazyState: LazyListState = rememberLazyListState()
) {
    val items by viewModel.artistItemList.collectAsStateWithLifecycle(emptyList())
    if (items.isEmpty()) {
        CircularProgressIndicator()
    } else {
        val i = items.sortedBy { it.title.toString() }
        val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                skipHiddenState = false,
                initialValue = Hidden
            )
        )
        val coroutineScope = rememberCoroutineScope()
        BottomSheetScaffold(
            sheetShape = BottomSheetDefaults.HiddenShape,
            sheetPeekHeight = 70.dp,
            scaffoldState = bottomSheetScaffoldState,
            sheetContent = {
                MyFragmentLayoutCompose()
            }
        ) {
            LazyVerticalGrid (
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize()
            ) {
                items(i.size) {
                    val item = i[it]
                    ItemAlbum(
                        circular = true,
                        imageVector = Uri.parse(item.artistUri),
                        title = item.title.toString(),
                        subtitle = item.artist.toString(),
                        onClick = {
                            onClick.invoke()
                        }
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    showSystemUi = true
)
@Composable
private fun GreetingPreviewArt() {
    ComposePlayerTheme {
        ArtistPager(onClick = {})
    }
}