package com.example.workman

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workman.adaptes.ImageAdapterSelectedImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.android.gms.tasks.Tasks
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth


class CreateWorkActivity : AppCompatActivity() {

    private lateinit var etWorkTitle: EditText
    private lateinit var etWorkDescription: EditText
    private lateinit var etWorkDate: EditText
    private lateinit var btnSubmitWork: Button
    private lateinit var btnSelectImages: Button

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private val PICK_IMAGES_REQUEST_CODE = 1
    private val imageUris = mutableListOf<Uri>()

    private lateinit var toolbar: Toolbar
    private lateinit var rvSelectedImages: RecyclerView
    private lateinit var imageAdapter: ImageAdapterSelectedImage // Make sure you have this adapter implemented

    // Default image URI (make sure to add a default image in your drawable resources)
//    private val defaultImageUri = Uri.parse("android.resource://com.example.workman/drawable/notification_img.png") // Change to your default image resource

    private val defaultImageUri = Uri.parse("android.resource://com.example.workman/drawable/notification_img") // Corrected

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_work)

        toolbar = findViewById(R.id.toolbar1)
        setSupportActionBar(toolbar)


        // Initialize Firebase Firestore and Storage instances.
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize views.
        etWorkTitle = findViewById(R.id.etWorkTitle)
        etWorkDescription = findViewById(R.id.etWorkDescription)
        etWorkDate = findViewById(R.id.etWorkDate)
        btnSubmitWork = findViewById(R.id.btnSubmitWork)
        btnSelectImages = findViewById(R.id.btnSelectImages)

        // Set up RecyclerView for selected images.
        rvSelectedImages = findViewById(R.id.rvSelectedImages)
        imageAdapter = ImageAdapterSelectedImage(imageUris) // Ensure this adapter is properly implemented to display images.
        rvSelectedImages.layoutManager = GridLayoutManager(this, 3)
        rvSelectedImages.adapter = imageAdapter

        // Set click listeners.
        btnSelectImages.setOnClickListener { requestStoragePermission() }
        btnSubmitWork.setOnClickListener { validateAndCreateWork() }
    }

    // Request storage permission before picking images.
    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PICK_IMAGES_REQUEST_CODE)
        } else {
            pickImagesFromGallery()
        }
    }

    // Pick images from the gallery.
    private fun pickImagesFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST_CODE)
    }

    // Handle the result of image selection.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGES_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    uri?.let { imageUris.add(it) }
                }
            } ?: data?.data?.let { uri ->
                imageUris.add(uri)
            }

            // Notify adapter of data change and refresh view.
            imageAdapter.notifyDataSetChanged()
            Toast.makeText(this, "${imageUris.size} images selected", Toast.LENGTH_SHORT).show()
        }
    }

    // Validate input fields before proceeding with upload.
    private fun validateAndCreateWork() {
        val title = etWorkTitle.text.toString().trim()
        val description = etWorkDescription.text.toString().trim()
        val date = etWorkDate.text.toString().trim()

        when {
            title.isEmpty() || description.isEmpty() || date.isEmpty() -> {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
            else -> uploadImagesAndSaveWork(title, description, date)
        }
    }


    private fun uploadImagesAndSaveWork(title: String, description: String, date: String) {
        val imageUrls = mutableListOf<String>()
        val storageRef = storage.reference.child("workOffers")

        btnSubmitWork.isEnabled = false // Disable button to prevent multiple submissions.

        // Create a list of upload tasks
        val uploadTasks = if (imageUris.isNotEmpty()) {
            imageUris.map { uri ->
                val imageRef = storageRef.child("${System.currentTimeMillis()}_${uri.lastPathSegment}")
                imageRef.putFile(uri)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                        imageRef.downloadUrl // Get the download URL after upload.
                    }.addOnSuccessListener { downloadUri ->
                        imageUrls.add(downloadUri.toString())
                    }.addOnFailureListener { exception ->
                        Log.e("ImageUpload", "Failed to upload image: ${exception.message}")
                        Toast.makeText(this, "Error uploading an image", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            // If no images are selected, create a completed task for the default image
            listOf(Tasks.forResult(defaultImageUri)) // Create a task that returns the default image URI
        }

        // Use whenAllSuccess to wait for all uploads to complete
        Tasks.whenAllSuccess<Uri>(uploadTasks).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveWorkToFirestore(title, description, date, if (imageUrls.isNotEmpty()) imageUrls else listOf(defaultImageUri.toString()))
            } else {
                Log.e("ImageUpload", "Failed to upload some images.")
                Toast.makeText(this, "Error uploading some images", Toast.LENGTH_SHORT).show()
            }
            btnSubmitWork.isEnabled = true // Re-enable button after upload completes.
        }
    }


    private fun saveWorkToFirestore(title: String, description: String, date: String, imageUrls: List<String>) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val jobId = db.collection("workOffers").document().id // Generating a unique jobId
        val workData = hashMapOf(
            "jobId" to jobId,
            "title" to title,
            "description" to description,
            "date" to date,
            "images" to imageUrls,
            "createdBy" to (currentUser?.uid ?: "unknown"), // Replace with user ID from FirebaseAuth
            "isAccepted" to false, // Default value
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("workOffers").document(jobId).set(workData)
            .addOnSuccessListener {
                Toast.makeText(this, "Work offer created successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, BossWorkListActivity::class.java)
                startActivity(intent)
                finish() // Close the activity after successful creation.
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Failed to save work offer: ${exception.message}")
                Toast.makeText(this, "Error creating work offer", Toast.LENGTH_SHORT).show()
                btnSubmitWork.isEnabled = true // Re-enable button on failure.
            }
    }

}






















//class CreateWorkActivity : AppCompatActivity() {
//
//    private lateinit var etWorkTitle: EditText
//    private lateinit var etWorkDescription: EditText
//    private lateinit var etWorkDate: EditText
//    private lateinit var btnSubmitWork: Button
//    private lateinit var btnSelectImages: Button
//
//    private lateinit var db: FirebaseFirestore
//    private lateinit var storage: FirebaseStorage
//
//    private val PICK_IMAGES_REQUEST_CODE = 1
//    private val imageUris = mutableListOf<Uri>()
//
//    private lateinit var rvSelectedImages: RecyclerView
//    private lateinit var imageAdapter: ImageAdapterSelectedImage
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_create_work)
//
//        // Initialize Firebase Firestore and Storage instances.
//        db = FirebaseFirestore.getInstance()
//        storage = FirebaseStorage.getInstance()
//
//        // Initialize views.
//        etWorkTitle = findViewById(R.id.etWorkTitle)
//        etWorkDescription = findViewById(R.id.etWorkDescription)
//        etWorkDate = findViewById(R.id.etWorkDate)
//        btnSubmitWork = findViewById(R.id.btnSubmitWork)
//        btnSelectImages = findViewById(R.id.btnSelectImages)
//
//        // Set up RecyclerView for selected images.
//        rvSelectedImages = findViewById(R.id.rvSelectedImages)
//        imageAdapter = ImageAdapterSelectedImage(imageUris)
//        rvSelectedImages.layoutManager = GridLayoutManager(this, 3)
//        rvSelectedImages.adapter = imageAdapter
//
//        // Set click listeners.
//        btnSelectImages.setOnClickListener { pickImagesFromGallery() }
//        btnSubmitWork.setOnClickListener { validateAndCreateWork() }
//    }
//
//    private fun pickImagesFromGallery() {
//        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
//            type = "image/*"
//            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        }
//        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST_CODE)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == PICK_IMAGES_REQUEST_CODE && resultCode == RESULT_OK) {
//            data?.clipData?.let { clipData ->
//                for (i in 0 until clipData.itemCount) {
//                    clipData.getItemAt(i).uri?.let { uri ->
//                        imageUris.add(uri)
//                    }
//                }
//            } ?: data?.data?.let { uri ->
//                imageUris.add(uri)
//            }
//
//            // Notify adapter of data change and refresh view.
//            imageAdapter.notifyDataSetChanged()
//            Toast.makeText(this, "${imageUris.size} images selected", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun validateAndCreateWork() {
//        val title = etWorkTitle.text.toString().trim()
//        val description = etWorkDescription.text.toString().trim()
//        val date = etWorkDate.text.toString().trim()
//
//        when {
//            title.isEmpty() || description.isEmpty() || date.isEmpty() -> {
//                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
//            }
//            imageUris.isEmpty() -> {
//                Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
//            }
//            else -> uploadImagesAndSaveWork(title, description, date)
//        }
//    }
//    private fun uploadImagesAndSaveWork(title: String, description: String, date: String) {
//        val imageUrls = mutableListOf<String>()
//        val storageRef = storage.reference.child("workOffers")
//
//        btnSubmitWork.isEnabled = false // Disable button to prevent multiple submissions.
//
//        Toast.makeText(this, "Uploading images, please wait...", Toast.LENGTH_SHORT).show()
//
//        // Create a list of upload tasks
//        val uploadTasks = imageUris.mapIndexed { index, uri ->
//            val imageRef = storageRef.child("${System.currentTimeMillis()}_${uri.lastPathSegment}")
//            imageRef.putFile(uri)
//                .continueWithTask { task ->
//                    if (!task.isSuccessful) task.exception?.let { throw it }
//                    imageRef.downloadUrl // Get the download URL after upload.
//                }.addOnSuccessListener { downloadUri ->
//                    imageUrls.add(downloadUri.toString())
//                }.addOnFailureListener { exception ->
//                    Log.e("ImageUpload", "Failed to upload image at index $index: ${exception.message}")
//                    Toast.makeText(this, "Error uploading image at index $index", Toast.LENGTH_SHORT).show()
//                }
//        }
//
//        // Use Tasks to wait for all uploads to complete
//        Tasks.whenAllSuccess<Uri>(uploadTasks).addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                saveWorkToFirestore(title, description, date, imageUrls)
//            } else {
//                Log.e("ImageUpload", "Failed to upload some images.")
//                Toast.makeText(this, "Error uploading some images", Toast.LENGTH_SHORT).show()
//            }
//            btnSubmitWork.isEnabled = true // Re-enable button after upload completes.
//        }
//    }
//
////    private fun uploadImagesAndSaveWork(title: String, description: String, date: String) {
////        val imageUrls = mutableListOf<String>()
////        val storageRef = storage.reference.child("workOffers")
////
////        btnSubmitWork.isEnabled = false // Disable button to prevent multiple submissions.
////
////        Toast.makeText(this, "Uploading images, please wait...", Toast.LENGTH_SHORT).show()
////
////        val uploadTasks = imageUris.mapIndexed { index, uri ->
////            val imageRef = storageRef.child("${System.currentTimeMillis()}_${uri.lastPathSegment}")
////            imageRef.putFile(uri)
////                .continueWithTask { task ->
////                    if (!task.isSuccessful) task.exception?.let { throw it }
////                    imageRef.downloadUrl // Get the download URL after upload.
////                }.addOnSuccessListener { downloadUri ->
////                    imageUrls.add(downloadUri.toString())
////                }.addOnFailureListener { exception ->
////                    Log.e("ImageUpload", "Failed to upload image at index $index: ${exception.message}")
////                    Toast.makeText(this, "Error uploading image at index $index", Toast.LENGTH_SHORT).show()
////                }
////        }
////
////        Tasks.whenAllSuccess(uploadTasks).addOnCompleteListener {
////            saveWorkToFirestore(title, description, date, imageUrls)
////            btnSubmitWork.isEnabled = true // Re-enable button after upload completes.
////        }.addOnFailureListener { exception ->
////            Log.e("ImageUpload", "Failed to upload some images: ${exception.message}")
////            Toast.makeText(this, "Error uploading some images", Toast.LENGTH_SHORT).show()
////            btnSubmitWork.isEnabled = true // Re-enable button on failure.
////        }
////    }
//
//    private fun saveWorkToFirestore(title: String, description: String, date: String, imageUrls: List<String>) {
//        val workData = hashMapOf(
//            "title" to title,
//            "description" to description,
//            "date" to date,
//            "images" to imageUrls,
//            "createdAt" to FieldValue.serverTimestamp()
//        )
//
//        db.collection("workOffers").add(workData)
//            .addOnSuccessListener {
//                Toast.makeText(this, "Work offer created successfully!", Toast.LENGTH_SHORT).show()
//                finish() // Close the activity after successful creation.
//            }
//            .addOnFailureListener { exception ->
//                Log.e("Firestore", "Failed to save work offer: ${exception.message}")
//                Toast.makeText(this, "Error creating work offer", Toast.LENGTH_SHORT).show()
//                btnSubmitWork.isEnabled = true // Re-enable button on failure.
//            }
//    }
//}
//

















