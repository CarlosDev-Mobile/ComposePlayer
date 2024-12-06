package com.carlosdev.player.logic.screen.item

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.carlosdev.player.R

@SuppressLint("CheckResult")
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ItemMusic(onClick: () -> Unit, imageVector: Uri?, title: String, subtitle: String) {
    val iconBackground = MaterialTheme.colorScheme.secondaryContainer
    Column(Modifier.clickable {
        onClick.invoke()
    }) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = {

                GlideImage(
                    contentScale = ContentScale.Crop,
                    model = imageVector,
                    contentDescription = "getString(R.id.picture_of_cat)",
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBackground),
                ) {
                    it.apply {
                        placeholder(R.drawable.ic_album)
                        error(R.drawable.ic_album)
                    }
                }
            }
        )
    }
}