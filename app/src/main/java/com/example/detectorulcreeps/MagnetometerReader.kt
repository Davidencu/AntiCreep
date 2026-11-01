package com.example.detectorulcreeps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

/** O citire a câmpului magnetic, în microtesla, pe cele 3 axe. */
data class MagneticReading(val x: Float, val y: Float, val z: Float, val timestampNanos: Long = 0L) {
    val magnitude: Float get() = sqrt(x * x + y * y + z * z)
}

/** Citește magnetometrul telefonului și emite valorile ca Flow. */
class MagnetometerReader(context: Context) {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val isAvailable: Boolean get() = sensor != null

    fun readings(): Flow<MagneticReading> = callbackFlow {
        val magnetometer = sensor
        if (magnetometer == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(
                    MagneticReading(event.values[0], event.values[1], event.values[2], event.timestamp)
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
