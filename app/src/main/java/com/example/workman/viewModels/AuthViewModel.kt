package com.example.workman.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.workman.SharedPreferencesHelper
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
                val role = document.getString("role") ?: "Worker"
                _authState.value = AuthState.Success(role)
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Failed to fetch user data")
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
            "role" to role
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
                db.collection("users").document(userId).update("fcmToken", fcmToken)
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
