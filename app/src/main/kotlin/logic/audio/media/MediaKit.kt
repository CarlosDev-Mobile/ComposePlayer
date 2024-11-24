package com.carlosdev.player.logic.audio.media

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import androidx.activity.result.launch
import androidx.core.database.getStringOrNull
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.carlosdev.player.logic.audio.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId

class MediaKit : ViewModel() {
    fun loadList(onListLoaded: () -> Unit) {
        viewModelScope.launch {
            // Carrega a lista aqui
            onListLoaded()
        }
    }
    //val mediaItemList: MutableStateFlow<MutableList<MediaItem>> = MutableLiveData(mutableListOf())
    private val _mediaItems = MutableStateFlow<MutableList<MediaItem>>(mutableListOf())
    val mediaItemList: StateFlow<MutableList<MediaItem>> = _mediaItems.asStateFlow()

    val albumItemList: MutableLiveData<MutableList<Utils.Album>> =
        MutableLiveData(mutableListOf())
    val albumArtistItemList: MutableLiveData<MutableList<Utils.Artist>> =
        MutableLiveData(
            mutableListOf(),
        )
    val artistItemList: MutableLiveData<MutableList<Utils.Artist>> =
        MutableLiveData(
            mutableListOf(),
        )
    val genreItemList: MutableLiveData<MutableList<Utils.Genre>> =
        MutableLiveData(
            mutableListOf(),
        )
    val dateItemList: MutableLiveData<MutableList<Utils.Date>> =
        MutableLiveData(
            mutableListOf(),
        )
    val playlistList: MutableLiveData<MutableList<Utils.Playlist>> =
        MutableLiveData(
            mutableListOf(),
        )
    val folderStructure: MutableLiveData<Utils.FileNode> =
        MutableLiveData(
            Utils.FileNode("storage", mutableListOf(), mutableListOf())
        )
    val shallowFolderStructure: MutableLiveData<Utils.FileNode> =
        MutableLiveData(
            Utils.FileNode("shallowFolder", mutableListOf(), mutableListOf())
        )

    val artistMusicItemList: MutableLiveData<MutableList<MediaItem>> =
        MutableLiveData(
            mutableListOf(),
        )
}

object Utils {

    interface Item {
        val id: Long?
        val title: String?
        val songList: List<MediaItem>
    }


    data class Album(
        val artUri: Uri?,
        override val id: Long?,
        override val title: String?,
        val artist: String?,
        val albumYear: Int?,
        override val songList: List<MediaItem>,
    ) : Item

    data class Artist(
        override val id: Long?,
        override val title: String?,
        val artist: String?, // MODIFER
        val artistUri: String?,
        override val songList: List<MediaItem>,
        val albumList: List<Album>
    ) : Item


    data class Genre(
        override val id: Long?,
        override val title: String?,
        override val songList: List<MediaItem>,
    ) : Item


    data class Date(
        override val id: Long,
        override val title: String?,
        override val songList: List<MediaItem>,
    ) : Item


    open class Playlist(
        override val id: Long,
        override var title: String?,
        override val songList: MutableList<MediaItem>
    ) : Item

    @SuppressLint("ParcelCreator")
    data class Lyric(
        val timeStamp: Long = 0,
        val content: String = "",
        val isTranslation: Boolean = false
    ) : Parcelable {
        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {

        }
    }

    class RecentlyAdded(id: Long, songList: List<MediaItem>) : Playlist(
        id, null, songList
            .sortedByDescending { it.mediaMetadata.extras?.getLong("AddDate") ?: 0 }.toMutableList()
    ) {
        private val rawList: List<MediaItem> = super.songList
        private var filteredList: List<MediaItem>? = null
        var minAddDate: Long = 0
            set(value) {
                if (field != value) {
                    field = value
                    filteredList = null
                }
            }
        override val songList: MutableList<MediaItem>
            get() {
                if (filteredList == null) {
                    filteredList = rawList.filter {
                        (it.mediaMetadata.extras?.getLong("AddDate") ?: 0) >= minAddDate
                    }
                }
                return filteredList!!.toMutableList()
            }
    }

