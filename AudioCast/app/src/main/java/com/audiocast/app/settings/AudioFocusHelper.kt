package com.audiocast.app.settings

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log


class AudioFocusHelper(
    context: Context,
    private val onFocusLost: () -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            Log.d("AudioFocus", "🔇 Focus lost due to another media app")
            onFocusLost()
        }
    }

    private var audioFocusRequest: AudioFocusRequest? = null

    fun requestFocus() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest!!)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d("AudioFocus", "✅ Focus granted via AudioFocusRequest")
        } else {
            Log.w("AudioFocus", "❌ Focus NOT granted via AudioFocusRequest")
        }
    }

    fun releaseFocus() {
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
        Log.d("AudioFocus", "🔕 Focus released")
    }
}