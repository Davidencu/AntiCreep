package com.example.detectorulcreeps

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkTriageTest {
    private fun ip(a: Int, b: Int, c: Int, d: Int) = byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte())

    // --- subnetHosts ---

    @Test
    fun `o retea 24 are 253 de gazde, fara telefon, fara adresa retelei si fara broadcast`() {
        val hosts = subnetHosts(ip(192, 168, 1, 23), 24)
        assertEquals(253, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.254", hosts.last())
        assertFalse("192.168.1.23" in hosts)
        assertFalse("192.168.1.0" in hosts)
        assertFalse("192.168.1.255" in hosts)
    }

    @Test
    fun `retelele mai mari de 24 se limiteaza la 24 care contine telefonul`() {
        val hosts = subnetHosts(ip(10, 200, 130, 200), 16)
        assertEquals(253, hosts.size)
        assertEquals("10.200.130.1", hosts.first())
        assertEquals("10.200.130.254", hosts.last())
    }

    @Test
    fun `retelele mici au doar gazdele lor`() {
        // /28: 14 gazde, minus telefonul
        val hosts = subnetHosts(ip(192, 168, 1, 18), 28)
        assertEquals(13, hosts.size)
        assertEquals("192.168.1.17", hosts.first())
        assertEquals("192.168.1.30", hosts.last())
        assertFalse("192.168.1.18" in hosts)
    }

    @Test
    fun `retelele 31 si 32 si adresele invalide nu au gazde de scanat`() {
        assertTrue(subnetHosts(ip(192, 168, 1, 18), 31).isEmpty())
        assertTrue(subnetHosts(ip(192, 168, 1, 18), 32).isEmpty())
        assertTrue(subnetHosts(byteArrayOf(1, 2, 3), 24).isEmpty())
    }

    // --- NetworkDevice ---

    @Test
    fun `un dispozitiv cu port RTSP sau HTTP deschis este suspect`() {
        assertTrue(NetworkDevice("192.168.1.9", listOf(554)).suspect)
        assertTrue(NetworkDevice("192.168.1.9", listOf(8554)).suspect)
        for (port in HTTP_PORTS) assertTrue(NetworkDevice("192.168.1.9", listOf(port)).suspect)
    }

    @Test
    fun `un dispozitiv fara porturi de camera nu este suspect`() {
        assertFalse(NetworkDevice("192.168.1.9").suspect)
        assertFalse(NetworkDevice("192.168.1.9", listOf(22, 443)).suspect)
    }

    @Test
    fun `un serviciu mDNS RTSP face dispozitivul suspect, unul HTTP nu`() {
        assertTrue(NetworkDevice("192.168.1.9", services = listOf(DeviceService("_rtsp._tcp", "Cam"))).suspect)
        assertFalse(NetworkDevice("192.168.1.9", services = listOf(DeviceService("_http._tcp", "Printer"))).suspect)
    }

    @Test
    fun `routerul nu este niciodata suspect`() {
        val router = NetworkDevice(
            "192.168.1.1",
            openPorts = listOf(80, 554),
            services = listOf(DeviceService("_rtsp._tcp", "Router")),
            isGateway = true,
        )
        assertFalse(router.suspect)
    }

    @Test
    fun `descrierea contine adresa, porturile si serviciile`() {
        val device = NetworkDevice(
            "192.168.1.9",
            openPorts = listOf(80, 554),
            services = listOf(DeviceService("_rtsp._tcp", "Cam")),
        )
        assertEquals("192.168.1.9 · porturi 80, 554 · Cam (_rtsp._tcp)", device.describe())
        assertEquals("192.168.1.20", NetworkDevice("192.168.1.20").describe())
    }

    // --- NetworkTriageResult ---

    @Test
    fun `indiciul de retea exista doar daca sunt dispozitive suspecte`() {
        val none = NetworkTriageResult(listOf(NetworkDevice("192.168.1.1", listOf(80), isGateway = true)))
        assertFalse(none.hint)
        assertTrue(none.complete)

        val suspect = NetworkTriageResult(listOf(NetworkDevice("192.168.1.9", listOf(554))))
        assertTrue(suspect.hint)
    }

    @Test
    fun `un triaj cu motiv de incompletitudine nu este complet si nu are indiciu`() {
        val result = NetworkTriageResult(emptyList(), "permisiune refuzata")
        assertFalse(result.complete)
        assertFalse(result.hint)
        assertEquals("", result.suspectsSummary())
    }

    @Test
    fun `un triaj oprit dupa ce a gasit un suspect pastreaza indiciul`() {
        val result = NetworkTriageResult(listOf(NetworkDevice("192.168.1.9", listOf(554))), "oprit")
        assertFalse(result.complete)
        assertTrue(result.hint)
    }

    @Test
    fun `rezumatul listeaza suspectii, cate unul pe linie`() {
        val result = NetworkTriageResult(
            listOf(
                NetworkDevice("192.168.1.9", listOf(554)),
                NetworkDevice("192.168.1.1", listOf(80), isGateway = true),
                NetworkDevice("192.168.1.12", listOf(8080)),
            )
        )
        assertEquals("192.168.1.9 · porturi 554\n192.168.1.12 · porturi 8080", result.suspectsSummary())
    }

    // --- sortDevices ---

    @Test
    fun `suspectii sunt primii, apoi dupa adresa numerica`() {
        val sorted = sortDevices(
            listOf(
                NetworkDevice("192.168.1.100"),
                NetworkDevice("192.168.1.20"),
                NetworkDevice("192.168.1.50", listOf(554)),
                NetworkDevice("192.168.1.9", listOf(80)),
            )
        )
        assertEquals(listOf("192.168.1.9", "192.168.1.50", "192.168.1.20", "192.168.1.100"), sorted.map { it.address })
    }

    // --- mergeDevices / normalizeType ---

    @Test
    fun `serviciile mDNS se adauga la dispozitivul de la aceeasi adresa, fara duplicate`() {
        val merged = mergeDevices(
            probed = listOf(NetworkDevice("192.168.1.20")),
            services = listOf(
                MdnsService("_rtsp._tcp", "Cam", "192.168.1.20"),
                MdnsService("_rtsp._tcp", "Cam", "192.168.1.20"),
            ),
            gateway = "192.168.1.1",
            self = "192.168.1.5",
        )
        assertEquals(1, merged.size)
        assertEquals(listOf(DeviceService("_rtsp._tcp", "Cam")), merged[0].services)
        assertTrue(merged[0].suspect)
    }

    @Test
    fun `un serviciu de la o adresa nesondata creeaza un dispozitiv nou`() {
        val merged = mergeDevices(
            probed = emptyList(),
            services = listOf(MdnsService("_http._tcp", "Imprimanta", "192.168.1.40")),
            gateway = null,
            self = null,
        )
        assertEquals(listOf("192.168.1.40"), merged.map { it.address })
        assertFalse(merged[0].suspect)
    }

    @Test
    fun `serviciile anuntate de telefon se ignora`() {
        val merged = mergeDevices(
            probed = emptyList(),
            services = listOf(MdnsService("_rtsp._tcp", "Eu", "192.168.1.5")),
            gateway = null,
            self = "192.168.1.5",
        )
        assertTrue(merged.isEmpty())
    }

    @Test
    fun `un serviciu RTSP anuntat de router nu il face suspect`() {
        val merged = mergeDevices(
            probed = emptyList(),
            services = listOf(MdnsService("_rtsp._tcp", "Router", "192.168.1.1")),
            gateway = "192.168.1.1",
            self = null,
        )
        assertTrue(merged[0].isGateway)
        assertFalse(merged[0].suspect)
    }

    @Test
    fun `tipul serviciului se normalizeaza`() {
        assertEquals("_rtsp._tcp", normalizeType("_rtsp._tcp."))
        assertEquals("_rtsp._tcp", normalizeType("_rtsp._tcp.local."))
        assertEquals("_http._tcp", normalizeType("._http._tcp."))
    }

    // --- sweep ---

    private class FakeProber(
        private val responses: Map<String, HostProbe>,
        private val failing: Set<String> = emptySet(),
        private val delayMs: Long = 0,
    ) : HostProber {
        var inFlight = 0
        var maxInFlight = 0

        override suspend fun probe(address: String): HostProbe {
            inFlight++
            maxInFlight = maxOf(maxInFlight, inFlight)
            try {
                if (delayMs > 0) delay(delayMs)
                if (address in failing) error("sonda a esuat")
                return responses[address] ?: HostProbe(alive = false)
            } finally {
                inFlight--
            }
        }
    }

    @Test
    fun `baleierea gaseste dispozitivele active si marcheaza suspectii`() = runTest {
        val prober = FakeProber(
            mapOf(
                "192.168.1.1" to HostProbe(true, listOf(80)),
                "192.168.1.9" to HostProbe(true, listOf(8554, 80)),
                "192.168.1.20" to HostProbe(true),
            )
        )
        val steps = sweep(subnetHosts(ip(192, 168, 1, 5), 24), prober, gateway = "192.168.1.1").toList()
        val devices = steps.last().devices
        assertEquals(listOf("192.168.1.9", "192.168.1.1", "192.168.1.20"), devices.map { it.address })
        assertEquals(listOf(80, 8554), devices[0].openPorts)
        assertTrue(devices[0].suspect)
        assertTrue(devices[1].isGateway)
        assertFalse(devices[1].suspect)
        assertFalse(devices[2].suspect)
    }

    @Test
    fun `baleierea emite progres crescator, pana la toate adresele`() = runTest {
        val hosts = subnetHosts(ip(192, 168, 1, 5), 24)
        val steps = sweep(hosts, FakeProber(emptyMap()), gateway = null).toList()
        assertEquals(hosts.size, steps.size)
        assertEquals((1..hosts.size).toList(), steps.map { it.done })
        assertTrue(steps.all { it.total == hosts.size })
        assertTrue(steps.last().devices.isEmpty())
    }

    @Test
    fun `o sonda care esueaza este tratata ca adresa fara raspuns`() = runTest {
        val prober = FakeProber(
            responses = mapOf("192.168.1.9" to HostProbe(true, listOf(554))),
            failing = setOf("192.168.1.30"),
        )
        val steps = sweep(subnetHosts(ip(192, 168, 1, 5), 24), prober, gateway = null).toList()
        assertEquals(253, steps.last().done)
        assertEquals(listOf("192.168.1.9"), steps.last().devices.map { it.address })
    }

    @Test
    fun `baleierea nu depaseste limita de sonde paralele`() = runTest {
        val prober = FakeProber(emptyMap(), delayMs = 100)
        sweep(subnetHosts(ip(192, 168, 1, 5), 24), prober, gateway = null, parallelism = 8).toList()
        assertTrue("maxim ${prober.maxInFlight}", prober.maxInFlight in 2..8)
    }

    @Test
    fun `fara adrese de scanat baleierea emite un singur pas gol`() = runTest {
        val steps = sweep(emptyList(), FakeProber(emptyMap()), gateway = null).toList()
        assertEquals(1, steps.size)
        assertEquals(0, steps[0].total)
        assertTrue(steps[0].devices.isEmpty())
    }
}
