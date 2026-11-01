package com.example.detectorulcreeps

import kotlin.math.max
import kotlin.math.min

/** Un cadru de luminanță (planul Y al imaginii), cu valori 0..255. */
class LumaFrame(val width: Int, val height: Int, val rowStride: Int, val data: ByteArray) {
    fun at(x: Int, y: Int): Int = data[y * rowStride + x].toInt() and 0xFF
}

/** Un punct luminos candidat; coordonatele sunt normalizate în [0, 1]. */
data class Blob(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val score: Float,
)

/** Rotește punctul cu [degrees] (0, 90, 180, 270, în sens orar), ca să fie pe imaginea afișată vertical. */
fun Blob.rotated(degrees: Int): Blob = when (((degrees % 360) + 360) % 360) {
    90 -> copy(centerX = 1f - centerY, centerY = centerX, width = height, height = width)
    180 -> copy(centerX = 1f - centerX, centerY = 1f - centerY)
    270 -> copy(centerX = centerY, centerY = 1f - centerX, width = height, height = width)
    else -> this
}

data class OpticDetection(val score: Float, val blobs: List<Blob>)

/**
 * Detector clasic de retroreflexie: cu blițul aprins, lentila unei camere reflectă lumina înapoi
 * și apare ca un punct foarte luminos, mic și rotund.
 *
 * Pași: prag pe luminanță, componente conexe, apoi se păstrează doar punctele de mărime potrivită,
 * cu formă aproape pătrată și umplere apropiată de a unui cerc (~0,785, mai mică pe puncte mici, discretizate). Obiectele mari sau alungite
 * (becuri, ferestre, reflexii pe margini) sunt respinse.
 */
class RetroreflectionDetector(
    private val threshold: Int = 240,
    private val step: Int = 2,
    private val minCells: Int = 4,
    private val maxAreaFraction: Float = 0.01f,
    private val maxAspect: Float = 1.7f,
    private val minFill: Float = 0.5f,
) {
    fun detect(frame: LumaFrame): OpticDetection {
        val gw = frame.width / step
        val gh = frame.height / step
        if (gw == 0 || gh == 0) return OpticDetection(0f, emptyList())

        val bright = BooleanArray(gw * gh)
        for (gy in 0 until gh) for (gx in 0 until gw) {
            bright[gy * gw + gx] = frame.at(gx * step, gy * step) >= threshold
        }

        val visited = BooleanArray(gw * gh)
        val stack = IntArray(gw * gh)
        val maxCells = (maxAreaFraction * gw * gh).toInt()
        val blobs = mutableListOf<Blob>()

        for (start in bright.indices) {
            if (!bright[start] || visited[start]) continue
            var sp = 0
            stack[sp++] = start
            visited[start] = true
            var count = 0
            var minX = gw; var maxX = 0; var minY = gh; var maxY = 0
            while (sp > 0) {
                val idx = stack[--sp]
                val x = idx % gw
                val y = idx / gw
                count++
                minX = min(minX, x); maxX = max(maxX, x)
                minY = min(minY, y); maxY = max(maxY, y)
                if (x > 0) sp = push(idx - 1, bright, visited, stack, sp)
                if (x < gw - 1) sp = push(idx + 1, bright, visited, stack, sp)
                if (y > 0) sp = push(idx - gw, bright, visited, stack, sp)
                if (y < gh - 1) sp = push(idx + gw, bright, visited, stack, sp)
            }
            if (count < minCells || count > maxCells) continue
            val bw = maxX - minX + 1
            val bh = maxY - minY + 1
            val aspect = max(bw, bh).toFloat() / min(bw, bh)
            val fill = count.toFloat() / (bw * bh)
            if (aspect > maxAspect || fill < minFill) continue
            val roundness = min(1f, fill / CIRCLE_FILL)
            blobs += Blob(
                centerX = (minX + bw / 2f) / gw,
                centerY = (minY + bh / 2f) / gh,
                width = bw.toFloat() / gw,
                height = bh.toFloat() / gh,
                score = roundness,
            )
        }

        if (blobs.isEmpty()) return OpticDetection(0f, emptyList())
        // Multe puncte luminoase într-o scenă sunt mai probabil reflexii decât o lentilă: scorul scade.
        val best = blobs.maxOf { it.score }
        return OpticDetection(best / (1f + 0.25f * (blobs.size - 1)), blobs)
    }

    private fun push(idx: Int, bright: BooleanArray, visited: BooleanArray, stack: IntArray, sp: Int): Int {
        if (!bright[idx] || visited[idx]) return sp
        visited[idx] = true
        stack[sp] = idx
        return sp + 1
    }

    private companion object {
        const val CIRCLE_FILL = 0.65f
    }
}

/** Starea modulului optic: scorul curent, netezit, și cel mai mare scor din scanare. */
data class OpticState(
    val score: Float,
    val filteredScore: Float,
    val peakScore: Float,
    val blobCount: Int,
    /** Punctele din cadrul curent, în coordonatele imaginii afișate vertical. */
    val blobs: List<Blob> = emptyList(),
    /** Raportul lățime / înălțime al imaginii afișate. */
    val frameAspect: Float = 0.75f,
)

class OpticProcessor(private val alpha: Float = 0.3f) {
    private var filtered = 0f
    private var peak = 0f

    fun reset() {
        filtered = 0f
        peak = 0f
    }

    fun process(detection: OpticDetection, frameAspect: Float = 0.75f): OpticState {
        filtered += alpha * (detection.score - filtered)
        peak = max(peak, filtered)
        return OpticState(detection.score, filtered, peak, detection.blobs.size, detection.blobs, frameAspect)
    }
}
