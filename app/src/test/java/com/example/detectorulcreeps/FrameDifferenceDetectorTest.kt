package com.example.detectorulcreeps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameDifferenceDetectorTest {
    private val width = 160
    private val height = 120
    private val detector = FrameDifferenceDetector()

    private fun frame(value: (x: Int, y: Int) -> Int): LumaFrame {
        val data = ByteArray(width * height)
        for (y in 0 until height) for (x in 0 until width) data[y * width + x] = value(x, y).toByte()
        return LumaFrame(width, height, width, data)
    }

    private fun inDisk(x: Int, y: Int, cx: Int, cy: Int, r: Int) = (x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r

    @Test
    fun `lentila care apare doar cu blitul aprins este detectata`() {
        val off = frame { _, _ -> 20 }
        val on = frame { x, y -> if (inDisk(x, y, 80, 60, 6)) 255 else 30 }
        val d = detector.detect(off, on)!!
        assertEquals(1, d.blobs.size)
        assertTrue(d.score > 0.8f)
        assertEquals(0.5f, d.blobs[0].centerX, 0.05f)
    }

    @Test
    fun `becul aprins in ambele cadre dispare din diferenta`() {
        val bulb = { x: Int, y: Int -> if (inDisk(x, y, 40, 40, 6)) 255 else 20 }
        val d = detector.detect(frame(bulb), frame(bulb))!!
        assertTrue(d.blobs.isEmpty())
        assertEquals(0f, d.score, 0f)
    }

    @Test
    fun `becul ramane ignorat iar lentila este gasita in acelasi cadru`() {
        val off = frame { x, y -> if (inDisk(x, y, 40, 40, 6)) 255 else 20 }
        val on = frame { x, y -> if (inDisk(x, y, 40, 40, 6) || inDisk(x, y, 120, 80, 6)) 255 else 30 }
        val d = detector.detect(off, on)!!
        assertEquals(1, d.blobs.size)
        assertEquals(0.75f, d.blobs[0].centerX, 0.05f)
    }

    @Test
    fun `cresterea slaba de luminozitate nu este lentila`() {
        val off = frame { _, _ -> 20 }
        val on = frame { x, y -> if (inDisk(x, y, 80, 60, 6)) 90 else 30 }
        assertTrue(detector.detect(off, on)!!.blobs.isEmpty())
    }

    @Test
    fun `cadre de marimi diferite nu se compara`() {
        val small = LumaFrame(10, 10, 10, ByteArray(100))
        assertNull(detector.detect(small, frame { _, _ -> 0 }))
    }

    @Test
    fun `diferenta nu are valori negative`() {
        val off = frame { _, _ -> 200 }
        val on = frame { _, _ -> 50 }
        val diff = differenceFrame(off, on)!!
        assertEquals(0, diff.at(10, 10))
    }
}
