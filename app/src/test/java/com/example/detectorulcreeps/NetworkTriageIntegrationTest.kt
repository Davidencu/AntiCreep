package com.example.detectorulcreeps

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import javax.net.SocketFactory

/**
 * Teste de integrare pe socket-uri reale (bucla locală 127.0.0.1), fără telefon:
 * sondă reală, baleiere, îmbinare cu serviciile mDNS, indiciul de rețea și fuziune, într-un singur lanț.
 * Doar Network.socketFactory și NsdManager sunt specifice Android și nu sunt acoperite aici.
 */
class NetworkTriageIntegrationTest {
    private val loopback = "127.0.0.1"
    private val servers = mutableListOf<ServerSocket>()

    @Before
    fun checkLoopback() {
        // Pe un sistem fără bucla locală nu are sens să rulăm testele.
        assumeTrue(runCatching { ServerSocket(0, 1, InetAddress.getByName(loopback)).close() }.isSuccess)
    }

    @After
    fun closeServers() {
        servers.forEach { runCatching { it.close() } }
    }

    private fun listen(port: Int = 0): ServerSocket =
        ServerSocket(port, 50, InetAddress.getByName(loopback)).also { servers += it }

    /** Deschide un port de cameră real; sare testul dacă toate sunt ocupate sau cer drepturi de root. */
    private fun listenOnCameraPort(): ServerSocket {
        for (port in listOf(8081, 8080, 8554, 80, 554)) {
            runCatching { listen(port) }.getOrNull()?.let { return it }
        }
        assumeTrue("niciun port de cameră nu este liber", false)
        error("neatins")
    }

    private fun closedPort(): Int = ServerSocket(0, 1, InetAddress.getByName(loopback)).use { it.localPort }

    private fun prober(vararg ports: Int) = SocketHostProber(SocketFactory.getDefault(), ports = ports.toList())

    @Test
    fun `sonda gaseste un port deschis`() = runBlocking {
        val server = listen()
        val probe = prober(server.localPort).probe(loopback)
        assertTrue(probe.alive)
        assertEquals(listOf(server.localPort), probe.openPorts)
    }

    @Test
    fun `un port inchis nu este raportat ca deschis`() = runBlocking {
        val open = listen()
        val closed = closedPort()
        val probe = prober(closed, open.localPort).probe(loopback)
        assertTrue(probe.alive)
        assertEquals(listOf(open.localPort), probe.openPorts)
    }

    @Test
    fun `gazda este activa chiar daca niciun port nu este deschis`() = runBlocking {
        val probe = prober(closedPort()).probe(loopback)
        assertTrue(probe.alive)
        assertTrue(probe.openPorts.isEmpty())
    }

    @Test
    fun `porturile deschise se gasesc toate, indiferent de ordine`() = runBlocking {
        val a = listen()
        val b = listen()
        val probe = prober(b.localPort, closedPort(), a.localPort).probe(loopback)
        assertEquals(setOf(a.localPort, b.localPort), probe.openPorts.toSet())
    }

    @Test
    fun `un port de cameră deschis duce de la sonda reala la risc mediu`() = runBlocking {
        val camera = listenOnCameraPort()
        val steps = sweep(
            hosts = listOf(loopback),
            prober = SocketHostProber(SocketFactory.getDefault()),
            gateway = null,
        ).toList()

        val devices = mergeDevices(steps.last().devices, emptyList(), gateway = null, self = null)
        assertEquals(1, devices.size)
        assertTrue(camera.localPort in devices[0].openPorts)
        assertTrue(devices[0].suspect)

        val triage = NetworkTriageResult(devices)
        assertTrue(triage.hint)
        val fused = FusionModule().fuse(0f, 0f, triage)
        assertEquals(RiskLevel.MEDIUM, fused.level)
        assertTrue(fused.networkFlag)
        assertTrue(fused.networkDevices.startsWith(loopback))
    }

    @Test
    fun `acelasi dispozitiv, ca router, nu ridica riscul`() = runBlocking {
        listenOnCameraPort()
        val steps = sweep(listOf(loopback), SocketHostProber(SocketFactory.getDefault()), gateway = loopback).toList()

        val triage = NetworkTriageResult(steps.last().devices)
        assertTrue(triage.devices[0].isGateway)
        assertFalse(triage.hint)
        assertEquals(RiskLevel.LOW, FusionModule().fuse(0f, 0f, triage).level)
    }

    @Test
    fun `un serviciu RTSP din mDNS plus un modul fizic duc la risc ridicat`() = runBlocking {
        val steps = sweep(listOf(loopback), prober(closedPort()), gateway = null).toList()
        val devices = mergeDevices(
            steps.last().devices,
            listOf(MdnsService("_rtsp._tcp", "Camera sufragerie", loopback)),
            gateway = null,
            self = null,
        )
        assertTrue(devices[0].suspect)

        val fused = FusionModule().fuse(0.9f, 0f, NetworkTriageResult(devices))
        assertEquals(RiskLevel.HIGH, fused.level)
        assertTrue(fused.networkDevices.contains("Camera sufragerie"))
    }
}
