package com.example.detectorulcreeps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val tips = listOf(
    "Scoateți husa telefonului dacă are magneți sau plăcuțe metalice.",
    "La calibrare, stați la cel puțin 1 m de laptopuri, încărcătoare, televizoare și boxe.",
    "Stingeți lumina sau trageți draperiile: lentilele se văd mai bine pe întuneric.",
    "Verificați locurile obișnuite: detectoare de fum, încărcătoare, ceasuri, rame, prize, oglinzi, aerul condiționat.",
    "Mișcați telefonul lent, la 30–50 cm de obiecte.",
)

@Composable
fun PreparationScreen(onContinue: () -> Unit, onBack: () -> Unit) {
    var understood by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Pregătire", style = MaterialTheme.typography.titleLarge)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Atenție", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Aplicația face o verificare preliminară, nu oferă o garanție. Un rezultat „risc scăzut” " +
                        "nu înseamnă că nu există o cameră. Dacă găsiți un dispozitiv suspect, nu îl atingeți: " +
                        "fotografiați-l și anunțați gazda sau autoritățile.",
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Text(
            "Sfaturi",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        tips.forEachIndexed { i, tip ->
            Text("${i + 1}. $tip", modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }

        Text(
            "Pașii scanării",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        Text(
            "Calibrare magnetică (3 secunde, telefonul nemișcat), scanare magnetică, scanare optică cu blițul, rezultat.",
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = understood, onCheckedChange = { understood = it })
            Text("Am citit și am înțeles")
        }
        Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBack) { Text("Înapoi") }
            Button(onClick = onContinue, enabled = understood) { Text("Începe calibrarea") }
        }
    }
}