    data class LibraryStoreClass(
        val songList: MutableList<MediaItem>,
        val albumList: MutableList<Album>,
        val albumArtistList: MutableList<Artist>,
        val artistList: MutableList<Artist>,
        val genreList: MutableList<Genre>,
        val dateList: MutableList<Date>,
        val playlistList: MutableList<Playlist>,
        val folderStructure: FileNode,
        val shallowFolder: FileNode
    )

    data class FileNode(
        val folderName: String,
        val folderList: MutableList<FileNode>,
        val songList: MutableList<MediaItem>,
    )

    private fun handleMediaItem(mediaItem: MediaItem, path: String, rootNode: FileNode) {
        val rootFolderIndex = path.indexOf('/', 1)
        if (rootFolderIndex != -1) {
            val folderName = path.substring(1, rootFolderIndex)
            val remainingPath = path.substring(rootFolderIndex)
            val existingFolder = rootNode.folderList.find { it.folderName == folderName }
            if (existingFolder != null) {
                handleMediaItem(mediaItem, remainingPath, existingFolder)
            } else {
                val newFolder = FileNode(folderName = folderName, mutableListOf(), mutableListOf())
                rootNode.folderList.add(newFolder)
                handleMediaItem(mediaItem, remainingPath, newFolder)
            }
        } else {
            rootNode.songList.add(mediaItem)
        }
    }

    private fun handleShallowMediaItem(
        mediaItem: MediaItem,
        path: String,
        shallowFolder: FileNode,
        folderArray: MutableList<String>
    ) {
        val lastFolderName = path.substringBeforeLast("/").substringAfterLast("/")
        val existingFolder = shallowFolder.folderList.find { it.folderName == lastFolderName }
        if (existingFolder == null) {
            val newFolder = FileNode(folderName = lastFolderName, mutableListOf(), mutableListOf())
            newFolder.songList.add(mediaItem)
            shallowFolder.folderList.add(newFolder)
            folderArray.add(
                path.substringAfter('/').substringAfter('/').substringAfter('/').substringAfter('/')
                    .substringBeforeLast('/')
            )
        } else {
            existingFolder.songList.add(mediaItem)
        }
    }

    private fun handleBlacklistFolder(
        path: String,
        shallowFolder: FileNode,
        folderArray: MutableList<String>
    ) {
        val lastFolderName = path.substringBeforeLast("/").substringAfterLast("/")
        val existingFolder = shallowFolder.folderList.find { it.folderName == lastFolderName }
        if (existingFolder == null) {
            folderArray.add(
                path.substringAfter('/').substringAfter('/').substringAfter('/').substringAfter('/')
                    .substringBeforeLast('/')
            )
        }
    }

    private val formatCollection = mutableListOf(
        "audio/x-wav",
        "audio/ogg",
        "audio/aac",
        "audio/midi"
    )

