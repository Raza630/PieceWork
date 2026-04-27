package com.example.workman

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.workman.screens.SignInScreen
import com.example.workman.viewModels.AuthViewModel

import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class SignInActivity : ComponentActivity() {
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            viewModel.signInWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.d("Google", "Google sign in failed: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferencesHelper = SharedPreferencesHelper(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            SignInScreen(
                viewModel = viewModel,
                onNavigateToSignUp = {
                    startActivity(Intent(this, SignUp::class.java))
                    finish()
                },
                onGoogleSignIn = {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                onLoginSuccess = { role ->
                    sharedPreferencesHelper.setLoggedIn(true)
                    sharedPreferencesHelper.saveUserChoice(role)
                    
                    val intent = if (role == "Hiring") {
                        Intent(this, HomeBossDashboardActivity::class.java)
                    } else {
                        Intent(this, HomeWorkerDashboardActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}
