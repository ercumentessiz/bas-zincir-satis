package com.baszincir.satis

import android.app.Application
import com.google.firebase.FirebaseApp

class BasZincirApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
