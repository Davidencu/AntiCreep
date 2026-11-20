# AntiCreep (Detectorul de Creeps)

Aplicație Android care ajută pe cineva aflat într-un spațiu de cazare (apartament închiriat, cameră de hotel) să verifice rapid dacă există indicii ale unei **camere ascunse**. Folosește doar senzorii telefonului, fără echipament special.

Proiect pentru SMA/CHS, de Popa Tijana și Păun David. Planul complet este în `Detectorul-de-Creeps-Documentatie-Simplificat.docx`.

> **Important:** aplicația este o verificare preliminară, nu o garanție. Un rezultat „risc scăzut" nu înseamnă că nu există o cameră. Starea actuală a detecției este descrisă mai jos, la „Cât de bine detectează acum".

## Cum funcționează

Scanarea are trei pași, de la larg la îngust:

| Pas | Ce face | Ce folosește |
|---|---|---|
| 1. Triaj de rețea | Caută în rețeaua Wi-Fi dispozitive care se comportă ca o cameră (porturi video deschise, servicii mDNS) | Radio Wi-Fi |
| 2. Scanare optică | Caută cu blițul aprins puncte foarte luminoase și rotunde, cum ar fi reflexia unei lentile | Cameră, bliț |
| 3. Confirmare magnetică | Măsoară dacă lângă un obiect apare un câmp magnetic anormal | Magnetometru |

Rezultatele se combină într-un nivel de risc: **scăzut**, **mediu** sau **ridicat**.
- Fiecare modul fizic (optic, magnetic) „semnalează" când scorul lui trece de prag. Fie n numărul celor care semnalează.
- n = 0: scăzut. n = 1: mediu. n = 2: ridicat.
- Indiciul de rețea poate doar să ridice nivelul: doar el înseamnă mediu, iar n = 1 plus rețea înseamnă ridicat.
- Un triaj de rețea incomplet nu coboară niciodată nivelul.

Totul rulează pe telefon. Imaginile și rapoartele nu pleacă de pe dispozitiv.

## Ce este implementat

**Fluxul din aplicație:** Start, Pregătire, Triaj de rețea, Scanare optică, Confirmare magnetică, Rezultat, plus Istoric și Detalii raport.

- **Pregătire:** avertisment că aplicația nu e o garanție, sfaturi, explicarea permisiunilor, bifa „Am citit și am înțeles".
- **Triaj de rețea:**
  - Ping și conectare pe porturile 554, 8554, 80, 8080 și 8081 pentru adresele subrețelei Wi-Fi (cel mult un /24), în paralel.
  - Căutare mDNS pentru `_rtsp._tcp` și `_http._tcp`.
  - Lista dispozitivelor cu adrese și porturi, cele suspecte marcate. Routerul nu e niciodată suspect.
  - Se tratează cazurile: permisiune refuzată, telefon fără Wi-Fi, rețea care nu răspunde. Triajul apare atunci „incomplet".
- **Scanare optică:** CameraX cu blițul aprins, imagine 640×480 pe planul de luminanță, detector clasic în Kotlin (puncte luminoase, rotunde, mici). Punctele găsite se încadrează cu roșu, iar cadrul cu scorul cel mai mare se păstrează.
- **Confirmare magnetică:** calibrare de 3 secunde pe fundal, filtru trece-jos, prag și scor în [0, 1], indicator în timp real.
- **Rezultat:** nivelul de risc cu culoare și text, scorurile, imaginea cu chenar roșu, zonele suspecte din rețea.
- **Rapoarte (CRUD):** creare din ecranul Rezultat, listare cu cele mai noi primele, editare (nume, notă) și ștergere cu confirmare. Stocare locală în SQLite.

**Testare:** teste unit pentru fuziune, magnetometru, detectorul optic, imagine, validare și triaj. Teste de integrare pe socket-uri reale (bucla locală) pentru lanțul sondă, fuziune. Test instrumentat pentru baza de date, inclusiv migrarea.

**Testat pe telefon:** Samsung Galaxy A53. Senzorii (cameră, bliț, magnetometru) funcționează. Triajul de rețea a fost verificat doar cu teste pe calculator, nu pe telefon.

## Cât de bine detectează acum

Aplicația rulează cap-coadă, dar **nu este încă un detector de camere de încredere**. Motivele, pe module:

- **Rețea (cel mai promițător):** majoritatea camerelor ascunse din cazări sunt IP, pe Wi-Fi, deci acesta este pasul cu cele mai mari șanse. Marchează orice dispozitiv (în afară de router) cu un port de cameră deschis. Imprimantele, televizoarele inteligente și NAS-urile au de obicei portul 80 deschis și vor da alarme false. Nu vede camere care înregistrează pe card SD sau folosesc date mobile. Unele hoteluri izolează clienții și blochează triajul.
- **Optic:** caută orice punct rotund și luminos, nu o lentilă. Șuruburile, cromul, ramele și ecranele lucioase produc la fel de multe puncte ca o lentilă. Pragul de luminanță este fix, iar pragul de 0,5 este depășit de aproape orice reflexie. La rezoluția folosită, lentilele mai mici de 3–4 mm (camerele pinhole) sunt prea mici ca să fie văzute (estimare din cod, neverificată pe telefon).
- **Magnetic:** reacționează la orice metal sau curent, nu doar la o cameră. Pragul minim este 3 µT, iar un perete cu armătură sau o piesă de mobilier îl depășesc ușor. Câmpul unei camere mici este adesea mai slab.
- **Ordinea modulelor:** magnetometrul și optica nu sunt legate. Magnetometrul nu știe unde a văzut optica o zonă suspectă.

## Ce mai trebuie îmbunătățit

În ordinea în care ar conta cel mai mult pentru ca aplicația să detecteze camere în practică.

