package com.titanbiosync.gym.media

import android.net.Uri

object ExerciseMediaUriResolver {
    fun resolveVideoUri(source: String, url: String): Uri {
        val u = if (source == "asset") {
            "asset:///${url.removePrefix("/")}" // es: asset:///gym_media/bench_press.mp4
        } else {
            url
        }
        return Uri.parse(u)
    }
}