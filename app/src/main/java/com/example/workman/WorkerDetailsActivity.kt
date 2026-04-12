package com.example.workman

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.workman.databinding.ActivityWorkerDetailsBinding
import com.google.firebase.firestore.FirebaseFirestore

class WorkerDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWorkerDetailsBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val workerId = intent.getStringExtra("worker_id")
        if (workerId != null) {
            fetchWorkerDetails(workerId)
        } else {
            Toast.makeText(this, "Worker ID missing", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchWorkerDetails(workerId: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("users").document(workerId).get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                if (document.exists()) {
                    val name = document.getString("name") ?: "Unknown"
                    val email = document.getString("email") ?: "N/A"
                    val dob = document.getString("dob") ?: "N/A"
                    val gender = document.getString("gender") ?: "N/A"
                    val phone = document.getString("phone") ?: ""
                    val photoUrl = document.getString("photoUrl") ?: ""

                    // Update UI
                    binding.textViewShowWelcome.text = name
                    binding.textViewShowFullName.text = name
                    binding.textViewShowEmail.text = email
                    binding.textViewShowDob.text = dob
                    binding.textViewShowGender.text = gender
                    binding.textViewShowMobile.text = phone

                    if (photoUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_profile)
                            .into(binding.imageViewProfileDp)
                    }

                    setupContactActions(phone, workerId)
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupContactActions(phone: String, workerId: String) {
        // Chat within the app
        binding.iconChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("receiver_id", workerId)
            startActivity(intent)
        }

        // WhatsApp action
        binding.showWhatsappIcon.setOnClickListener {
            if (phone.isNotEmpty()) {
                val url = "https://api.whatsapp.com/send?phone=+91$phone"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
