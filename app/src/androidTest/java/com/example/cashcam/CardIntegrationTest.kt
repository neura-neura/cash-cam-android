package com.example.cashcam

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CardIntegrationTest {
    @Test fun cardsSurviveReloadAndExportWithoutDuplicatesAndShareWithGrants() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = CardRepository(context)
        val photo = File(context.cacheDir, "test-photo.jpg")
        Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
            photo.outputStream().use { compress(Bitmap.CompressFormat.JPEG, 90, it) }
            recycle()
        }
        val created = mutableListOf<ProductCard>()
        try {
            for (ratio in listOf(1f, 16f / 9f, 9f / 16f)) created += repository.create(photo, ProductDraft("Producto de prueba", "250", ratio = ratio))
            assertTrue(repository.list().map { it.id }.containsAll(created.map { it.id }))
            created.zip(listOf(1240, 1044, 2211)).forEach { (card, expectedHeight) ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(card.file.path, bounds)
                assertEquals(1040, bounds.outWidth); assertEquals(expectedHeight, bounds.outHeight)
                val persisted = repository.list().single { it.id == card.id }
                assertEquals(expectedHeight, persisted.height)
                BitmapFactory.decodeFile(card.thumbnail.path, bounds)
                assertEquals(260, bounds.outWidth)
                assertEquals(card.aspectRatio, bounds.outWidth.toFloat() / bounds.outHeight, .005f)
            }
            val oldInfo = File(created.first().file.parentFile, "info.json")
            val legacy = org.json.JSONObject(oldInfo.readText()).apply { remove("width"); remove("height") }
            oldInfo.writeText(legacy.toString())
            assertEquals(1240, repository.list().single { it.id == created.first().id }.height)
            BitmapFactory.decodeFile(created.first().file.path).apply {
                assertEquals(1040, width); assertEquals(1240, height)
                assertTrue(Color.red(getPixel(520, 670)) > 200)
                recycle()
            }
            val first = repository.saveToGallery(created)
            assertEquals(3, first.saved); assertEquals(0, first.failed)
            val repeated = repository.saveToGallery(created)
            assertEquals(0, repeated.saved); assertEquals(3, repeated.existing)
            val single = repository.shareIntent(created.take(1))
            assertEquals(Intent.ACTION_SEND, single.action)
            assertEquals("content", single.clipData!!.getItemAt(0).uri.scheme)
            context.contentResolver.openInputStream(single.clipData!!.getItemAt(0).uri)!!.use { assertTrue(it.read() >= 0) }
            val multiple = repository.shareIntent(created)
            assertEquals(Intent.ACTION_SEND_MULTIPLE, multiple.action)
            assertEquals(3, multiple.clipData!!.itemCount)
            assertTrue(multiple.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        } finally {
            photo.delete()
            created.forEach { card ->
                context.contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Images.Media.DISPLAY_NAME} = ?", arrayOf("CashCam-${card.id}.png"))
                card.file.parentFile!!.listFiles()!!.forEach { it.delete() }
                card.file.parentFile!!.delete()
            }
        }
    }
}