//class CreateWorkActivity : AppCompatActivity() {
//
//    private lateinit var etWorkTitle: EditText
//    private lateinit var etWorkDescription: EditText
//    private lateinit var etWorkDate: EditText
//    private lateinit var btnSubmitWork: Button
//    private lateinit var btnSelectImages: Button
//    private lateinit var db: FirebaseFirestore
//    private lateinit var storage: FirebaseStorage
//
//    private val PICK_IMAGES_REQUEST_CODE = 1
//    private val imageUris = mutableListOf<Uri>()
//
//    private lateinit var rvSelectedImages: RecyclerView
//    private lateinit var imageAdapter: ImageAdapterSelectedImage
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_create_work)
//
//        db = FirebaseFirestore.getInstance()
//        storage = FirebaseStorage.getInstance()
//
//        // Initialize views
//        etWorkTitle = findViewById(R.id.etWorkTitle)
//        etWorkDescription = findViewById(R.id.etWorkDescription)
//        etWorkDate = findViewById(R.id.etWorkDate)
//        btnSubmitWork = findViewById(R.id.btnSubmitWork)
//        btnSelectImages = findViewById(R.id.btnSelectImages)
//
//        btnSelectImages.setOnClickListener { pickImagesFromGallery() }
//        btnSubmitWork.setOnClickListener { validateAndCreateWork() }
//
//
//        rvSelectedImages = findViewById(R.id.rvSelectedImages)
//        imageAdapter = ImageAdapterSelectedImage(imageUris)
//        rvSelectedImages.layoutManager = GridLayoutManager(this, 3)
//        rvSelectedImages.adapter = imageAdapter
//    }
//
//    private fun pickImagesFromGallery() {
//        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
//            type = "image/*"
//            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        }
//        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST_CODE)
//    }
//
////    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
////        super.onActivityResult(requestCode, resultCode, data)
////        if (requestCode == PICK_IMAGES_REQUEST_CODE && resultCode == RESULT_OK) {
////            data?.clipData?.let { clipData ->
////                for (i in 0 until clipData.itemCount) {
////                    clipData.getItemAt(i).uri?.let { uri -> imageUris.add(uri) }
////                }
////            } ?: data?.data?.let { uri -> imageUris.add(uri) }
////
////            Toast.makeText(this, "${imageUris.size} images selected", Toast.LENGTH_SHORT).show()
////        }
////    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == PICK_IMAGES_REQUEST_CODE && resultCode == RESULT_OK) {
//            data?.clipData?.let { clipData ->
//                for (i in 0 until clipData.itemCount) {
//                    clipData.getItemAt(i).uri?.let { uri -> imageUris.add(uri) }
//                }
//            } ?: data?.data?.let { uri -> imageUris.add(uri) }
//
//            // Notify adapter of data change
//            imageAdapter.notifyDataSetChanged()
//            Toast.makeText(this, "${imageUris.size} images selected", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun validateAndCreateWork() {
//        val title = etWorkTitle.text.toString().trim()
//        val description = etWorkDescription.text.toString().trim()
//        val date = etWorkDate.text.toString().trim()
//
//        when {
//            title.isEmpty() || description.isEmpty() || date.isEmpty() -> {
//                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
//            }
//            imageUris.isEmpty() -> {
//                Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
//            }
//            else -> uploadImagesAndSaveWork(title, description, date)
//        }
//    }
//
//    private fun uploadImagesAndSaveWork(title: String, description: String, date: String) {
//        val imageUrls = mutableListOf<String>()
//        val storageRef = storage.reference.child("workOffers")
//
//        btnSubmitWork.isEnabled = false // Disable button to prevent multiple submissions
//        Toast.makeText(this, "Uploading images, please wait...", Toast.LENGTH_SHORT).show()
//
//        imageUris.forEachIndexed { index, uri ->
//            val imageRef = storageRef.child("${System.currentTimeMillis()}_${uri.lastPathSegment}")
//            imageRef.putFile(uri)
//                .continueWithTask { task ->
//                    if (!task.isSuccessful) task.exception?.let { throw it }
//                    imageRef.downloadUrl
//                }
//                .addOnSuccessListener { downloadUri ->
//                    imageUrls.add(downloadUri.toString())
//                    if (imageUrls.size == imageUris.size) { // All images uploaded
//                        saveWorkToFirestore(title, description, date, imageUrls)
//                    }
//                }
//                .addOnFailureListener { exception ->
//                    Log.e("ImageUpload", "Failed to upload image: ${exception.message}")
//                    Toast.makeText(this, "Error uploading images", Toast.LENGTH_SHORT).show()
//                    btnSubmitWork.isEnabled = true
//                }
//        }
//    }
//
//    private fun saveWorkToFirestore(title: String, description: String, date: String, imageUrls: List<String>) {
//        val workData = hashMapOf(
//            "title" to title,
//            "description" to description,
//            "date" to date,
//            "images" to imageUrls,
//            "createdAt" to FieldValue.serverTimestamp()
//        )
//
//        db.collection("workOffers").add(workData)
//            .addOnSuccessListener {
//                Toast.makeText(this, "Work offer created successfully!", Toast.LENGTH_SHORT).show()
//                finish() // Close the activity after successful creation
//            }
//            .addOnFailureListener { exception ->
//                Log.e("Firestore", "Failed to save work: ${exception.message}")
//                Toast.makeText(this, "Error creating work offer", Toast.LENGTH_SHORT).show()
//                btnSubmitWork.isEnabled = true
//            }
//    }
//}





















