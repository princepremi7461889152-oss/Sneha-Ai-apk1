package com.example.data.model

import android.graphics.drawable.Drawable

data class InstalledApp(
    val name: String,
    val packageName: String,
    val isSystemApp: Boolean = false,
    val icon: Drawable? = null
)
