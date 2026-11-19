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

/** Senzorul folosit pentru mișcare: giroscopul (rotație) sau, dacă lipsește, accelerometrul (deplasare). */
enum class MotionSource { GYROSCOPE, ACCELEROMETER }

/** Pragurile peste care mișcarea strică diferența dintre cadre: 0,8 rad/s la rotație, 1,5 m/s² la deplasare. */
const val GYRO_LIMIT = 0.8f
const val ACCEL_LIMIT = 1.5f

/**
 * Netezește mărimea mișcării (filtru exponențial) și spune dacă telefonul se mișcă prea repede.
 * Un singur eșantion mare (o tresărire) nu declanșează avertizarea; mișcarea trebuie să dureze puțin.
 */
class MotionFilter(private val source: MotionSource, private val alpha: Float = 0.2f) {
    var value = 0f
        private set

    private val limit = if (source == MotionSource.GYROSCOPE) GYRO_LIMIT else ACCEL_LIMIT

    fun process(x: Float, y: Float, z: Float): Boolean {
        val magnitude = sqrt(x * x + y * y + z * z)
        value += alpha * (magnitude - value)
        return value > limit
    }
}

/** Citește giroscopul (sau accelerometrul liniar) și emite true când telefonul se mișcă prea repede. */
class MotionReader(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    val source: MotionSource? = when {
        gyroscope != null -> MotionSource.GYROSCOPE
        accelerometer != null -> MotionSource.ACCELEROMETER
        else -> null
    }

    val hasGyroscope: Boolean get() = gyroscope != null

    /**
     * Unghiul total cu care s-a rotit telefonul de la pornirea fluxului, în grade: viteza unghiulară
     * din giroscop, integrată în timp. Nu emite nimic dacă telefonul nu are giroscop.
     */
    fun rotationDegrees(): Flow<Float> = callbackFlow {
        val gyro = gyroscope
        if (gyro == null) {
            close()
            return@callbackFlow
        }
        var total = 0f
        var lastTimestamp = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (lastTimestamp != 0L) {
                    val dt = (event.timestamp - lastTimestamp) / 1_000_000_000f
                    val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                    total += Math.toDegrees((sqrt(x * x + y * y + z * z) * dt).toDouble()).toFloat()
                    trySend(total)
                }
                lastTimestamp = event.timestamp
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    fun tooFast(): Flow<Boolean> = callbackFlow {
        val sensor = gyroscope ?: accelerometer
        val kind = source
        if (sensor == null || kind == null) {
            close()
            return@callbackFlow
        }
        val filter = MotionFilter(kind)
        var last: Boolean? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val fast = filter.process(event.values[0], event.values[1], event.values[2])
                if (fast != last) {
                    last = fast
                    trySend(fast)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
