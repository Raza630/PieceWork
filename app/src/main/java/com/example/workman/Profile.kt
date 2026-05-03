package com.example.workman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.workman.screens.ProfileScreen
import com.example.workman.viewModels.ProfileViewModel
import androidx.activity.viewModels

class Profile : ComponentActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProfileScreen(
                viewModel = viewModel,
                onBack = { finish() }
            )
        }
    }
}