1. **Reducerea alarmelor false din rețea.**
   - Greutate mai mare pentru porturile RTSP (554, 8554) și pentru serviciile mDNS de cameră decât pentru portul 80.
   - Citirea antetului și a titlului răspunsului HTTP (de exemplu „IP Camera", „Hikvision", „Dahua").
   - Recunoașterea producătorilor de camere după prefixul adresei MAC. Din Android 10 tabela ARP nu mai poate fi citită, deci este nevoie de altă sursă, cum e mDNS sau UPnP/SSDP.
   - Scanarea rețelelor mai mari de /24, când este posibil.
2. **Detectorul optic, cea mai mare diferență pentru camerele fără Wi-Fi.**
   - Compararea cadrelor cu bliț și fără bliț: o lentilă reflectă lumina înapoi spre sursă, o suprafață lucioasă oarecare nu.
   - Urmărirea reflexiei în timp, când telefonul se mișcă (apare doar la un unghi îngust).
   - Scăderea expunerii (compensare negativă), astfel încât doar reflexiile puternice să rămână vizibile.
   - Scanare într-o încăpere întunecată, cu verificarea acestei condiții înainte de pornire.
   - Rezoluție mai mare și analiza culorii, pentru emisia în infraroșu a LED-urilor de noapte (vizibile adesea ca un punct violet-alb în camera telefonului).
   - Rețeaua neuronală din plan: MobileNetV3-small cu transfer learning în PyTorch, export TensorFlow Lite INT8, aplicată pe zona din jurul punctului luminos. Lipsesc încă setul de date, scripturile de antrenare și modelul.
3. **Magnetometrul ca pas de confirmare locală.**
   - Calibrare în zona suspectă, nu în altă parte a camerei, cu compararea față de fundalul de acolo.
   - Legarea pasului de zonele marcate de optică, cu instrucțiuni pe ecran.
   - Un număr minim de secunde peste prag, nu un singur vârf, ca zgomotul să nu declanșeze alarma.
   - Verificarea acurateței raportate de senzor și recalibrare când este scăzută.
4. **Rezultate incomplete.** În prezent se poate sări peste pași, iar rezultatul „risc scăzut" apare chiar dacă nu s-a scanat nimic. Planul cere ca rezultatele parțiale să nu se salveze ca raport complet, plus un avertisment clar.
5. **Evaluare reală, cerută în plan.** Un set de imagini pozitive și negative (negativele cu LED-uri, șuruburi, orificii), rată de detecție, rată de alarme false, precizie, recall, timp de scanare și consum de baterie, cu scripturi Python (scikit-learn, Matplotlib). Fără măsurători nu se poate spune cât de bine funcționează detecția.
6. **Verificare pe mai multe telefoane.** Valorile magnetometrului și ale camerei diferă între modele, iar pragurile au fost încercate doar pe A53.
7. **Triajul pe Android 17.** Din Android 17 (API 37) aplicația cere permisiunea de rulare „rețea locală". Cererea și refuzul ei nu au fost încă încercate pe un telefon cu această versiune.

## Rularea proiectului

Cerințe: Android Studio (sau SDK Android 37 și JDK 17+), un telefon Android 7.0 (API 24) sau mai nou, cu cameră, bliț și magnetometru.

```bash
sh ./gradlew :app:assembleDebug            # construiește aplicația
sh ./gradlew :app:testDebugUnitTest        # teste unit și de integrare, pe calculator
sh ./gradlew :app:connectedDebugAndroidTest # teste pe telefon/emulator conectat
```

Fișierul `gradlew` nu are drept de execuție în depozit, de aceea comenzile folosesc `sh`.

La prima rulare telefonul cere permisiunea pentru **cameră** (scanare optică) și, pe Android 17 sau mai nou, pentru **rețeaua locală** (triaj).

## Structura codului

Totul este în `app/src/main/java/com/example/detectorulcreeps/`:

| Fișier | Rol |
|---|---|
| `MainActivity.kt` | Navigarea între ecrane și fluxul scanării |
| `PreparationScreen.kt` | Avertisment, sfaturi, explicarea permisiunilor |
| `NetworkTriage.kt`, `NetworkScanner.kt`, `NetworkTriageScreen.kt` | Triajul de rețea: logică, partea Android (Wi-Fi, socket-uri, mDNS), ecran |
| `RetroreflectionDetector.kt`, `OpticScanScreen.kt`, `Snapshot.kt` | Scanarea optică, detectorul și imaginea cu chenar |
| `MagnetometerReader.kt`, `MagnetometerProcessor.kt` | Citirea și prelucrarea magnetometrului |
| `Fusion.kt` | Combinarea modulelor în nivelul de risc |
| `ResultScreen.kt`, `ReportScreens.kt`, `Report.kt`, `ReportDatabase.kt` | Rezultat, istoric, detalii și stocarea rapoartelor în SQLite |

## Permisiuni

| Permisiune | La ce folosește |
|---|---|
| `CAMERA` | Scanarea optică |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Triajul de rețea |
| `CHANGE_WIFI_MULTICAST_STATE` | Descoperirea serviciilor mDNS |
| `ACCESS_LOCAL_NETWORK` | Acces la rețeaua locală (cerută de Android 17 sau mai nou) |

## Confidențialitate și limite

- Procesarea este locală, fără server. Scanarea de rețea se face doar în rețeaua la care este conectat utilizatorul.
- Aplicația nu garantează că un spațiu este liber de dispozitive de supraveghere. În cazul unui dispozitiv suspect, recomandarea este să nu fie atins, ci fotografiat, iar gazda sau autoritățile să fie anunțate.

## Licență

Vezi fișierul `LICENSE`.
