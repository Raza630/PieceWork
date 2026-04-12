package com.example.workman

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.workman.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import java.util.UUID

class Profile : BaseBottomNavigationActivity() {

    private lateinit var database: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var binding: ActivityProfileBinding

    private var selectedImageUri: Uri? = null

    private val dashboardCategories = listOf(
        "Professionals",
        "Associate Professionals",
        "Clerks",
        "Service Workers",
        "Skilled Agricultural",
        "Craft & Trades",
        "Machine Operators",
        "Elementary"
    )

    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this).load(it).into(binding.setProfileImage)
            binding.imgButton.visibility = android.view.View.GONE
            binding.setProfile.visibility = android.view.View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupBottomNavigation(binding.bottomNavigation)
        updateBottomNavigationSelection(binding.bottomNavigation, R.id.nav_profile)

        setupDatePicker()
        setupSpinners()

        binding.imgButton.setOnClickListener {
            getImage.launch("image/*")
        }
        
        binding.imageView4.setOnClickListener {
            getImage.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            saveProfileData()
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.editText.setText(currentUser.email)
            binding.editText.isEnabled = false 
            loadExistingProfileData(currentUser.uid)
        }
    }

    private fun loadExistingProfileData(userId: String) {
        database.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.firstName.setText(document.getString("firstName") ?: "")
                    binding.lastName.setText(document.getString("lastName") ?: "")
                    binding.idTVSelectedDate.text = document.getString("dob") ?: ""
                    binding.inputNo.setText(document.getString("phone") ?: "")
                    
                    val photoUrl = document.getString("photoUrl")
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this).load(photoUrl).into(binding.setProfileImage)
                        binding.imgButton.visibility = android.view.View.GONE
                        binding.setProfile.visibility = android.view.View.VISIBLE
                    }

                    val gender = document.getString("gender")
                    val genderAdapter = binding.Gender.adapter as? ArrayAdapter<String>
                    genderAdapter?.getPosition(gender)?.let { if (it >= 0) binding.Gender.setSelection(it) }

                    val category = document.getString("category")
                    val occupationAdapter = binding.Occupation.adapter as? ArrayAdapter<String>
                    occupationAdapter?.getPosition(category)?.let { if (it >= 0) binding.Occupation.setSelection(it) }

                    binding.check1.isChecked = document.getString("speciallyAbled") == "Yes"
                    binding.check2.isChecked = document.getString("speciallyAbled") == "No"
                    binding.check3.isChecked = document.getString("acceptNotifications") == "Yes"
                    binding.check4.isChecked = document.getString("acceptNotifications") == "No"
                }
            }
    }

    private fun saveProfileData() {
        val currentUser = auth.currentUser ?: return
        
        val firstName = binding.firstName.text.toString().trim()
        val lastName = binding.lastName.text.toString().trim()
        val dob = binding.idTVSelectedDate.text.toString().trim()
        val gender = binding.Gender.selectedItem.toString()
        val phone = binding.inputNo.text.toString().trim()
        val category = binding.Occupation.selectedItem.toString()
        val speciallyAbled = if (binding.check1.isChecked) "Yes" else "No"
        val acceptNotifications = if (binding.check3.isChecked) "Yes" else "No"

        if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(currentUser.uid, firstName, lastName, dob, gender, phone, category, speciallyAbled, acceptNotifications)
        } else {
            // Check if there's already an image
            database.collection("users").document(currentUser.uid).get().addOnSuccessListener { doc ->
                val existingPhotoUrl = doc.getString("photoUrl") ?: ""
                updateFirestore(currentUser.uid, firstName, lastName, dob, gender, phone, category, speciallyAbled, acceptNotifications, existingPhotoUrl)
            }
        }
    }

    private fun uploadImageAndSaveProfile(userId: String, fName: String, lName: String, dob: String, gender: String, phone: String, category: String, sAbled: String, aNotify: String) {
        val ref = storage.reference.child("profile_images/$userId/${UUID.randomUUID()}")
        ref.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    updateFirestore(userId, fName, lName, dob, gender, phone, category, sAbled, aNotify, uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFirestore(userId: String, firstName: String, lastName: String, dob: String, gender: String, phone: String, category: String, speciallyAbled: String, acceptNotifications: String, photoUrl: String) {
        val profileData = hashMapOf(
            "name" to "$firstName $lastName",
            "firstName" to firstName,
            "lastName" to lastName,
            "dob" to dob,
            "gender" to gender,
            "phone" to phone,
            "category" to category,
            "speciallyAbled" to speciallyAbled,
            "acceptNotifications" to acceptNotifications,
            "photoUrl" to photoUrl,
            "yearsOfExperience" to 5,
            "rating" to 4.5,
            "reviewCount" to "120",
            "ratePerHour" to 150
        )

        database.collection("users").document(userId)
            .set(profileData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        binding.idTVSelectedDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                binding.idTVSelectedDate.text = "$day/${month + 1}/$year"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupSpinners() {
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Male", "Female", "Other"))
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.Gender.adapter = genderAdapter

        val occupationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dashboardCategories)
        occupationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.Occupation.adapter = occupationAdapter
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavigationSelection(binding.bottomNavigation, R.id.nav_profile)
    }
}
