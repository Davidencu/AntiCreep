package com.example.detectorulcreeps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionAndGuideTest {
    @Test
    fun `telefonul tinut nemiscat nu este prea rapid`() {
        val filter = MotionFilter(MotionSource.GYROSCOPE)
        repeat(50) { assertFalse(filter.process(0.05f, 0.02f, 0.01f)) }
    }

    @Test
    fun `o rotatie rapida si sustinuta declanseaza avertizarea`() {
        val filter = MotionFilter(MotionSource.GYROSCOPE)
        var fast = false
        repeat(30) { fast = filter.process(1.5f, 0.5f, 0f) }
        assertTrue(fast)
    }

    @Test
    fun `o singura tresarire nu declanseaza avertizarea`() {
        val filter = MotionFilter(MotionSource.GYROSCOPE)
        assertFalse(filter.process(3f, 0f, 0f))
        repeat(20) { filter.process(0f, 0f, 0f) }
        assertFalse(filter.process(0f, 0f, 0f))
    }

    @Test
    fun `accelerometrul are pragul lui`() {
        val filter = MotionFilter(MotionSource.ACCELEROMETER)
        var fast = false
        repeat(30) { fast = filter.process(1.0f, 0f, 0f) } // sub 1,5 m/s²
        assertFalse(fast)
        repeat(30) { fast = filter.process(3.0f, 0f, 0f) }
        assertTrue(fast)
    }

    @Test
    fun `ghidul are limitarile si sectiunea despre`() {
        val titles = guideSections.map { it.title }
        assertTrue("Limitări" in titles)
        assertTrue("Despre" in titles)
        assertTrue(guideSections.all { it.paragraphs.isNotEmpty() })
        assertFalse(guideSections.flatMap { it.paragraphs }.any { "A53" in it })
    }
}
