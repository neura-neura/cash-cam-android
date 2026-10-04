package com.example.cashcam.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CashCamTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) darkColorScheme(primary = Color(0xFFA5D5BA), secondary = Color(0xFFB6CCC0))
    else lightColorScheme(primary = Color(0xFF246749), secondary = Color(0xFF4D6357), tertiary = Color(0xFF806326))
    MaterialExpressiveTheme(colorScheme = colors, content = content)
}
