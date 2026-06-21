package com.example.workman.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.workman.ChatListActivity
import com.example.workman.HomeBossDashboardActivity
import com.example.workman.HomeWorkerDashboardActivity
import com.example.workman.MyJobOffersActivity
import com.example.workman.NotificationsActivity
import com.example.workman.Profile
import com.example.workman.R
import com.example.workman.SharedPreferencesHelper
import com.example.workman.WorkOfferDetailsActivity
import com.example.workman.WorkerJobsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationUtils {

    fun navigateToHome(context: Context) {
        val role = SharedPreferencesHelper(context).getUserChoice()
        val targetClass = if (role == "Hiring") {
            HomeBossDashboardActivity::class.java
        } else {
            HomeWorkerDashboardActivity::class.java
        }
        
        if (context.javaClass == targetClass) return

        val intent = Intent(context, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
        if (context is Activity) context.overridePendingTransition(0, 0)
    }

    fun navigateToChat(context: Context) {
        if (context is ChatListActivity) return

        val intent = Intent(context, ChatListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
        if (context is Activity) context.overridePendingTransition(0, 0)
    }

    fun navigateToNotifications(context: Context) {
        if (context is NotificationsActivity) return

        val intent = Intent(context, NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
        if (context is Activity) context.overridePendingTransition(0, 0)
    }

    fun navigateToProfile(context: Context) {
        if (context is Profile) return

        val intent = Intent(context, Profile::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
        if (context is Activity) context.overridePendingTransition(0, 0)
    }

    fun navigateToMyJobs(context: Context) {
        val role = SharedPreferencesHelper(context).getUserChoice()
        val targetClass = if (role == "Hiring") {
            MyJobOffersActivity::class.java
        } else {
            WorkerJobsActivity::class.java
        }
        
        val intent = Intent(context, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
        if (context is Activity) context.overridePendingTransition(0, 0)
    }

    fun navigateToOfferDetails(context: Context, offerId: String) {
        val intent = Intent(context, WorkOfferDetailsActivity::class.java).apply {
            putExtra("OFFER_ID", offerId)
        }
        context.startActivity(intent)
    }

    fun setupBottomNavigation(activity: Activity, bottomNavigation: BottomNavigationView) {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    navigateToHome(activity)
                    true
                }
                R.id.nav_Chat -> {
                    navigateToChat(activity)
                    true
                }
                R.id.nav_profile -> {
                    navigateToProfile(activity)
                    true
                }
                else -> false
            }
        }
    }
    
    fun updateBottomNavigationSelection(bottomNavigation: BottomNavigationView, selectedItemId: Int) {
        bottomNavigation.selectedItemId = selectedItemId
    }
}
