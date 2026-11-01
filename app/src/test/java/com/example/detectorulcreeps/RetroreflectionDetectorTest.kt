package com.example.detectorulcreeps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroreflectionDetectorTest {
    private val width = 160
    private val height = 120
    private val detector = RetroreflectionDetector()

    private fun frame(draw: (x: Int, y: Int) -> Boolean): LumaFrame {
        val data = ByteArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            data[y * width + x] = (if (draw(x, y)) 255 else 20).toByte()
        }
        return LumaFrame(width, height, width, data)
    }

    private fun disk(cx: Int, cy: Int, r: Int) = { x: Int, y: Int ->
        (x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r
    }

    @Test
    fun `cadru intunecat nu da detectie`() {
        val d = detector.detect(frame { _, _ -> false })
        assertEquals(0f, d.score, 0f)
        assertTrue(d.blobs.isEmpty())
    }

    @Test
    fun `un punct luminos rotund si mic este detectat`() {
        val d = detector.detect(frame(disk(80, 60, 6)))
        assertEquals(1, d.blobs.size)
        assertTrue(d.score > 0.8f)
        assertEquals(0.5f, d.blobs[0].centerX, 0.05f)
        assertEquals(0.5f, d.blobs[0].centerY, 0.05f)
    }

    @Test
    fun `o zona luminoasa mare este respinsa`() {
        val d = detector.detect(frame { x, y -> x in 40..90 && y in 30..80 })
        assertEquals(0f, d.score, 0f)
    }

    @Test
    fun `o dunga alungita este respinsa`() {
        val d = detector.detect(frame { x, y -> x in 30..90 && y in 60..62 })
        assertEquals(0f, d.score, 0f)
    }

    @Test
    fun `mai multe puncte reduc scorul`() {
        val one = detector.detect(frame(disk(80, 60, 6))).score
        val many = detector.detect(
            frame { x, y -> disk(30, 30, 6)(x, y) || disk(80, 60, 6)(x, y) || disk(130, 90, 6)(x, y) }
        )
        assertEquals(3, many.blobs.size)
        assertTrue(many.score < one)
    }

    @Test
    fun `rotirea punctelor muta coordonatele corect`() {
        val b = Blob(0.2f, 0.1f, 0.05f, 0.03f, 1f)
        val r90 = b.rotated(90)
        assertEquals(0.9f, r90.centerX, 0.001f)
        assertEquals(0.2f, r90.centerY, 0.001f)
        assertEquals(0.03f, r90.width, 0.001f)
        val r180 = b.rotated(180)
        assertEquals(0.8f, r180.centerX, 0.001f)
        assertEquals(0.9f, r180.centerY, 0.001f)
        val r270 = b.rotated(270)
        assertEquals(0.1f, r270.centerX, 0.001f)
        assertEquals(0.8f, r270.centerY, 0.001f)
        assertEquals(b, b.rotated(0))
    }

    @Test
    fun `procesorul pastreaza scorul de varf`() {
        val p = OpticProcessor()
        repeat(20) { p.process(OpticDetection(1f, emptyList())) }
        val s = (1..30).map { p.process(OpticDetection(0f, emptyList())) }.last()
        assertTrue(s.filteredScore < 0.05f)
        assertTrue(s.peakScore > 0.9f)
    }
}
