package com.example.detectorulcreeps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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

private enum class Screen { Start, Preparation, NetworkTriage, OpticScan, MagneticScan, Result, History, ReportDetail, Settings, Guide }

@Composable
private fun DetectorApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.Start) }
    var opticPeak by rememberSaveable { mutableFloatStateOf(0f) }
    var networkResult by remember { mutableStateOf<NetworkTriageResult?>(null) }
    var checkedZones by remember { mutableStateOf(setOf<String>()) }
    var angleVerdict by remember { mutableStateOf<AngleVerdict?>(null) }
    var result by remember { mutableStateOf<FusionResult?>(null) }
    var snapshot by remember { mutableStateOf<Bitmap?>(null) }
    var selectedReportId by rememberSaveable { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val database = remember { ReportDatabase(context) }
    DisposableEffect(database) { onDispose { database.close() } }
    when (screen) {
        Screen.Start -> StartScreen(
            onStart = { screen = Screen.Preparation },
            onHistory = { screen = Screen.History },
            onSettings = { screen = Screen.Settings },
            onGuide = { screen = Screen.Guide },
            onDemo = {
                // Mod de demonstrație: simulează un rezultat RIDICAT (apăsare lungă pe titlu).
                result = FusionModule(threshold = settings.threshold).fuse(0.9f, 0.9f)
                snapshot = demoSnapshot()
                screen = Screen.Result
            },
        )
        Screen.Preparation -> PreparationScreen(
            onContinue = { screen = Screen.NetworkTriage },
            onBack = { screen = Screen.Start },
        )
        Screen.NetworkTriage -> NetworkTriageScreen(
            onResult = { triage ->
                networkResult = triage
                screen = Screen.OpticScan
            },
            onBack = { screen = Screen.Preparation },
        )
        Screen.OpticScan -> OpticScanScreen(
            onResult = { peak, opticSnapshot, zones, angle ->
                opticPeak = peak
                snapshot = opticSnapshot
                checkedZones = zones
                angleVerdict = angle
                screen = Screen.MagneticScan
            },
            onBack = { screen = Screen.NetworkTriage },
        )
        Screen.MagneticScan -> MagneticScanScreen(
            onNext = { magneticPeak ->
                result = FusionModule(threshold = settings.threshold).fuse(magneticPeak, opticPeak, networkResult)
                    .copy(checkedZones = zonesToText(checkedZones), angleCheck = angleVerdict)
                screen = Screen.Result
            },
            onBack = { screen = Screen.OpticScan },
        )
        Screen.Result -> result?.let { current ->
            val currentSnapshot = snapshot
            ResultScreen(
                result = current,
                snapshot = currentSnapshot,
                onSave = { name, note ->
                    val jpeg = currentSnapshot?.let { withContext(Dispatchers.Default) { it.toJpeg() } }
                    database.create(
                        Report(
                            name = name,
                            note = note,
                            createdAt = System.currentTimeMillis(),
                            result = current,
                            snapshot = jpeg,
                        )
                    )
                },
                onRestart = { screen = Screen.Start },
                onHistory = { screen = Screen.History },
            )
        } ?: LaunchedEffect(Unit) { screen = Screen.Start } // rezultatul se pierde la recrearea activității
        Screen.History -> HistoryScreen(
            database = database,
            onOpen = { id ->
                selectedReportId = id
                screen = Screen.ReportDetail
            },
            onBack = { screen = Screen.Start },
        )
        Screen.Guide -> GuideScreen(onBack = { screen = Screen.Start })
        Screen.Settings -> SettingsScreen(
            database = database,
            onBack = { screen = Screen.Start },
        )
        Screen.ReportDetail -> ReportDetailScreen(
            database = database,
            reportId = selectedReportId,
            onBack = { screen = Screen.History },
        )
    }
}

@Composable
private fun StartScreen(onStart: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit, onGuide: () -> Unit, onDemo: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Detectorul de Creeps",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { onDemo() }) },
        )
        Text(
            "Verificare preliminară, nu o garanție",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
        )
        Button(onClick = onStart) { Text("Începe scanarea") }
        Button(onClick = onHistory, modifier = Modifier.padding(top = 12.dp)) { Text("Istoric rapoarte") }
        Button(onClick = onSettings, modifier = Modifier.padding(top = 12.dp)) { Text("Setări") }
        Button(onClick = onGuide, modifier = Modifier.padding(top = 12.dp)) { Text("Ghid și Despre") }
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
                    "Apropie lent telefonul de zonele marcate la scanarea optică și de obiectele suspecte.",
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
        ) { Text("Vezi rezultatul") }
    }
}

/** Imagine simulată (fundal gri cu un chenar roșu) pentru modul de demonstrație. */
private fun demoSnapshot(): Bitmap {
    val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.rgb(70, 70, 75))
    val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = AndroidColor.RED
    }
    canvas.drawRect(380f, 150f, 470f, 240f, paint)
    paint.style = Paint.Style.FILL
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(425f, 195f, 8f, paint)
    return bitmap
}
