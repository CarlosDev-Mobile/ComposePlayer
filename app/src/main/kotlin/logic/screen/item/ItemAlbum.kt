package com.carlosdev.player.logic.screen.item

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.carlosdev.player.R
import com.carlosdev.player.ui.theme.ComposePlayerTheme

@SuppressLint("CheckResult")
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ItemAlbum(
    onClick: () -> Unit,
    circular: Boolean = false,
    imageVector: Uri?,
    title: String,
    subtitle: String
) {
    val iconBackground = MaterialTheme.colorScheme.secondaryContainer
    Column(
        Modifier
            .wrapContentSize()
            .clickable {
                onClick.invoke()
            }
    ) {
        GlideImage(
            contentScale = ContentScale.Crop,
            model = imageVector,
            contentDescription = "getString(R.id.picture_of_cat)",
            modifier = Modifier
                .padding(top = 10.dp, bottom = 5.dp, start = 10.dp, end = 10.dp)

                .fillMaxWidth()
                .requiredHeight(200.dp)
                .fillMaxHeight()
                .clip(
                    if (circular)
                        CircleShape
                    else
                        RoundedCornerShape(10.dp)
                )
                .background(iconBackground),
        ) {
            it.apply {
                placeholder(R.drawable.ic_album)
                error(R.drawable.ic_album)
            }
        }

        Text(
            text = subtitle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp)
                .basicMarquee(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    showSystemUi = true, device = "id:pixel_9_pro"
)
@Composable
private fun GreetingPreview() {
    ComposePlayerTheme {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize()
        ) {
            items(100) {
                ItemAlbum(
                    imageVector = Uri.EMPTY,
                    title = "Album",
                    subtitle = "Artist",
                    onClick = {

                    }
                )
            }
        }
    }
}