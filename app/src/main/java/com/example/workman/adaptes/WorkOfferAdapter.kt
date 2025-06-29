package com.example.workman.adaptes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workman.R
import com.example.workman.dataClass.WorkOffer
//import com.google.android.gms.common.api.Response
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

import android.util.Log
//import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
//import org.json.JSONObject
import com.example.workman.BuildConfig
import org.json.JSONException
import java.nio.charset.Charset


class WorkOfferAdapter(
    private val context: Context,
    private val workOffers: MutableList<WorkOffer>,  // Keep mutable for dynamic changes
    private val db: FirebaseFirestore                // Firestore instance
) : ArrayAdapter<WorkOffer>(context, 0, workOffers) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the current work offer
        val currentWorkOffer = getItem(position) ?: return convertView ?: View(context)

        // Inflate the view if not already done
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.work_offer_item, parent, false)


        // Bind views
        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
        val dateTextView = view.findViewById<TextView>(R.id.dateTextView)
        val createdAtTextView = view.findViewById<TextView>(R.id.createdAtTextView)
        val acceptButton = view.findViewById<Button>(R.id.acceptButton)
        val imageRecyclerView: RecyclerView = view.findViewById(R.id.recyclerViewImages)

        // Set up the RecyclerView for images
        imageRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        currentWorkOffer.images?.let { images ->
            val imageAdapter = ImageAdapter(images) // Assumes ImageAdapter handles image URLs
            imageRecyclerView.adapter = imageAdapter
        }

        // Populate text views
        titleTextView.text = currentWorkOffer.title ?: "Untitled"
        descriptionTextView.text = currentWorkOffer.description ?: "No description"
        dateTextView.text = "Date: ${currentWorkOffer.date ?: "No date"}"
        createdAtTextView.text = "Created at: ${currentWorkOffer.createdAt ?: "No date available"}"

        // Handle Accept Button
        acceptButton.isEnabled = currentWorkOffer.acceptedBy == null // Enable only if not already accepted
        acceptButton.text = if (currentWorkOffer.acceptedBy == null) "Accept Work" else "Accepted"
        acceptButton.setOnClickListener {
            acceptWork(currentWorkOffer, acceptButton)
        }

        return view
    }

    /**
     * Handles the acceptance of a work offer.
     */
    private fun acceptWork(workOffer: WorkOffer, acceptButton: Button) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("workOffers")
            .document(workOffer.id)
            .update(
                mapOf(
                    "acceptedBy" to userId,
                    "status" to "accepted",
                    "isAccepted" to true
                )
            )
            .addOnSuccessListener {
                // Update UI on successful acceptance
                (context as? Activity)?.runOnUiThread {
                    acceptButton.isEnabled = false
                    acceptButton.text = "Accepted"
                    Toast.makeText(context, "Work accepted successfully!", Toast.LENGTH_SHORT).show()

                    // Now send a notification to the boss
                    sendNotificationToBoss(workOffer, userId)


                }
            }
            .addOnFailureListener { exception ->
                // Display an error message
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Error accepting work: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Sends a notification to the boss when the work is accepted.
     */
//    private fun sendNotificationToBoss(workOffer: WorkOffer) {
//        // Get the boss's FCM token from Firestore
//        db.collection("workOffers")
//            .document(workOffer.id)
//            .get()
//            .addOnSuccessListener { document ->
//                val fcmToken = document.getString("fcmToken") // Assuming the FCM token is stored in the 'fcmToken' field
//
//                Log.e("WorkOfferAdapter", "No FCM token found for work offer ID: ${workOffer.id}")
//                if (fcmToken != null) {
//                    // Create a notification payload
//
//                    val notification = mapOf(
//                        "to" to fcmToken,
//                        "notification" to mapOf(
//                            "title" to "Work Accepted",
//                            "body" to "Your work offer has been accepted by a worker."
//                        )
//                    )
//
//                    // Send the notification (using FCM)
//                    sendFCMNotification(notification)
//                }
//            }
//    }
//
//    /**
//     * Sends the notification via Firebase Cloud Messaging (FCM).
//     */
//    private fun sendFCMNotification(notification: Map<String, Any>) {
//        // FCM Server URL and API Key for authorization
//        val fcmUrl = "https://fcm.googleapis.com/fcm/send"
////        val apiKey = "AIzaSyCEGbbOF-ZAs3wVmdUhRonGJGJ78oqH4Us"
//        val apiKey = BuildConfig.FCM_API_KEY// Replace with your actual Firebase Server Key
//
//
//        val json = JSONObject(notification)
//
//        // Create the JSON Object Request for sending the FCM notification
//        val request = object : JsonObjectRequest(
//            Method.POST, // HTTP method (POST)
//            fcmUrl, // URL
//            json, // JSON object as body
//            Response.Listener { response ->
//                Log.d("FCM", "Notification sent successfully: $response")
//            },
//            Response.ErrorListener { error ->
//                Log.e("FCM", "Failed to send notification: ${error.message}")
//            }) {
//
//            // Set headers, including the authorization key
//            @Throws(AuthFailureError::class)
//            override fun getHeaders(): MutableMap<String, String> {
//                val headers = mutableMapOf<String, String>()
//                headers["Authorization"] = "key=$apiKey"
//                return headers
//            }
//        }
//
//        // Add the request to the Volley request queue
//        Volley.newRequestQueue(context).add(request)
//    }


    private fun sendNotificationToBoss(workOffer: WorkOffer, workerId: String) {
        // Get the boss's ID from the work offer
        val bossId = "vYnWdsuRWNPPfjUNj0DUlKzG7nK2" // Assuming createdBy holds the boss's ID

        // Retrieve the boss's FCM token from Firestore
        db.collection("users").document(bossId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    Log.d("FCM", "Document data: ${document.data}")
                    val bossToken = document.getString("fcmToken") // Assuming fcmToken is stored in user document

                    if (bossToken != null) {
                        // Prepare notification data
                        val notificationData = mapOf(
                            "title" to "Work Accepted",
                            "message" to "${FirebaseAuth.getInstance().currentUser?.displayName} has accepted the work '${workOffer.title}'."
                        )

                        // Send notification
                        sendFCMNotification(bossToken, notificationData)
                    } else {
                        Log.e("FCM", "Boss token is null")
                    }
                } else {
                    Log.e("FCM", "No such document or document does not exist")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FCM", "Error retrieving boss's token: ${exception.message}")
            }
    }

    private fun sendFCMNotification(token: String, data: Map<String, String>) {
        val jsonBody = JSONObject()
        val apiKey = BuildConfig.FCM_API_KEY
        try {
            jsonBody.put("to", token)
            jsonBody.put("data", JSONObject(data)) // Sending data payload
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val requestQueue = Volley.newRequestQueue(context)
        val stringRequest = object : StringRequest(Method.POST, "https://fcm.googleapis.com/fcm/send",
            Response.Listener { response ->
                Log.d("FCM", "Notification sent successfully: $response")
            },
            Response.ErrorListener { error ->
                Log.e("FCM", "Error sending notification: ${error.message}")
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "key=$apiKey" // Replace with your server key
                headers["Content-Type"] = "application/json"
                return headers
            }

            override fun getBody(): ByteArray {
                return jsonBody.toString().toByteArray(Charset.defaultCharset())
            }
        }

        requestQueue.add(stringRequest)
    }




}












//class WorkOfferAdapter(
//    private val context: Context,
//    private val workOffers: MutableList<WorkOffer>,  // Keep mutable for dynamic changes
//    private val db: FirebaseFirestore                // Firestore instance
//) : ArrayAdapter<WorkOffer>(context, 0, workOffers) {
//
//    private var imageAdapter: ImageAdapter? = null // Cache the image adapter
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//        // Get the current work offer
//        val currentWorkOffer = getItem(position) ?: return convertView ?: View(context)
//
//        // Inflate the view if not already done
//        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.work_offer_item, parent, false)
//
//        // Bind views
//        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
//        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
//        val dateTextView = view.findViewById<TextView>(R.id.dateTextView)
//        val createdAtTextView = view.findViewById<TextView>(R.id.createdAtTextView)
//        val acceptButton = view.findViewById<Button>(R.id.acceptButton)
//        val imageRecyclerView: RecyclerView = view.findViewById(R.id.recyclerViewImages)
//
//        // Set up the RecyclerView for images only once
////        if (imageAdapter == null) {
////            imageAdapter = ImageAdapter(currentWorkOffer.images ?: listOf()) // Assumes ImageAdapter handles image URLs
////            imageRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
////            imageRecyclerView.adapter = imageAdapter
////        } else {
//////            imageAdapter?.updateImages(currentWorkOffer.images ?: listOf())
////        }
//
//        imageRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        currentWorkOffer?.images?.let { images ->
//            val imageAdapter = ImageAdapter(images) // Assumes ImageAdapter handles image URLs
//            imageRecyclerView.adapter = imageAdapter
//        }
//
//        // Populate text views
//        titleTextView.text = currentWorkOffer.title ?: "Untitled"
//        descriptionTextView.text = currentWorkOffer.description ?: "No description"
//        dateTextView.text = "Date: ${currentWorkOffer.date ?: "No date"}"
//        createdAtTextView.text = "Created at: ${currentWorkOffer.createdAt ?: "No date available"}"
//
//        // Handle Accept Button
//        acceptButton.isEnabled = currentWorkOffer.acceptedBy == null // Enable only if not already accepted
//        acceptButton.text = if (currentWorkOffer.acceptedBy == null) "Accept Work" else "Accepted"
//        acceptButton.setOnClickListener {
//            acceptWork(currentWorkOffer, acceptButton)
//        }
//
//        return view
//    }
//
//    /**
//     * Handles the acceptance of a work offer.
//     */
//    private fun acceptWork(workOffer: WorkOffer, acceptButton: Button) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//        db.collection("workOffers")
//            .document(workOffer.id)
//            .update(
//                mapOf(
//                    "acceptedBy" to userId,
//                    "status" to "accepted"
//                )
//            )
//            .addOnSuccessListener {
//                // Update UI on successful acceptance
//                (context as? Activity)?.runOnUiThread {
//                    acceptButton.isEnabled = false
//                    acceptButton.text = "Accepted"
//                    Toast.makeText(context, "Work accepted successfully!", Toast.LENGTH_SHORT).show()
//
//                    // Now send a notification to the boss
//                    sendNotificationToBoss(workOffer)
//                }
//            }
//            .addOnFailureListener { exception ->
//                // Display an error message
//                (context as? Activity)?.runOnUiThread {
//                    Toast.makeText(context, "Error accepting work: ${exception.message}", Toast.LENGTH_SHORT).show()
//                }
//            }
//    }
//
//    /**
//     * Sends a notification to the boss when the work is accepted.
//     */
//    private fun sendNotificationToBoss(workOffer: WorkOffer) {
//        // Get the boss's FCM token from Firestore
//        db.collection("workOffers")
//            .document(workOffer.id)
//            .get()
//            .addOnSuccessListener { document ->
//                val fcmToken = document.getString("fcmToken") // Assuming the FCM token is stored in the 'fcmToken' field
//
//                if (fcmToken != null) {
//                    // Create a notification payload
//                    val notification = mapOf(
//                        "to" to fcmToken,
//                        "notification" to mapOf(
//                            "title" to "Work Accepted",
//                            "body" to "Your work offer has been accepted by a worker."
//                        )
//                    )
//
//                    // Send the notification (using FCM)
//                    sendFCMNotification(notification)
//                }
//            }
//    }
//
//    /**
//     * Sends the notification via Firebase Cloud Messaging (FCM).
//     */
////    private fun sendFCMNotification(notification: Map<String, Any>) {
////        // FCM Server URL and API Key for authorization
////        val fcmUrl = "https://fcm.googleapis.com/fcm/send"
////        val apiKey = "AIzaSyCEGbbOF-ZAs3wVmdUhRonGJGJ78oqH4Us" // Replace with your actual Firebase Server Key
////
////        val json = JSONObject(notification)
////
////        val request = JsonObjectRequest(Request.Method.POST, fcmUrl, json,
////            Response.Listener { response ->
////                Log.d("FCM", "Notification sent successfully: $response")
////            },
////            Response.ErrorListener { error ->
////                Log.e("FCM", "Failed to send notification: ${error.message}")
////            }) {
////            @Throws(AuthFailureError::class)
////            override fun getHeaders(): MutableMap<String, String> {
////                val headers = mutableMapOf<String, String>()
////                headers["Authorization"] = "key=$apiKey"
////                return headers
////            }
////        }
////
////        // Add the request to the queue
////        Volley.newRequestQueue(context).add(request)
////    }
//    private fun sendFCMNotification(notification: Map<String, Any>) {
//        // FCM Server URL and API Key for authorization
//        val fcmUrl = "https://fcm.googleapis.com/fcm/send"
//        val apiKey = "actual Firebase Server Key" // Replace with your actual Firebase Server Key
//
//        val json = JSONObject(notification)
//
//        // Create the JSON Object Request for sending the FCM notification
//        val request = object : JsonObjectRequest(
//            Method.POST, // HTTP method (POST)
//            fcmUrl, // URL
//            json, // JSON object as body
//            Response.Listener { response ->
//                Log.d("FCM", "Notification sent successfully: $response")
//            },
//            Response.ErrorListener { error ->
//                Log.e("FCM", "Failed to send notification: ${error.message}")
//            }) {
//
//            // Set headers, including the authorization key
//            @Throws(AuthFailureError::class)
//            override fun getHeaders(): MutableMap<String, String> {
//                val headers = mutableMapOf<String, String>()
//                headers["Authorization"] = "key=$apiKey"
//                return headers
//            }
//        }
//
//        // Add the request to the Volley request queue
//        Volley.newRequestQueue(context).add(request)
//    }
//
//}




//val apiKey = "AIzaSyCEGbbOF-ZAs3wVmdUhRonGJGJ78oqH4Us"



//class WorkOfferAdapter(
//    private val context: Context,
//    private val workOffers: MutableList<WorkOffer>,  // Keep mutable for dynamic changes
//    private val db: FirebaseFirestore                // Firestore instance
//) : ArrayAdapter<WorkOffer>(context, 0, workOffers) {
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//        // Get the current work offer
//        val currentWorkOffer = getItem(position)
//
//        // Inflate the view if not already done
//        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.work_offer_item, parent, false)
//
//        // Bind views
//        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
//        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
//        val dateTextView = view.findViewById<TextView>(R.id.dateTextView)
//        val createdAtTextView = view.findViewById<TextView>(R.id.createdAtTextView)
//        val acceptButton = view.findViewById<Button>(R.id.acceptButton)
//        val imageRecyclerView: RecyclerView = view.findViewById(R.id.recyclerViewImages)
//
//        // Set up the RecyclerView for images
//        imageRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        currentWorkOffer?.images?.let { images ->
//            val imageAdapter = ImageAdapter(images) // Assumes ImageAdapter handles image URLs
//            imageRecyclerView.adapter = imageAdapter
//        }
//
//        // Populate text views
//        titleTextView.text = currentWorkOffer?.title ?: "Untitled"
//        descriptionTextView.text = currentWorkOffer?.description ?: "No description"
//        dateTextView.text = "Date: ${currentWorkOffer?.date ?: "No date"}"
//
//        // Format and display creation date
//        createdAtTextView.text = "Created at: ${currentWorkOffer?.createdAt ?: "No date available"}"
//
//        // Handle Accept Button
//        acceptButton.isEnabled = currentWorkOffer?.acceptedBy == null // Enable only if not already accepted
//        acceptButton.text = if (currentWorkOffer?.acceptedBy == null) "Accept Work" else "Accepted"
//        acceptButton.setOnClickListener {
//            currentWorkOffer?.let { workOffer ->
//                acceptWork(workOffer, acceptButton)
//            }
//        }
//
//        return view
//    }
//
//    /**
//     * Handles the acceptance of a work offer.
//     */
//    private fun acceptWork(workOffer: WorkOffer, acceptButton: Button) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//        db.collection("workOffers")
//            .document(workOffer.id)
//            .update(
//                mapOf(
//                    "acceptedBy" to userId,
//                    "status" to "accepted"
//                )
//            )
//            .addOnSuccessListener {
//                // Update UI on successful acceptance
//                acceptButton.isEnabled = false
//                acceptButton.text = "Accepted"
//                Toast.makeText(context, "Work accepted successfully!", Toast.LENGTH_SHORT).show()
//
//                // Now send a notification to the boss
//                sendNotificationToBoss(workOffer)
//            }
//            .addOnFailureListener { exception ->
//                // Display an error message
//                Toast.makeText(context, "Error accepting work: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    /**
//     * Sends a notification to the boss when the work is accepted.
//     */
//    private fun sendNotificationToBoss(workOffer: WorkOffer) {
//        // Get the boss's FCM token from Firestore
//        db.collection("workOffers")
//            .document(workOffer.id)
//            .get()
//            .addOnSuccessListener { document ->
//                val fcmToken = document.getString("fcmToken") // Assuming the FCM token is stored in the 'fcmToken' field
//
//                if (fcmToken != null) {
//                    // Create a notification payload
//                    val notification = mapOf(
//                        "to" to fcmToken,
//                        "notification" to mapOf(
//                            "title" to "Work Accepted",
//                            "body" to "Your work offer has been accepted by a worker."
//                        )
//                    )
//
//                    // Send the notification (using FCM)
//                    sendFCMNotification(notification)
//                }
//            }
//    }
//
//    /**
//     * Sends the notification via Firebase Cloud Messaging (FCM).
//     */
//    private fun sendFCMNotification(notification: Map<String, Any>) {
//        // FCM Server URL and API Key for authorization
//        val fcmUrl = "https://fcm.googleapis.com/fcm/send"
//        val apiKey = "YOUR_SERVER_KEY" // Replace with your actual Firebase Server Key
//
//        val json = JSONObject(notification)
//
//        val request = JsonObjectRequest(Request.Method.POST, fcmUrl, json,
//            Response.Listener { response ->
//                Log.d("FCM", "Notification sent successfully: $response")
//            },
//            Response.ErrorListener { error ->
//                Log.e("FCM", "Failed to send notification: ${error.message}")
//            }) {
//            @Throws(AuthFailureError::class)
//            override fun getHeaders(): MutableMap<String, String> {
//                val headers = mutableMapOf<String, String>()
//                headers["Authorization"] = "key=$apiKey"
//                return headers
//            }
//        }
//
//        // Add the request to the queue
//        Volley.newRequestQueue(context).add(request)
//    }
//}























































//notification update

//class WorkOfferAdapter(
//    private val context: Context,
//    private val workOffers: MutableList<WorkOffer>,  // Keep mutable for dynamic changes
//    private val db: FirebaseFirestore                // Firestore instance
//) : ArrayAdapter<WorkOffer>(context, 0, workOffers) {
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//        // Get the current work offer
//        val currentWorkOffer = getItem(position)
//
//        // Inflate the view if not already done
//        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.work_offer_item, parent, false)
//
//        // Bind views
//        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
//        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
//        val dateTextView = view.findViewById<TextView>(R.id.dateTextView)
//        val createdAtTextView = view.findViewById<TextView>(R.id.createdAtTextView)
//        val acceptButton = view.findViewById<Button>(R.id.acceptButton)
//        val imageRecyclerView: RecyclerView = view.findViewById(R.id.recyclerViewImages)
//
//        // Set up the RecyclerView for images
//        imageRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        currentWorkOffer?.images?.let { images ->
//            val imageAdapter = ImageAdapter(images) // Assumes ImageAdapter handles image URLs
//            imageRecyclerView.adapter = imageAdapter
//        }
//
//        // Populate text views
//        titleTextView.text = currentWorkOffer?.title ?: "Untitled"
//        descriptionTextView.text = currentWorkOffer?.description ?: "No description"
//        dateTextView.text = "Date: ${currentWorkOffer?.date ?: "No date"}"
//
//        // Format and display creation date
//        createdAtTextView.text = "Created at: ${currentWorkOffer?.createdAt ?: "No date available"}"
//
//        // Handle Accept Button
//        acceptButton.isEnabled = currentWorkOffer?.acceptedBy == null // Enable only if not already accepted
//        acceptButton.text = if (currentWorkOffer?.acceptedBy == null) "Accept Work" else "Accepted"
//        acceptButton.setOnClickListener {
//            currentWorkOffer?.let { workOffer ->
//                acceptWork(workOffer, acceptButton)
//            }
//        }
//
//        return view
//    }
//
//    /**
//     * Handles the acceptance of a work offer.
//     */
//    private fun acceptWork(workOffer: WorkOffer, acceptButton: Button) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//        db.collection("workOffers")
//            .document(workOffer.id)
//            .update(
//                mapOf(
//                    "acceptedBy" to userId,
//                    "status" to "accepted"
//                )
//            )
//            .addOnSuccessListener {
//                // Update UI on successful acceptance
//                acceptButton.isEnabled = false
//                acceptButton.text = "Accepted"
//                Toast.makeText(context, "Work accepted successfully!", Toast.LENGTH_SHORT).show()
//            }
//            .addOnFailureListener { exception ->
//                // Display an error message
//                Toast.makeText(context, "Error accepting work: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//}




























//class WorkOfferAdapter(
//    private val context: Context,
//    private val workOffers: MutableList<WorkOffer>,  // Keep it mutable for deletion
//    private val db: FirebaseFirestore                // Pass Firestore instance
//) : ArrayAdapter<WorkOffer>(context, 0, workOffers) {
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//        val currentWorkOffer = getItem(position)
//
//        // Inflate the custom view if it doesn't already exist
//        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.work_offer_item, parent, false)
//
//        // Bind data to views
//        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
//        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
//        val dateTextView = view.findViewById<TextView>(R.id.dateTextView)
//        val createdAtTextView = view.findViewById<TextView>(R.id.createdAtTextView)
//        val acceptButton = view.findViewById<Button>(R.id.acceptButton)
//        val imageRecyclerView: RecyclerView = view.findViewById(R.id.recyclerViewImages)
//
//        // Set up RecyclerView for displaying images
//        imageRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        currentWorkOffer?.imageUrls?.let { imageUrls ->
//            val imageAdapter = ImageAdapter(imageUrls) // Assuming ImageAdapter is defined to handle image URLs.
//            imageRecyclerView.adapter = imageAdapter
//        }
//
//        // Set text for each field with safe calls to avoid NullPointerException
//        titleTextView.text = currentWorkOffer?.title ?: "Untitled"
//        descriptionTextView.text = currentWorkOffer?.description ?: "No description"
//        dateTextView.text = "Date: ${currentWorkOffer?.date ?: "No date"}"
//
//        // Format and set createdAt if it's not null
//        createdAtTextView.text = "Created at: ${currentWorkOffer?.createdAt ?: "No date available"}"
//
//
//        acceptButton.setOnClickListener {
//            currentWorkOffer?.let {
//                acceptWork(it)
//            }
//        }
//
//
//        // Handle Delete Button Click
////        deleteButton.setOnClickListener {
////            currentWorkOffer?.id?.let { workOfferId ->
////                // Remove the work offer from Firestore
////                db.collection("workOffers").document(workOfferId).delete()
////                    .addOnSuccessListener {
////                        // Remove from local list and update adapter
////                        workOffers.remove(currentWorkOffer)
////                        notifyDataSetChanged()
////                        Toast.makeText(context, "Work offer deleted", Toast.LENGTH_SHORT).show()
////                    }
////                    .addOnFailureListener {
////                        Toast.makeText(context, "Failed to delete work offer", Toast.LENGTH_SHORT).show()
////                    }
////            }
////        }
//
//        return view
//    }
//
//    fun acceptWork(workOffer: WorkOffer) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//        val db = FirebaseFirestore.getInstance()
//        db.collection("workOffers")
//            .document(workOffer.id)
//            .update("acceptedBy", userId, "status", "accepted")
//            .addOnSuccessListener {
//                Toast.makeText(context, "Work accepted successfully!", Toast.LENGTH_SHORT).show()
//                // Optionally refresh the UI or redirect
//            }
//            .addOnFailureListener { exception ->
//                Toast.makeText(context, "Error accepting work: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//    }


//    private fun acceptWork(workOffer: WorkOffer) {
//        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
//        if (currentUserId == null) {
//            Toast.makeText(context, "You must be logged in to accept work", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        db.collection("workOffers").document(workOffer.id)
//            .update("acceptedBy", currentUserId)
//            .addOnSuccessListener {
//
//                Toast.makeText(context, "Work accepted successfully!", Toast.LENGTH_SHORT).show()
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(context, "Failed to accept work: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//}






//class WorkOfferAdapter(
//    private val context: Context,
//    private val workOffers: MutableList<WorkOffer>,  // Change this to mutable for deletion
//    private val db: FirebaseFirestore                // Pass Firestore instance
//) : ArrayAdapter<WorkOffer>(context, 0, workOffers) {
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//        val currentWorkOffer = getItem(position)
//
//        // Inflate the custom view if it doesn't already exist
//        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.work_offer_item, parent, false)
//
//        // Bind data to views
//        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
//        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
//        val dateTextView = view.findViewById<TextView>(R.id.dateTextView)
//        val createdAtTextView = view.findViewById<TextView>(R.id.createdAtTextView)
//        val deleteButton = view.findViewById<Button>(R.id.deleteButton)
//        val imageRecyclerView: RecyclerView = view.findViewById(R.id.recyclerViewImages)
//
//        imageRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        val imageAdapter = currentWorkOffer?.imageUrls?.let { ImageAdapter(it) }
//        imageRecyclerView.adapter = imageAdapter
//
//
//        // Set text for each field
//        titleTextView.text = currentWorkOffer?.title
//        descriptionTextView.text = currentWorkOffer?.description
//        dateTextView.text = "Date: ${currentWorkOffer?.date}"
//
//        // Format and set createdAt if it's not null
////        currentWorkOffer?.createdAt?.let {
////            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
////            createdAtTextView.text = "Created at: ${sdf.format(it.toDate())}"
////        }
//
//        createdAtTextView.text = "Created at: ${currentWorkOffer?.createdAt}"
//
//        // Handle Delete Button Click
//        deleteButton.setOnClickListener {
//            // Remove the work offer from Firestore
//            db.collection("workOffers").document(currentWorkOffer?.id!!).delete()
//                .addOnSuccessListener {
//                    // Remove from local list and update adapter
//                    workOffers.remove(currentWorkOffer)
//                    notifyDataSetChanged()
//                    Toast.makeText(context, "Work offer deleted", Toast.LENGTH_SHORT).show()
//                }
//                .addOnFailureListener {
//                    Toast.makeText(context, "Failed to delete work offer", Toast.LENGTH_SHORT).show()
//                }
//        }
//
//        return view
//    }
//}



