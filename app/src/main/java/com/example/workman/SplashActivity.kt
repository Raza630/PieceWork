package com.example.workman

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        sharedPreferencesHelper = SharedPreferencesHelper(this)


        if (sharedPreferencesHelper.isFirstRun()) {
            sharedPreferencesHelper.clearLoginData()
            sharedPreferencesHelper.setFirstRunDone()
        }


        Handler(Looper.getMainLooper()).postDelayed({
            val isLoggedIn = sharedPreferencesHelper.isLoggedIn()
            val userChoice = sharedPreferencesHelper.getUserChoice()

            Log.d("SplashActivity", "LoggedIn: $isLoggedIn, Choice: $userChoice")

            if (!isLoggedIn || userChoice.isNullOrEmpty()) {
                // Either not logged in or no choice selected
                startActivity(Intent(this, ChooseActivity::class.java))
            } else {
                // Logged in and has choice
                val homeIntent = when (userChoice) {
                    "Hiring" -> Intent(this, HomeBossDashboardActivity::class.java)
                    "Worker" -> Intent(this, HomeWorkerDashboardActivity::class.java)
                    else -> Intent(this, ChooseActivity::class.java) // fallback
                }
                startActivity(homeIntent)
            }

            finish()
        }, 3000)

    }
}











//class SplashActivity : AppCompatActivity() {
//
//    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_splash)
//
//        // Initialize SharedPreferencesHelper
//        sharedPreferencesHelper = SharedPreferencesHelper(this)
//
//        // Wait for 3 seconds, then navigate based on user's status
//        Handler(Looper.getMainLooper()).postDelayed({
//            when {
//                !sharedPreferencesHelper.isLoggedIn() -> {
//                    // User not logged in, redirect to ChooseActivity
//                    val chooseIntent = Intent(this, ChooseActivity::class.java)
//                    startActivity(chooseIntent)
//                }
//                else -> {
//                    // User is logged in, check their choice and navigate accordingly
//                    val userChoice = sharedPreferencesHelper.getUserChoice()
//                    val homeIntent = when (userChoice) {
//                        "Hiring" -> Intent(this, HomeBossDashboardActivity::class.java) // Activity for hiring users
//                        "Worker" -> Intent(this, HomeWorkerDashboardActivity::class.java) // Activity for workers
//                        else -> Intent(this, ChooseActivity::class.java) // Default to ChooseActivity if no choice is saved
//                    }
//                    startActivity(homeIntent)
//                }
//            }
//            finish()
//        }, 3000) // 3 seconds delay
//    }
//}

//class SplashActivity : AppCompatActivity() {
//    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_splash)
//
//        // Initialize SharedPreferencesHelper
//        sharedPreferencesHelper = SharedPreferencesHelper(this)
//
//        // Wait for 3 seconds, then navigate based on user's status
//        Handler(Looper.getMainLooper()).postDelayed({
//            when {
//                !sharedPreferencesHelper.isLoggedIn() -> {
//                    // If not logged in, redirect to ChooseActivity
//                    val chooseIntent = Intent(this, ChooseActivity::class.java)
//                    startActivity(chooseIntent)
//                }
//                else -> {
//                    // Redirect to HomeActivity based on user choice
//                    val choice = sharedPreferencesHelper.getUserChoice()
//                    val homeIntent = when (choice) {
//                        "hiring" -> Intent(this, testHomelistUI::class.java) // Add specific activity if needed
//                        "looking" -> Intent(this, HomeListActivity::class.java) // Add specific activity if needed
//                        else -> Intent(this, ChooseActivity::class.java) // Default to ChooseActivity
//                    }
//                    startActivity(homeIntent)
//                }
//            }
//            finish()
//        }, 3000) // 3 seconds delay
//    }
//}



//class SplashActivity : AppCompatActivity() {
//    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_splash)
//
//        // Initialize SharedPreferencesHelper
//        sharedPreferencesHelper = SharedPreferencesHelper(this)
//
//        // Wait for 3 seconds, then check the login status
//        Handler(Looper.getMainLooper()).postDelayed({
//            if (sharedPreferencesHelper.isLoggedIn()) {
//                // User is logged in, redirect to MainActivity
//                val mainIntent = Intent(this@SplashActivity, HomeListActivity::class.java)
//                startActivity(mainIntent)
//                finish()
//            } else {
//                // User is not logged in, redirect to LoginActivity
//                val loginIntent = Intent(this@SplashActivity, MainActivity::class.java)
//                startActivity(loginIntent)
//                finish()
//            }
//        }, 3000) // 3 seconds delay
//    }
//}
