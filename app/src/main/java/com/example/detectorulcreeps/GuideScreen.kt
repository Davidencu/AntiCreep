package com.example.detectorulcreeps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** O secțiune din ghid: titlu și paragrafe. */
data class GuideSection(val title: String, val paragraphs: List<String>)

val guideSections = listOf(
    GuideSection(
        "Cum se folosește",
        listOf(
            "1. Pe ecranul Pregătire citiți sfaturile și verificați lumina din încăpere.",
            "2. Triajul de rețea caută în Wi-Fi-ul cazării dispozitive care se comportă ca o cameră.",
            "3. Scanarea optică: stingeți lumina, țineți telefonul la 30–50 cm și treceți lent pe lângă obiectele suspecte.",
            "4. Confirmarea magnetică: după calibrare, apropiați telefonul de zonele găsite la pasul optic.",
            "5. Rezultatul arată nivelul de risc și motivele; raportul se poate salva în Istoric.",
        ),
    ),
    GuideSection(
        "Triajul de rețea",
        listOf(
            "Aplicația verifică adresele din rețeaua locală și porturile folosite de camere: 554 și 8554 (RTSP, " +
                "fluxul video al camerelor IP) și 80, 8080, 8081 (interfețe web). Caută și servicii anunțate prin mDNS. " +
                "Routerul nu este considerat suspect.",
            "Un dispozitiv suspect în rețea poate doar să ridice nivelul de risc, niciodată să îl coboare.",
        ),
    ),
    GuideSection(
        "Scanarea optică",
        listOf(
            "Lentila unei camere reflectă lumina blițului înapoi spre telefon, ca ochii unei pisici (retroreflexie). " +
                "Blițul clipește, iar aplicația compară un cadru cu blițul stins cu unul cu blițul aprins: becurile și " +
                "ferestrele sunt luminoase în ambele și dispar, lentila apare doar cu blițul.",
            "Se păstrează doar punctele mici și rotunde. Dacă telefonul se mișcă prea repede, cadrele nu se mai " +
                "suprapun, așa că aplicația vă cere să încetiniți și ignoră cadrele mișcate.",
        ),
    ),
    GuideSection(
        "Confirmarea magnetică",
        listOf(
            "Magnetometrul măsoară câmpul magnetic. O cameră activă are componente electronice și o sursă de " +
                "alimentare care modifică puțin câmpul de lângă ea. Calibrarea măsoară întâi fundalul încăperii, " +
                "apoi aplicația semnalează creșterile peste un prag.",
        ),
    ),
    GuideSection(
        "Limitări",
        listOf(
            "Aplicația face o verificare preliminară, nu oferă o garanție. Un rezultat „risc scăzut” nu înseamnă că nu există o cameră.",
            "Camerele care înregistrează pe card, fără Wi-Fi, nu apar în triajul de rețea. Unele rețele de hotel " +
                "izolează clienții, iar atunci triajul nu vede alte dispozitive.",
            "Scanarea optică poate da alarme false pe suprafețe lucioase, șuruburi sau LED-uri și poate rata o " +
                "lentilă acoperită sau orientată în altă parte.",
            "Magnetometrul reacționează la orice aparat electric sau obiect metalic: încărcătoare, boxe, prize, mobilier.",
        ),
    ),
    GuideSection(
        "Dacă găsiți ceva suspect",
        listOf(
            "Nu atingeți și nu demontați dispozitivul. Fotografiați-l, salvați raportul în aplicație și anunțați " +
                "gazda sau platforma de cazare. Dacă vă simțiți în pericol, sunați la 112.",
        ),
    ),
    GuideSection(
        "Despre",
        listOf(
            "Detectorul de Creeps (AntiCreep) este un proiect studențesc realizat de Popa Tijana și Păun David.",
            "Toate datele rămân pe telefon: rapoartele se salvează local, iar triajul de rețea sondează doar rețeaua locală.",
        ),
    ),
)

@Composable
fun GuideScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Ghid și Despre", style = MaterialTheme.typography.titleLarge)
        guideSections.forEach { section ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (section.title == "Limitări") {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    section.paragraphs.forEach { Text(it, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) { Text("Înapoi") }
    }
}
