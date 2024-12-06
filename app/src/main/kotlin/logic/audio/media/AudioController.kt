package com.carlosdev.player.logic.audio.media

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

class AudioController (private val player: Player) {
    private var currentMediaItems: MutableList<MediaItem>? = null

    fun player() = player

    fun mediaItems() = currentMediaItems

    fun setMediaItem(list: MutableList<MediaItem>, position: Int, startPosition: Long) {
        run {
            currentMediaItems = list
            player.setMediaItems(list, position, startPosition)
            player.prepare()
            player.playWhenReady = false
        }
    }

    fun setMediaItem(list: MutableList<MediaItem>) {
        run {
            currentMediaItems = list
            player.setMediaItems(list)
            player.prepare()
            player.playWhenReady = false
        }
    }

    private fun playItemAtIndex(index: Int = 0, position: Long = 0) {
        if (index >= 0 && index < player.mediaItemCount) {
            player.seekTo(index, position)
            player.playWhenReady = false
        } else {
            Log.e("AudioService", "Índice inválido: $index")
        }
    }

    fun playItemAtIndex(index: Int) {
        if (index >= 0 && index < player.mediaItemCount) {
            player.seekTo(index, player.currentPosition)
            player.playWhenReady = true
            player.play()
        } else {
            throw IndexOutOfBoundsException("Índice inválido: $index")
        }
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        player.shuffleModeEnabled = enabled
    }

    fun play() {
        player.playWhenReady = true
        player.play()
    }

    fun playOrPause() {
        if (player.isPlaying) {
            player.playWhenReady = false
            player.pause()
        } else {
            player.playWhenReady = true
            player.play()
        }
    }

    fun playPrevious() {
        if (player.isPlaying) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekToPreviousMediaItem()
            player.play()
        }
    }

    fun playNext() {
        player.seekToNextMediaItem()
    }
}