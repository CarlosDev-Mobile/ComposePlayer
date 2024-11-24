package com.carlosdev.player.logic.audio.media

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.carlosdev.player.logic.audio.util.Prefs
import java.nio.charset.StandardCharsets

@androidx.annotation.OptIn(UnstableApi::class)
class RememberState(context: Context, private val player: Player) {

    private val prefs = Prefs.with(context, "RememberState")

    private fun dumpPlaylist(): MediaItemsWithStartPosition {
        val items = mutableListOf<MediaItem>()
        for (i in 0 until player.mediaItemCount) {
            items.add(player.getMediaItemAt(i))
        }
        return MediaItemsWithStartPosition(
            items, player.currentMediaItemIndex, player.currentPosition
        )
    }

    fun save() {
        val data = dumpPlaylist()
        val playerManager = PrefsListUtils.dump(
            data.mediaItems.map {
                val b = SafeDelimitedStringConcat(":")
                b.writeStringUnsafe(it.mediaId)
                b.writeUri(it.localConfiguration?.uri)
                b.writeStringSafe(it.localConfiguration?.mimeType)
                b.writeStringSafe(it.mediaMetadata.title)
                b.writeStringSafe(it.mediaMetadata.artist)
                b.writeStringSafe(it.mediaMetadata.albumTitle)
                b.writeStringSafe(it.mediaMetadata.albumArtist)
                b.writeUri(it.mediaMetadata.artworkUri)
                b.writeInt(it.mediaMetadata.trackNumber)
                b.writeInt(it.mediaMetadata.discNumber)
                b.writeInt(it.mediaMetadata.recordingYear)
                b.writeInt(it.mediaMetadata.releaseYear)
                b.writeBool(it.mediaMetadata.isBrowsable)
                b.writeBool(it.mediaMetadata.isPlayable)
                b.toString()
            })
        prefs?.putStringSet("player_first", playerManager.first)
        prefs?.writeString("player_second", playerManager.second)
        prefs?.writeInt("player_startIndex", data.startIndex)
        prefs?.writeLong("player_startPositionMs", data.startPositionMs)
        prefs?.writeBoolean("player_shuffle", player.shuffleModeEnabled)
        prefs?.writeInt("player_repeat", player.repeatMode)
    }

    fun restore(): MediaItemsWithStartPosition? {
        val playerFirst = prefs?.getStringSet("player_first", null)
        val playerSecond = prefs?.readString("player_second", null)
        val playerStartIndex = prefs?.readInt("player_startIndex", 0)
        val playerStartPositionMs = prefs?.readLong("player_startPositionMs", 0)
        if (playerSecond == null || playerFirst == null) {
            return null
        }
        return MediaItemsWithStartPosition(
            PrefsListUtils.parse(playerFirst, playerSecond)
                .map {
                    val b = SafeDelimitedStringDecat(":", it)
                    val mediaId = b.readStringUnsafe()
                    val uri = b.readUri()
                    val mimeType = b.readStringSafe()
                    val title = b.readStringSafe()
                    val artist = b.readStringSafe()
                    val album = b.readStringSafe()
                    val albumArtist = b.readStringSafe()
                    val imgUri = b.readUri()
                    val trackNumber = b.readInt()
                    val discNumber = b.readInt()
                    val recordingYear = b.readInt()
                    val releaseYear = b.readInt()
                    val isBrowsable = b.readBool()
                    val isPlayable = b.readBool()
                    MediaItem.Builder()
                        .setUri(uri)
                        .setMediaId(mediaId!!)
                        .setMimeType(mimeType)
                        .setMediaMetadata(
                            MediaMetadata
                                .Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .setAlbumTitle(album)
                                .setAlbumArtist(albumArtist)
                                .setArtworkUri(imgUri)
                                .setTrackNumber(trackNumber)
                                .setDiscNumber(discNumber)
                                .setRecordingYear(recordingYear)
                                .setReleaseYear(releaseYear)
                                .setIsBrowsable(isBrowsable)
                                .setIsPlayable(isPlayable)
                                .build()
                        )
                        .build()
                },
            playerStartIndex!!,
            playerStartPositionMs!!
        )
    }
}

private class SafeDelimitedStringConcat(private val delimiter: String) {
    private val b = StringBuilder()
    private var hadFirst = false

    private fun append(s: String?) {
        if (s?.contains(delimiter, false) == true) {
            throw IllegalArgumentException("argument must not contain delimiter")
        }
        if (hadFirst) {
            b.append(delimiter)
        } else {
            hadFirst = true
        }
        s?.let { b.append(it) }
    }

    override fun toString(): String {
        return b.toString()
    }

    fun writeStringUnsafe(s: CharSequence?) = append(s?.toString())
    fun writeBase64(b: ByteArray?) = append(b?.let { Base64.encodeToString(it, Base64.DEFAULT) })
    fun writeStringSafe(s: CharSequence?) =
        writeBase64(s?.toString()?.toByteArray(StandardCharsets.UTF_8))

    fun writeInt(i: Int?) = append(i?.toString())
    fun writeBool(b: Boolean?) = append(b?.toString())
    fun writeUri(u: Uri?) = writeStringSafe(u?.toString())
}

private class SafeDelimitedStringDecat(delimiter: String, str: String) {
    private val items = str.split(delimiter)
    private var pos = 0

    private fun read(): String? {
        return items[pos++].ifEmpty { null }
    }

    fun readStringUnsafe(): String? = read()
    fun readBase64(): ByteArray? = read()?.let { Base64.decode(it, Base64.DEFAULT) }
    fun readStringSafe(): String? = readBase64()?.toString(StandardCharsets.UTF_8)
    fun readInt(): Int? = read()?.toInt()
    fun readBool(): Boolean? = read()?.toBooleanStrict()
    fun readUri(): Uri? = Uri.parse(readStringSafe())
}

private object PrefsListUtils {
    fun parse(stringSet: Set<String>, groupStr: String): List<String> {
        val groups = groupStr.split(",")
        return stringSet.sortedBy { groups.indexOf(it.hashCode().toString()) }
    }

    fun dump(list: List<String>): Pair<Set<String>, String> {
        return Pair(list.toSet(), list.joinToString(",") { it.hashCode().toString() })
    }
}