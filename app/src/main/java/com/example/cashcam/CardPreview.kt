package com.example.cashcam

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LiveCardPreview(photoPath: String?, draft: ProductDraft, modifier: Modifier = Modifier) {
    val photo by produceState<Bitmap?>(null, photoPath) {
        value = null
        value = photoPath?.let { path ->
            withContext(Dispatchers.IO) {
                try { CardRenderer.decodePhoto(File(path), maxEdge = 1024) }
                catch (_: java.io.IOException) { null }
                catch (_: IllegalArgumentException) { null }
            }
        }
    }
    // Compose can still draw a previous frame during replacement; let GC own preview bitmaps.
    // The export renderer explicitly releases its exclusively owned bitmaps.
    ProductCardPreview(photo, draft, modifier)
}

@Composable
fun ProductCardPreview(photo: Bitmap?, draft: ProductDraft, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics {
        contentDescription = "Vista previa: ${draft.title}, ${draft.price}, ${draft.status}, ${draft.brand}, ${draft.subtitle}, ${draft.properties}"
    }) {
        drawIntoCanvas { target ->
            val canvas = target.nativeCanvas
            canvas.save()
            try {
                val layout = cardLayout(draft.ratio)
                canvas.scale(size.width / layout.width, size.height / layout.height)
                CardRenderer.draw(canvas, photo, draft)
            } finally { canvas.restore() }
        }
    }
}
