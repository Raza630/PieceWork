package com.example.workman

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workman.adaptes.WorkOfferAdapter
import com.example.workman.dataClass.WorkOffer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class BossWorkListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var workRecyclerView: RecyclerView

    private lateinit var workOfferAdapter: WorkOfferAdapter 
    private val workOffers = mutableListOf<WorkOffer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boss_work_list)

        db = FirebaseFirestore.getInstance()
        workRecyclerView = findViewById(R.id.workRecyclerView)

        // Set up RecyclerView
        workRecyclerView.layoutManager = LinearLayoutManager(this)
        workOfferAdapter = WorkOfferAdapter(this, workOffers, db)
        workRecyclerView.adapter = workOfferAdapter

        loadBossWorkOffers()
    }

    private fun loadBossWorkOffers() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return

        db.collection("workOffers")
            .whereEqualTo("createdBy", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Failed to load work offers", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val newList = mutableListOf<WorkOffer>()
                snapshots?.forEach { document ->
                    val title = document.getString("title") ?: "Untitled"
                    val description = document.getString("description") ?: "No description"
                    val date = document.getString("date") ?: "No date"
                    val createdAt = document.getTimestamp("createdAt")?.toDate()
                    val acceptedBy = document.getString("acceptedBy")
                    val isAccepted = document.getBoolean("isAccepted") ?: false

                    val workOffer = WorkOffer(
                        title = title,
                        description = description,
                        date = date,
                        createdAt = createdAt?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it) } ?: "No date available",
                        images = document.get("images") as? List<String> ?: emptyList(),
                        id = document.id,
                        acceptedBy = acceptedBy,
                        isAccepted = isAccepted
                    )
                    newList.add(workOffer)
                }
                workOfferAdapter.updateList(newList)
            }
    }
}
