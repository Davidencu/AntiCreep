package com.example.detectorulcreeps

enum class RiskLevel { LOW, MEDIUM, HIGH }

/**
 * @param combinedScore scorul combinat w1·S_m + w2·S_o, în [0, 1]
 * @param magneticFlag modulul magnetic a depășit pragul
 * @param opticFlag modulul optic a depășit pragul
 * @param networkFlag indiciul de rețea: există cel puțin un dispozitiv suspect
 * @param networkComplete false dacă triajul de rețea nu a putut rula până la capăt
 * @param networkDevices dispozitivele suspecte din rețea, câte unul pe linie (gol dacă nu sunt)
 */
data class FusionResult(
    val level: RiskLevel,
    val combinedScore: Float,
    val magneticScore: Float,
    val opticScore: Float,
    val magneticFlag: Boolean,
    val opticFlag: Boolean,
    val networkFlag: Boolean = false,
    val networkComplete: Boolean = true,
    val networkDevices: String = "",
)

/**
 * Combină modulele. Fiecare modul fizic (magnetic, optic) "semnalează" dacă scorul lui depășește [threshold];
 * n este numărul modulelor care semnalează. Indiciul de rețea R poate doar să ridice nivelul:
 * - n = 0 și fără R: risc scăzut
 * - n = 1, sau doar R: risc mediu
 * - n = 2, sau n = 1 împreună cu R: risc ridicat
 *
 * Un triaj de rețea incomplet nu are R și nu coboară nivelul.
 */
class FusionModule(
    private val magneticWeight: Float = 0.5f,
    private val opticWeight: Float = 0.5f,
    private val threshold: Float = 0.5f,
) {
    fun fuse(magneticScore: Float, opticScore: Float, network: NetworkTriageResult? = null): FusionResult {
        val m = magneticScore.coerceIn(0f, 1f)
        val o = opticScore.coerceIn(0f, 1f)
        val magneticFlag = m >= threshold
        val opticFlag = o >= threshold
        val networkFlag = network?.hint == true
        val n = listOf(magneticFlag, opticFlag).count { it }
        val level = when {
            n >= 2 || (n == 1 && networkFlag) -> RiskLevel.HIGH
            n == 1 || networkFlag -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        val combined = (magneticWeight * m + opticWeight * o).coerceIn(0f, 1f)
        return FusionResult(
            level = level,
            combinedScore = combined,
            magneticScore = m,
            opticScore = o,
            magneticFlag = magneticFlag,
            opticFlag = opticFlag,
            networkFlag = networkFlag,
            networkComplete = network?.complete ?: false,
            networkDevices = network?.suspectsSummary().orEmpty(),
        )
    }
}
