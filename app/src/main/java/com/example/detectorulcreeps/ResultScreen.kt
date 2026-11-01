package com.example.detectorulcreeps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ResultScreen(result: FusionResult, onRestart: () -> Unit) {
    val (label, color, explanation) = when (result.level) {
        RiskLevel.LOW -> Triple("RISC SCĂZUT", Color(0xFFC6E0B4), "Niciun modul nu a semnalat indicii.")
        RiskLevel.MEDIUM -> Triple("RISC MEDIU", Color(0xFFFFE699), "Un singur modul a semnalat indicii. Verificați zona.")
        RiskLevel.HIGH -> Triple("RISC RIDICAT", Color(0xFFF4B6B6), "Ambele module au semnalat indicii. Verificați atent zona.")
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Rezultat", style = MaterialTheme.typography.titleLarge)
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(color)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(explanation, color = Color.Black, modifier = Modifier.padding(top = 4.dp))
        }

        ScoreRow("Scor magnetic", result.magneticScore)
        ScoreRow("Scor optic", result.opticScore)
        ScoreRow("Scor combinat", result.combinedScore)

        Text("Zone suspecte", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        when {
            !result.magneticFlag && !result.opticFlag -> Text("Nicio zonă suspectă.")
            else -> {
                if (result.magneticFlag) Text("• Câmp magnetic anormal (aparat electronic activ în apropiere)")
                if (result.opticFlag) Text("• Punct luminos de tip lentilă (posibilă cameră)")
            }
        }
        Text(
            "Aplicația este o verificare preliminară, nu o garanție.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Button(onClick = onRestart, modifier = Modifier.padding(top = 24.dp)) { Text("Scanează din nou") }
    }
}

@Composable
private fun ScoreRow(label: String, value: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("%s: %.2f".format(label, value))
        LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth())
    }
}
