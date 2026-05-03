package com.example.workman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.workman.screens.WorkOfferDetailsScreen

class WorkOfferDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val offerId = intent.getStringExtra("OFFER_ID") ?: ""
        
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorkOfferDetailsScreen(
                        offerId = offerId,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}
