package com.fahin.music.audio.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor

@OptIn(UnstableApi::class)
class RendererFactory
    (context: Context, var listener: TeeAudioProcessor.AudioBufferSink) :
    DefaultRenderersFactory(context) {


    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf(TeeAudioProcessor(listener)))
            .build()
    }
}