//class CreateWorkActivity : AppCompatActivity() {
//
//    private lateinit var etWorkTitle: EditText
//    private lateinit var etWorkDescription: EditText
//    private lateinit var etWorkDate: EditText
//    private lateinit var btnSubmitWork: Button
//    private lateinit var btnSelectImages: Button
//    private lateinit var db: FirebaseFirestore
//    private lateinit var storage: FirebaseStorage
//
//    private val PICK_IMAGES_REQUEST_CODE = 1
////    private var imageUris: MutableList<Uri> = mutableListOf()
//
//    private lateinit var rvSelectedImages: RecyclerView
//    private lateinit var imageAdapter: ImageAdapterSelectedImage
//    private val imageUris = mutableListOf<Uri>()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_create_work)
//
//        // Initialize Firebase Firestore and Storage
//        db = FirebaseFirestore.getInstance()
//        storage = FirebaseStorage.getInstance()
//
//        // Find views by ID
//        etWorkTitle = findViewById(R.id.etWorkTitle)
//        etWorkDescription = findViewById(R.id.etWorkDescription)
//        etWorkDate = findViewById(R.id.etWorkDate)
//        btnSubmitWork = findViewById(R.id.btnSubmitWork)
//        btnSelectImages = findViewById(R.id.btnSelectImages)
//
//        rvSelectedImages = findViewById(R.id.rvSelectedImages)
//        imageAdapter = ImageAdapterSelectedImage(imageUris)
//        rvSelectedImages.layoutManager = GridLayoutManager(this, 3)
//        rvSelectedImages.adapter = imageAdapter
//
//        // Set a click listener for selecting images
//        btnSelectImages.setOnClickListener {
//            pickImagesFromGallery()
//        }
//
//        // Set a click listener on the submit button
//        btnSubmitWork.setOnClickListener {
//            createWork()
//        }
//    }
//
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == PICK_IMAGES_REQUEST_CODE && resultCode == RESULT_OK) {
//            data?.clipData?.let { clipData ->
//                for (i in 0 until clipData.itemCount) {
//                    clipData.getItemAt(i).uri?.let { uri -> imageUris.add(uri) }
//                }
//            } ?: data?.data?.let { uri -> imageUris.add(uri) }
//
//            // Notify adapter of data change
//            imageAdapter.notifyDataSetChanged()
//            Toast.makeText(this, "${imageUris.size} images selected", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun pickImagesFromGallery() {
//        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
//            type = "image/*"
//            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        }
//        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST_CODE)
//    }
//
//    // Handle the result of the image picker
////    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
////        super.onActivityResult(requestCode, resultCode, data)
////        if (requestCode == PICK_IMAGES_REQUEST_CODE && resultCode == RESULT_OK) {
////            data?.let {
////                if (it.clipData != null) {
////                    val count = it.clipData?.itemCount ?: 0
////                    for (i in 0 until count) {
////                        val imageUri = it.clipData?.getItemAt(i)?.uri
////                        imageUri?.let { uri -> imageUris.add(uri) }
////                    }
////                } else {
////                    it.data?.let { uri -> imageUris.add(uri) }
////                }
////            }
////        }
////    }
//
//    // Method to upload images and create work
//    private fun createWork() {
//        val title = etWorkTitle.text.toString().trim()
//        val description = etWorkDescription.text.toString().trim()
//        val date = etWorkDate.text.toString().trim()
//
//        if (title.isEmpty() || description.isEmpty() || date.isEmpty()) {
//            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        if (imageUris.isEmpty()) {
//            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // Upload images to Firebase Storage and get their URLs
//        val imageUrls: MutableList<String> = mutableListOf()
//        val storageRef = storage.reference
//        var uploadCount = 0
//
//        for (uri in imageUris) {
//            val imageRef = storageRef.child("workOffers/${System.currentTimeMillis()}_${uri.lastPathSegment}")
//
//            imageRef.putFile(uri).continueWithTask { task ->
//                if (!task.isSuccessful) {
//                    task.exception?.let {
//                        throw it
//                    }
//                }
//                imageRef.downloadUrl
//            }.addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    val downloadUri = task.result
//                    imageUrls.add(downloadUri.toString())
//                    uploadCount++
//
//                    if (uploadCount == imageUris.size) {
//                        saveWorkOffer(title, description, date, imageUrls)
//                    }
//                } else {
//                    task.exception?.let {
//                        Log.e("FirebaseStorage", "Image upload failed: ${it.message}")
//                    }
//                    Toast.makeText(this, "Failed to upload images", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//        }
//    }
//
//
//
//
//
//
//
//
//    // Method to save the work offer with image URLs to Firestore
//    private fun saveWorkOffer(title: String, description: String, date: String, imageUrls: List<String>) {
//        val workData = hashMapOf(
//            "title" to title,
//            "description" to description,
//            "date" to date,
//            "images" to imageUrls,
//            "createdAt" to FieldValue.serverTimestamp()
//        )
//
//        // Add the work offer to Firestore
//        db.collection("workOffers").add(workData)
//            .addOnSuccessListener {
//                Toast.makeText(this, "Work offer created!", Toast.LENGTH_SHORT).show()
//                finish() // Close the activity after creating work
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(this, "Error creating work: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//}























//class CreateWorkActivity : AppCompatActivity() {
//
//    private lateinit var etWorkTitle: EditText
//    private lateinit var etWorkDescription: EditText
//    private lateinit var etWorkDate: EditText
//    private lateinit var btnSubmitWork: Button
//    private lateinit var btnSelectImages: Button
//    private lateinit var db: FirebaseFirestore
//    private lateinit var storage: FirebaseStorage
//
////    private val PICK_IMAGES_REQUEST = 1
////    private var selectedImagesUriList: MutableList<Uri> = mutableListOf()
//
//    private val PICK_IMAGES_REQUEST_CODE = 1
//    private var imageUris: MutableList<Uri> = mutableListOf()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_create_work)
//
//        // Initialize Firebase Firestore and Storage
//        db = FirebaseFirestore.getInstance()
//        storage = FirebaseStorage.getInstance()
//
//        // Find views by ID
//        etWorkTitle = findViewById(R.id.etWorkTitle)
//        etWorkDescription = findViewById(R.id.etWorkDescription)
//        etWorkDate = findViewById(R.id.etWorkDate)
//        btnSubmitWork = findViewById(R.id.btnSubmitWork)
//        btnSelectImages = findViewById(R.id.btnSelectImages)
//
//        // Set a click listener for selecting images
//        btnSelectImages.setOnClickListener {
//            selectImages()
//        }
//
//        // Set a click listener on the submit button
//        btnSubmitWork.setOnClickListener {
//            createWork()
//        }
//    }
//
//    private fun pickImagesFromGallery() {
//        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
//            type = "image/*"
//            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        }
//        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST_CODE)
//    }
//
//    // Handle the result of the image picker
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == PICK_IMAGES_REQUEST_CODE && resultCode == RESULT_OK) {
//            if (data?.clipData != null) {
//                val count = data.clipData?.itemCount ?: 0
//                for (i in 0 until count) {
//                    val imageUri = data.clipData?.getItemAt(i)?.uri
//                    if (imageUri != null) {
//                        imageUris.add(imageUri)
//                    }
//                }
//            } else if (data?.data != null) {
//                imageUris.add(data.data!!)
//            }
//            // Upload selected images
//            uploadImagesToFirebase(imageUris)
//        }
//    }
//
//
//    // Upload images to Firebase Storage
//    private fun uploadImagesToFirebase(imageUris: List<Uri>) {
//        for (uri in imageUris) {
//            val storageRef = FirebaseStorage.getInstance().reference.child("workImages/${UUID.randomUUID()}")
//            storageRef.putFile(uri)
//                .addOnSuccessListener {
//                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
//                        Log.d("FirebaseStorage", "Image uploaded: $downloadUrl")
//                        // Add the image URL to Firestore or handle it as needed
//                        saveImageUrlToFirestore(downloadUrl.toString())
//                    }
//                }
//                .addOnFailureListener { e ->
//                    Log.e("FirebaseStorage", "Failed to upload image: ${e.message}")
//                }
//        }
//    }
//
//    // Method to upload images and create work
//    private fun createWork() {
//        val title = etWorkTitle.text.toString().trim()
//        val description = etWorkDescription.text.toString().trim()
//        val date = etWorkDate.text.toString().trim()
//
//        if (title.isEmpty() || description.isEmpty() || date.isEmpty()) {
//            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        if (selectedImagesUriList.isEmpty()) {
//            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // Upload images to Firebase Storage and get their URLs
//        val imageUrls: MutableList<String> = mutableListOf()
//        val storageRef = storage.reference
//        var uploadCount = 0
//
//        for (uri in selectedImagesUriList) {
//            val imageRef = storageRef.child("workOffers/${System.currentTimeMillis()}_${uri.lastPathSegment}")
//            val uploadTask = imageRef.putFile(uri)
//
//            uploadTask.continueWithTask { task ->
//                if (!task.isSuccessful) {
//                    throw task.exception!!
//                }
//                // Continue to get the download URL
//                imageRef.downloadUrl
//            }.addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    val downloadUri = task.result
//                    imageUrls.add(downloadUri.toString())
//
//                    // When all images are uploaded, store the work offer
//                    uploadCount++
//                    if (uploadCount == selectedImagesUriList.size) {
//                        saveWorkOffer(title, description, date, imageUrls)
//                    }
//                } else {
//                    Toast.makeText(this, "Failed to upload images", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//    // Method to save the work offer with image URLs to Firestore
//    private fun saveWorkOffer(title: String, description: String, date: String, imageUrls: List<String>) {
//        val workData = hashMapOf(
//            "title" to title,
//            "description" to description,
//            "date" to date,
//            "images" to imageUrls,
//            "createdAt" to FieldValue.serverTimestamp()
//        )
//
//        // Add the work offer to Firestore
//        db.collection("workOffers").add(workData)
//            .addOnSuccessListener {
//                Toast.makeText(this, "Work offer created!", Toast.LENGTH_SHORT).show()
//                finish() // Close the activity after creating work
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(this, "Error creating work: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    // Save image URLs to Firestore
//    private fun saveImageUrlToFirestore(imageUrl: String) {
//        val workData = hashMapOf(
//            "title" to etWorkTitle.text.toString().trim(),
//            "description" to etWorkDescription.text.toString().trim(),
//            "date" to etWorkDate.text.toString().trim(),
//            "createdAt" to FieldValue.serverTimestamp(),
//            "imageUrls" to imageUris.map { it.toString() }  // Store image URLs as a list
//        )
//        db.collection("workOffers").add(workData)
//            .addOnSuccessListener {
//                Toast.makeText(this, "Work offer created with images!", Toast.LENGTH_SHORT).show()
//                finish() // Close the activity after creating work
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(this, "Error creating work: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//}
