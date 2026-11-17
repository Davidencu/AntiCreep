package com.example.detectorulcreeps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(database: ReportDatabase, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var vibration by remember { mutableStateOf(settings.vibrationEnabled) }
    var threshold by remember { mutableFloatStateOf(settings.threshold) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var cleared by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Setări", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Vibrație la risc ridicat", style = MaterialTheme.typography.titleMedium)
                Text("Telefonul vibrează când rezultatul este „risc ridicat”.", style = MaterialTheme.typography.bodyMedium)
            }
            Switch(
                checked = vibration,
                onCheckedChange = {
                    vibration = it
                    settings.vibrationEnabled = it
                },
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text(
                "Prag de semnalare: ${String.format(Locale.US, "%.2f", threshold)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Un modul semnalează când scorul lui ajunge la acest prag. Un prag mai mic găsește mai mult, " +
                    "dar dă mai multe alarme false.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = threshold,
                onValueChange = { threshold = it },
                onValueChangeFinished = { settings.threshold = threshold },
                valueRange = MIN_THRESHOLD..MAX_THRESHOLD,
            )
            TextButton(onClick = {
                threshold = DEFAULT_THRESHOLD
                settings.threshold = DEFAULT_THRESHOLD
            }) { Text("Readu la valoarea implicită (0.50)") }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Istoric", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { confirmClear = true }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Șterge tot istoricul")
            }
            if (cleared) Text("Istoricul a fost șters.", modifier = Modifier.padding(top = 8.dp))
        }

        Button(onClick = onBack, modifier = Modifier.padding(top = 32.dp)) { Text("Înapoi") }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Ștergeți tot istoricul?") },
            text = { Text("Toate rapoartele salvate vor fi șterse definitiv.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch {
                        database.deleteAll()
                        cleared = true
                    }
                }) { Text("Șterge tot") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Anulează") } },
        )
    }
}
