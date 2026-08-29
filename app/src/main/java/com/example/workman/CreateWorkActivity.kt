package com.example.workman

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workman.adaptes.ImageAdapterSelectedImage
import com.example.workman.utils.LocationHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateWorkActivity : AppCompatActivity() {

    // Views
    private lateinit var etWorkTitle: TextInputEditText
    private lateinit var etWorkDescription: TextInputEditText
    private lateinit var etWorkDate: TextInputEditText
    private lateinit var btnSubmitWork: MaterialButton
    private lateinit var btnSelectImages: MaterialButton
    private lateinit var toolbar: Toolbar
    private lateinit var rvSelectedImages: RecyclerView
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var actvUrgency: AutoCompleteTextView
    private lateinit var etBudgetAmount: TextInputEditText
    private lateinit var actvBudgetType: AutoCompleteTextView
    private lateinit var actvPaymentMethod: AutoCompleteTextView
    private var progressBar: ProgressBar? = null
    private var loadingOverlay: View? = null

    // Firebase
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Data
    private val imageUris = mutableListOf<Uri>()
    private lateinit var imageAdapter: ImageAdapterSelectedImage
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedCategory: String = ""
    private var selectedUrgency: String = "THIS_WEEK"
    private var selectedBudgetType: String = "FIXED"
    private var selectedPaymentMethod: String = "CASH"

    // Map-picked location (overrides user profile location)
    private var pickedLatitude: Double = 0.0
    private var pickedLongitude: Double = 0.0
    private var pickedLocationName: String = ""
    private var hasPickedLocation: Boolean = false

    // Boss's saved profile location — used to center the map picker initially
    private var bossLatitude: Double = 0.0
    private var bossLongitude: Double = 0.0

    // Job categories — loaded dynamically from CategoryRepository
    private val jobCategories: List<String>
        get() = com.example.workman.utils.CategoryRepository.getCategoriesForSelection()

    private val defaultImageUrl = "android.resource://com.example.workman/drawable/notification_img"

    // Modern image picker using Activity Result API (no permission needed)
    private val pickMultipleImages = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val startPos = imageUris.size
            imageUris.addAll(uris)
            imageAdapter.notifyItemRangeInserted(startPos, uris.size)
            showToast(getString(R.string.images_selected_count, imageUris.size))
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
            showToast(getString(R.string.images_selected_count, imageUris.size))
        }
    }

    // Map location picker
    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                pickedLatitude = data.getDoubleExtra("latitude", 0.0)
                pickedLongitude = data.getDoubleExtra("longitude", 0.0)
                pickedLocationName = data.getStringExtra("locationName") ?: ""
                hasPickedLocation = true

                // Update the location label in UI
                val tvLocation = findViewById<TextView>(R.id.tvSelectedLocation)
                tvLocation?.text = if (pickedLocationName.isNotEmpty()) {
                    "📍 $pickedLocationName"
                } else {
                    "📍 %.4f, %.4f".format(pickedLatitude, pickedLongitude)
                }
                tvLocation?.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_work)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadBossLocation()
        applyPrefilledDate()
    }

    /**
     * When launched from the booking calendar the boss already picked a day, so
     * seed the date field instead of making them choose it twice.
     */
    private fun applyPrefilledDate() {
        val millis = intent.getLongExtra(EXTRA_PREFILL_DATE, 0L)
        if (millis > 0L) {
            calendar.timeInMillis = millis
            etWorkDate.setText(dateFormat.format(calendar.time))
        }
    }

    /** Pre-load the boss's saved location so the map picker can center on it. */
    private fun loadBossLocation() {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            try {
                val userDoc = db.collection("users").document(uid).get().await()
                bossLatitude = userDoc.getDouble("latitude") ?: 0.0
                bossLongitude = userDoc.getDouble("longitude") ?: 0.0
            } catch (e: Exception) {
                Log.w(TAG, "Could not preload boss location", e)
            }
        }
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

        // Category dropdown
        actvCategory = findViewById(R.id.actvCategory)
        val categoryAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jobCategories)
        actvCategory.setAdapter(categoryAdapter)
        actvCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = jobCategories[position]
        }

        // Urgency dropdown
        actvUrgency = findViewById(R.id.actvUrgency)
        val urgencyOptions = listOf(
            getString(R.string.urgency_urgent),
            getString(R.string.urgency_this_week),
            getString(R.string.urgency_flexible)
        )
        val urgencyValues = listOf("URGENT", "THIS_WEEK", "FLEXIBLE")
        val urgencyAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, urgencyOptions)
        actvUrgency.setAdapter(urgencyAdapter)
        actvUrgency.setOnItemClickListener { _, _, position, _ ->
            selectedUrgency = urgencyValues[position]
        }

        // Budget / pay
        etBudgetAmount = findViewById(R.id.etBudgetAmount)
        actvBudgetType = findViewById(R.id.actvBudgetType)
        val budgetTypeOptions = listOf(
            getString(R.string.budget_fixed),
            getString(R.string.budget_hourly),
            getString(R.string.budget_negotiable)
        )
        val budgetTypeValues = listOf("FIXED", "HOURLY", "NEGOTIABLE")
        val budgetTypeAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, budgetTypeOptions)
        actvBudgetType.setAdapter(budgetTypeAdapter)
        actvBudgetType.setOnItemClickListener { _, _, position, _ ->
            selectedBudgetType = budgetTypeValues[position]
        }

        // Payment method — how the boss will pay the worker (Phase 1: no gateway)
        actvPaymentMethod = findViewById(R.id.actvPaymentMethod)
        val paymentMethodOptions = listOf(
            getString(R.string.payment_cash),
            getString(R.string.payment_online)
        )
        val paymentMethodValues = listOf("CASH", "ONLINE")
        val paymentMethodAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, paymentMethodOptions)
        actvPaymentMethod.setAdapter(paymentMethodAdapter)
        actvPaymentMethod.setOnItemClickListener { _, _, position, _ ->
            selectedPaymentMethod = paymentMethodValues[position]
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.create_job_title)
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

        // Map picker button
        findViewById<MaterialButton>(R.id.btnPickLocation)?.setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            if (hasPickedLocation) {
                intent.putExtra("initial_lat", pickedLatitude)
                intent.putExtra("initial_lng", pickedLongitude)
            } else if (bossLatitude != 0.0 && bossLongitude != 0.0) {
                // Center on the boss's saved location so the map isn't a world view
                intent.putExtra("initial_lat", bossLatitude)
                intent.putExtra("initial_lng", bossLongitude)
            }
            mapPickerLauncher.launch(intent)
        }
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
                etWorkTitle.error = getString(R.string.error_title_required)
                etWorkTitle.requestFocus()
            }
            description.isEmpty() -> {
                etWorkDescription.error = getString(R.string.error_description_required)
                etWorkDescription.requestFocus()
            }
            date.isEmpty() -> {
                etWorkDate.error = getString(R.string.error_date_required)
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
                    showToast(getString(R.string.job_posted_success))
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload/Save failed", e)
                withContext(Dispatchers.Main) {
                    showToast(getString(R.string.job_post_error, e.localizedMessage ?: ""))
                    setLoading(false)
                }
            }
        }
    }

    private suspend fun uploadImages(): List<String> = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) {
            return@withContext listOf(defaultImageUrl)
        }

        // Upload to Cloudinary (free, no Firebase Storage billing needed)
        val uploadedUrls = com.example.workman.utils.CloudinaryUploader.uploadImages(
            context = this@CreateWorkActivity,
            uris = imageUris,
            folder = "workOffers"
        )

        if (uploadedUrls.isEmpty()) {
            Log.w(TAG, "Cloudinary upload returned no URLs (check CloudinaryUploader config)")
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

        // Parse budget/pay. Empty or 0 => Negotiable.
        val budgetAmount = etBudgetAmount.text?.toString()?.trim()
            ?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        val budgetType = if (budgetAmount <= 0.0) "NEGOTIABLE" else selectedBudgetType
        val currency = "Rs"
        val payLabel = if (budgetAmount <= 0.0) {
            "Negotiable"
        } else {
            val amountStr = if (budgetAmount % 1.0 == 0.0) budgetAmount.toLong().toString()
            else budgetAmount.toString()
            val suffix = if (budgetType == "HOURLY") "/hr" else ""
            "$currency $amountStr$suffix"
        }

        // Get location: prefer map-picked location, fallback to boss's profile location
        var latitude = 0.0
        var longitude = 0.0
        var geohash = ""
        var locationName = ""

        if (hasPickedLocation && pickedLatitude != 0.0) {
            // Use the map-picked location
            latitude = pickedLatitude
            longitude = pickedLongitude
            geohash = LocationHelper.encode(latitude, longitude)
            locationName = pickedLocationName
        } else {
            // Fallback: use boss's stored profile location
            try {
                currentUser?.uid?.let { uid ->
                    val userDoc = db.collection("users").document(uid).get().await()
                    latitude = userDoc.getDouble("latitude") ?: 0.0
                    longitude = userDoc.getDouble("longitude") ?: 0.0
                    geohash = userDoc.getString("geohash") ?: ""
                    locationName = userDoc.getString("location") ?: ""

                    if (geohash.isEmpty() && latitude != 0.0 && longitude != 0.0) {
                        geohash = LocationHelper.encode(latitude, longitude)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch boss location for job", e)
            }
        }

        // Fetch the boss's profile photo (Cloudinary URL saved in Firestore) so
        // job cards show the correct poster image. FirebaseAuth.photoUrl is not
        // populated by our profile flow, so it is only used as a fallback.
        var bossPhotoUrl = currentUser?.photoUrl?.toString() ?: ""
        // Likewise, the display name lives in the Firestore profile (our sign-up
        // saves `name`, the profile screen saves `firstName`/`lastName`).
        // FirebaseAuth.displayName is usually null, so pull the real name from
        // Firestore and only fall back to auth/"User" when nothing is stored.
        var bossName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: ""
        try {
            currentUser?.uid?.let { uid ->
                val profileDoc = db.collection("users").document(uid).get().await()
                val storedPhoto = profileDoc.getString("photoUrl") ?: ""
                if (storedPhoto.isNotBlank()) bossPhotoUrl = storedPhoto

                val storedName = profileDoc.getString("name")?.trim().orEmpty()
                val composedName = listOfNotNull(
                    profileDoc.getString("firstName")?.trim(),
                    profileDoc.getString("lastName")?.trim()
                ).filter { it.isNotBlank() }.joinToString(" ")
                val resolvedName = when {
                    storedName.isNotBlank() -> storedName
                    composedName.isNotBlank() -> composedName
                    else -> ""
                }
                if (resolvedName.isNotBlank()) bossName = resolvedName
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch boss profile for job", e)
        }
        if (bossName.isBlank()) bossName = "WorkMan Client"

        val workData = hashMapOf(
            "jobId" to jobId,
            "title" to title,
            "description" to description,
            "date" to date,
            "images" to imageUrls,
            "bossId" to (currentUser?.uid ?: "unknown"),
            "bossName" to bossName,
            "bossPhoto" to bossPhotoUrl,
            "status" to "OPEN",
            "isAccepted" to false,
            "category" to selectedCategory,
            "urgency" to selectedUrgency,
            // Pay / budget the worker will earn on completion
            "budgetAmount" to budgetAmount,
            "budgetType" to budgetType,
            "currency" to currency,
            // How the boss intends to pay the worker (Phase 1 manual flow)
            "paymentMethod" to selectedPaymentMethod,
            "createdAt" to FieldValue.serverTimestamp(),
            // Location data for geo-based filtering
            "latitude" to latitude,
            "longitude" to longitude,
            "geohash" to geohash,
            "locationName" to locationName
        )

        db.collection("workOffers").document(jobId).set(workData).await()
        Log.d(TAG, "Job saved: $jobId with location ($latitude, $longitude)")

        // Create a linked PENDING booking so the job appears in the Bookings → Pending tab.
        // Booking ID == jobId for a clean 1:1 link (avoids duplicates).
        // This is BEST-EFFORT: if it fails (e.g. rules not deployed), the job post still succeeds.
        try {
            val scheduledDate = try {
                dateFormat.parse(date) ?: calendar.time
            } catch (e: Exception) {
                calendar.time
            }
            val bookingData = hashMapOf(
                "jobId" to jobId,
                "bossId" to (currentUser?.uid ?: "unknown"),
                "bossName" to bossName,
                "workerId" to "",
                "workerName" to "",
                "workerPhotoUrl" to "",
                "serviceName" to title,
                "agreedRate" to payLabel,
                "status" to "PENDING",
                "date" to scheduledDate,
                // Manual payment tracking (Phase 1)
                "paymentStatus" to "UNPAID",
                "paymentMethod" to selectedPaymentMethod,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("bookings").document(jobId).set(bookingData).await()
            Log.d(TAG, "Pending booking created for job: $jobId")
        } catch (e: Exception) {
            // Don't fail the whole post — booking is a convenience mirror of the job.
            Log.w(TAG, "Could not create pending booking (job still posted): ${e.message}")
        }
    }

    private fun setLoading(loading: Boolean) {
        btnSubmitWork.isEnabled = !loading
        btnSelectImages.isEnabled = !loading
        loadingOverlay?.visibility = if (loading) View.VISIBLE else View.GONE

        btnSubmitWork.text =
            if (loading) getString(R.string.posting) else getString(R.string.post_job)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "CreateWorkActivity"

        /** Optional Long extra (epoch millis) used to pre-fill the work date. */
        const val EXTRA_PREFILL_DATE = "prefill_date_millis"
    }
}
