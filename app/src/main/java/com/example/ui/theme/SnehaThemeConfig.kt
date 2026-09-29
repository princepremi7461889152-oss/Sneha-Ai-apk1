package com.example.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

enum class SnehaAppTheme(
    val id: String,
    val displayName: String,
    val emoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color
) {
    CYBERPUNK(
        id = "cyberpunk",
        displayName = "साइबरपंक नियॉन",
        emoji = "🌌",
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFFA855F7),
        backgroundColor = Color(0xFF0D0E1A),
        surfaceColor = Color(0xFF151829)
    ),
    SWEET_ROSE(
        id = "sweet_rose",
        displayName = "स्वीट रोज़ गोल्ड",
        emoji = "🌸",
        primaryColor = Color(0xFFFB7185),
        secondaryColor = Color(0xFFFBBF24),
        backgroundColor = Color(0xFF160B14),
        surfaceColor = Color(0xFF261222)
    ),
    AMOLED_DARK(
        id = "amoled_dark",
        displayName = "प्योर एमोलेड ब्लैक",
        emoji = "🖤",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF818CF8),
        backgroundColor = Color(0xFF000000),
        surfaceColor = Color(0xFF111111)
    ),
    ROYAL_EMERALD(
        id = "royal_emerald",
        displayName = "रॉयल एमराल्ड",
        emoji = "💎",
        primaryColor = Color(0xFF34D399),
        secondaryColor = Color(0xFF2DD4BF),
        backgroundColor = Color(0xFF061412),
        surfaceColor = Color(0xFF0F2623)
    );

    companion object {
        private const val PREF_NAME = "sneha_theme_prefs"
        private const val KEY_THEME = "selected_theme_id"

        fun fromId(id: String): SnehaAppTheme {
            return entries.find { it.id == id } ?: CYBERPUNK
        }

        fun getSavedTheme(context: Context): SnehaAppTheme {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val id = prefs.getString(KEY_THEME, CYBERPUNK.id) ?: CYBERPUNK.id
            return fromId(id)
        }

        fun saveTheme(context: Context, theme: SnehaAppTheme) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_THEME, theme.id).apply()
        }
    }
}

fun getSnehaColorScheme(theme: SnehaAppTheme): ColorScheme {
    return darkColorScheme(
        primary = theme.primaryColor,
        onPrimary = Color.Black,
        primaryContainer = theme.primaryColor.copy(alpha = 0.2f),
        onPrimaryContainer = Color.White,
        secondary = theme.secondaryColor,
        onSecondary = Color.Black,
        secondaryContainer = theme.secondaryColor.copy(alpha = 0.2f),
        onSecondaryContainer = Color.White,
        tertiary = Color(0xFFF472B6),
        onTertiary = Color.Black,
        background = theme.backgroundColor,
        onBackground = Color(0xFFF8FAFC),
        surface = theme.surfaceColor,
        onSurface = Color(0xFFF8FAFC),
        surfaceVariant = theme.surfaceColor.copy(alpha = 0.85f),
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = Color(0xFF2C3252),
        error = Color(0xFFF87171),
        onError = Color.Black
    )
}
