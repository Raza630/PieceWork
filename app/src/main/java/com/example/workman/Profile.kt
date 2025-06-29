package com.example.workman

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.workman.dataClass.UserProfile
import com.example.workman.databinding.ActivityProfileBinding
import com.example.workman.databinding.ActivityTestHomelistUiBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

import android.widget.ArrayAdapter
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class Profile : AppCompatActivity() {



    private lateinit var database: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_profile)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        // Set up date picker and spinners
        setupDatePicker()
        setupSpinners()

        binding.saveButton.setOnClickListener {
            saveProfileData()
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userEmail = currentUser.email
            binding.editText.setText(userEmail)
            binding.editText.isEnabled = false // Lock the email field
        }
    }

    private fun saveProfileData() {
        val firstName = binding.firstName.text.toString().trim()
        val lastName = binding.lastName.text.toString().trim()
        val dob = binding.idTVSelectedDate.text.toString().trim()
        val gender = binding.Gender.selectedItem.toString()
//        val email = binding.editText.text.toString().trim()
        val phone = binding.inputNo.text.toString().trim()
        val occupation = binding.Occupation.selectedItem.toString()
        val speciallyAbled = if (binding.check1.isChecked) "Yes" else "No"
        val acceptNotifications = if (binding.check3.isChecked) "Yes" else "No"

        // Validate the input fields
        if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the current user's UID
//        val userId = auth.currentUser?.uid ?: return
        val currentUser = FirebaseAuth.getInstance().currentUser


        // Create a UserProfile object
        val userProfile = UserProfile(
            firstName,
            lastName,
            dob,
            gender,
            phone,
            occupation,
            speciallyAbled,
            acceptNotifications
        )

        if (currentUser == null) return
//        database.collection("users").document(currentUser.uid)
        database.collection("users").document(currentUser.uid).collection("profile").document("details")
            .set(userProfile)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to save profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }



    }



    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        binding.idTVSelectedDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    binding.idTVSelectedDate.text = selectedDate
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }
    }

    private fun setupSpinners() {
        val genders = arrayOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.Gender.adapter = genderAdapter

        val occupations = arrayOf("Engineer", "Doctor", "Teacher", "Student", "Other")
        val occupationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, occupations)
        occupationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.Occupation.adapter = occupationAdapter
    }


}













//class Profile : AppCompatActivity() {
//
//    private lateinit var setProfileImage: ImageView
//    private lateinit var idTVSelectedDate: TextView
//    private lateinit var imgButton: ConstraintLayout
//    private lateinit var genderSpinner: Spinner
//    private lateinit var occupationSpinner: Spinner
//    private lateinit var storageReference: StorageReference
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_profile)
//
//        // Initialize Firebase Storage reference
//        storageReference = FirebaseStorage.getInstance().reference.child("workOffers/images")
//
//        // Initialize views
//        setProfileImage = findViewById(R.id.set_profile_image)
//        idTVSelectedDate = findViewById(R.id.idTVSelectedDate)
//        imgButton = findViewById(R.id.img_button)
//        genderSpinner = findViewById(R.id.Gender)
//        occupationSpinner = findViewById(R.id.Occupation)
//
//        // Image picker listener
////        imgButton.setOnClickListener {
////            ImagePicker.with(this)
////                .crop()
////                .compress(1024)
////                .maxResultSize(1080, 1080)
////                .start()
////        }
//
//        // Date picker for the date selection TextView
//        idTVSelectedDate.setOnClickListener {
//            showDatePickerDialog()
//        }
//
//        // Populate gender and occupation spinners
////        setupSpinners()
//    }
//
//    // Handle image picker result
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (resultCode == Activity.RESULT_OK && data != null) {
//            val imageUri = data.data
//            setProfileImage.setImageURI(imageUri)
//            imageUri?.let {
//                uploadImageToFirebase(it)
//            }
//        }
//    }
//
//    // Firebase Storage image upload method
//    private fun uploadImageToFirebase(imageUri: Uri) {
//        val fileRef = storageReference.child("${System.currentTimeMillis()}.jpg")
//        val uploadTask = fileRef.putFile(imageUri)
//
//        uploadTask.addOnSuccessListener {
//            fileRef.downloadUrl.addOnSuccessListener {
//                Toast.makeText(this, "Image Uploaded", Toast.LENGTH_SHORT).show()
//                // Optionally, save uri.toString() to your database
//            }
//        }.addOnFailureListener {
//            Toast.makeText(this, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    // Date picker dialog
//    private fun showDatePickerDialog() {
//        val c = Calendar.getInstance()
//        val year = c.get(Calendar.YEAR)
//        val month = c.get(Calendar.MONTH)
//        val day = c.get(Calendar.DAY_OF_MONTH)
//
//        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
//            idTVSelectedDate.text = "$selectedDay-${selectedMonth + 1}-$selectedYear"
//        }, year, month, day)
//
//        datePickerDialog.show()
//    }
//
//    // Setup gender and occupation spinners
////    private fun setupSpinners() {
////        val genderAdapter = ArrayAdapter.createFromResource(
////            this, R.array.gender_options, android.R.layout.simple_spinner_item
////        )
////        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
////        genderSpinner.adapter = genderAdapter
////
////        val occupationAdapter = ArrayAdapter.createFromResource(
////            this, R.array.occupation_options, android.R.layout.simple_spinner_item
////        )
////        occupationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
////        occupationSpinner.adapter = occupationAdapter
////    }
//}
