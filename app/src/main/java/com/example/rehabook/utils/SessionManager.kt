package com.example.rehabook.utils

import android.content.Context

object SessionManager {

    private const val PREFS_NAME = "user_prefs"
    private const val KEY_KEEP_LOGGED_IN = "keepLoggedIn"

    fun setKeepLoggedIn(context: Context, keep: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_KEEP_LOGGED_IN, keep).apply()
    }

    fun getKeepLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_KEEP_LOGGED_IN, true) // default: mantener sesión
    }
}
