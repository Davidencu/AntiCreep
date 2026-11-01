package com.example.detectorulcreeps

enum class RiskLevel { LOW, MEDIUM, HIGH }

/**
 * @param combinedScore scorul combinat w1·S_m + w2·S_o, în [0, 1]
 * @param magneticFlag modulul magnetic a depășit pragul
 * @param opticFlag modulul optic a depășit pragul
 */
data class FusionResult(
    val level: RiskLevel,
    val combinedScore: Float,
    val magneticScore: Float,
    val opticScore: Float,
    val magneticFlag: Boolean,
    val opticFlag: Boolean,
)

/**
 * Combină scorurile celor două module. Fiecare modul "semnalează" dacă scorul lui depășește [threshold]:
 * niciunul înseamnă risc scăzut, unul risc mediu (avertizare), ambele risc ridicat.
 */
class FusionModule(
    private val magneticWeight: Float = 0.5f,
    private val opticWeight: Float = 0.5f,
    private val threshold: Float = 0.5f,
) {
    fun fuse(magneticScore: Float, opticScore: Float): FusionResult {
        val m = magneticScore.coerceIn(0f, 1f)
        val o = opticScore.coerceIn(0f, 1f)
        val magneticFlag = m >= threshold
        val opticFlag = o >= threshold
        val level = when {
            magneticFlag && opticFlag -> RiskLevel.HIGH
            magneticFlag || opticFlag -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        val combined = (magneticWeight * m + opticWeight * o).coerceIn(0f, 1f)
        return FusionResult(level, combined, m, o, magneticFlag, opticFlag)
    }
}
