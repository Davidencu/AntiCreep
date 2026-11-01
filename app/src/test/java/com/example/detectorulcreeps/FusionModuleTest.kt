package com.example.detectorulcreeps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FusionModuleTest {
    private val fusion = FusionModule()

    @Test
    fun `niciun modul semnaleaza inseamna risc scazut`() {
        val r = fusion.fuse(0.1f, 0.2f)
        assertEquals(RiskLevel.LOW, r.level)
        assertFalse(r.magneticFlag || r.opticFlag)
    }

    @Test
    fun `doar modulul magnetic inseamna risc mediu`() {
        val r = fusion.fuse(0.9f, 0.1f)
        assertEquals(RiskLevel.MEDIUM, r.level)
        assertTrue(r.magneticFlag)
        assertFalse(r.opticFlag)
    }

    @Test
    fun `doar modulul optic inseamna risc mediu`() {
        assertEquals(RiskLevel.MEDIUM, fusion.fuse(0.1f, 0.8f).level)
    }

    @Test
    fun `ambele module inseamna risc ridicat`() {
        assertEquals(RiskLevel.HIGH, fusion.fuse(0.7f, 0.6f).level)
    }

    @Test
    fun `scorul combinat este media ponderata si ramane in 0 1`() {
        assertEquals(0.5f, fusion.fuse(1f, 0f).combinedScore, 0.001f)
        assertEquals(1f, fusion.fuse(2f, 2f).combinedScore, 0.001f)
        val weighted = FusionModule(magneticWeight = 0.3f, opticWeight = 0.7f).fuse(1f, 0f)
        assertEquals(0.3f, weighted.combinedScore, 0.001f)
    }
}
