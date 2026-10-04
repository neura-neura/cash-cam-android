package com.example.cashcam

import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class ProductDraft(
    val title: String = "", val price: String = "", val status: String = "Nuevo",
    val subtitle: String = "Calidad · Estilo · Confianza", val properties: String = "",
    val ratio: Float = 1f, val brand: String = ""
)

/** Same centered crop used by the camera viewport and the exported product image. */
fun centerCrop(width: Int, height: Int, ratio: Float): FloatArray {
    require(width > 0 && height > 0 && ratio.isFinite() && ratio > 0)
    val w = minOf(width.toFloat(), height * ratio)
    val h = w / ratio
    return floatArrayOf((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2)
}

data class CardLayout(val width: Int, val height: Int, val imageWidth: Int, val imageHeight: Int) {
    val aspectRatio: Float get() = width.toFloat() / height
    val imageTop: Float get() = 310f
    val imageBottom: Float get() = imageTop + imageHeight
}

fun cardLayout(photoRatio: Float): CardLayout {
    require(photoRatio.isFinite() && photoRatio > 0)
    // Preserve the existing square composition; other formats let the photo fill the width.
    val imageWidth = if (photoRatio == 1f) 736 else 960
    val imageHeight = (imageWidth / photoRatio).roundToInt()
    return CardLayout(1040, 504 + imageHeight, imageWidth, imageHeight)
}

object CardRenderer {
    const val WIDTH = 1040

    fun render(photo: File, draft: ProductDraft): Bitmap {
        val source = decodePhoto(photo)
        try {
            val layout = cardLayout(draft.ratio)
            val output = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
            try { draw(Canvas(output), source, draft) }
            catch (error: Exception) { output.recycle(); throw error }
            return output
        } finally { source.recycle() }
    }

    /** Draws the same composition for the live preview and the saved PNG. No decoding or PNG encoding. */
    fun draw(canvas: Canvas, source: Bitmap?, draft: ProductDraft) {
        val layout = cardLayout(draft.ratio)
        canvas.drawColor(Color.rgb(247, 249, 245))
        val ink = Color.rgb(28, 45, 35)
        val green = Color.rgb(36, 103, 73)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        fun label(text: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
            paint.color = color; paint.textSize = size
            paint.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            canvas.drawText(text, x, y, paint)
        }
        fun badge(text: String, bounds: RectF, size: Float, radius: Float) {
            paint.textSize = size
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            val glyphs = Rect()
            paint.getTextBounds(text, 0, text.length, glyphs)
            paint.color = green
            canvas.drawRoundRect(bounds, radius, radius, paint)
            paint.color = Color.WHITE
            canvas.drawText(text, bounds.centerX() - glyphs.exactCenterX(), bounds.centerY() - glyphs.exactCenterY(), paint)
        }
        if (draft.status.isNotBlank()) {
            badge(draft.status.uppercase(), RectF(40f, 32f, 330f, 92f), 30f, 22f)
        }
        paint.textSize = 28f; paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        val brand = android.text.TextUtils.ellipsize(draft.brand.trim(), TextPaint(paint), 620f, android.text.TextUtils.TruncateAt.END).toString()
        label(brand, 1000f - paint.measureText(brand), 72f, 28f, ink, true)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 58f; typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val titleText = draft.title.trim().ifBlank { "Tu producto" }
        val title = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, 960)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(2)
            .setEllipsize(android.text.TextUtils.TruncateAt.END).setIncludePad(false).build()
        canvas.save(); canvas.translate(40f, 112f); title.draw(canvas); canvas.restore()
        paint.textSize = 28f; paint.typeface = Typeface.DEFAULT
        val sub = android.text.TextUtils.ellipsize(draft.subtitle.trim(), TextPaint(paint), 960f, android.text.TextUtils.TruncateAt.END).toString()
        label(sub, (WIDTH - paint.measureText(sub)) / 2, 278f, 28f, Color.DKGRAY)
        val w = layout.imageWidth.toFloat()
        val frame = RectF((WIDTH - w) / 2, layout.imageTop, (WIDTH + w) / 2, layout.imageBottom)
        val path = Path().apply { addRoundRect(frame, 28f, 28f, Path.Direction.CW) }
        canvas.save(); canvas.clipPath(path)
        if (source != null) {
            val crop = centerCrop(source.width, source.height, draft.ratio)
            val src = Rect(crop[0].toInt(), crop[1].toInt(), crop[2].toInt(), crop[3].toInt())
            canvas.drawBitmap(source, src, frame, paint)
        } else {
            paint.color = Color.rgb(228, 235, 228); canvas.drawRect(frame, paint)
        }
        canvas.restore()
        val price = draft.price.trim().ifBlank { "0" }.let { if (it.startsWith("$")) it else "$$it" }
        paint.textSize = 62f; paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        val priceText = android.text.TextUtils.ellipsize(price, TextPaint(paint), 880f, android.text.TextUtils.TruncateAt.END).toString()
        val priceWidth = max(240f, paint.measureText(priceText) + 48f)
        badge(priceText, RectF(40f, layout.imageBottom + 22f, 40f + priceWidth, layout.imageBottom + 114f), 62f, 26f)
        paint.textSize = 26f; paint.typeface = Typeface.DEFAULT
        val props = android.text.TextUtils.ellipsize(draft.properties.trim(), TextPaint(paint), 950f, android.text.TextUtils.TruncateAt.END).toString()
        label(props, 40f, layout.height - 30f, 26f, ink)
    }

    fun decodePhoto(file: File, maxEdge: Int = 2048): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        val bitmap = requireNotNull(BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })) { "Foto no válida" }
        val exif = ExifInterface(file)
        val matrix = Matrix().apply {
            if (exif.isFlipped) postScale(-1f, 1f)
            postRotate(exif.rotationDegrees.toFloat())
        }
        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }
}
