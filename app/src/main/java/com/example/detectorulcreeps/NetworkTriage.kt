package com.example.detectorulcreeps

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.max

/** Porturile RTSP, folosite de aproape toate camerele IP pentru flux video. */
val RTSP_PORTS = listOf(554, 8554)

/** Porturile HTTP pe care camerele IP își expun interfața web sau fluxul MJPEG. */
val HTTP_PORTS = listOf(80, 8080, 8081)

val CAMERA_PORTS = RTSP_PORTS + HTTP_PORTS

/** Tipurile de servicii mDNS căutate. */
val MDNS_SERVICE_TYPES = listOf("_rtsp._tcp", "_http._tcp")

private const val RTSP_SERVICE_TYPE = "_rtsp._tcp"

/** Un serviciu mDNS anunțat de un dispozitiv, de ex. „Camera sufragerie” de tip _rtsp._tcp. */
data class DeviceService(val type: String, val name: String)

/** Un serviciu mDNS găsit în rețea, cu adresa IPv4 la care a fost rezolvat. */
data class MdnsService(val type: String, val name: String, val address: String)

/**
 * Un dispozitiv care a răspuns în rețea.
 * Routerul nu este niciodată suspect: răspunde pe portul 80 pentru pagina lui de administrare.
 */
data class NetworkDevice(
    val address: String,
    val openPorts: List<Int> = emptyList(),
    val services: List<DeviceService> = emptyList(),
    val isGateway: Boolean = false,
) {
    /** Un dispozitiv necunoscut care răspunde pe un port de cameră sau anunță un serviciu RTSP. */
    val suspect: Boolean
        get() = !isGateway && (openPorts.any { it in CAMERA_PORTS } || services.any { it.type == RTSP_SERVICE_TYPE })

    fun describe(): String = buildList {
        add(address)
        if (openPorts.isNotEmpty()) add("porturi ${openPorts.joinToString(", ")}")
        if (services.isNotEmpty()) add(services.joinToString(", ") { "${it.name} (${it.type})" })
    }.joinToString(" · ")
}

/**
 * Rezultatul triajului. [incompleteReason] este null când triajul a rulat până la capăt.
 * Un triaj incomplet nu coboară niciodată nivelul de risc.
 */
data class NetworkTriageResult(
    val devices: List<NetworkDevice>,
    val incompleteReason: String? = null,
) {
    val complete: Boolean get() = incompleteReason == null
    val suspects: List<NetworkDevice> get() = devices.filter { it.suspect }

    /** Indiciul de rețea R: există cel puțin un dispozitiv suspect. */
    val hint: Boolean get() = suspects.isNotEmpty()

    /** Dispozitivele suspecte, câte unul pe linie, pentru raport. */
    fun suspectsSummary(): String = suspects.joinToString("\n") { it.describe() }
}

sealed interface TriageState {
    data class Running(val progress: Float, val devices: List<NetworkDevice>, val phase: String) : TriageState
    data class Finished(val result: NetworkTriageResult) : TriageState
}

/** Răspunsul unei adrese la sondare. */
data class HostProbe(val alive: Boolean, val openPorts: List<Int> = emptyList())

interface HostProber {
    suspend fun probe(address: String): HostProbe
}

data class SweepProgress(val done: Int, val total: Int, val devices: List<NetworkDevice>)

/**
 * Sondează adresele în paralel (cel mult [parallelism] deodată) și emite progresul după fiecare adresă.
 * O sondare care eșuează cu o excepție este tratată ca „adresă fără răspuns”.
 */
fun sweep(
    hosts: List<String>,
    prober: HostProber,
    gateway: String?,
    parallelism: Int = 32,
): Flow<SweepProgress> = channelFlow {
    if (hosts.isEmpty()) {
        send(SweepProgress(0, 0, emptyList()))
        return@channelFlow
    }
    val results = Channel<Pair<String, HostProbe>>(Channel.UNLIMITED)
    val gate = Semaphore(parallelism)
    for (host in hosts) {
        launch {
            val probe = gate.withPermit {
                try {
                    prober.probe(host)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    HostProbe(alive = false)
                }
            }
            results.send(host to probe)
        }
    }
    val found = mutableListOf<NetworkDevice>()
    repeat(hosts.size) { index ->
        val (host, probe) = results.receive()
        if (probe.alive) found += NetworkDevice(host, probe.openPorts.sorted(), isGateway = host == gateway)
        send(SweepProgress(index + 1, hosts.size, sortDevices(found)))
    }
}

/**
 * Adresele de gazdă ale subrețelei lui [own] (patru octeți), fără adresa proprie, fără adresa rețelei
 * și fără adresa de broadcast. Rețelele mai mari de /24 se limitează la /24 care conține adresa proprie,
 * ca scanarea să rămână rapidă. Rețelele /31 și /32 nu au gazde de scanat.
 */
fun subnetHosts(own: ByteArray, prefixLength: Int): List<String> {
    if (own.size != 4 || prefixLength > 30) return emptyList()
    val ip = own.fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xFF) }
    val prefix = max(prefixLength, 24)
    val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
    val network = ip and mask
    val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
    return (network + 1 until broadcast).filter { it != ip }.map(::formatIpv4)
}

private fun formatIpv4(value: Long): String =
    "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"

private fun ipv4Key(address: String): Long =
    address.split('.').mapNotNull { it.toLongOrNull() }.takeIf { it.size == 4 }
        ?.fold(0L) { acc, part -> (acc shl 8) or part } ?: Long.MAX_VALUE

/** Suspecții primii, apoi după adresă. */
fun sortDevices(devices: Collection<NetworkDevice>): List<NetworkDevice> =
    devices.sortedWith(compareByDescending<NetworkDevice> { it.suspect }.thenBy { ipv4Key(it.address) })

/**
 * Adaugă serviciile mDNS la dispozitivele găsite prin sondare. Un serviciu de la o adresă care nu a
 * răspuns la sondare creează un dispozitiv nou. Serviciile anunțate de telefon se ignoră.
 */
fun mergeDevices(
    probed: List<NetworkDevice>,
    services: List<MdnsService>,
    gateway: String?,
    self: String?,
): List<NetworkDevice> {
    val byAddress = LinkedHashMap<String, NetworkDevice>()
    probed.forEach { byAddress[it.address] = it }
    for (service in services) {
        if (service.address == self) continue
        val device = byAddress[service.address] ?: NetworkDevice(service.address, isGateway = service.address == gateway)
        val entry = DeviceService(service.type, service.name)
        byAddress[service.address] =
            if (entry in device.services) device else device.copy(services = device.services + entry)
    }
    return sortDevices(byAddress.values)
}
