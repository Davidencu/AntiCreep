package com.example.detectorulcreeps

import android.content.Context

const val MIN_THRESHOLD = 0.3f
const val MAX_THRESHOLD = 0.8f
const val DEFAULT_THRESHOLD = 0.5f

/** Limitează pragul la intervalul permis (valorile greșite din preferințe nu strică fuziunea). */
fun clampThreshold(value: Float): Float =
    if (value.isNaN()) DEFAULT_THRESHOLD else value.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)

/** Setările aplicației, păstrate în SharedPreferences. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("setari", Context.MODE_PRIVATE)

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(VIBRATION, true)
        set(value) = prefs.edit().putBoolean(VIBRATION, value).apply()

    /** Pragul de la care un modul semnalează (folosit de fuziune). */
    var threshold: Float
        get() = clampThreshold(prefs.getFloat(THRESHOLD, DEFAULT_THRESHOLD))
        set(value) = prefs.edit().putFloat(THRESHOLD, clampThreshold(value)).apply()

    private companion object {
        const val VIBRATION = "vibratie"
        const val THRESHOLD = "prag"
    }
}
