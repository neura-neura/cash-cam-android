package com.example.cashcam

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.cashcam.theme.CashCamTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class GalleryTest {
    @get:Rule val compose = createComposeRule()
    @Test fun selectedCardTapTogglesSelectionAndUnselectedCardOpensActions() {
        val card = ProductCard("one", "Zapatos", File("missing.png"), File("missing.jpg"), 1)
        var selected by mutableStateOf(setOf("one"))
        var saved = emptyList<ProductCard>()
        compose.setContent {
            CashCamTheme {
                GalleryContent(listOf(card), selected, true, true,
                    onSelect = { selected = if (it in selected) selected - it else selected + it },
                    onGallery = {}, onSave = { saved = it }, onShare = {})
            }
        }
        compose.onNodeWithContentDescription("Zapatos").assertIsSelected().performClick()
        compose.onNodeWithContentDescription("Zapatos").assertIsNotSelected().performClick()
        compose.onNodeWithText("Guardar").performClick()
        assertEquals(listOf(card), saved)
        compose.onNodeWithText("Seleccionar").performClick()
        compose.onNodeWithContentDescription("Zapatos").assertIsSelected()
    }

    @Test fun tallPortraitDetailFitsWithoutHidingSaveAndShare() {
        val card = ProductCard("vertical", "Vertical", File("missing.png"), File("missing.jpg"), 1, height = 2211)
        compose.setContent {
            CashCamTheme {
                GalleryContent(listOf(card), emptySet(), true, true, {}, {}, {}, {})
            }
        }
        compose.onNodeWithContentDescription("Vertical").performClick()
        val image = compose.onNodeWithContentDescription("Ficha de Vertical").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(2211f / 1040f, image.height / image.width, .01f)
        compose.onNodeWithText("Guardar").assertIsDisplayed()
        compose.onNodeWithText("Compartir").assertIsDisplayed()
        compose.onNodeWithText("Cerrar").assertIsDisplayed()
    }
}
