package com.example.cashcam

import android.content.Context

/** Defaults apply to future cards; existing exported PNGs keep their original content. */
class CardDefaults(context: Context) {
    private val preferences = context.getSharedPreferences("card_defaults", Context.MODE_PRIVATE)
    fun brand(): String = preferences.getString("brand", "") ?: ""
    fun subtitle(): String = preferences.getString("subtitle", ProductDraft().subtitle) ?: ""
    fun save(brand: String, subtitle: String) {
        preferences.edit().putString("brand", brand).putString("subtitle", subtitle).apply()
    }
}
