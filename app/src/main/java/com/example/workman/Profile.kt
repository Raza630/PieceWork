package com.example.workman

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.workman.screens.ProfileScreen
import com.example.workman.viewModels.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

class Profile : ComponentActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProfileScreen(
                viewModel = viewModel,
                onBack = { finish() },
                onLogout = {
                    // Clear login state
                    SharedPreferencesHelper(this).clearLoginData()
                    FirebaseAuth.getInstance().signOut()
                    // Navigate to ChooseActivity (role selection → login)
                    val intent = Intent(this, ChooseActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}
