package com.example.cashcam

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.cashcam.theme.CashCamTheme
import org.junit.Rule
import org.junit.Test

class LivePreviewTest {
    @get:Rule val compose = createComposeRule()
    @Test fun editingTheActualFormUpdatesTheVisiblePreviewBeforeCreatingACard() {
        var state by mutableStateOf(CashCamState(loaded = true))
        compose.setContent {
            CashCamTheme {
                CaptureContent(state, permission = false, denied = false, onPermission = {}, onSettings = {},
                    onEdit = { state = state.copy(draft = it) }, onCaptured = {}, onRetake = {}, onCreate = {}, onError = {})
            }
        }
        compose.onNodeWithText("Título del producto").performScrollTo().performTextInput("Dulce")
        compose.onNode(hasContentDescription("Vista previa: Dulce", substring = true)).assertIsDisplayed()
        compose.onNodeWithText("Texto de la esquina (opcional)").performScrollTo().performTextInput("Mi tienda")
        compose.onNode(hasContentDescription("Mi tienda", substring = true)).assertIsDisplayed()
        compose.onNodeWithText("Subtítulo (opcional)").performScrollTo().performTextClearance()
        compose.onNodeWithText("Subtítulo (opcional)").performTextInput("Hecho a mano")
        compose.onNode(hasContentDescription("Hecho a mano", substring = true)).assertIsDisplayed()
        compose.onNodeWithText("Crear ficha").performScrollTo().assertIsNotEnabled()
        compose.onNode(hasContentDescription("Vista previa: Dulce", substring = true)).assertIsDisplayed()
    }
}
