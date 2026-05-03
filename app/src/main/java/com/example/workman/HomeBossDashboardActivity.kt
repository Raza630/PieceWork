package com.example.workman

import android.os.Bundle
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workman.screens.HomeBossDashboardScreen
import android.content.Intent
import com.example.workman.utils.NavigationUtils
import com.example.workman.viewModels.HomeBossDashboardViewModel

class HomeBossDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val viewModel: HomeBossDashboardViewModel = viewModel()
            val context = LocalContext.current
            
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
                    viewModel.updateLocation(context)
                }
            }

            LaunchedEffect(Unit) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                HomeBossDashboardScreen(
                    viewModel = viewModel,
                    onWorkerClick = { worker ->
                        val intent = Intent(this, WorkerDetailsActivity::class.java)
                        intent.putExtra("worker_id", worker.id)
                        startActivity(intent)
                    },
                    onViewOffers = {
                        startActivity(Intent(this, MyJobOffersActivity::class.java))
                    },
                    onCreateWork = {
                        startActivity(Intent(this, CreateWorkActivity::class.java))
                    },
                    onNavProfile = {
                        NavigationUtils.navigateToProfile(this)
                    },
                    onNavChat = {
                        NavigationUtils.navigateToChat(this)
                    }
                )
            }
        }
    }
}
