# To-Do List (Android)

Aplicație simplă de tip to-do list, scrisă în Kotlin cu Jetpack Compose.

## Funcții
- Adaugă task-uri
- Bifează ca terminate
- Șterge un task sau toate cele terminate
- Task-urile se salvează în cloud (Firebase Firestore)

## Backend Firebase
Task-urile sunt stocate în Firestore (`users/{uid}/todos`), cu autentificare anonimă.
Datele se sincronizează în cloud și funcționează și offline.

### Configurare Firebase (o singură dată)
1. În [Firebase Console](https://console.firebase.google.com) creează un proiect.
2. Adaugă o aplicație Android cu package name `com.example.todolist` și descarcă `google-services.json`.
3. Pune fișierul în `app/google-services.json`.
4. **Authentication > Sign-in method**: activează **Anonymous**.
5. **Firestore Database**: creează baza de date, apoi la tab-ul Rules lipește conținutul din `firestore.rules` și apasă Publish.

## Rulare
1. Deschide folderul în Android Studio (File > Open).
2. Așteaptă Gradle Sync (Android Studio generează singur Gradle wrapper-ul dacă lipsește).
3. Apasă Run pe un emulator sau pe telefon.

Cerințe: Android Studio recent, JDK 17, minSdk 24.
