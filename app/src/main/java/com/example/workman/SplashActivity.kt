package com.example.workman

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    /**
     * Job id delivered by a push notification.
     *
     * When the app is in the background/killed, FCM messages that carry a
     * `notification` payload are rendered by the system and
     * `onMessageReceived()` is NEVER called — tapping simply launches this
     * activity with the `data` payload as intent extras. Capturing `jobId` here
     * is what makes those notifications deep-link correctly instead of just
     * dumping the user on the dashboard.
     */
    private val deepLinkJobId: String? by lazy {
        intent?.extras?.getString("jobId")?.takeIf { it.isNotBlank() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Light status bar
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        sharedPreferencesHelper = SharedPreferencesHelper(this)

        if (sharedPreferencesHelper.isFirstRun()) {
            sharedPreferencesHelper.clearLoginData()
            sharedPreferencesHelper.setFirstRunDone()
        }

        // Animate logo
        val logo = findViewById<ImageView>(R.id.imgSplashLogo)
        val appName = findViewById<TextView>(R.id.tvSplashAppName)
        val tagline = findViewById<TextView>(R.id.tvSplashTagline)

        // Logo: scale up with overshoot
        val scaleAnim = ScaleAnimation(
            0.3f, 1f, 0.3f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            interpolator = OvershootInterpolator(1.5f)
        }
        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 800 }
        val logoAnim = AnimationSet(false).apply {
            addAnimation(scaleAnim)
            addAnimation(fadeIn)
        }
        logo.startAnimation(logoAnim)

        // App name: fade in after delay
        appName.alpha = 0f
        appName.animate().alpha(1f).setStartDelay(500).setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator()).start()

        // Tagline: fade in after longer delay
        tagline.alpha = 0f
        tagline.animate().alpha(1f).setStartDelay(800).setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator()).start()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateNext()
        }, 2500)
    }

    private fun navigateNext() {
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userChoice = sharedPreferencesHelper.getUserChoice()
        val onboardingDone = sharedPreferencesHelper.isOnboardingDone()

        Log.d(
            "SplashActivity",
            "FirebaseUser: ${firebaseUser?.uid}, Choice: $userChoice, Onboarding: $onboardingDone"
        )

        // 1. First-time user → onboarding.
        if (!onboardingDone) {
            goTo(Intent(this, OnboardingActivity::class.java))
            return
        }

        // 2. Firebase Auth is the SOURCE OF TRUTH for login — not the local flag.
        // This prevents stale or backup-restored SharedPreferences (e.g. after a
        // reinstall, or after backing out of an unfinished sign-up) from pushing
        // an unauthenticated user straight into a dashboard.
        if (firebaseUser == null) {
            sharedPreferencesHelper.setLoggedIn(false)
            goTo(Intent(this, ChooseActivity::class.java))
            return
        }

        // 3. Genuinely authenticated → keep the flag in sync and route by role.
        sharedPreferencesHelper.setLoggedIn(true)
        when (userChoice) {
            "Hiring" -> goTo(
                Intent(this, HomeBossDashboardActivity::class.java),
                allowDeepLink = true
            )

            "Worker" -> goTo(
                Intent(this, HomeWorkerDashboardActivity::class.java),
                allowDeepLink = true
            )
            // Logged in but no saved role → recover it from Firestore.
            else -> fetchRoleAndRoute(firebaseUser.uid)
        }
    }

    private fun fetchRoleAndRoute(uid: String) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role")
                if (!role.isNullOrEmpty()) sharedPreferencesHelper.saveUserChoice(role)
                when (role) {
                    "Hiring" -> goTo(
                        Intent(this, HomeBossDashboardActivity::class.java),
                        allowDeepLink = true
                    )

                    "Worker" -> goTo(
                        Intent(this, HomeWorkerDashboardActivity::class.java),
                        allowDeepLink = true
                    )

                    else -> goTo(Intent(this, ChooseActivity::class.java))
                }
            }
            .addOnFailureListener {
                goTo(Intent(this, ChooseActivity::class.java))
            }
    }

    /**
     * @param allowDeepLink when true and the app was opened from a job
     *   notification, the job details screen is stacked on top of [intent] so
     *   Back still returns to the dashboard.
     */
    private fun goTo(intent: Intent, allowDeepLink: Boolean = false) {
        val jobId = deepLinkJobId
        if (allowDeepLink && jobId != null) {
            TaskStackBuilder.create(this)
                .addNextIntent(intent)
                .addNextIntent(
                    Intent(this, WorkOfferDetailsActivity::class.java)
                        .putExtra("OFFER_ID", jobId)
                )
                .startActivities()
            finish()
            return
        }

        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
