package com.example.rehabook

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar Firebase antes de MainActivity
        FirebaseApp.initializeApp(this)
    }
}