    private fun getAllSongs(context: Context): LibraryStoreClass {
        val folderArray: MutableList<String> = mutableListOf()
        var selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        for (i in formatCollection) {
            selection = "$selection or ${MediaStore.Audio.Media.MIME_TYPE} = '$i'"
        }
        val projection =
            arrayListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.SIZE,
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    add(MediaStore.Audio.Media.SIZE)
                    add(MediaStore.Audio.Media.GENRE)
                    add(MediaStore.Audio.Media.GENRE_ID)
                    add(MediaStore.Audio.Media.CD_TRACK_NUMBER)
                    add(MediaStore.Audio.Media.COMPILATION)
                    add(MediaStore.Audio.Media.COMPOSER)
                    add(MediaStore.Audio.Media.DATE_TAKEN)
                    add(MediaStore.Audio.Media.WRITER)
                    add(MediaStore.Audio.Media.DISC_NUMBER)
                    add(MediaStore.Audio.Media.AUTHOR)
                }
            }.toTypedArray()
        val prefs = Prefs.with(context)
        val limitValue = prefs!!.readInt(
            "mediastore_filter",
            100
        )
        val root = FileNode(folderName = "storage", mutableListOf(), mutableListOf())
        val shallowRoot = FileNode(folderName = "shallow", mutableListOf(), mutableListOf())

        val songs = mutableListOf<MediaItem>()
        val albumMap = mutableMapOf<Pair<Long?, String?>, MutableList<MediaItem>>()
        val artistMap = mutableMapOf<Pair<Long?, String?>, MutableList<MediaItem>>()
        val albumArtistMap = mutableMapOf<String?, MutableList<MediaItem>>()
        val genreMap = mutableMapOf<Pair<Long?, String?>, MutableList<MediaItem>>()
        val dateMap = mutableMapOf<Int?, MutableList<MediaItem>>()
        val cursor =
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null,
            )

        cursor?.use {
            val size = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumArtistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val yearColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val discNumberColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                it.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER) else null
            val trackNumberColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val genreColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE) else null
            val genreIdColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE_ID) else null
            val cdTrackNumberColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.CD_TRACK_NUMBER) else null
            val compilationColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPILATION) else null
            val dateTakenColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_TAKEN) else null
            val composerColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER) else null
            val writerColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.WRITER) else null
            val authorColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.AUTHOR) else null
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addDateColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val modifiedDateColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)


            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                    .let { v -> if (v == "<unknown>") null else v }
                val album = it.getStringOrNull(albumColumn)
                val albumArtist =
                    it.getString(albumArtistColumn)
                        ?: null
                val path = it.getString(pathColumn)
                val year = it.getInt(yearColumn).let { v -> if (v == 0) null else v }
                val albumId = it.getLong(albumIdColumn)
                val artistId = it.getLong(artistIdColumn)
                val mimeType = it.getString(mimeTypeColumn)
                var discNumber = discNumberColumn?.let { col -> it.getInt(col) }
                var trackNumber = it.getInt(trackNumberColumn)
                val duration = it.getLong(durationColumn)
                val cdTrackNumber = cdTrackNumberColumn?.let { col -> it.getStringOrNull(col) }
                val compilation = compilationColumn?.let { col -> it.getStringOrNull(col) }
                val dateTaken = dateTakenColumn?.let { col -> it.getStringOrNull(col) }
                val composer = composerColumn?.let { col -> it.getStringOrNull(col) }
                val writer = writerColumn?.let { col -> it.getStringOrNull(col) }
                val author = authorColumn?.let { col -> it.getStringOrNull(col) }
                val genre = genreColumn?.let { col -> it.getStringOrNull(col) }
                val genreId = genreIdColumn?.let { col -> it.getLong(col) }
                val addDate = it.getLong(addDateColumn)
                val modifiedDate = it.getLong(modifiedDateColumn)
                val dateTakenParsed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dateTaken?.toLongOrNull()?.let { it1 -> Instant.ofEpochMilli(it1) }
                        ?.atZone(ZoneId.systemDefault())
                        ?.let { time -> Pair(Pair(time.dayOfMonth, time.monthValue), time.year) }
                } else null


                val artworkUri = Uri.parse("content://media/external/audio/albumart")
                val imgUri =
                    ContentUris.withAppendedId(
                        artworkUri,
                        albumId,
                    )

                if (trackNumber >= 1000) {
                    discNumber = trackNumber / 1000
                    trackNumber %= 1000
                }

                if (duration >= limitValue * 1000 &&
                    !prefs.readBoolean(
                        "folderFilter_${
                            path.substringAfter('/').substringAfter('/').substringAfter('/')
                                .substringAfter('/').substringBeforeLast('/')
                        }", false
                    )
                ) {
                    songs.add(
                        MediaItem
                            .Builder()
                            .setUri(Uri.fromFile(File(path)))
                            .setMediaId(id.toString())
                            .setMimeType(mimeType)
                            .setMediaMetadata(
                                MediaMetadata
                                    .Builder()
                                    .setIsBrowsable(false)
                                    .setIsPlayable(true)
                                    .setTitle(title)
                                    .setWriter(writer)
                                    .setCompilation(compilation)
                                    .setComposer(composer)
                                    .setArtist(artist)
                                    .setAlbumTitle(album)
                                    .setAlbumArtist(albumArtist)
                                    .setArtworkUri(imgUri)
                                    .setTrackNumber(trackNumber)
                                    .setDiscNumber(discNumber)
                                    .setGenre(genre)
                                    .setRecordingDay(dateTakenParsed?.first?.first)
                                    .setRecordingMonth(dateTakenParsed?.first?.second)
                                    .setRecordingYear(dateTakenParsed?.second)
                                    .setReleaseYear(year)
                                    .setExtras(Bundle().apply {
                                        putString("Path", path)
                                        putLong("ArtistId", artistId)
                                        putLong("AlbumId", albumId)
                                        if (genreId != null) {
                                            putLong("GenreId", genreId)
                                        }
                                        putInt("Size", size)
                                        putString("Author", author)
                                        putLong("AddDate", addDate)
                                        putLong("Duration", duration)
                                        putLong("ModifiedDate", modifiedDate)
                                        putString("MimeType", mimeType)
                                        cdTrackNumber?.toIntOrNull()
                                            ?.let { it1 -> putInt("CdTrackNumber", it1) }
                                    })
                                    .build(),
                            ).build(),

                        )

                    albumMap.getOrPut(Pair(albumId, album)) { mutableListOf() }.add(songs.last())
                    artistMap.getOrPut(Pair(artistId, artist)) { mutableListOf() }.add(songs.last())
                    albumArtistMap.getOrPut(albumArtist) { mutableListOf() }.add(songs.last())
                    genreMap.getOrPut(Pair(genreId, genre)) { mutableListOf() }.add(songs.last())
                    dateMap.getOrPut(year) { mutableListOf() }.add(songs.last())
                    handleMediaItem(songs.last(), path.toString(), root)
                    handleShallowMediaItem(songs.last(), path.toString(), shallowRoot, folderArray)
                }
                handleBlacklistFolder(path, shallowRoot, folderArray)
            }
        }
        cursor?.close()

        val albumList = albumMap.entries.map { (key, value) ->
            val albumArtist = value.first().mediaMetadata.albumArtist
                ?: value.first().mediaMetadata.artist?.toString()
            Album(
                value.first().mediaMetadata.artworkUri,
                key.first, key.second, albumArtist?.toString(),
                value.first().mediaMetadata.releaseYear, value
            )
        }.toMutableList()
        val artistList = artistMap.entries.map { (cat, songs) ->
            Artist(cat.first, cat.second, songs.firstOrNull()?.mediaMetadata?.artist.toString(), songs.firstOrNull()?.mediaMetadata?.artworkUri.toString(), songs,  albumList.filter { cat.second == it.artist })
        }.toMutableList()
        val albumArtistList = albumArtistMap.entries.map { (cat, songs) ->
            val at = artistList.find { it.title == cat }
            Artist(at?.id, cat, songs.firstOrNull()?.mediaMetadata?.artist.toString(), songs.firstOrNull()?.mediaMetadata?.artworkUri.toString(), songs,at?.albumList ?: mutableListOf())
        }.toMutableList()
        val genreList = genreMap.entries.map { (cat, songs) ->
            Genre(cat.first, cat.second, songs)
        }.toMutableList()
        val dateList = dateMap.entries.mapIndexed { index, (cat, songs) ->
            Date(index.toLong(), cat?.toString(), songs)
        }.toMutableList()

        prefs.putStringSet("folderArray", folderArray.toSet())

        return LibraryStoreClass(
            songs,
            albumList,
            albumArtistList,
            artistList,
            genreList,
            dateList,
            getPlaylists(context, songs),
            root,
            shallowRoot
        )
    }

    private fun getPlaylists(
        context: Context,
        songList: MutableList<MediaItem>
    ): MutableList<Playlist> {
        val playlists = mutableListOf<Playlist>()
        playlists.add(
            RecentlyAdded(
                -1,
                songList.toMutableList()
            )
                .apply {
                    minAddDate = (System.currentTimeMillis() / 1000) - (2 * 7 * 24 * 60 * 60)
                })

        val contentResolver: ContentResolver = context.contentResolver

        val playlistUri: Uri =
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists._ID,
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.NAME
        )

        val cursor = contentResolver.query(playlistUri, projection, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val playlistId = it.getLong(
                    it.getColumnIndexOrThrow(
                        @Suppress("DEPRECATION") MediaStore.Audio.Playlists._ID
                    )
                )
                val playlistName = it.getString(
                    it.getColumnIndexOrThrow(
                        @Suppress("DEPRECATION") MediaStore.Audio.Playlists.NAME
                    )
                ).ifEmpty { null }

                val songs = getSongsInPlaylist(contentResolver, playlistId, songList)

                val playlist = Playlist(playlistId, playlistName, songs.toMutableList())
                playlists.add(playlist)
            }
        }

        cursor?.close()

        if (playlists.none { it.title == "gramophone_favourite" }) {
            val values = ContentValues()
            @Suppress("DEPRECATION")
            values.put(MediaStore.Audio.Playlists.NAME, "gramophone_favourite")

            @Suppress("DEPRECATION")
            values.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis())

            values.put(
                @Suppress("DEPRECATION")
                MediaStore.Audio.Playlists.DATE_MODIFIED,
                System.currentTimeMillis()
            )


            val resolver: ContentResolver = contentResolver

            @Suppress("DEPRECATION")
            val favPlaylistUri =
                resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values)
            if (favPlaylistUri != null) {
                val playlistId = favPlaylistUri.lastPathSegment!!.toLong()
                playlists.add(
                    Playlist(
                        playlistId,
                        "fs",
                        mutableListOf()
                    )
                )
            }
        } else {
            playlists.first { it.title == "gramophone_favourite" }.title =
                "context.getString(R.string.playlist_favourite"
        }

        return playlists
    }

    private fun getSongsInPlaylist(
        contentResolver: ContentResolver,
        playlistId: Long,
        songList: MutableList<MediaItem>
    ): List<MediaItem> {
        val songs = mutableListOf<MediaItem>()

        val uri = @Suppress("DEPRECATION") MediaStore.Audio
            .Playlists.Members.getContentUri("external", playlistId)

        val projection = arrayOf(
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.AUDIO_ID,
        )

        val cursor = contentResolver.query(
            uri, projection, null, null,
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val audioId = it.getLong(
                    it.getColumnIndexOrThrow(
                        @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.AUDIO_ID
                    )
                )

                val song = songList.find { it1 ->
                    it1.mediaId.toLong() == audioId
                }
                if (song != null) {
                    songs.add(song)
                }
            }
        }

        cursor?.close()
        return songs
    }

    suspend fun updateLibraryWithInCoroutine(libraryViewModel: MediaKit, context: Context) {
        val pairObject = getAllSongs(context)
        withContext(Dispatchers.Main) {
            libraryViewModel.mediaItemList.value.addAll(pairObject.songList)
            libraryViewModel.albumItemList.value = pairObject.albumList
            libraryViewModel.artistItemList.value = pairObject.artistList
            libraryViewModel.albumArtistItemList.value = pairObject.albumArtistList
            libraryViewModel.genreItemList.value = pairObject.genreList
            libraryViewModel.dateItemList.value = pairObject.dateList
            libraryViewModel.playlistList.value = pairObject.playlistList
            libraryViewModel.folderStructure.value = pairObject.folderStructure
            libraryViewModel.shallowFolderStructure.value = pairObject.shallowFolder
        }
    }

}