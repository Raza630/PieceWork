package com.example.workman.adaptes

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.workman.BuildConfig
import com.example.workman.R
import com.example.workman.dataClass.WorkOffer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.Charset

class WorkOfferAdapter(
    private val context: Context,
    private var workOffers: List<WorkOffer>,
    private val db: FirebaseFirestore
) : RecyclerView.Adapter<WorkOfferAdapter.WorkOfferViewHolder>() {

    class WorkOfferViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.titleTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.descriptionTextView)
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val createdAtTextView: TextView = view.findViewById(R.id.createdAtTextView)
        val acceptButton: Button = view.findViewById(R.id.acceptButton)
        val imageRecyclerView: RecyclerView = view.findViewById(R.id.recyclerViewImages)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkOfferViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.work_offer_item, parent, false)
        return WorkOfferViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkOfferViewHolder, position: Int) {
        val currentWorkOffer = workOffers[position]

        holder.titleTextView.text = currentWorkOffer.title ?: "Untitled"
        holder.descriptionTextView.text = currentWorkOffer.description ?: "No description"
        holder.dateTextView.text = "Date: ${currentWorkOffer.date ?: "No date"}"
        holder.createdAtTextView.text = "Created at: ${currentWorkOffer.createdAt ?: "No date available"}"

        holder.imageRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        currentWorkOffer.images?.let { images ->
            holder.imageRecyclerView.adapter = ImageAdapter(images)
        }

        holder.acceptButton.isEnabled = currentWorkOffer.acceptedBy == null
        holder.acceptButton.text = if (currentWorkOffer.acceptedBy == null) "Accept Work" else "Accepted"
        holder.acceptButton.setOnClickListener {
            acceptWork(currentWorkOffer, holder.acceptButton)
        }
    }

    override fun getItemCount(): Int = workOffers.size

    fun updateList(newList: List<WorkOffer>) {
        workOffers = newList
        notifyDataSetChanged()
    }

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
                (context as? Activity)?.runOnUiThread {
                    acceptButton.isEnabled = false
                    acceptButton.text = "Accepted"
                    Toast.makeText(context, "Work accepted successfully!", Toast.LENGTH_SHORT).show()
                    sendNotificationToBoss(workOffer, userId)
                }
            }
            .addOnFailureListener { exception ->
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Error accepting work: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendNotificationToBoss(workOffer: WorkOffer, workerId: String) {
        val bossId = "vYnWdsuRWNPPfjUNj0DUlKzG7nK2" 

        db.collection("users").document(bossId).get()
            .addOnSuccessListener { document ->
                if (document?.exists() == true) {
                    val bossToken = document.getString("fcmToken")
                    if (bossToken != null) {
                        val notificationData = mapOf(
                            "title" to "Work Accepted",
                            "message" to "A worker has accepted the work '${workOffer.title}'."
                        )
                        sendFCMNotification(bossToken, notificationData)
                    }
                }
            }
    }

    private fun sendFCMNotification(token: String, data: Map<String, String>) {
        val jsonBody = JSONObject()
        val apiKey = BuildConfig.FCM_API_KEY
        try {
            jsonBody.put("to", token)
            jsonBody.put("data", JSONObject(data))
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val requestQueue = Volley.newRequestQueue(context)
        val stringRequest = object : StringRequest(Method.POST, "https://fcm.googleapis.com/fcm/send",
            Response.Listener { Log.d("FCM", "Success: $it") },
            Response.ErrorListener { Log.e("FCM", "Error: ${it.message}") }) {
            override fun getHeaders(): Map<String, String> = mapOf(
                "Authorization" to "key=$apiKey",
                "Content-Type" to "application/json"
            )
            override fun getBody(): ByteArray = jsonBody.toString().toByteArray(Charset.defaultCharset())
        }
        requestQueue.add(stringRequest)
    }
}
