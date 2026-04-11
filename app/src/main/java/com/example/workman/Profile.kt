package com.example.workman

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.workman.dataClass.UserProfile
import com.example.workman.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

class Profile : BaseBottomNavigationActivity() {

    private lateinit var database: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityProfileBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        setupBottomNavigation(binding.bottomNavigation)
        updateBottomNavigationSelection(binding.bottomNavigation, R.id.nav_profile)

        setupDatePicker()
        setupSpinners()

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
                    
                    val gender = document.getString("gender")
                    val genderAdapter = binding.Gender.adapter as? ArrayAdapter<String>
                    genderAdapter?.getPosition(gender)?.let { if (it >= 0) binding.Gender.setSelection(it) }

                    val category = document.getString("category")
                    val occupationAdapter = binding.Occupation.adapter as? ArrayAdapter<String>
                    occupationAdapter?.getPosition(category)?.let { if (it >= 0) binding.Occupation.setSelection(it) }

                    binding.check1.isChecked = document.getString("speciallyAbled") == "Yes"
                    binding.check3.isChecked = document.getString("acceptNotifications") == "Yes"
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
            // Add default values for the UI if they don't exist
            "yearsOfExperience" to 5,
            "rating" to 4.5,
            "reviewCount" to "120",
            "ratePerHour" to 150
        )

        database.collection("users").document(currentUser.uid)
            .set(profileData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to save profile: ${exception.message}", Toast.LENGTH_SHORT).show()
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
