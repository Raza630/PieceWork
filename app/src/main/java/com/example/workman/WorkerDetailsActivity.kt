package com.example.workman

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.workman.screens.WorkerDetailsScreen

class WorkerDetailsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val workerId = intent.getStringExtra("worker_id")
        if (workerId.isNullOrBlank()) {
            Toast.makeText(this, "Worker ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            WorkerDetailsScreen(
                workerId = workerId,
                onBack = { finish() }
            )
        }
    }
}
