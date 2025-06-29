package com.example.workman

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.workman.adaptes.WorkOfferAdapter
import com.example.workman.dataClass.WorkOffer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class BossWorkListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var workListView: ListView
    private lateinit var workOfferAdapter: WorkOfferAdapter // Or a dedicated adapter for listing created works
    private val workOffers = mutableListOf<WorkOffer>()


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boss_work_list) // Create this layout with a ListView (or RecyclerView)

        db = FirebaseFirestore.getInstance()
        workListView = findViewById(R.id.workListView)

        workOfferAdapter = WorkOfferAdapter(this, workOffers, db)
        workListView.adapter = workOfferAdapter

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

                workOffers.clear()
                snapshots?.forEach { document ->
                    val title = document.getString("title") ?: "Untitled"
                    val description = document.getString("description") ?: "No description"
                    val date = document.getString("date") ?: "No date"
                    val createdAt = document.getTimestamp("createdAt")?.toDate()
                    val acceptedBy = document.getString("acceptedBy")
                    val isAccepted = document.getBoolean("isAccepted") ?: false

                    val workOffer = WorkOffer(
                        title,
                        description,
                        date,
                        createdAt?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it) } ?: "No date available",
                        document.get("images") as? List<String> ?: emptyList(),
                        document.id,
                        acceptedBy,
                        isAccepted
                    )
                    workOffers.add(workOffer)
                }
                workOfferAdapter.notifyDataSetChanged()
            }
    }
}
