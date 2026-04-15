package com.example.workman

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workman.adaptes.ImageAdapterSelectedImage
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CreateWorkActivity : AppCompatActivity() {

    // Views
    private lateinit var etWorkTitle: TextInputEditText
    private lateinit var etWorkDescription: TextInputEditText
    private lateinit var etWorkDate: TextInputEditText
    private lateinit var btnSubmitWork: MaterialButton
    private lateinit var btnSelectImages: MaterialButton
    private lateinit var toolbar: Toolbar
    private lateinit var rvSelectedImages: RecyclerView
    private var progressBar: ProgressBar? = null
    private var loadingOverlay: View? = null

    // Firebase
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Data
    private val imageUris = mutableListOf<Uri>()
    private lateinit var imageAdapter: ImageAdapterSelectedImage
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val defaultImageUrl = "android.resource://com.example.workman/drawable/notification_img"

    // Modern image picker using Activity Result API (no permission needed)
    private val pickMultipleImages = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val startPos = imageUris.size
            imageUris.addAll(uris)
            imageAdapter.notifyItemRangeInserted(startPos, uris.size)
            showToast("${imageUris.size} image(s) selected")
        }
    }

    // Alternative: Photo Picker for Android 13+ with fallback
    private val pickVisualMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val startPos = imageUris.size
            imageUris.addAll(uris)
            imageAdapter.notifyItemRangeInserted(startPos, uris.size)
            showToast("${imageUris.size} image(s) selected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_work)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar1)
        etWorkTitle = findViewById(R.id.etWorkTitle)
        etWorkDescription = findViewById(R.id.etWorkDescription)
        etWorkDate = findViewById(R.id.etWorkDate)
        btnSubmitWork = findViewById(R.id.btnSubmitWork)
        btnSelectImages = findViewById(R.id.btnSelectImages)
        rvSelectedImages = findViewById(R.id.rvSelectedImages)
        // Loading overlay for upload state
        loadingOverlay = findViewById(R.id.loadingOverlay)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Post a Job"
        }
    }

    private fun setupRecyclerView() {
        imageAdapter = ImageAdapterSelectedImage(imageUris)
        rvSelectedImages.apply {
            layoutManager = GridLayoutManager(this@CreateWorkActivity, 3)
            adapter = imageAdapter
        }
    }

    private fun setupClickListeners() {
        etWorkDate.setOnClickListener { showDatePicker() }
        btnSelectImages.setOnClickListener { pickImages() }
        btnSubmitWork.setOnClickListener { validateAndSubmit() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                etWorkDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickImages() {
        // Use Photo Picker on Android 11+ for better UX, fallback to GetMultipleContents
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
            pickVisualMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            pickMultipleImages.launch("image/*")
        }
    }

    private fun validateAndSubmit() {
        val title = etWorkTitle.text.toString().trim()
        val description = etWorkDescription.text.toString().trim()
        val date = etWorkDate.text.toString().trim()

        when {
            title.isEmpty() -> {
                etWorkTitle.error = "Title is required"
                etWorkTitle.requestFocus()
            }
            description.isEmpty() -> {
                etWorkDescription.error = "Description is required"
                etWorkDescription.requestFocus()
            }
            date.isEmpty() -> {
                etWorkDate.error = "Date is required"
                etWorkDate.requestFocus()
            }
            else -> uploadAndSave(title, description, date)
        }
    }

    private fun uploadAndSave(title: String, description: String, date: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val imageUrls = uploadImages()
                saveToFirestore(title, description, date, imageUrls)

                withContext(Dispatchers.Main) {
                    showToast("Job posted successfully!")
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload/Save failed", e)
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.localizedMessage}")
                    setLoading(false)
                }
            }
        }
    }

    private suspend fun uploadImages(): List<String> = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) {
            return@withContext listOf(defaultImageUrl)
        }

        val storageRef = storage.reference.child("workOffers")
        val uploadedUrls = mutableListOf<String>()

        imageUris.forEachIndexed { index, uri ->
            try {
                val fileName = "${System.currentTimeMillis()}_${index}_${uri.lastPathSegment ?: "image"}"
                val imageRef = storageRef.child(fileName)

                // Upload and get download URL
                imageRef.putFile(uri).await()
                val downloadUrl = imageRef.downloadUrl.await()
                uploadedUrls.add(downloadUrl.toString())

                Log.d(TAG, "Uploaded image $index: $downloadUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload image $index: ${e.message}")
                // Continue with other images, don't fail entire batch
            }
        }

        // Return uploaded URLs or default if all failed
        uploadedUrls.ifEmpty { listOf(defaultImageUrl) }
    }

    private suspend fun saveToFirestore(
        title: String,
        description: String,
        date: String,
        imageUrls: List<String>
    ) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
        val jobId = db.collection("workOffers").document().id

        val workData = hashMapOf(
            "jobId" to jobId,
            "title" to title,
            "description" to description,
            "date" to date,
            "images" to imageUrls,
            "bossId" to (currentUser?.uid ?: "unknown"),
            "bossName" to (currentUser?.displayName ?: "User"),
            "bossPhoto" to (currentUser?.photoUrl?.toString() ?: ""),
            "status" to "OPEN",
            "isAccepted" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("workOffers").document(jobId).set(workData).await()
        Log.d(TAG, "Job saved: $jobId")
    }

    private fun setLoading(loading: Boolean) {
        btnSubmitWork.isEnabled = !loading
        btnSelectImages.isEnabled = !loading
        loadingOverlay?.visibility = if (loading) View.VISIBLE else View.GONE

        btnSubmitWork.text = if (loading) "Posting..." else "Post Job"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "CreateWorkActivity"
    }
}
