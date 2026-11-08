package com.example.detectorulcreeps

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReportValidationTest {
    @Test
    fun `un nume si o nota obisnuite sunt acceptate`() {
        assertNull(validateReport("Hotel Cluj, camera 12", "Priză suspectă lângă pat"))
    }

    @Test
    fun `nota poate lipsi`() {
        assertNull(validateReport("Apartament", ""))
    }

    @Test
    fun `numele gol sau doar cu spatii este respins`() {
        assertNotNull(validateReport("", ""))
        assertNotNull(validateReport("   ", ""))
    }

    @Test
    fun `numele prea lung este respins`() {
        assertNull(validateReport("a".repeat(MAX_NAME_LENGTH), ""))
        assertNotNull(validateReport("a".repeat(MAX_NAME_LENGTH + 1), ""))
    }

    @Test
    fun `spatiile de la capete nu conteaza la lungime`() {
        assertNull(validateReport("  " + "a".repeat(MAX_NAME_LENGTH) + "  ", ""))
    }

    @Test
    fun `nota prea lunga este respinsa`() {
        assertNull(validateReport("Camera", "n".repeat(MAX_NOTE_LENGTH)))
        assertNotNull(validateReport("Camera", "n".repeat(MAX_NOTE_LENGTH + 1)))
    }
}
