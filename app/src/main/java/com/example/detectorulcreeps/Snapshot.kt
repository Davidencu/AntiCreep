package com.example.detectorulcreeps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** O imagine gri, cu pixelii în format ARGB. */
class GrayImage(val width: Int, val height: Int, val pixels: IntArray)

/** Transformă cadrul de luminanță într-o imagine gri, micșorată de [step] ori. */
fun LumaFrame.toGray(step: Int = 2): GrayImage {
    val w = width / step
    val h = height / step
    val pixels = IntArray(w * h)
    for (y in 0 until h) for (x in 0 until w) {
        val v = at(x * step, y * step)
        pixels[y * w + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    return GrayImage(w, h, pixels)
}

/**
 * Imaginea zonei suspecte: cadrul camerei, rotit vertical, cu un chenar roșu pe fiecare punct luminos.
 * [blobs] sunt deja în coordonatele imaginii verticale (vezi [Blob.rotated]).
 */
fun createSnapshot(frame: LumaFrame, rotationDegrees: Int, blobs: List<Blob>): Bitmap {
    val gray = frame.toGray()
    val base = Bitmap.createBitmap(gray.pixels, gray.width, gray.height, Bitmap.Config.ARGB_8888)
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val out = Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)
        .copy(Bitmap.Config.ARGB_8888, true)
    val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    val canvas = Canvas(out)
    for (blob in blobs) {
        val w = max(blob.width * out.width, MIN_BOX_PX)
        val h = max(blob.height * out.height, MIN_BOX_PX)
        val cx = blob.centerX * out.width
        val cy = blob.centerY * out.height
        canvas.drawRect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2, paint)
    }
    return out
}

private const val MIN_BOX_PX = 16f

fun Bitmap.toJpeg(quality: Int = 85): ByteArray =
    ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()

fun ByteArray.toBitmap(): Bitmap? = BitmapFactory.decodeByteArray(this, 0, size)
