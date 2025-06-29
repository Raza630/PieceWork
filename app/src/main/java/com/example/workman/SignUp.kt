package com.example.workman

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase and SharedPreferences
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sharedPreferencesHelper = SharedPreferencesHelper(this)

        val signUpEmail = findViewById<EditText>(R.id.sign_up_email)
        val signUpPassText = findViewById<EditText>(R.id.sign_up_password)
        val signUpButton = findViewById<Button>(R.id.sign_up_button)
        val loginScreen = findViewById<TextView>(R.id.login_txt)

        // Get the role from SharedPreferences (default to Worker if not found)
        val userRole = sharedPreferencesHelper.getUserChoice() ?: "Worker"

        signUpButton.setOnClickListener {
            val email = signUpEmail.text.toString().trim()
            val password = signUpPassText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(email, password, userRole)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        loginScreen.setOnClickListener {
            intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser(email: String, password: String, userRole: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    userId?.let { uid ->
                        // Save user data to Firestore, including the role
                        val user = hashMapOf(
                            "email" to email,
                            "role" to userRole
                        )

                        db.collection("users").document(uid).set(user)
                            .addOnSuccessListener {
                                // Now retrieve and save the FCM token
                                saveFCMToken(uid)

                                Toast.makeText(
                                    this,
                                    "Registration successful as $userRole!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Save login status and role in SharedPreferences
                                sharedPreferencesHelper.setLoggedIn(true)
                                sharedPreferencesHelper.saveUserChoice(userRole)

                                // Redirect to the correct dashboard
                                val intent = if (userRole == "Hiring") {
                                    Intent(this, HomeBossDashboardActivity::class.java)
                                } else {
                                    Intent(this, HomeWorkerDashboardActivity::class.java)
                                }
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun saveFCMToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fcmToken = task.result
                // Save the FCM token in Firestore under the user's document
                db.collection("users").document(userId).update("fcmToken", fcmToken)
                    .addOnSuccessListener {
                        Log.d("FCM", "FCM Token updated successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "Error updating FCM token: ${e.message}")
                    }
            } else {
                Log.e("FCM", "Fetching FCM registration token failed: ${task.exception}")
            }
        }
    }
}


//    private fun registerUser(email: String, password: String, userRole: String) {
//        auth.createUserWithEmailAndPassword(email, password)
//            .addOnCompleteListener(this) { task ->
//                if (task.isSuccessful) {
//                    val userId = auth.currentUser?.uid
//                    userId?.let { uid ->
//                        // Save user data to Firestore, including the role
//                        val user = hashMapOf(
//                            "email" to email,
//                            "role" to userRole
//                        )
//
//                        db.collection("users").document(uid).set(user)
//                            .addOnSuccessListener {
//                                Toast.makeText(this, "Registration successful as $userRole!", Toast.LENGTH_SHORT).show()
//
//                                // Save login status and role in SharedPreferences
//                                sharedPreferencesHelper.setLoggedIn(true)
//                                sharedPreferencesHelper.saveUserChoice(userRole)
//
//                                // Redirect to the correct dashboard
//                                val intent = if (userRole == "Hiring") {
//                                    Intent(this, HomeBossDashboardActivity::class.java)
//                                } else {
//                                    Intent(this, HomeWorkerDashboardActivity::class.java)
//                                }
//                                startActivity(intent)
//                                finish()
//                            }
//                            .addOnFailureListener {
//                                Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
//                            }
//                    }
//                } else {
//                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
//                }
//            }
//    }


//}









//class SignUp : AppCompatActivity() {
//
//    private lateinit var auth: FirebaseAuth
//    private lateinit var db: FirebaseFirestore
//    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_sign_up)
//
//        // Initialize Firebase Authentication and Firestore
//        auth = FirebaseAuth.getInstance()
//        db = FirebaseFirestore.getInstance()
//
//        val signUpEmail = findViewById<EditText>(R.id.sign_up_email)
//        val signUpPassText = findViewById<EditText>(R.id.sign_up_password)
//        val signUpButton = findViewById<Button>(R.id.sign_up_button)
//        val loginScreen = findViewById<TextView>(R.id.login_txt)
//
////        val userRole = intent.getStringExtra("USER_ROLE") ?: "Worker" // Default to Worker
//
//        sharedPreferencesHelper = SharedPreferencesHelper(this)
//        val userRole = sharedPreferencesHelper.getUserChoice()
//
//
//        if (userRole == "Hiring") {
//            // Proceed with the boss-specific flow
//            navigateToBossDashboard()
//        } else if (userRole == "Worker") {
//            // Proceed with the worker-specific flow
//            navigateToWorkerDashboard()
//        }
////        signUpButton.setOnClickListener {
////            val userRole = intent.getStringExtra("USER_ROLE") ?: "Worker" // Default to Worker
////            val email = signUpEmail.text.toString().trim()
////            val password = signUpPassText.text.toString().trim()
////
////            if (email.isNotEmpty() && password.isNotEmpty()){
////                registerUser(email,password, userRole)
////            }else{
////                Toast.makeText(this, "Please enter email, password, and select a role", Toast.LENGTH_SHORT).show()
////            }
////        }
//        signUpButton.setOnClickListener {
//
//            val email = signUpEmail.text.toString().trim()
//            val password = signUpPassText.text.toString().trim()
//
//            if (email.isNotEmpty() && password.isNotEmpty()) {
//                registerUser(email, password, userRole!!)
//            } else {
//                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//
//
//        loginScreen.setOnClickListener {
//            intent = Intent(this, MainActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
////
//
//    }
//
//    private fun registerUser(email: String, password: String, userRole: String) {
//        auth.createUserWithEmailAndPassword(email, password)
//            .addOnCompleteListener(this) { task ->
//                if (task.isSuccessful) {
//                    val userId = auth.currentUser?.uid
//                    userId?.let { uid ->
//                        // Include user role in the data to be stored
//                        val user = hashMapOf(
//                            "email" to email,
//                            "role" to userRole // Save the role (Hiring or Worker)
//                        )
//
//                        // Save user data to Firestore
//                        db.collection("users").document(uid).set(user)
//                            .addOnSuccessListener {
//                                Toast.makeText(this, "Registration successful as $userRole!", Toast.LENGTH_SHORT).show()
//
//                                // Set login status in SharedPreferences
//                                SharedPreferencesHelper(this).setLoggedIn(true)
//
//                                // Redirect to the appropriate dashboard
//                                val intent = if (userRole == "Hiring") {
//                                    Intent(this, HomeWorkerDashboardActivity::class.java)
//                                } else {
//                                    Intent(this, HomeBossDashboardActivity::class.java)
//                                }
//                                startActivity(intent)
//                                finish()
//                            }
//                            .addOnFailureListener {
//                                Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
//                            }
//                    }
//                } else {
//                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
//                }
//            }
//    }
//
//
//    private fun navigateToBossDashboard() {
//        startActivity(Intent(this, HomeBossDashboardActivity::class.java))
//        finish()
//    }
//
//    private fun navigateToWorkerDashboard() {
//        startActivity(Intent(this, HomeWorkerDashboardActivity::class.java))
//        finish()
//    }
//    private fun registerUser(email: String, password: String) {
//        auth.createUserWithEmailAndPassword(email, password)
//            .addOnCompleteListener(this) { task ->
//                if (task.isSuccessful) {
//                    val userId = auth.currentUser?.uid
//                    userId?.let { uid ->
//                        val user = hashMapOf("email" to email)
//
//                        // Save user data to Firestore
//                        db.collection("users").document(uid).set(user)
//                            .addOnSuccessListener {
//                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
//
//                                // Set login status in SharedPreferences
//                                SharedPreferencesHelper(this).setLoggedIn(true)
//
//                                // Redirect to the common dashboard
//                                startActivity(Intent(this, HomeListActivity::class.java))
//                                finish()
//                            }
//                            .addOnFailureListener {
//                                Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
//                            }
//                    }
//                } else {
//                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
//                }
//            }
//    }

//}