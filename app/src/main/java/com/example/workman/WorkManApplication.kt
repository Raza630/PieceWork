package com.example.workman

import android.app.Application
import com.example.workman.utils.CategoryRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mappls.sdk.maps.Mappls
import com.mappls.sdk.services.account.MapplsAccountManager
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

        // Initialize the Mappls (MapmyIndia) Maps SDK. Keys come from local.properties
        // via BuildConfig so they never get committed to source control.
        initMappls()

        // Load dynamic categories from Firestore at app startup
        appScope.launch {
            CategoryRepository.loadFromFirestore()
        }
    }

    private fun initMappls() {
        // Mappls Maps SDK v8.x authenticates with your account keys set BEFORE the SDK
        // is initialized. Values come from local.properties via BuildConfig.
        MapplsAccountManager.getInstance().apply {
            setMapSDKKey(BuildConfig.MAPPLS_MAP_SDK_KEY)
            setRestAPIKey(BuildConfig.MAPPLS_REST_API_KEY)
            setAtlasClientId(BuildConfig.MAPPLS_ATLAS_CLIENT_ID)
            setAtlasClientSecret(BuildConfig.MAPPLS_ATLAS_CLIENT_SECRET)
        }
        Mappls.getInstance(applicationContext)
    }
}

