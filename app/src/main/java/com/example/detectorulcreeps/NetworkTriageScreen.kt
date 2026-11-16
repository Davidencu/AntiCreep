package com.example.detectorulcreeps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Triajul de rețea: pornește singur, arată progresul și lista dispozitivelor găsite.
 * [onResult] primește rezultatul complet, sau unul incomplet dacă utilizatorul sare peste triaj.
 */
@Composable
fun NetworkTriageScreen(onResult: (NetworkTriageResult) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var permissionResolved by remember { mutableStateOf(hasLocalNetworkPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionResolved = true
    }
    LaunchedEffect(Unit) {
        if (!permissionResolved) launcher.launch(LOCAL_NETWORK_PERMISSION)
    }

    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<TriageState?>(null) }
    LaunchedEffect(permissionResolved, attempt) {
        if (!permissionResolved) return@LaunchedEffect
        state = null
        NetworkScanner(context).scan().collect { state = it }
    }

    val current = state
    val finished = current as? TriageState.Finished
    val running = current as? TriageState.Running
    val devices = finished?.result?.devices ?: running?.devices.orEmpty()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Triaj de rețea", style = MaterialTheme.typography.titleLarge)

        when {
            finished != null -> TriageSummary(finished.result)
            running != null -> {
                Text(running.phase, modifier = Modifier.padding(top = 16.dp))
                LinearProgressIndicator(progress = { running.progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            else -> Text("Se așteaptă permisiunea pentru rețeaua locală...", modifier = Modifier.padding(top = 16.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (devices.isEmpty() && current != null) {
                item { Text("Niciun dispozitiv găsit încă.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(devices, key = { it.address }) { DeviceRow(it) }
        }

        Text(
            "Rezultatul este un indiciu, nu o localizare. O cameră fără rețea sau pe date mobile nu poate fi văzută aici.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBack) { Text("Înapoi") }
            Button(onClick = {
                if (!hasLocalNetworkPermission(context)) {
                    permissionResolved = false
                    launcher.launch(LOCAL_NETWORK_PERMISSION)
                } else {
                    attempt++
                }
            }) { Text("Repetă") }
        }
        Button(
            onClick = {
                onResult(
                    finished?.result
                        ?: NetworkTriageResult(devices, "triajul a fost oprit înainte de final")
                )
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text(if (finished != null) "Continuă: scanare optică" else "Sari peste triaj") }
    }
}

@Composable
private fun TriageSummary(result: NetworkTriageResult) {
    val text = when {
        result.hint -> "Indiciu de rețea: ${result.suspects.size} dispozitiv(e) suspect(e)."
        result.complete -> "Indiciu de rețea: niciun dispozitiv suspect."
        else -> "Triaj incomplet: ${result.incompleteReason}. Scanarea continuă cu celelalte module, iar nivelul de risc nu este coborât."
    }
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
    if (result.complete && result.devices.size == 1 && result.devices[0].isGateway) {
        Text(
            "Doar routerul a răspuns. Unele hoteluri izolează clienții, caz în care triajul nu vede alte dispozitive.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DeviceRow(device: NetworkDevice) {
    val colors = if (device.suspect) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                when {
                    device.suspect -> "${device.address}  ·  SUSPECT"
                    device.isGateway -> "${device.address}  ·  router"
                    else -> device.address
                },
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                if (device.openPorts.isEmpty()) "Porturi de cameră deschise: niciunul"
                else "Porturi deschise: ${device.openPorts.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
            )
            device.services.forEach {
                Text("Serviciu mDNS: ${it.name} (${it.type})", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
