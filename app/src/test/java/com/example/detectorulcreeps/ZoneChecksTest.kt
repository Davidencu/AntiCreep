package com.example.detectorulcreeps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneChecksTest {
    @Test
    fun `zonele bifate se salveaza in ordinea din lista si se citesc inapoi`() {
        val text = zonesToText(setOf("Oglinzi", "Detector de fum"))
        assertEquals("Detector de fum\nOglinzi", text)
        assertEquals(listOf("Detector de fum", "Oglinzi"), zonesFromText(text))
        assertTrue(zonesFromText("").isEmpty())
    }

    @Test
    fun `punctul care dispare la inclinare se comporta ca o lentila`() {
        val check = AngleCheck(startScore = 0.9f)
        assertNull(check.sample(5f, 0.9f, 500))
        assertNull(check.sample(12f, 0.8f, 1000))
        assertEquals(AngleVerdict.LENS_LIKE, check.sample(17f, 0.1f, 1500))
    }

    @Test
    fun `punctul care ramane la inclinare este probabil o sursa de lumina`() {
        val check = AngleCheck(startScore = 0.9f)
        check.sample(16f, 0.85f, 1000)
        assertEquals(AngleVerdict.PERSISTENT, check.sample(26f, 0.8f, 2000))
    }

    @Test
    fun `scaderea inainte de 15 grade nu conteaza`() {
        val check = AngleCheck(startScore = 0.9f)
        assertNull(check.sample(5f, 0f, 500)) // punctul a ieșit din cadru fără înclinare
        assertEquals(AngleVerdict.PERSISTENT, check.sample(26f, 0.9f, 2000))
    }

    @Test
    fun `fara inclinare suficienta verificarea nu este concludenta`() {
        val check = AngleCheck(startScore = 0.9f)
        check.sample(3f, 0.9f, 4000)
        assertEquals(AngleVerdict.INCONCLUSIVE, check.sample(6f, 0.9f, 8000))
    }

    @Test
    fun `fara punct de pornire verificarea nu poate incepe`() {
        val check = AngleCheck(startScore = 0f)
        assertFalse(check.canStart)
        assertEquals(AngleVerdict.INCONCLUSIVE, check.sample(20f, 0f, 100))
    }

    @Test
    fun `progresul urmareste inclinarea pana la 25 de grade`() {
        val check = AngleCheck(startScore = 0.9f)
        check.sample(12.5f, 0.9f, 100)
        assertEquals(0.5f, check.progress, 0.01f)
    }
}
