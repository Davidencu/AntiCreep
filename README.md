# AntiCreep (Detectorul de Creeps)

Aplicație Android care ajută pe cineva aflat într-un spațiu de cazare (apartament închiriat, cameră de hotel) să verifice rapid dacă există indicii ale unei **camere ascunse**. Folosește doar senzorii telefonului, fără echipament special.

Proiect pentru SMA/CHS, de Popa Tijana și Păun David.

> **Important:** aplicația este o verificare preliminară, nu o garanție. Un rezultat „risc scăzut" nu înseamnă că nu există o cameră. Starea actuală a detecției este descrisă mai jos, la „Cât de bine detectează acum".

## Motivația proiectului

Utilizarea camerelor video miniaturale în spații private, precum hoteluri, apartamente închiriate sau vestiare, reprezintă o posibilă problemă de confidențialitate. Identificarea acestora este dificilă, deoarece pot fi camuflate în obiecte obișnuite, pot funcționa fără conexiune la rețeaua Wi-Fi sau pot stoca înregistrările local. Echipamentele specializate de detecție pot fi costisitoare și mai puțin accesibile utilizatorilor obișnuiți.

Proiectul **AntiCreep** urmărește dezvoltarea unei aplicații Android care utilizează componentele hardware disponibile pe un smartphone pentru a identifica indicii ale existenței unor camere ascunse. Soluția combină analiza optică a retroreflexiei lentilelor, scanarea rețelei locale și analiza câmpului magnetic, urmând să integreze și metode de inteligență artificială pentru clasificarea obiectelor suspecte.

Un obiectiv important este reducerea alarmelor false și furnizarea unor rezultate explicabile, însoțite de niveluri de incertitudine și recomandări pentru utilizator. Procesarea locală a datelor contribuie, de asemenea, la protejarea confidențialității.

AntiCreep nu își propune să înlocuiască echipamentele profesionale de detecție, ci să ofere un instrument accesibil de verificare preliminară.

## Stadiul actual al soluțiilor existente (State of the Art)

Pe platforma Android există deja numeroase aplicații destinate identificării camerelor ascunse. Acestea utilizează în general camera telefonului, blițul, magnetometrul, conexiunile Wi-Fi și Bluetooth. Printre soluțiile relevante se numără:

### Aplicații dedicate detecției camerelor ascunse

