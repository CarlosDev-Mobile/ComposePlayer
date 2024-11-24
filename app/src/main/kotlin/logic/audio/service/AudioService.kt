/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.carlosdev.player.logic.audio.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.carlosdev.player.R
import com.carlosdev.player.logic.audio.media.RememberState
import com.carlosdev.player.ui.icons.AlbumVector
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture

@OptIn(UnstableApi::class)
open class AudioService : MediaLibraryService(), MediaLibraryService.MediaLibrarySession.Callback {

    private lateinit var handler: Handler
    private lateinit var rememberState: RememberState
    private lateinit var mediaLibrarySession: MediaLibrarySession


    companion object {
        private const val NOTIFICATION_ID = 9090
        private const val CHANNEL_ID = "fahin_music"

        private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON =
            "com.fahin.music.SHUFFLE_ON"
        private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF =
            "com.fahin.music.SHUFFLE_OFF"
        private const val CUSTOM_COMMAND_REPEAT_MODE_ONE =
            "com.fahin.music.REPEAT_MODE_ONE"
        private const val CUSTOM_COMMAND_REPEAT_MODE_OFF =
            "com.fahin.music.REPEAT_MODE_OFF"
        private const val CUSTOM_COMMAND_REPEAT_MODE_ALL =
            "com.fahin.music.REPEAT_ONE_ALL"
    }

