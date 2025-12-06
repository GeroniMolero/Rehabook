package com.example.rehabook.utils

import java.text.SimpleDateFormat
import java.util.*

fun formatTimestamp(ts: Long?): String {
    if (ts == null) return "Sin mensajes"
    val date = java.util.Date(ts)
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(date)
}

