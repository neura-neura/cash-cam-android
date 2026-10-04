package com.example.cashcam

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext

data class ProductCard(val id: String, val title: String, val file: File, val thumbnail: File, val created: Long,
    val width: Int = 1040, val height: Int = 1240) {
    val aspectRatio: Float get() = width.toFloat() / height
}
data class ExportResult(val saved: Int, val existing: Int, val failed: Int)

class CardRepository(private val context: Context) {
    private val root = File(context.filesDir, "cards").apply { mkdirs() }
    private val exports = context.getSharedPreferences("exports", Context.MODE_PRIVATE)

    suspend fun list(): List<ProductCard> = withContext(Dispatchers.IO) {
        root.listFiles().orEmpty().asSequence().filter { it.isDirectory && !it.name.startsWith(".") }
            .mapNotNull { folder ->
                try {
                    val info = JSONObject(File(folder, "info.json").readText())
                    ProductCard(folder.name, info.getString("title"), File(folder, "card.png"), File(folder, "thumb.jpg"), info.getLong("created"),
                        info.optInt("width", 1040).coerceAtLeast(1), info.optInt("height", 1240).coerceAtLeast(1))
                        .takeIf { it.file.exists() && it.thumbnail.exists() }
                } catch (_: java.io.IOException) { null } catch (_: org.json.JSONException) { null }
            }.sortedByDescending { it.created }.toList()
    }

    suspend fun create(photo: File, draft: ProductDraft): ProductCard = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val staging = File(root, ".$id").apply { check(mkdir()) }
        val layout = cardLayout(draft.ratio)
        try {
            val bitmap = CardRenderer.render(photo, draft)
            try {
                File(staging, "card.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                val thumbnail = Bitmap.createScaledBitmap(bitmap, 260, (bitmap.height + 2) / 4, true)
                try { File(staging, "thumb.jpg").outputStream().use { check(thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, it)) } }
                finally { thumbnail.recycle() }
            } finally { bitmap.recycle() }
            val created = System.currentTimeMillis()
            File(staging, "info.json").writeText(JSONObject().put("title", draft.title).put("created", created)
                .put("width", layout.width).put("height", layout.height).toString())
            coroutineContext.ensureActive()
            val destination = File(root, id)
            check(staging.renameTo(destination)) { "No se pudo guardar la ficha" }
            ProductCard(id, draft.title, File(destination, "card.png"), File(destination, "thumb.jpg"), created, layout.width, layout.height)
        } finally {
            // Only uncommitted staging files owned by this operation.
            if (staging.exists()) { staging.listFiles().orEmpty().forEach { it.delete() }; staging.delete() }
        }
    }

    suspend fun saveToGallery(cards: List<ProductCard>): ExportResult = withContext(Dispatchers.IO) {
        var saved = 0; var existing = 0; var failed = 0
        val resolver = context.contentResolver
        for (card in cards) {
            coroutineContext.ensureActive()
            var pending: Uri? = null
            try {
                val previous = exports.getString(card.id, null)?.let(Uri::parse)
                val present = previous != null && resolver.query(previous, arrayOf(MediaStore.Images.Media._ID), null, null, null)?.use { it.moveToFirst() } == true
                if (present) { existing++; continue }
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "CashCam-${card.id}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CashCam")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                pending = uri
                requireNotNull(resolver.openOutputStream(uri)).use { output -> card.file.inputStream().use { it.copyTo(output) } }
                check(resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null) == 1)
                exports.edit().putString(card.id, uri.toString()).commit()
                pending = null; saved++
            } catch (_: java.io.IOException) { failed++ }
            catch (_: SecurityException) { failed++ }
            catch (_: IllegalStateException) { failed++ }
            catch (_: IllegalArgumentException) { failed++ }
            finally { pending?.let { resolver.delete(it, null, null) } }
        }
        ExportResult(saved, existing, failed)
    }

    fun shareIntent(cards: List<ProductCard>): Intent {
        require(cards.isNotEmpty())
        val uris = ArrayList(cards.map { FileProvider.getUriForFile(context, "${context.packageName}.files", it.file) })
        return Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, "CashCam", uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
            else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
    }
}
