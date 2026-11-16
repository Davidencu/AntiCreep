package com.example.detectorulcreeps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import javax.net.SocketFactory

const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
private const val LOCAL_NETWORK_MIN_SDK = 37

/** Din Android 17 accesul la rețeaua locală cere o permisiune de rulare; înainte nu se cere nimic. */
fun hasLocalNetworkPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < LOCAL_NETWORK_MIN_SDK ||
        ContextCompat.checkSelfPermission(context, LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED

/** Rețeaua Wi-Fi curentă: adresa telefonului, lungimea prefixului și adresa routerului. */
data class LocalNetwork(
    val network: Network,
    val ownAddress: Inet4Address,
    val prefixLength: Int,
    val gateway: String?,
)

/** Găsește rețeaua Wi-Fi activă, sau întoarce null dacă telefonul nu e conectat la Wi-Fi cu adresă IPv4. */
fun findWifiNetwork(context: Context): LocalNetwork? {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return null
    val capabilities = cm.getNetworkCapabilities(network) ?: return null
    if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
    val properties = cm.getLinkProperties(network) ?: return null
    val own = properties.linkAddresses.firstOrNull { it.address is Inet4Address } ?: return null
    val gateway = properties.routes
        .firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
        ?.gateway?.hostAddress
    return LocalNetwork(network, own.address as Inet4Address, own.prefixLength, gateway)
}

/**
 * Sondează o adresă: ping (ICMP) și conectare TCP pe porturile de cameră, toate deodată.
 * Adresa e considerată activă dacă răspunde la ping, are un port deschis sau refuză conexiunea
 * (un port închis pe o gazdă activă răspunde „connection refused”, spre deosebire de o adresă goală).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SocketHostProber(
    /** Pe telefon: `network.socketFactory`, ca sondele să treacă prin Wi-Fi chiar dacă datele mobile sunt active. */
    private val socketFactory: SocketFactory,
    private val pingTimeoutMs: Int = 400,
    private val connectTimeoutMs: Int = 500,
    private val ports: List<Int> = CAMERA_PORTS,
) : HostProber {
    // Operațiile de socket blochează firul; limita implicită de 64 ar încetini baleierea.
    private val io: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(IO_THREADS)

    override suspend fun probe(address: String): HostProbe = coroutineScope {
        val ping = async(io) { ping(address) }
        val checks = ports.map { port -> port to async(io) { connect(address, port) } }
        val outcomes = checks.map { (port, deferred) -> port to deferred.await() }
        val open = outcomes.filter { it.second == PortState.OPEN }.map { it.first }
        val refused = outcomes.any { it.second == PortState.REFUSED }
        HostProbe(alive = ping.await() || open.isNotEmpty() || refused, openPorts = open)
    }

    private fun ping(address: String): Boolean =
        try {
            InetAddress.getByName(address).isReachable(pingTimeoutMs)
        } catch (e: java.io.IOException) {
            false
        }

    private fun connect(address: String, port: Int): PortState =
        try {
            socketFactory.createSocket().use { socket ->
                socket.connect(InetSocketAddress(address, port), connectTimeoutMs)
            }
            PortState.OPEN
        } catch (e: ConnectException) {
            if (e.message?.contains("refused", ignoreCase = true) == true) PortState.REFUSED else PortState.NO_ANSWER
        } catch (e: java.io.IOException) {
            PortState.NO_ANSWER
        }

    private enum class PortState { OPEN, REFUSED, NO_ANSWER }

    private companion object {
        const val IO_THREADS = 160
    }
}

/** Caută servicii _rtsp._tcp și _http._tcp prin mDNS și le rezolvă la adrese IPv4. */
class MdnsDiscovery(context: Context) {
    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    suspend fun discover(
        windowMs: Long = DISCOVERY_WINDOW_MS,
        types: List<String> = MDNS_SERVICE_TYPES,
    ): List<MdnsService> = coroutineScope {
        val found = Channel<NsdServiceInfo>(Channel.UNLIMITED)
        val services = mutableListOf<MdnsService>() // scris doar de corutina `resolver`
        // Fără blocarea multicast, multe telefoane nu primesc răspunsurile mDNS cu ecranul stins sau în economie.
        val lock = wifi.createMulticastLock("anticreep-mdns").apply { setReferenceCounted(false); acquire() }

        // Android permite o singură rezolvare deodată, deci serviciile găsite se rezolvă pe rând.
        val resolver = launch {
            for (info in found) resolve(info)?.let { services += it }
        }
        val listeners = types.mapNotNull { type ->
            val listener = discoveryListener(found)
            try {
                nsd.discoverServices("$type.", NsdManager.PROTOCOL_DNS_SD, listener)
                listener
            } catch (e: IllegalArgumentException) {
                null
            } catch (e: SecurityException) {
                null
            }
        }
        try {
            delay(windowMs)
        } finally {
            withContext(NonCancellable) {
                listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
                found.close()
                lock.release()
            }
        }
        if (withTimeoutOrNull(RESOLVE_GRACE_MS) { resolver.join() } == null) resolver.cancelAndJoin()
        services.toList()
    }

