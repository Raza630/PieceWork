package com.example.workman

import android.content.Context
import android.content.SharedPreferences


class SharedPreferencesHelper(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "USER_LOGGED_IN"
        private const val KEY_USER_CHOICE = "USER_CHOICE"
        private const val KEY_FIRST_RUN = "FIRST_RUN"
    }

    // ✅ Login status
    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // ✅ User choice: "Hiring" or "Worker"
    fun saveUserChoice(choice: String) {
        sharedPreferences.edit().putString(KEY_USER_CHOICE, choice).apply()
    }

    fun getUserChoice(): String? {
        return sharedPreferences.getString(KEY_USER_CHOICE, null)
    }

    // ✅ First run flag
    fun isFirstRun(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_RUN, true)
    }

    fun setFirstRunDone() {
        sharedPreferences.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }

    // ✅ Clear all login-related data
    fun clearLoginData() {
        sharedPreferences.edit().clear().apply()
    }
}





//class SharedPreferencesHelper(context: Context) {
//
//    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
//    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
//
//    // Save login status
//    fun saveLoginStatus(isLoggedIn: Boolean) {
//        editor.putBoolean("USER_LOGGED_IN", isLoggedIn)
//        editor.apply()
//    }
//
//    // Check login status
//    fun isLoggedIn(): Boolean {
//        return sharedPreferences.getBoolean("USER_LOGGED_IN", false)
//    }
//
//
//    // Clear login data
//    fun clearLoginData() {
//        editor.clear()
//        editor.apply()
//    }
//}
