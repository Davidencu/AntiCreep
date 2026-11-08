package com.example.detectorulcreeps

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotTest {
    @Test
    fun `imaginea gri este micsorata de doua ori`() {
        val frame = LumaFrame(8, 6, 8, ByteArray(8 * 6))
        val gray = frame.toGray()
        assertEquals(4, gray.width)
        assertEquals(3, gray.height)
        assertEquals(12, gray.pixels.size)
    }

    @Test
    fun `luminanta devine un pixel gri opac`() {
        val data = ByteArray(4 * 4) { 200.toByte() }
        val gray = LumaFrame(4, 4, 4, data).toGray()
        assertEquals(0xFFC8C8C8.toInt(), gray.pixels[0])
    }

    @Test
    fun `octetii de completare de la capatul randului sunt ignorati`() {
        // Rând de 4 pixeli, stocat pe 6 octeți; ultimii 2 sunt completare și au valoarea 255.
        val width = 4
        val stride = 6
        val data = ByteArray(stride * 2) { i -> if (i % stride < width) 10 else 255.toByte() }
        val gray = LumaFrame(width, 2, stride, data).toGray(step = 1)
        assertEquals(8, gray.pixels.size)
        gray.pixels.forEach { assertEquals(0xFF0A0A0A.toInt(), it) }
    }
}
