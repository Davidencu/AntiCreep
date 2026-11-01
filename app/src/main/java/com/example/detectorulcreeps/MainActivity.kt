package com.example.detectorulcreeps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DetectorApp()
                }
            }
        }
    }
}

private enum class Screen { Start, MagneticScan, OpticScan, Result }

@Composable
private fun DetectorApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.Start) }
    var magneticPeak by rememberSaveable { mutableFloatStateOf(0f) }
    var result by remember { mutableStateOf<FusionResult?>(null) }
    val fusion = remember { FusionModule() }
    when (screen) {
        Screen.Start -> StartScreen(onStart = { screen = Screen.MagneticScan })
        Screen.MagneticScan -> MagneticScanScreen(
            onNext = { peak ->
                magneticPeak = peak
                screen = Screen.OpticScan
            },
            onBack = { screen = Screen.Start },
        )
        Screen.OpticScan -> OpticScanScreen(
            onResult = { opticPeak ->
                result = fusion.fuse(magneticPeak, opticPeak)
                screen = Screen.Result
            },
            onBack = { screen = Screen.MagneticScan },
        )
        Screen.Result -> result?.let { ResultScreen(it, onRestart = { screen = Screen.Start }) }
    }
}

@Composable
private fun StartScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Detectorul de Creeps", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Verificare preliminară, nu o garanție",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
        )
        Button(onClick = onStart) { Text("Începe scanarea") }
    }
}

@Composable
private fun MagneticScanScreen(onNext: (Float) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val reader = remember { MagnetometerReader(context) }
    var session by remember { mutableIntStateOf(0) }
    // O sesiune nouă (după „Recalibrează”) pornește un procesor curat.
    val states = remember(session) {
        val processor = MagnetometerProcessor()
        reader.readings().map { processor.process(it) }
    }
    val state by states.collectAsStateWithLifecycle(initialValue = null)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Scanare magnetică", style = MaterialTheme.typography.titleLarge)
        val current = state
        when {
            !reader.isAvailable -> Text(
                "Acest telefon nu are magnetometru.",
                modifier = Modifier.padding(top = 24.dp),
            )
            current == null -> Text(
                "Se așteaptă datele senzorului...",
                modifier = Modifier.padding(top = 24.dp),
            )
            current is MagneticState.Calibrating -> {
                Text(
                    "Calibrare: ține telefonul nemișcat, departe de aparate",
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                )
                LinearProgressIndicator(progress = { current.progress })
            }
            current is MagneticState.Scanning -> {
                Text(
                    if (current.anomaly) "ANOMALIE" else "Fără anomalie",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (current.anomaly) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text("%.1f µT (fundal %.1f µT)".format(current.filtered, current.baseline))
                Text("abatere %.1f µT, prag %.1f µT".format(current.deviation, current.threshold))
                Text(
                    "scor magnetic %.2f (maxim %.2f)".format(current.score, current.peakScore),
                    modifier = Modifier.padding(top = 8.dp),
                )
                LinearProgressIndicator(
                    progress = { current.score },
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "Mișcă lent telefonul de-a lungul pereților și mobilei.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
        Row(modifier = Modifier.padding(top = 32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { session++ }) { Text("Recalibrează") }
            Button(onClick = onBack) { Text("Înapoi") }
        }
        Button(
            onClick = { onNext((state as? MagneticState.Scanning)?.peakScore ?: 0f) },
            modifier = Modifier.padding(top = 12.dp),
        ) { Text("Continuă: scanare optică") }
    }
}