    open fun getSingleTopActivity(): PendingIntent? = null
    open fun getBackStackedActivity(): PendingIntent? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        initializeSessionAndPlayer()
        setListener(MediaSessionServiceListener())
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession
    }

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        rememberState.save()
        getBackStackedActivity()?.let { mediaLibrarySession.setSessionActivity(it) }
        mediaLibrarySession.release()
        mediaLibrarySession.player.release()
        clearListener()
        super.onDestroy()
    }

    private fun initializeSessionAndPlayer() {
        handler = Handler(mainLooper)

        val player =
            ExoPlayer.Builder(this, /*renderersFactory BETA*/)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .setSkipSilenceEnabled(false)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(), true
                )
                .build()


        mediaLibrarySession =
            MediaLibrarySession.Builder(this, player, this)
                .also { builder -> getSingleTopActivity()?.let { builder.setSessionActivity(it) } }
                .build()

        player.addListener(object : Player.Listener {

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                mediaLibrarySession.setCustomLayout(
                    ImmutableList.of(
                        getShuffleCommand(mediaLibrarySession),
                        getRepeatCommand(mediaLibrarySession)
                    )
                )
                //instance?.binding?.playerShuffle?.toggle(shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                mediaLibrarySession.setCustomLayout(
                    ImmutableList.of(
                        getShuffleCommand(mediaLibrarySession),
                        getRepeatCommand(mediaLibrarySession)
                    )
                )
                //instance?.binding?.playerRepeat?.setRepeatMode(repeatMode)
            }
        })

        rememberState = RememberState(this, player)
    }


    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: ControllerInfo
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val settable = SettableFuture.create<MediaItemsWithStartPosition>()
        handler.post {
            settable.set(rememberState.restore())
        }
        return settable
    }

    @OptIn(UnstableApi::class)
    private val customLayoutCommandButtons: List<CommandButton> =
        listOf(
            CommandButton.Builder(CommandButton.ICON_SHUFFLE_OFF)
                .setDisplayName("Shuffle")
                .setSessionCommand(
                    SessionCommand(
                        CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON,
                        Bundle.EMPTY
                    )
                )
                .build(),
            CommandButton.Builder(CommandButton.ICON_SHUFFLE_ON)
                .setDisplayName("Disable shuffling")
                .setSessionCommand(
                    SessionCommand(
                        CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF,
                        Bundle.EMPTY
                    )
                )
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_OFF)
                .setDisplayName("Disable repeat")
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REPEAT_MODE_OFF, Bundle.EMPTY))
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_ALL)
                .setDisplayName("repeat all")
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REPEAT_MODE_ALL, Bundle.EMPTY))
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_ONE)
                .setDisplayName("repeat one")
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REPEAT_MODE_ONE, Bundle.EMPTY))
                .build()
        )


    fun getRepeatCommand(session: MediaSession) = customLayoutCommandButtons[
        when (session.player.repeatMode) {
            Player.REPEAT_MODE_OFF -> 2
            Player.REPEAT_MODE_ALL -> 3
            Player.REPEAT_MODE_ONE -> 4
            else -> throw IllegalArgumentException()
        }
    ]

    private fun repeatCommand(session: MediaSession) = when (session.player.repeatMode) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
        else -> Player.REPEAT_MODE_OFF
    }

    fun getShuffleCommand(session: MediaSession?) =
        customLayoutCommandButtons[if (session!!.player.shuffleModeEnabled) 1 else 0]

    @OptIn(UnstableApi::class)
    override fun onConnect(
        session: MediaSession,
        controller: ControllerInfo,
    ): MediaSession.ConnectionResult {
        val availableSessionCommands =
            MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
        if (session.isMediaNotificationController(controller)
            || session.isAutoCompanionController(controller)
            || session.isAutomotiveController(controller)
        ) {
            // currently, all custom actions are only useful when used by notification
            // other clients hopefully have repeat/shuffle buttons like MCT does
            for (commandButton in customLayoutCommandButtons) {
                // Add custom command to available session commands.
                commandButton.sessionCommand?.let { availableSessionCommands.add(it) }
            }
        }

        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(availableSessionCommands.build())
            .setCustomLayout(
                ImmutableList.of(
                    getShuffleCommand(session),
                    getRepeatCommand(session)
                )
            )
            .build()
    }

    @OptIn(UnstableApi::class) // MediaSession.isMediaNotificationController
    override fun onCustomCommand(
        session: MediaSession,
        controller: ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        return Futures.immediateFuture(
            when (customCommand.customAction) {
                CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF -> {
                    session.player.shuffleModeEnabled = false
                    session.setCustomLayout(
                        ImmutableList.of(
                            getShuffleCommand(session),
                            getRepeatCommand(session)
                        )
                    )
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON -> {
                    session.player.shuffleModeEnabled = true
                    session.setCustomLayout(
                        ImmutableList.of(
                            getShuffleCommand(session),
                            getRepeatCommand(session)
                        )
                    )
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CUSTOM_COMMAND_REPEAT_MODE_OFF -> {
                    Log.d("Callback", "onCustomCommand: CUSTOM_COMMAND_REPEAT_MODE_OFF")
                    session.player.repeatMode = repeatCommand(session)
                    session.setCustomLayout(
                        ImmutableList.of(
                            getShuffleCommand(session),
                            getRepeatCommand(session)
                        )
                    )
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }


                CUSTOM_COMMAND_REPEAT_MODE_ALL -> {
                    session.player.repeatMode = repeatCommand(session)
                    session.setCustomLayout(
                        ImmutableList.of(
                            getShuffleCommand(session),
                            getRepeatCommand(session)
                        )
                    )
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CUSTOM_COMMAND_REPEAT_MODE_ONE -> {
                    session.player.repeatMode = repeatCommand(session)
                    session.setCustomLayout(
                        ImmutableList.of(
                            getShuffleCommand(session),
                            getRepeatCommand(session)
                        )
                    )
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                else -> {
                    SessionResult(SessionError.ERROR_NOT_SUPPORTED)
                }
            }
        )

    }

    @OptIn(UnstableApi::class) // MediaSessionService.Listener
    private inner class MediaSessionServiceListener : Listener {

        /**
         * This method is only required to be implemented on Android 12 or above when an attempt is made
         * by a media controller to resume playback when the {@link MediaSessionService} is in the
         * background.
         */
        @SuppressLint("NotificationPermission", "MissingPermission")
        override fun onForegroundServiceStartNotAllowedException() {
            if (
                Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Notification permission is required but not granted
                return
            }
            val notificationManagerCompat = NotificationManagerCompat.from(this@AudioService)
            ensureNotificationChannel(notificationManagerCompat)
            val builder =
                NotificationCompat.Builder(this@AudioService, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(getString(R.string.app_name))
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .also { builder -> getBackStackedActivity()?.let { builder.setContentIntent(it) } }
            notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun ensureNotificationChannel(notificationManagerCompat: NotificationManagerCompat) {
        if (
            Build.VERSION.SDK_INT < 26 ||
            notificationManagerCompat.getNotificationChannel(CHANNEL_ID) != null
        ) {
            return
        }

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        notificationManagerCompat.createNotificationChannel(channel)
    }
}
