package com.example.workman

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var btnNext: TextView
    private lateinit var btnSkip: TextView
    private lateinit var adapter: OnboardingAdapter
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    private val pages = mutableListOf<OnboardingPage>()

    // ── Permission launchers ─────────────────────────────────────────────────

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            adapter.markPermissionGranted(PermissionType.LOCATION)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            adapter.markPermissionGranted(PermissionType.NOTIFICATION)
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // Light status bar
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        sharedPreferencesHelper = SharedPreferencesHelper(this)

        viewPager = findViewById(R.id.viewPager)
        dotsLayout = findViewById(R.id.dotsLayout)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        buildPages()

        adapter = OnboardingAdapter(pages) { permissionType, _ ->
            requestPermission(permissionType)
        }

        viewPager.adapter = adapter
        setupDots(pages.size)
        updateDots(0)

        // Pre-check already-granted permissions
        if (isLocationGranted()) adapter.markPermissionGranted(PermissionType.LOCATION)
        if (isNotificationGranted()) adapter.markPermissionGranted(PermissionType.NOTIFICATION)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateBottomBar(position)
            }
        })

        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < pages.size - 1) {
                viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    // ── Pages setup ──────────────────────────────────────────────────────────

    private fun buildPages() {
        pages.add(
            OnboardingPage(
                imageRes = R.drawable.ic_onboarding_find,
                title = "Find Skilled Workers\nInstantly",
                description = "Browse verified professionals near you — plumbers, electricians, carpenters, and more. Hire the right person in minutes."
            )
        )
        pages.add(
            OnboardingPage(
                imageRes = R.drawable.ic_onboarding_location,
                title = "Discover Workers\nNear You",
                description = "Enable location access so we can show you the closest available workers and provide accurate distance estimates.",
                permissionType = PermissionType.LOCATION,
                permissionButtonText = "📍  Enable Location"
            )
        )
        pages.add(
            OnboardingPage(
                imageRes = R.drawable.ic_onboarding_notify,
                title = "Never Miss\nan Update",
                description = "Get instant notifications for booking confirmations, new work offers, and messages from workers or employers.",
                permissionType = PermissionType.NOTIFICATION,
                permissionButtonText = "🔔  Enable Notifications"
            )
        )
        pages.add(
            OnboardingPage(
                imageRes = R.drawable.ic_onboarding_ready,
                title = "You're All Set!",
                description = "Whether you're hiring or looking for work — WorkMan connects you with the right people. Let's get started!"
            )
        )
    }

    // ── Permission handling ──────────────────────────────────────────────────

    private fun requestPermission(type: PermissionType) {
        when (type) {
            PermissionType.LOCATION -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            PermissionType.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Notifications are enabled by default below Android 13
                    adapter.markPermissionGranted(PermissionType.NOTIFICATION)
                }
            }

            PermissionType.NONE -> { /* nothing */
            }
        }
    }

    private fun isLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isNotificationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // ── Dot indicators ───────────────────────────────────────────────────────

    private fun setupDots(count: Int) {
        dotsLayout.removeAllViews()
        for (i in 0 until count) {
            val dot = ImageView(this).apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        this@OnboardingActivity,
                        R.drawable.dot_inactive
                    )
                )
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(8, 0, 8, 0)
                layoutParams = params
            }
            dotsLayout.addView(dot)
        }
    }

    private fun updateDots(position: Int) {
        for (i in 0 until dotsLayout.childCount) {
            val dot = dotsLayout.getChildAt(i) as ImageView
            val drawableRes = if (i == position) R.drawable.dot_active else R.drawable.dot_inactive
            dot.setImageDrawable(ContextCompat.getDrawable(this, drawableRes))
        }
    }

    // ── Bottom bar ───────────────────────────────────────────────────────────

    private fun updateBottomBar(position: Int) {
        if (position == pages.size - 1) {
            btnNext.text = "Get Started"
            btnSkip.visibility = View.INVISIBLE
        } else {
            btnNext.text = "Next"
            btnSkip.visibility = View.VISIBLE
        }
    }

    // ── Finish ───────────────────────────────────────────────────────────────

    private fun finishOnboarding() {
        sharedPreferencesHelper.setOnboardingDone()
        startActivity(Intent(this, ChooseActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

