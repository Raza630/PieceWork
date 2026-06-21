package com.example.workman

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.workman.screens.FavoriteWorkersScreen
import com.example.workman.ui.theme.WorkManTheme

/**
 * Activity for boss to manage favorite workers and repeat hiring.
 */
class FavoriteWorkersActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkManTheme {
                FavoriteWorkersScreen(
                    onBack = { finish() },
                    onWorkerClick = { workerId ->
                        val intent = Intent(this, WorkerDetailsActivity::class.java)
                        intent.putExtra("workerId", workerId)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

