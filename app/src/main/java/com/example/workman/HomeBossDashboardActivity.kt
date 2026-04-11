package com.example.workman

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.workman.screens.HomeBossDashboardScreen
import android.content.Intent

class HomeBossDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                HomeBossDashboardScreen(
                    onWorkerClick = { worker ->
                        val intent = Intent(this, WorkerDetailsActivity::class.java)
                        intent.putExtra("worker_id", worker.id)
                        startActivity(intent)
                    },
                    onViewOffers = {
                        startActivity(Intent(this, BossWorkListActivity::class.java))
                    },
                    onCreateWork = {
                        startActivity(Intent(this, CreateWorkActivity::class.java))
                    },
                    onNavProfile = {
                        startActivity(Intent(this, Profile::class.java))
                    },
                    onNavChat = {
                        startActivity(Intent(this, ChatActivity::class.java))
                    }
                )
            }
        }
    }
}
