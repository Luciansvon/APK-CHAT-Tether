package com.tether.app.core.sensor

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticResonanceEngine(private val context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Denyut Jantung Lembut (Lub-Dub)
    fun playHeartbeatResonance() {
        if (!vibrator.hasVibrator()) return

        val timings = longArrayOf(0, 50, 120, 70) // Diam, Lub, jeda, Dub
        val amplitudes = intArrayOf(0, 180, 0, 240)

        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    // Sentuhan Langsung Pasangan
    fun playTouchPulse() {
        if (!vibrator.hasVibrator()) return

        val effect = VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    // Tarikan Benang Tether (Electric Tug! ⚡)
    fun playTugHaptic() {
        if (!vibrator.hasVibrator()) return

        val timings = longArrayOf(0, 45, 60, 45, 60, 120) // 3-stage crisp rhythmic pull
        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }
}
