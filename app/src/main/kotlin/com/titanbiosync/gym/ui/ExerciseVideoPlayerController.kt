package com.titanbiosync.gym.ui

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class ExerciseVideoPlayerController(
    context: Context,
    private val playerView: PlayerView
) {
    private val player = ExoPlayer.Builder(context).build()

    init {
        playerView.player = player
    }

    fun play(mediaItem: MediaItem, autoPlay: Boolean = false, loop: Boolean = true) {
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = autoPlay
        player.repeatMode = if (loop) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
    }

    fun release() {
        playerView.player = null
        player.release()
    }
}