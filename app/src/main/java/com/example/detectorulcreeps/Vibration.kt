package com.example.detectorulcreeps

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Pattern de vibrație (pauză, vibrație, pauză, vibrație...) în milisecunde. */
internal val HIGH_RISK_PATTERN = longArrayOf(0, 400, 150, 400, 150, 700)

/** Vibrează telefonul (actuator) doar pentru risc ridicat. Nu face nimic dacă nu există vibrator. */
fun vibrateForRisk(context: Context, level: RiskLevel) {
    if (level != RiskLevel.HIGH) return
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    if (vibrator == null || !vibrator.hasVibrator()) return
    vibrator.vibrate(VibrationEffect.createWaveform(HIGH_RISK_PATTERN, -1))
}
