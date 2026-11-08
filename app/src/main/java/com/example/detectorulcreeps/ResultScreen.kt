package com.example.detectorulcreeps

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ResultScreen(
    result: FusionResult,
    snapshot: Bitmap?,
    onSave: suspend (name: String, note: String) -> Unit,
    onRestart: () -> Unit,
    onHistory: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saved by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Rezultat", style = MaterialTheme.typography.titleLarge)
        RiskPanel(result.level)

        ScoreRow("Scor magnetic", result.magneticScore)
        ScoreRow("Scor optic", result.opticScore)
        ScoreRow("Scor combinat", result.combinedScore)

        SuspectZones(result, snapshot)
        Text(
            "Aplicația este o verificare preliminară, nu o garanție.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp),
        )

        Text("Salvează raportul", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
        if (saved) {
            Text("Raportul a fost salvat.", modifier = Modifier.padding(top = 8.dp))
        } else {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Nume (de ex. Hotel Cluj, camera 12)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it; error = null },
                label = { Text("Notă (opțional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
            Button(
                onClick = {
                    error = validateReport(name, note)
                    if (error == null) {
                        saving = true
                        scope.launch {
                            runCatching { onSave(name, note) }
                                .onSuccess { saved = true }
                                .onFailure { error = "Raportul nu a putut fi salvat." }
                            saving = false
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Salvează raportul") }
        }

        Row(modifier = Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRestart) { Text("Scanează din nou") }
            Button(onClick = onHistory) { Text("Istoric") }
        }
    }
}

fun RiskLevel.label(): String = when (this) {
    RiskLevel.LOW -> "RISC SCĂZUT"
    RiskLevel.MEDIUM -> "RISC MEDIU"
    RiskLevel.HIGH -> "RISC RIDICAT"
}

fun RiskLevel.color(): Color = when (this) {
    RiskLevel.LOW -> Color(0xFFC6E0B4)
    RiskLevel.MEDIUM -> Color(0xFFFFE699)
    RiskLevel.HIGH -> Color(0xFFF4B6B6)
}

private fun RiskLevel.explanation(): String = when (this) {
    RiskLevel.LOW -> "Niciun modul nu a semnalat indicii."
    RiskLevel.MEDIUM -> "Un singur modul a semnalat indicii. Verificați zona."
    RiskLevel.HIGH -> "Ambele module au semnalat indicii. Verificați atent zona."
}

/** Panoul colorat cu nivelul de risc. */
@Composable
fun RiskPanel(level: RiskLevel) {
    Column(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(level.color())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(level.label(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(level.explanation(), color = Color.Black, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun SuspectZones(result: FusionResult, snapshot: Bitmap?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Zone suspecte", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        when {
            !result.magneticFlag && !result.opticFlag -> Text("Nicio zonă suspectă.")
            else -> {
                if (result.magneticFlag) Text("• Câmp magnetic anormal (aparat electronic activ în apropiere)")
                if (result.opticFlag) Text("• Punct luminos de tip lentilă (posibilă cameră)")
            }
        }
        if (snapshot != null) {
            Text(
                "Cadrul cu scorul optic cel mai mare. Punctele luminoase sunt încadrate cu roșu:",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Image(
                bitmap = snapshot.asImageBitmap(),
                contentDescription = "Zona suspectă, cu chenar roșu pe punctele luminoase",
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .aspectRatio(snapshot.width.toFloat() / snapshot.height)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Composable
fun ScoreRow(label: String, value: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("%s: %.2f".format(label, value))
        LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth())
    }
}
