package com.example.detectorulcreeps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MagnetometerProcessorTest {
    private val stepNanos = 20_000_000L // 50 Hz
    private var t = 0L

    private fun feed(p: MagnetometerProcessor, magnitude: Float, seconds: Float): MagneticState {
        var last: MagneticState = MagneticState.Calibrating(0f)
        repeat((seconds * 50).toInt()) {
            t += stepNanos
            last = p.process(MagneticReading(magnitude, 0f, 0f, t))
        }
        return last
    }

    @Test
    fun `calibrarea raporteaza progres si apoi trece la scanare`() {
        val p = MagnetometerProcessor()
        val mid = feed(p, 45f, 1.5f)
        assertTrue(mid is MagneticState.Calibrating)
        assertEquals(0.5f, (mid as MagneticState.Calibrating).progress, 0.1f)
        val end = feed(p, 45f, 2f)
        assertTrue(end is MagneticState.Scanning)
        assertEquals(45f, (end as MagneticState.Scanning).baseline, 0.01f)
    }

    @Test
    fun `fundalul constant nu declanseaza anomalie`() {
        val p = MagnetometerProcessor()
        feed(p, 45f, 3.5f)
        val s = feed(p, 45.5f, 2f) as MagneticState.Scanning
        assertFalse(s.anomaly)
        assertEquals(0f, s.peakScore, 0.2f)
    }

    @Test
    fun `o crestere puternica a campului declanseaza anomalie`() {
        val p = MagnetometerProcessor()
        feed(p, 45f, 3.5f)
        val s = feed(p, 70f, 1f) as MagneticState.Scanning
        assertTrue(s.anomaly)
        assertEquals(1f, s.peakScore, 0.01f)
    }

    @Test
    fun `scorul de varf ramane dupa ce anomalia dispare`() {
        val p = MagnetometerProcessor()
        feed(p, 45f, 3.5f)
        feed(p, 70f, 1f)
        val s = feed(p, 45f, 3f) as MagneticState.Scanning
        assertFalse(s.anomaly)
        assertEquals(1f, s.peakScore, 0.01f)
    }

    @Test
    fun `reset reia calibrarea`() {
        val p = MagnetometerProcessor()
        feed(p, 45f, 3.5f)
        p.reset()
        t += stepNanos
        assertTrue(p.process(MagneticReading(50f, 0f, 0f, t)) is MagneticState.Calibrating)
    }
}
