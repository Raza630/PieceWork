package com.example.workman

import android.app.Application
import com.example.workman.utils.CategoryRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkManApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase Crashlytics for crash reporting
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Initialize Firebase Analytics for usage tracking
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)

        // Load dynamic categories from Firestore at app startup
        appScope.launch {
            CategoryRepository.loadFromFirestore()
        }
    }
}

