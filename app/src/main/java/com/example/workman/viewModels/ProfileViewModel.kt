package com.example.workman.viewModels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workman.utils.CloudinaryUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val dob: String = "",
    val gender: String = "Male",
    val email: String = "",
    val phone: String = "",
    val category: String = "Professionals",
    val speciallyAbled: String = "No",
    val acceptNotifications: String = "No",
    val photoUrl: String = "",
    val portfolioImages: List<String> = emptyList(),
    val isUploadingPortfolio: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val currentUser = auth.currentUser ?: return
        _uiState.value = _uiState.value.copy(email = currentUser.email ?: "", isLoading = true)
        
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    _uiState.value = _uiState.value.copy(
                        firstName = document.getString("firstName") ?: "",
                        lastName = document.getString("lastName") ?: "",
                        dob = document.getString("dob") ?: "",
                        gender = document.getString("gender") ?: "Male",
                        phone = document.getString("phone") ?: "",
                        category = document.getString("category") ?: "Professionals",
                        speciallyAbled = document.getString("speciallyAbled") ?: "No",
                        acceptNotifications = document.getString("acceptNotifications") ?: "No",
                        photoUrl = document.getString("photoUrl") ?: "",
                        portfolioImages = document.get("portfolioImages") as? List<String>
                            ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(isLoading = false, message = "Error loading profile: ${it.message}")
            }
    }

    fun onFirstNameChange(value: String) { _uiState.value = _uiState.value.copy(firstName = value) }
    fun onLastNameChange(value: String) { _uiState.value = _uiState.value.copy(lastName = value) }
    fun onDobChange(value: String) { _uiState.value = _uiState.value.copy(dob = value) }
    fun onGenderChange(value: String) { _uiState.value = _uiState.value.copy(gender = value) }
    fun onPhoneChange(value: String) { _uiState.value = _uiState.value.copy(phone = value) }
    fun onCategoryChange(value: String) { _uiState.value = _uiState.value.copy(category = value) }
    fun onSpeciallyAbledChange(value: String) { _uiState.value = _uiState.value.copy(speciallyAbled = value) }
    fun onAcceptNotificationsChange(value: String) { _uiState.value = _uiState.value.copy(acceptNotifications = value) }

    fun saveProfile(imageUri: Uri? = null) {
        val userId = auth.currentUser?.uid ?: return
        val state = _uiState.value

        if (state.firstName.isEmpty() || state.lastName.isEmpty() || state.dob.isEmpty() || state.phone.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "Please fill all required fields")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true)

        if (imageUri != null) {
            viewModelScope.launch {
                val url = CloudinaryUploader.uploadImage(
                    context = getApplication(),
                    uri = imageUri,
                    folder = "profiles"
                )
                if (url != null) {
                    updateFirestore(userId, state, url)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        message = "Image upload failed. Saving without photo."
                    )
                    // Still save the rest of the profile with existing photo
                    updateFirestore(userId, state, state.photoUrl)
                }
            }
        } else {
            updateFirestore(userId, state, state.photoUrl)
        }
    }

    private fun updateFirestore(userId: String, state: ProfileUiState, photoUrl: String) {
        val profileData = hashMapOf(
            "name" to "${state.firstName} ${state.lastName}",
            "firstName" to state.firstName,
            "lastName" to state.lastName,
            "dob" to state.dob,
            "gender" to state.gender,
            "phone" to state.phone,
            "category" to state.category,
            "speciallyAbled" to state.speciallyAbled,
            "acceptNotifications" to state.acceptNotifications,
            "photoUrl" to photoUrl,
            "yearsOfExperience" to 5,
            "rating" to 4.5,
            "reviewCount" to "120",
            "ratePerHour" to 150
        )

        db.collection("users").document(userId)
            .set(profileData, SetOptions.merge())
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(isSaving = false, photoUrl = photoUrl, message = "Profile saved successfully")
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, message = "Save failed: ${e.message}")
            }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun uploadPortfolioImages(uris: List<Uri>) {
        val userId = auth.currentUser?.uid ?: return
        if (uris.isEmpty()) return

        _uiState.value = _uiState.value.copy(isUploadingPortfolio = true)

        viewModelScope.launch {
            val uploadedUrls = CloudinaryUploader.uploadImages(
                context = getApplication(),
                uris = uris,
                folder = "portfolio"
            )
            if (uploadedUrls.isNotEmpty()) {
                savePortfolioToFirestore(userId, uploadedUrls)
            } else {
                _uiState.value = _uiState.value.copy(
                    isUploadingPortfolio = false,
                    message = "Portfolio upload failed. Check your connection."
                )
            }
        }
    }

    private fun savePortfolioToFirestore(userId: String, newUrls: List<String>) {
        val currentPortfolio = _uiState.value.portfolioImages
        val updatedPortfolio = currentPortfolio + newUrls

        db.collection("users").document(userId)
            .update("portfolioImages", updatedPortfolio)
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(
                    isUploadingPortfolio = false,
                    portfolioImages = updatedPortfolio,
                    message = "Portfolio updated successfully"
                )
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(
                    isUploadingPortfolio = false,
                    message = "Failed to update portfolio"
                )
            }
    }
}