    private fun discoveryListener(found: Channel<NsdServiceInfo>) = object : NsdManager.DiscoveryListener {
        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            found.trySend(serviceInfo)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
        override fun onDiscoveryStarted(serviceType: String) = Unit
        override fun onDiscoveryStopped(serviceType: String) = Unit
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
    }

    @Suppress("DEPRECATION") // resolveService și getHost sunt singurele variante disponibile de la API 24.
    private suspend fun resolve(info: NsdServiceInfo): MdnsService? {
        val result = CompletableDeferred<MdnsService?>()
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                result.complete(null)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val address = (serviceInfo.host as? Inet4Address)?.hostAddress
                val type = normalizeType(serviceInfo.serviceType)
                result.complete(address?.let { MdnsService(type, serviceInfo.serviceName, it) })
            }
        }
        return try {
            nsd.resolveService(info, listener)
            withTimeoutOrNull(RESOLVE_TIMEOUT_MS) { result.await() }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        const val DISCOVERY_WINDOW_MS = 5_000L
        const val RESOLVE_TIMEOUT_MS = 2_000L
        const val RESOLVE_GRACE_MS = 3_000L
    }
}

/** Tipul întors de sistem poate avea punct la început sau la sfârșit și sufixul domeniului. */
internal fun normalizeType(raw: String): String =
    raw.trim('.').split('.').take(2).joinToString(".")

/** Rulează triajul: baleiere ping + porturi și descoperire mDNS în paralel, pe rețeaua Wi-Fi curentă. */
class NetworkScanner(private val context: Context) {

    fun scan(): Flow<TriageState> = channelFlow {
        if (!hasLocalNetworkPermission(context)) {
            send(incomplete("permisiunea pentru rețeaua locală nu a fost acordată"))
            return@channelFlow
        }
        val local = findWifiNetwork(context)
        if (local == null) {
            send(incomplete("telefonul nu este conectat la o rețea Wi-Fi"))
            return@channelFlow
        }
        val hosts = subnetHosts(local.ownAddress.address, local.prefixLength)
        if (hosts.isEmpty()) {
            send(incomplete("rețeaua Wi-Fi este prea mică pentru a fi scanată"))
            return@channelFlow
        }

        val self = local.ownAddress.hostAddress
        val mdns = async { runCatching { MdnsDiscovery(context).discover() }.getOrDefault(emptyList()) }
        send(TriageState.Running(0f, emptyList(), "Se sondează rețeaua"))

        var probed = emptyList<NetworkDevice>()
        sweep(hosts, SocketHostProber(local.network.socketFactory), local.gateway, SWEEP_PARALLELISM).collect { step ->
            probed = step.devices
            send(TriageState.Running(step.done.toFloat() / step.total * SWEEP_SHARE, step.devices, "Se sondează rețeaua"))
        }

        send(TriageState.Running(SWEEP_SHARE, probed, "Se caută servicii mDNS"))
        val devices = mergeDevices(probed, mdns.await(), local.gateway, self)
        send(
            TriageState.Finished(
                if (devices.isEmpty()) {
                    NetworkTriageResult(
                        devices,
                        "niciun dispozitiv nu a răspuns (rețeaua poate izola clienții sau poate bloca sondarea)",
                    )
                } else {
                    NetworkTriageResult(devices)
                }
            )
        )
    }

    private fun incomplete(reason: String) = TriageState.Finished(NetworkTriageResult(emptyList(), reason))

    private companion object {
        const val SWEEP_PARALLELISM = 24
        /** Partea din bara de progres ocupată de baleiere; restul e așteptarea mDNS. */
        const val SWEEP_SHARE = 0.9f
    }
}
