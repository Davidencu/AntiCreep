package com.example.detectorulcreeps

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Starea modulului magnetic: întâi calibrare, apoi scanare. */
sealed interface MagneticState {
    /** Telefonul stă nemișcat și se măsoară nivelul de fundal (progress între 0 și 1). */
    data class Calibrating(val progress: Float) : MagneticState

    /**
     * @param baseline nivelul de fundal al încăperii (µT), componenta constantă care se scade
     * @param filtered intensitatea curentă după filtrul trece-jos (µT)
     * @param deviation abaterea de la fundal (µT)
     * @param threshold pragul de anomalie (µT)
     * @param score scorul magnetic curent, în [0, 1]
     * @param peakScore cel mai mare scor din scanarea curentă, în [0, 1]
     */
    data class Scanning(
        val baseline: Float,
        val filtered: Float,
        val deviation: Float,
        val threshold: Float,
        val anomaly: Boolean,
        val score: Float,
        val peakScore: Float,
    ) : MagneticState
}

/**
 * Prelucrează citirile magnetometrului:
 * 1. calibrare: media și abaterea standard a intensității pe [calibrationNanos];
 * 2. eliminarea componentei constante (câmpul terestru și fundalul): se lucrează cu abaterea de la medie;
 * 3. filtru trece-jos exponențial pe intensitate;
 * 4. prag = max(k * abatere standard, minMargin).
 *
 * Se folosește intensitatea (modulul vectorului), care nu depinde de orientarea telefonului.
 */
class MagnetometerProcessor(
    private val calibrationNanos: Long = 3_000_000_000L,
    private val k: Float = 3f,
    private val minMargin: Float = 3f,
    private val alpha: Float = 0.2f,
    private val minCalibrationSamples: Int = 10,
) {
    private var startNanos = -1L
    private var count = 0
    private var mean = 0.0
    private var m2 = 0.0

    private var calibrated = false
    private var baseline = 0f
    private var threshold = 0f
    private var filtered = 0f
    private var peak = 0f

    fun reset() {
        startNanos = -1L
        count = 0
        mean = 0.0
        m2 = 0.0
        calibrated = false
        peak = 0f
    }

    fun process(reading: MagneticReading): MagneticState {
        val magnitude = reading.magnitude
        if (!calibrated) return calibrate(magnitude, reading.timestampNanos)

        filtered += alpha * (magnitude - filtered)
        val deviation = abs(filtered - baseline)
        val score = min(1f, deviation / (2f * threshold))
        peak = max(peak, score)
        return MagneticState.Scanning(
            baseline = baseline,
            filtered = filtered,
            deviation = deviation,
            threshold = threshold,
            anomaly = deviation > threshold,
            score = score,
            peakScore = peak,
        )
    }

    private fun calibrate(magnitude: Float, timestampNanos: Long): MagneticState {
        if (startNanos < 0) startNanos = timestampNanos
        // Welford: media și varianța fără a păstra toate eșantioanele
        count++
        val delta = magnitude - mean
        mean += delta / count
        m2 += delta * (magnitude - mean)

        val elapsed = timestampNanos - startNanos
        if (elapsed >= calibrationNanos && count >= minCalibrationSamples) {
            val std = if (count > 1) sqrt(m2 / (count - 1)).toFloat() else 0f
            baseline = mean.toFloat()
            threshold = max(k * std, minMargin)
            filtered = baseline
            peak = 0f
            calibrated = true
            return MagneticState.Scanning(baseline, filtered, 0f, threshold, false, 0f, 0f)
        }
        return MagneticState.Calibrating(min(1f, elapsed.toFloat() / calibrationNanos))
    }
}
