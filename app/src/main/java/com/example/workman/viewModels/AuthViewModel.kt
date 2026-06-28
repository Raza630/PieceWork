package com.example.workman.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email and Password cannot be empty")
            return
        }
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    uid?.let { fetchUserRole(it) }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Login failed")
                }
            }
    }

    private fun fetchUserRole(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Existing user → read role and refresh the FCM token.
                    saveFCMToken(uid)
                    val role = document.getString("role")
                    if (role.isNullOrEmpty()) {
                        // Partial doc (e.g. created only by a location-sync
                        // merge) → backfill the role so routing works.
                        db.collection("users").document(uid).set(
                            mapOf("role" to "Worker"),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        _authState.value = AuthState.Success("Worker")
                    } else {
                        _authState.value = AuthState.Success(role)
                    }
                } else {
                    // Brand-new account that signed in (e.g. Google) without
                    // going through registration → no users/{uid} doc exists yet.
                    // Create a minimal one so location/FCM/feature writes don't
                    // fail with NOT_FOUND.
                    createMissingUserDoc(uid)
                }
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Failed to fetch user data")
            }
    }

    private fun createMissingUserDoc(uid: String) {
        val role = "Worker" // sensible default; user can switch later
        val user = hashMapOf(
            "email" to (auth.currentUser?.email ?: ""),
            "role" to role,
            "isVerified" to false,
            "totalRatings" to 0,
            "averageRating" to 0.0,
            "portfolioImages" to emptyList<String>()
        )
        db.collection("users").document(uid).set(user)
            .addOnSuccessListener {
                saveFCMToken(uid)
                _authState.value = AuthState.Success(role)
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Failed to create user data")
            }
    }

    fun signUp(email: String, pass: String, role: String) {
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    uid?.let { saveUserToFirestore(it, email, role) }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Registration failed")
                }
            }
    }

    private fun saveUserToFirestore(uid: String, email: String, role: String) {
        val user = hashMapOf(
            "email" to email,
            "role" to role,
            "isVerified" to false,
            "totalRatings" to 0,
            "averageRating" to 0.0,
            "portfolioImages" to emptyList<String>()
        )
        db.collection("users").document(uid).set(user)
            .addOnSuccessListener {
                saveFCMToken(uid)
                _authState.value = AuthState.Success(role)
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Failed to save user data")
            }
    }

    private fun saveFCMToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fcmToken = task.result
                // set(merge) so it works even if the doc is missing/partial.
                db.collection("users").document(userId)
                    .set(
                        mapOf("fcmToken" to fcmToken),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        Log.d("FCM", "FCM Token updated successfully.")
                    }
            }
        }
    }
    
    fun signInWithGoogle(idToken: String, role: String? = null) {
        _authState.value = AuthState.Loading
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    uid?.let { 
                        if (role != null) {
                            // If a role is provided (from SignUp), save it
                            saveUserToFirestore(it, auth.currentUser?.email ?: "", role)
                        } else {
                            // Otherwise just fetch existing role
                            fetchUserRole(it)
                        }
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Google Login failed")
                }
            }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
