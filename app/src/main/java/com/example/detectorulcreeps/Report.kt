package com.example.detectorulcreeps

/** Un raport salvat: rezultatul unei scanări, cu numele și nota date de utilizator. */
data class Report(
    val id: Long = 0,
    val name: String,
    val note: String,
    val createdAt: Long,
    val result: FusionResult,
    /** Imaginea zonei suspecte (JPEG, cu chenarele desenate), sau null dacă nu s-a găsit niciun punct. */
    val snapshot: ByteArray? = null,
)

const val MAX_NAME_LENGTH = 60
const val MAX_NOTE_LENGTH = 500

/** Verifică numele și nota unui raport; întoarce mesajul de eroare sau null dacă sunt corecte. */
fun validateReport(name: String, note: String): String? = when {
    name.isBlank() -> "Dați un nume raportului."
    name.trim().length > MAX_NAME_LENGTH -> "Numele poate avea cel mult $MAX_NAME_LENGTH de caractere."
    note.trim().length > MAX_NOTE_LENGTH -> "Nota poate avea cel mult $MAX_NOTE_LENGTH de caractere."
    else -> null
}
