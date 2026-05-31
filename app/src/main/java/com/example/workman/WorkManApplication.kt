package com.example.workman

import android.app.Application
import com.example.workman.utils.CategoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkManApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Load dynamic categories from Firestore at app startup
        appScope.launch {
            CategoryRepository.loadFromFirestore()
        }
    }
}

