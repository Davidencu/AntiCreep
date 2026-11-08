package com.example.detectorulcreeps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ro")).format(Date(millis))

/** Read: lista rapoartelor salvate. */
@Composable
fun HistoryScreen(database: ReportDatabase, onOpen: (Long) -> Unit, onBack: () -> Unit) {
    var reports by remember { mutableStateOf<List<Report>?>(null) }
    LaunchedEffect(Unit) { reports = database.getAll() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Istoric rapoarte", style = MaterialTheme.typography.titleLarge)
        val list = reports
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp)) {
            when {
                list == null -> Text("Se încarcă...", modifier = Modifier.align(Alignment.Center))
                list.isEmpty() -> Text(
                    "Nu există rapoarte salvate. Salvați un raport din ecranul Rezultat.",
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.id }) { report -> ReportRow(report, onClick = { onOpen(report.id) }) }
                }
            }
        }
        Button(onClick = onBack) { Text("Înapoi") }
    }
}

@Composable
private fun ReportRow(report: Report, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(report.result.level.color()))
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(report.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "%s · %s".format(formatDate(report.createdAt), report.result.level.label()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Read, Update și Delete pentru un raport. */
@Composable
fun ReportDetailScreen(database: ReportDatabase, reportId: Long, onBack: () -> Unit) {
    var report by remember { mutableStateOf<Report?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reportId) {
        val r = database.get(reportId)
        // Câmpurile se completează doar prima dată, ca să nu se piardă modificările după o rotire a ecranului.
        if (r != null && name.isEmpty() && note.isEmpty()) {
            name = r.name
            note = r.note
        }
        report = r
        loaded = true
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Detalii raport", style = MaterialTheme.typography.titleLarge)
        val current = report
        when {
            !loaded -> Text("Se încarcă...", modifier = Modifier.padding(top = 24.dp))
            current == null -> Text("Raportul nu mai există.", modifier = Modifier.padding(top = 24.dp))
            else -> {
                Text(formatDate(current.createdAt), style = MaterialTheme.typography.bodyMedium)
                RiskPanel(current.result.level)
                ScoreRow("Scor magnetic", current.result.magneticScore)
                ScoreRow("Scor optic", current.result.opticScore)
                ScoreRow("Scor combinat", current.result.combinedScore)
                val snapshot = remember(current.id) { current.snapshot?.toBitmap() }
                SuspectZones(current.result, snapshot)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; message = null },
                    label = { Text("Nume") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; message = null },
                    label = { Text("Notă") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                message?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }

                Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        val error = validateReport(name, note)
                        if (error != null) {
                            message = error
                        } else scope.launch {
                            message = if (database.update(current.id, name, note)) {
                                report = current.copy(name = name.trim(), note = note.trim())
                                "Modificările au fost salvate."
                            } else {
                                "Modificările nu au putut fi salvate."
                            }
                        }
                    }) { Text("Salvează modificările") }
                    Button(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Șterge") }
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Înapoi la istoric") }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Ștergeți raportul?") },
            text = { Text("Raportul „$name” va fi șters definitiv.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        database.delete(reportId)
                        onBack()
                    }
                }) { Text("Șterge") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Anulează") } },
        )
    }
}
