package com.tether.app.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProximitySensorManager(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isNearEar = MutableStateFlow(false)
    val isNearEar = _isNearEar.asStateFlow()

    fun startListening() {
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        resetAudioRoute()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_PROXIMITY) return

        val distance = event.values[0]
        val maxRange = proximitySensor?.maximumRange ?: 5f
        val isNear = distance < maxRange && distance < 4.0f // Kurang dari 4cm = dekat telinga

        _isNearEar.value = isNear
        routeAudioToEarpiece(isNear)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun routeAudioToEarpiece(toEarpiece: Boolean) {
        try {
            if (toEarpiece) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
            } else {
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.isSpeakerphoneOn = true
            }
        } catch (_: Exception) {}
    }

    private fun resetAudioRoute() {
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = true
        } catch (_: Exception) {}
    }
}
