package com.example.workman

import android.Manifest
import android.content.Intent
import android.os.Bundle
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
import com.example.workman.screens.HomeWorkerDashboardScreen
import com.example.workman.utils.NavigationUtils
import com.example.workman.viewModels.HomeWorkerDashboardViewModel

class HomeWorkerDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val viewModel: HomeWorkerDashboardViewModel = viewModel()
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
                HomeWorkerDashboardScreen(
                    viewModel = viewModel,
                    onOfferClick = { offer ->
                        NavigationUtils.navigateToOfferDetails(this, offer.id)
                    },
                    onNotificationClick = {
                        startActivity(Intent(this, NotificationsActivity::class.java))
                    },
                    onNavHome = {
                        NavigationUtils.navigateToHome(this)
                    },
                    onNavJobs = {
                        NavigationUtils.navigateToMyJobs(this)
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
