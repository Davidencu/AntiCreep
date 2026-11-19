package com.example.detectorulcreeps

/** Locurile în care sunt ascunse cel mai des camerele; utilizatorul le bifează pe măsură ce le verifică. */
val PRIORITY_ZONES = listOf(
    "Detector de fum",
    "Încărcătoare și prize",
    "Ceasuri și radio",
    "Rame și tablouri",
    "Oglinzi",
    "Aer condiționat",
    "Televizor și boxe",
    "Lămpi și veioze",
)

/** Zonele bifate, în ordinea din listă, ca text salvat în raport (câte una pe linie). */
fun zonesToText(checked: Set<String>): String = PRIORITY_ZONES.filter { it in checked }.joinToString("\n")

fun zonesFromText(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

/** Rezultatul verificării unghiului pentru un punct suspect. */
enum class AngleVerdict {
    /** Punctul a dispărut sau a slăbit mult la înclinare: așa se comportă reflexia unei lentile. */
    LENS_LIKE,

    /** Punctul a rămas la fel de puternic: mai probabil un LED sau o sursă de lumină. */
    PERSISTENT,

    /** Telefonul nu a fost înclinat destul în timpul dat. */
    INCONCLUSIVE,
}

fun AngleVerdict.label(): String = when (this) {
    AngleVerdict.LENS_LIKE -> "Punctul a dispărut la înclinare: comportament de lentilă."
    AngleVerdict.PERSISTENT -> "Punctul a rămas la înclinare: mai probabil un LED sau o sursă de lumină."
    AngleVerdict.INCONCLUSIVE -> "Telefonul nu a fost înclinat destul; verificarea nu este concludentă."
}

/**
 * Verificarea unghiului (F8): retroreflexia unei lentile se vede doar când blițul și lentila sunt aproape
 * pe aceeași axă, deci la o înclinare de 15–25° punctul slăbește mult sau dispare. O sursă de lumină
 * (LED, bec mic) rămâne vizibilă din mai multe unghiuri.
 *
 * Se pornește cu un punct vizibil (scor > 0). Apoi, pentru fiecare eșantion, se dă unghiul total cu care
 * s-a rotit telefonul de la pornire (din giroscop) și scorul optic curent.
 */
class AngleCheck(
    private val startScore: Float,
    private val minTilt: Float = 15f,
    private val enoughTilt: Float = 25f,
    private val dropRatio: Float = 0.3f,
    private val timeoutMs: Long = 8000,
) {
    private var minScoreTilted = Float.MAX_VALUE
    var verdict: AngleVerdict? = null
        private set

    /** Cât din înclinarea necesară s-a făcut, în [0, 1] (pentru bara de progres). */
    var progress = 0f
        private set

    val canStart: Boolean get() = startScore > 0f

    fun sample(tiltDegrees: Float, score: Float, elapsedMs: Long): AngleVerdict? {
        if (verdict != null) return verdict
        if (!canStart) return AngleVerdict.INCONCLUSIVE.also { verdict = it }
        progress = (tiltDegrees / enoughTilt).coerceIn(0f, 1f)
        if (tiltDegrees >= minTilt) minScoreTilted = minOf(minScoreTilted, score)
        val dropped = minScoreTilted <= dropRatio * startScore
        verdict = when {
            dropped -> AngleVerdict.LENS_LIKE
            tiltDegrees >= enoughTilt -> AngleVerdict.PERSISTENT
            elapsedMs >= timeoutMs -> if (minScoreTilted == Float.MAX_VALUE) AngleVerdict.INCONCLUSIVE else AngleVerdict.PERSISTENT
            else -> null
        }
        return verdict
    }
}