- **[Glint – Hidden Camera Detector](https://play.google.com/store/apps/details?id=com.pssd.glint)** – combină verificarea reflexiilor lentilelor, detecția surselor infraroșii, scanarea rețelelor Wi-Fi și Bluetooth și analiza câmpului magnetic. Include ghiduri de inspecție și explicarea limitărilor fiecărei metode.

- **[HydraScan – Hidden Camera Scan](https://play.google.com/store/apps/details?id=com.hydrascan.app)** – oferă scanare optică prin iluminare intermitentă, analiză magnetică, identificarea dispozitivelor Bluetooth și auditul rețelei locale. Include și un ghid pentru verificarea încăperilor.

- **[SpyGuard – Hidden Cam Detector](https://play.google.com/store/apps/details?id=com.harun.spyguard)** – utilizează patru metode de verificare: identificarea reflexiilor lentilelor prin bliț controlat, detecție infraroșie, scanare Wi-Fi și măsurarea câmpului magnetic. Funcția avansată de identificare optică este disponibilă contra cost.

- **[Hidden Guard – Camera Detector](https://play.google.com/store/apps/details?id=com.aistudio.hiddenguard.wzpkqr)** – integrează scanare optică asistată de AI, verificarea reflexiilor, detecție infraroșie, magnetometru, scanare Wi-Fi și Bluetooth. Include combinarea rezultatelor, un ghid pentru inspectarea încăperilor și rapoarte.

- **[Hidden Camera Detector FindSpy](https://play.google.com/store/apps/details?id=com.astraler.android.hiddencamera)** – combină scanarea rețelei, detecția Bluetooth, analiza câmpului magnetic și verificarea surselor infraroșii. Oferă și recomandări pentru identificarea dispozitivelor suspecte.

- **[Hidden Camera Detector – FutureApps](https://play.google.com/store/apps/details?id=hiddencamdetector.futureapps.com.hiddencamdetector)** – una dintre aplicațiile cu răspândire mare din această categorie, cu peste 10 milioane de descărcări raportate în Google Play. Principalele funcționalități sunt analiza câmpului magnetic și verificarea surselor infraroșii cu ajutorul camerei telefonului.

### Soluții complementare

- **[Fing – Network Tools](https://play.google.com/store/apps/details?id=com.overlook.android.fing)** – aplicație generală de analiză a rețelelor, care identifică dispozitivele conectate la Wi-Fi, oferă informații despre producător și include scanarea porturilor și descoperirea serviciilor. Poate ajuta la identificarea camerelor conectate la rețeaua locală, dar nu este un detector optic dedicat.

## Cum funcționează

Scanarea are trei pași, de la larg la îngust:

| Pas | Ce face | Ce folosește |
|---|---|---|
| 1. Triaj de rețea | Caută în rețeaua Wi-Fi dispozitive care se comportă ca o cameră (porturi video deschise, servicii mDNS) | Radio Wi-Fi |
| 2. Scanare optică | Caută puncte foarte luminoase și rotunde, cum ar fi reflexia lentilei unei camere, comparând cadre cu blițul stins și aprins | Cameră, bliț, giroscop |
| 3. Confirmare magnetică | Măsoară dacă lângă un obiect apare un câmp magnetic anormal | Magnetometru |

Rezultatele se combină într-un nivel de risc: **scăzut**, **mediu** sau **ridicat**.
- Fiecare modul fizic (optic, magnetic) „semnalează" când scorul lui trece de prag. Fie n numărul celor care semnalează. Pragul este 0,50 și se poate schimba din Setări (între 0,30 și 0,80).
- n = 0: scăzut. n = 1: mediu. n = 2: ridicat.
- Indiciul de rețea poate doar să ridice nivelul: doar el înseamnă mediu, iar n = 1 plus rețea înseamnă ridicat.
- Un triaj de rețea incomplet nu coboară niciodată nivelul.
- Verificarea unghiului, lista zonelor bifate și nivelul de lumină sunt informații pentru utilizator. Ele nu intră în calculul nivelului de risc.

Totul rulează pe telefon. Imaginile și rapoartele nu pleacă de pe dispozitiv.

## Componentele telefonului folosite

| Componentă | Tip | Rol în aplicație |
|---|---|---|
| Camera spate | Senzor | Scanarea optică: puncte luminoase, imaginea zonei suspecte |
| Radio Wi-Fi | Senzor | Triajul de rețea (ping, porturi, mDNS) |
| Magnetometru | Senzor | Confirmarea magnetică de aproape |
| Giroscop | Senzor | Detectează mișcarea prea rapidă a telefonului la scanarea optică și măsoară unghiul de înclinare la verificarea unghiului. Dacă telefonul nu are giroscop, mișcarea se urmărește cu accelerometrul liniar, iar verificarea unghiului nu este disponibilă. |
| Senzor de lumină | Senzor | Afișează lumina din încăpere (în lucși) pe ecranul Pregătire și spune dacă e destul de întuneric |
| Bliț (torch) | Actuator | Sursa de lumină care provoacă reflexia lentilei. În modul implicit clipește. |
| Vibrator | Actuator | Vibrează la rezultatul „risc ridicat" (se poate dezactiva din Setări) |
| Ecran | Interfață | Instrucțiuni, indicatori în timp real, rezultat, rapoarte |

## Ce este implementat

**Fluxul din aplicație:** Start, Pregătire, Triaj de rețea, Scanare optică, Confirmare magnetică, Rezultat. De pe ecranul Start se mai ajunge la Istoric rapoarte (cu Detalii raport), Setări și Ghid și Despre.

- **Pregătire:** avertisment că aplicația nu e o garanție, indicatorul luminii din încăpere (sub 10 lx este destul de întuneric, între 10 și 50 lx lumină slabă, peste 50 lx prea multă lumină), sfaturi, explicarea permisiunilor, bifa „Am citit și am înțeles". Lumina este doar o recomandare și nu blochează scanarea.
- **Triaj de rețea:**
  - Ping și conectare pe porturile 554, 8554, 80, 8080 și 8081 pentru adresele subrețelei Wi-Fi (cel mult un /24), în paralel.
  - Căutare mDNS pentru `_rtsp._tcp` și `_http._tcp`.
  - Lista dispozitivelor cu adrese și porturi, cele suspecte marcate. Routerul nu e niciodată suspect.
  - Se tratează cazurile: permisiune refuzată, telefon fără Wi-Fi, rețea care nu răspunde. Triajul apare atunci „incomplet" și nu coboară nivelul de risc.
- **Scanare optică:**
  - CameraX, imagine 640×480 pe planul de luminanță, analiză cu detector clasic scris în Kotlin.
  - *Modul „diferență" (implicit):* blițul clipește (500 ms aprins, 500 ms stins). Aplicația compară cadrul cu blițul aprins cu cel stins. Becurile, ferestrele și ecranele sunt luminoase în ambele cadre și dispar din diferență, iar o lentilă apare doar cu blițul. Se păstrează punctele mici, rotunde și foarte luminoase în cadrul aprins.
  - *Modul „un cadru":* blițul rămâne aprins sau stins (comutator manual), iar detectorul caută puncte luminoase, mici și rotunde într-un singur cadru.
  - **Giroscop:** când telefonul se mișcă prea repede (peste 0,8 rad/s), cadrele sunt ignorate, iar ecranul cere încetinirea. Fără mișcare lentă, cadrele cu bliț și fără bliț nu se suprapun și apar puncte false.
  - **Verificarea unghiului** (buton, cere giroscop): cu un punct în chenar, utilizatorul înclină telefonul cu 20–25°. Giroscopul măsoară unghiul. Dacă punctul slăbește sub 30% din scorul inițial după 15° de înclinare, verdictul este „comportament de lentilă". Dacă rămâne la fel după 25°, este „mai probabil un LED sau o sursă de lumină". Altfel, „neconcludent".
  - **Zone prioritare:** o listă de 8 locuri obișnuite pentru camere ascunse (detector de fum, încărcătoare și prize, ceasuri și radio, rame și tablouri, oglinzi, aer condiționat, televizor și boxe, lămpi și veioze), pe care utilizatorul le bifează pe măsură ce le verifică.
  - Punctele găsite se încadrează cu roșu, iar cadrul cu scorul cel mai mare se păstrează pentru ecranul Rezultat.
- **Confirmare magnetică:** calibrare de 3 secunde pe fundal, filtru trece-jos, prag și scor în [0, 1], indicator în timp real, buton „Recalibrează".
- **Rezultat:** nivelul de risc cu culoare și text, scorurile, imaginea cu chenar roșu, dispozitivele suspecte din rețea, verdictul verificării unghiului, zonele bifate și cele neverificate. La risc ridicat telefonul vibrează, dacă vibrația este activată.
- **Rapoarte (CRUD):** creare din ecranul Rezultat (nume, notă), listare cu cele mai noi primele, editare și ștergere cu confirmare. Stocare locală în SQLite (versiunea 4 a bazei de date, cu migrări de la versiunile vechi). Raportul păstrează și rezultatul rețelei, zonele bifate și verdictul unghiului.
- **Setări:** prag de semnalare (0,30 – 0,80), vibrație pornită sau oprită, ștergerea întregului istoric.
- **Ghid și Despre:** instrucțiuni de folosire, explicarea fiecărui pas, limitări, ce se face dacă se găsește ceva suspect.
- **Mod demonstrație:** apăsarea lungă pe titlul din ecranul Start afișează un rezultat „risc ridicat" simulat, cu o imagine simulată. Este pentru prezentări. Un raport salvat din acest mod apare în Istoric ca orice alt raport.

**Testare:** 85 de teste unit pe calculator pentru fuziune, magnetometru, detectorul optic și diferența dintre cadre, imagine, validare, triaj de rețea, senzorul de lumină și setări, filtrul de mișcare și verificarea unghiului, zone. Printre ele, teste de integrare pe socket-uri reale (bucla locală) pentru lanțul sondă, baleiere, fuziune. Un test instrumentat pentru baza de date, inclusiv migrarea, care se rulează pe telefon sau emulator. La data scrierii compilează, dar nu a fost rulat în acest mediu.

**Testat pe telefon:** Samsung Galaxy A53. Senzorii telefonului funcționează. Acuratețea detecției nu a fost măsurată, iar triajul de rețea nu are încă o verificare documentată într-o rețea reală.

## Cât de bine detectează acum

Aplicația rulează cap-coadă, dar **nu este încă un detector de camere de încredere**. Motivele, pe module:

- **Rețea (cel mai promițător):** majoritatea camerelor ascunse din cazări sunt IP, pe Wi-Fi, deci acesta este pasul cu cele mai mari șanse. Marchează orice dispozitiv (în afară de router) cu un port de cameră deschis. Imprimantele, televizoarele inteligente și NAS-urile au de obicei portul 80 deschis și vor da alarme false. Nu vede camere care înregistrează pe card SD sau folosesc date mobile. Unele hoteluri izolează clienții și blochează triajul.
- **Optic:** modul cu diferența dintre cadre elimină sursele de lumină constante (becuri, ferestre, ecrane), iar giroscopul oprește analiza când telefonul se mișcă prea repede. Rămân însă problemele de bază:
  - Un punct primește scor mare dacă este mic și rotund. Nu există o verificare reală că este o lentilă. Șuruburile, cromul, ramele și ceramica lucioasă reflectă blițul la fel și apar ca puncte.
  - Aproape orice punct care trece filtrele are un scor ridicat. De aceea, un punct vizibil câteva cadre la rând depășește pragul din Setări.
  - Verificarea unghiului ajută utilizatorul să separe o lentilă de un LED, dar rezultatul ei nu schimbă nivelul de risc.
  - La rezoluția de 640×480, lentilele mai mici de 3–4 mm (camerele pinhole) sunt probabil prea mici ca să fie văzute (estimare din cod, neverificată pe telefon).
- **Magnetic:** reacționează la orice metal sau curent, nu doar la o cameră. Pragul minim este 3 µT, iar un perete cu armătură sau o piesă de mobilier îl depășesc ușor. Câmpul unei camere mici este adesea mai slab. Calibrarea se face într-un singur loc, iar în timpul scanării telefonul se mută prin cameră.
- **Legătura dintre module:** magnetometrul nu știe unde a văzut optica o zonă suspectă. Ecranul doar îndeamnă utilizatorul să apropie telefonul de acele zone.

## Ce mai trebuie îmbunătățit

În ordinea în care ar conta cel mai mult pentru ca aplicația să detecteze camere în practică.

1. **Reducerea alarmelor false din rețea.**
   - Greutate mai mare pentru porturile RTSP (554, 8554) și pentru serviciile mDNS de cameră decât pentru portul 80.
   - Citirea antetului și a titlului răspunsului HTTP (de exemplu „IP Camera", „Hikvision", „Dahua").
   - Recunoașterea producătorilor de camere după prefixul adresei MAC. Din Android 10 tabela ARP nu mai poate fi citită, deci este nevoie de altă sursă, cum e mDNS sau UPnP/SSDP.
   - Scanarea rețelelor mai mari de /24, când este posibil.
2. **Detectorul optic, cea mai mare diferență pentru camerele fără Wi-Fi.**
   - Un scor care măsoară cât seamănă punctul cu o lentilă (contrast față de zona din jur, stabilitate în timp, dispariția la înclinare), nu doar cât de rotund este.
   - Includerea verificării unghiului în scor (un punct care dispare la înclinare crește scorul, unul care rămâne îl scade) și rularea ei automată, nu doar la buton.
   - Scăderea expunerii (compensare negativă), astfel încât doar reflexiile puternice să rămână vizibile.
   - Avertizare sau blocare când lumina din încăpere este prea mare. În prezent senzorul de lumină doar informează.
   - Rezoluție mai mare și analiza culorii, pentru emisia în infraroșu a LED-urilor de noapte (vizibile adesea ca un punct violet-alb în camera telefonului).
   - Rețeaua neuronală din plan: MobileNetV3-small cu transfer learning în PyTorch, export TensorFlow Lite INT8, aplicată pe zona din jurul punctului luminos. Lipsesc încă setul de date, scripturile de antrenare și modelul.
3. **Magnetometrul ca pas de confirmare locală.**
   - Calibrare în zona suspectă, nu în altă parte a camerei, cu compararea față de fundalul de acolo.
   - Legarea pasului de zonele marcate de optică, cu instrucțiuni pe ecran.
   - Un număr minim de secunde peste prag, nu un singur vârf, ca zgomotul să nu declanșeze alarma.
   - Verificarea acurateței raportate de senzor și recalibrare când este scăzută.
4. **Rezultate incomplete.** În prezent se poate trece la rezultat fără să se fi scanat nimic, iar atunci apare „risc scăzut". Planul cere ca rezultatele parțiale să nu se salveze ca raport complet, plus un avertisment clar.
5. **Evaluare reală, cerută în plan.** Un set de imagini pozitive și negative (negativele cu LED-uri, șuruburi, orificii), rată de detecție, rată de alarme false, precizie, recall, timp de scanare și consum de baterie, cu scripturi Python (scikit-learn, Matplotlib). Fără măsurători nu se poate spune cât de bine funcționează detecția.
6. **Verificare pe mai multe telefoane.** Valorile magnetometrului și ale camerei diferă între modele, iar pragurile au fost încercate doar pe A53. Telefoanele fără giroscop nu au verificarea unghiului și folosesc doar accelerometrul pentru viteza de mișcare.
7. **Triajul pe Android 17.** Din Android 17 (API 37) aplicația cere permisiunea de rulare „rețea locală". Cererea și refuzul ei nu au fost încă încercate pe un telefon cu această versiune.
8. **Rapoarte din modul demonstrație.** Rapoartele create din modul demonstrație pot fi confundate cu cele reale. Ar fi utilă marcarea lor sau blocarea salvării.

## Rularea proiectului

Cerințe: Android Studio (sau SDK Android 37 și JDK 17+), un telefon Android 7.0 (API 24) sau mai nou, cu cameră, bliț și magnetometru. Giroscopul este recomandat, dar nu obligatoriu.

```bash
sh ./gradlew :app:assembleDebug            # construiește aplicația
sh ./gradlew :app:testDebugUnitTest        # teste unit și de integrare, pe calculator
sh ./gradlew :app:connectedDebugAndroidTest # teste pe telefon/emulator conectat
```

Fișierul `gradlew` nu are drept de execuție în depozit, de aceea comenzile folosesc `sh`. Fișierul `local.properties` nu este în depozit. Android Studio îl creează la deschiderea proiectului. În linia de comandă, calea către SDK se dă prin variabila `ANDROID_HOME` sau prin linia `sdk.dir=...` din `local.properties`.

La prima rulare telefonul cere permisiunea pentru **cameră** (scanare optică) și, pe Android 17 sau mai nou, pentru **rețeaua locală** (triaj).

## Structura codului

Totul este în `app/src/main/java/com/example/detectorulcreeps/`:

| Fișier | Rol |
|---|---|
| `MainActivity.kt` | Navigarea între ecrane, fluxul scanării, ecranul Start, scanarea magnetică, modul demonstrație |
| `PreparationScreen.kt`, `LightSensorReader.kt` | Pregătire: avertisment, sfaturi, permisiuni, citirea senzorului de lumină |
| `NetworkTriage.kt`, `NetworkScanner.kt`, `NetworkTriageScreen.kt` | Triajul de rețea: logică, partea Android (Wi-Fi, socket-uri, mDNS), ecran |
| `RetroreflectionDetector.kt`, `OpticScanScreen.kt`, `Snapshot.kt` | Scanarea optică: detectorul pe un cadru, diferența dintre cadre, ecranul cu camera și imaginea cu chenar |
| `MotionReader.kt` | Giroscop (sau accelerometru): mișcare prea rapidă și unghiul de înclinare |
| `ZoneChecks.kt` | Lista zonelor prioritare și logica verificării unghiului |
| `MagnetometerReader.kt`, `MagnetometerProcessor.kt` | Citirea și prelucrarea magnetometrului |
| `Fusion.kt` | Combinarea modulelor în nivelul de risc |
| `ResultScreen.kt`, `Vibration.kt` | Ecranul Rezultat și vibrația la risc ridicat |
| `ReportScreens.kt`, `Report.kt`, `ReportDatabase.kt` | Istoric, detalii și stocarea rapoartelor în SQLite |
| `SettingsScreen.kt`, `AppSettings.kt` | Setări: prag, vibrație, ștergerea istoricului |
| `GuideScreen.kt` | Ghid și Despre |

## Permisiuni

| Permisiune | La ce folosește |
|---|---|
| `CAMERA` | Scanarea optică |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Triajul de rețea |
| `CHANGE_WIFI_MULTICAST_STATE` | Descoperirea serviciilor mDNS |
| `ACCESS_LOCAL_NETWORK` | Acces la rețeaua locală (cerută de Android 17 sau mai nou) |
| `VIBRATE` | Vibrația la risc ridicat |

Magnetometrul, giroscopul și senzorul de lumină nu cer permisiuni.

### Concluzii asupra stadiului actual

Soluțiile existente oferă deja o mare parte dintre funcționalitățile propuse pentru AntiCreep, iar unele integrează inclusiv inteligență artificială și combinarea mai multor metode de detecție. Prin urmare, diferențierea proiectului nu se poate baza exclusiv pe numărul de senzori sau pe integrarea unui model AI.

AntiCreep urmărește să se diferențieze prin accentul pus pe **fiabilitatea detecției, reducerea alarmelor false și explicabilitatea rezultatelor**. Integrarea analizei temporale a reflexiilor, clasificarea locală a obiectelor suspecte și evaluarea experimentală a performanței pe dispozitive Android diferite pot contribui la obținerea unei soluții mai transparente și mai utile pentru utilizator.

Aplicațiile prezentate descriu funcționalități declarate de dezvoltatorii lor; existența acestora în Google Play nu demonstrează automat acuratețea detecției în condiții reale.

## Documentația proiectului

În depozit se află și documentele proiectului:
- `Detectorul-de-Creeps-Documentatie.docx`: documentația proiectului.
- `Detectorul-de-Creeps-Documentatie-Simplificat.docx`: varianta simplificată a documentației (descriere, cerințe, user stories, diagrame, tech stack).
- `Detectorul-de-Creeps-Prezentare.pptx`: prezentarea proiectului.

## Confidențialitate și limite

- Procesarea este locală, fără server. Scanarea de rețea se face doar în rețeaua la care este conectat utilizatorul.
- Aplicația nu garantează că un spațiu este liber de dispozitive de supraveghere. În cazul unui dispozitiv suspect, recomandarea este să nu fie atins, ci fotografiat, iar gazda sau autoritățile să fie anunțate.

## Licență

Vezi fișierul `LICENSE`.
