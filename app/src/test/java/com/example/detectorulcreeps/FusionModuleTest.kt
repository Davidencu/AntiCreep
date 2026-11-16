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

    private val suspect = NetworkTriageResult(listOf(NetworkDevice("192.168.1.9", listOf(554))))
    private val clean = NetworkTriageResult(listOf(NetworkDevice("192.168.1.1", listOf(80), isGateway = true)))
    private val incomplete = NetworkTriageResult(emptyList(), "permisiune refuzata")

    @Test
    fun `fara module fizice si fara indiciu de retea riscul este scazut`() {
        assertEquals(RiskLevel.LOW, fusion.fuse(0f, 0f, clean).level)
    }

    @Test
    fun `doar indiciul de retea inseamna risc mediu`() {
        val r = fusion.fuse(0f, 0f, suspect)
        assertEquals(RiskLevel.MEDIUM, r.level)
        assertTrue(r.networkFlag)
    }

    @Test
    fun `un modul fizic plus indiciul de retea inseamna risc ridicat`() {
        assertEquals(RiskLevel.HIGH, fusion.fuse(0.9f, 0f, suspect).level)
        assertEquals(RiskLevel.HIGH, fusion.fuse(0f, 0.9f, suspect).level)
    }

    @Test
    fun `ambele module fizice inseamna risc ridicat, cu sau fara indiciu de retea`() {
        assertEquals(RiskLevel.HIGH, fusion.fuse(0.9f, 0.9f, clean).level)
        assertEquals(RiskLevel.HIGH, fusion.fuse(0.9f, 0.9f, suspect).level)
    }

    @Test
    fun `un triaj incomplet nu coboara nivelul`() {
        assertEquals(RiskLevel.MEDIUM, fusion.fuse(0.9f, 0f, incomplete).level)
        assertEquals(RiskLevel.HIGH, fusion.fuse(0.9f, 0.9f, incomplete).level)
        assertEquals(RiskLevel.HIGH, fusion.fuse(0.9f, 0f, suspect.copy(incompleteReason = "oprit")).level)
    }

    @Test
    fun `indiciul de retea nu schimba scorul combinat`() {
        assertEquals(fusion.fuse(0.8f, 0.2f).combinedScore, fusion.fuse(0.8f, 0.2f, suspect).combinedScore, 0f)
    }

    @Test
    fun `rezultatul pastreaza starea triajului si lista suspectilor`() {
        val ok = fusion.fuse(0f, 0f, suspect)
        assertTrue(ok.networkComplete)
        assertEquals("192.168.1.9 · porturi 554", ok.networkDevices)

        val partial = fusion.fuse(0f, 0f, incomplete)
        assertFalse(partial.networkComplete)
        assertFalse(partial.networkFlag)
        assertEquals("", partial.networkDevices)

        assertFalse(fusion.fuse(0f, 0f, null).networkComplete)
    }

    @Test
    fun `scorul combinat este media ponderata si ramane in 0 1`() {
        assertEquals(0.5f, fusion.fuse(1f, 0f).combinedScore, 0.001f)
        assertEquals(1f, fusion.fuse(2f, 2f).combinedScore, 0.001f)
        val weighted = FusionModule(magneticWeight = 0.3f, opticWeight = 0.7f).fuse(1f, 0f)
        assertEquals(0.3f, weighted.combinedScore, 0.001f)
    }
}
