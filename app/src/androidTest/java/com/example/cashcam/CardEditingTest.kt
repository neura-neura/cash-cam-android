package com.example.cashcam

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test

class CardEditingTest {
    @Test fun statusAndPriceGlyphsAreCenteredInsideTheirBadges() {
        val bitmap = Bitmap.createBitmap(1040, 1240, Bitmap.Config.ARGB_8888)
        try {
            CardRenderer.draw(Canvas(bitmap), null, ProductDraft("dulce", "50", "Usado"))
            fun assertWhiteTextCentered(left: Int, top: Int, right: Int, bottom: Int) {
                var minX = right; var maxX = left; var minY = bottom; var maxY = top
                for (y in top until bottom) for (x in left until right) {
                    val pixel = bitmap.getPixel(x, y)
                    if (Color.red(pixel) > 250 && Color.green(pixel) > 250 && Color.blue(pixel) > 250) {
                        minX = minOf(minX, x); maxX = maxOf(maxX, x)
                        minY = minOf(minY, y); maxY = maxOf(maxY, y)
                    }
                }
                assertTrue("Debe haber texto blanco", minX < maxX)
                assertEquals((left + right) / 2f, (minX + maxX) / 2f, 1.5f)
                assertEquals((top + bottom) / 2f, (minY + maxY) / 2f, 1.5f)
            }
            assertWhiteTextCentered(40, 32, 330, 92)
            assertWhiteTextCentered(40, 1068, 280, 1160)
        } finally { bitmap.recycle() }
    }

    @Test fun brandAndSubtitleSurviveFreshViewModelAndAllowPersistentEmptyValues() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val defaults = CardDefaults(app)
        val oldBrand = defaults.brand(); val oldSubtitle = defaults.subtitle()
        try {
            val first = CashCamViewModel(app, SavedStateHandle())
            first.edit(first.state.value.draft.copy(brand = "Mi tienda", subtitle = "Envíos a todo México"))
            val reopened = CashCamViewModel(app, SavedStateHandle())
            assertEquals("Mi tienda", reopened.state.value.draft.brand)
            assertEquals("Envíos a todo México", reopened.state.value.draft.subtitle)
            reopened.edit(reopened.state.value.draft.copy(brand = "", subtitle = ""))
            val third = CashCamViewModel(app, SavedStateHandle())
            assertEquals("", third.state.value.draft.brand)
            assertEquals("", third.state.value.draft.subtitle)
        } finally { defaults.save(oldBrand, oldSubtitle) }
    }
}
