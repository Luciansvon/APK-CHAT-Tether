package com.tether.app.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.tether.app.R

class SoundEffectManager(context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sendSoundId: Int = soundPool.load(context, R.raw.sfx_chat_send, 1)
    private val receiveSoundId: Int = soundPool.load(context, R.raw.sfx_chat_receive, 1)

    fun playSendSound() {
        soundPool.play(sendSoundId, 0.9f, 0.9f, 1, 0, 1.0f)
    }

    fun playReceiveSound() {
        soundPool.play(receiveSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun release() {
        soundPool.release()
    }
}
