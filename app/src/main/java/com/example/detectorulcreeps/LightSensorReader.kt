package com.example.detectorulcreeps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class LightLevel { DARK, DIM, BRIGHT }

/** Pragurile sunt în lucși: sub 10 lx încăperea e destul de întunecată pentru testul cu bliț. */
fun lightLevel(lux: Float): LightLevel = when {
    lux < 10f -> LightLevel.DARK
    lux < 50f -> LightLevel.DIM
    else -> LightLevel.BRIGHT
}

/** Citește senzorul de lumină ambientală (TYPE_LIGHT) ca flux de valori în lucși. */
class LightSensorReader(context: Context) {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    val isAvailable: Boolean get() = sensor != null

    fun readings(): Flow<Float> = callbackFlow {
        val light = sensor
        if (light == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values[0])
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, light, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
