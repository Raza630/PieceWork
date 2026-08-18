package com.example.workman.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
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

/** One-off events for lightweight actions (e.g. password reset) that shouldn't
 *  disturb the main [AuthState] machine driving navigation. */
sealed class AuthEvent {
    object Idle : AuthEvent()
    object Loading : AuthEvent()
    data class Message(val text: String) : AuthEvent()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _authEvent = MutableStateFlow<AuthEvent>(AuthEvent.Idle)
    val authEvent: StateFlow<AuthEvent> = _authEvent

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
                    _authState.value = AuthState.Error(friendlyAuthError(task.exception))
                }
            }
    }

    /** Sends a password-reset email without touching the navigation state. */
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _authEvent.value = AuthEvent.Message("Enter your email to reset the password")
            return
        }
        _authEvent.value = AuthEvent.Loading
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                _authEvent.value = if (task.isSuccessful) {
                    AuthEvent.Message("Password reset link sent to $email")
                } else {
                    AuthEvent.Message(friendlyAuthError(task.exception))
                }
            }
    }

    fun resetEvent() {
        _authEvent.value = AuthEvent.Idle
    }

    /** Maps raw Firebase exceptions to friendly, user-facing messages. */
    private fun friendlyAuthError(e: Exception?): String = when (e) {
        is FirebaseAuthWeakPasswordException ->
            "Password is too weak. Use at least 6 characters."

        is FirebaseAuthInvalidCredentialsException ->
            "Invalid email or password. Please try again."

        is FirebaseAuthInvalidUserException ->
            "No account found with this email."

        is FirebaseAuthUserCollisionException ->
            "An account already exists with this email."

        else -> e?.localizedMessage ?: "Something went wrong. Please try again."
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
            "name" to (auth.currentUser?.displayName ?: ""),
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

    fun signUp(email: String, pass: String, role: String, name: String = "", phone: String = "") {
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    uid?.let { saveUserToFirestore(it, email, role, name, phone) }
                } else {
                    _authState.value = AuthState.Error(friendlyAuthError(task.exception))
                }
            }
    }

    private fun saveUserToFirestore(
        uid: String,
        email: String,
        role: String,
        name: String = "",
        phone: String = ""
    ) {
        val user = hashMapOf(
            "email" to email,
            "name" to name,
            "phone" to phone,
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
                            saveUserToFirestore(
                                it,
                                auth.currentUser?.email ?: "",
                                role,
                                auth.currentUser?.displayName ?: ""
                            )
                        } else {
                            // Otherwise just fetch existing role
                            fetchUserRole(it)
                        }
                    }
                } else {
                    _authState.value = AuthState.Error(friendlyAuthError(task.exception))
                }
            }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
