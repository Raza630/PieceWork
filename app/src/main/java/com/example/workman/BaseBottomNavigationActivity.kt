package com.example.workman

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

open class BaseBottomNavigationActivity : AppCompatActivity() {

    protected fun setupBottomNavigation(bottomNavigation: BottomNavigationView) {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    if (this !is HomeWorkerDashboardActivity) {
                        navigateToActivity(HomeWorkerDashboardActivity::class.java)
                    }
                    true
                }
                R.id.nav_Chat -> {
                    if (this !is ChatActivity) {
                        navigateToChat()
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (this !is Profile) {
                        navigateToActivity(Profile::class.java)
                    }
                    true
                }
                else -> false
            }
        }
    }

    protected fun navigateToActivity(activityClass: Class<*>, extras: Bundle? = null) {
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            extras?.let { putExtras(it) }
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    protected fun navigateToChat() {
        val chatId = "G8iV5Ci38lTbj8rt8dw3"
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("CHAT_ID", chatId)
            putExtra("USER_ID", FirebaseAuth.getInstance().currentUser?.uid)
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    protected fun updateBottomNavigationSelection(bottomNavigation: BottomNavigationView, selectedItemId: Int) {
        bottomNavigation.selectedItemId = selectedItemId
    }
}