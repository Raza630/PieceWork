package com.example.workman

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.workman.dataClass.WorkOffer
import com.example.workman.databinding.ActivityWorkerDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class WorkerDashboardActivity : AppCompatActivity() {

    private lateinit var workListView: ListView // ListView to display available work offers
    private lateinit var workListAdapter: ArrayAdapter<String> // Adapter for the ListView
    private val workOffers = mutableListOf<String>() // List to hold work offers

    private lateinit var binding: ActivityWorkerDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWorkerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        setContentView(R.layout.activity_worker_dashboard)

        // Ensure workListView1 exists in the layout
        workListView = findViewById(R.id.workListView1)

        // Initialize the adapter
        workListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, workOffers)
        workListView.adapter = workListAdapter

        // Load work offers from Firestore
        loadWorkOffers()

        // Set click listener for accepting a work offer
        workListView.setOnItemClickListener { _, _, position, _ ->
            val selectedOffer = workOffers[position]
            acceptWorkOffer(selectedOffer)
        }
    }

    private fun loadWorkOffers() {
        // Fetch work offers from Firestore
        val db = FirebaseFirestore.getInstance()
        db.collection("workOffers").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // Assuming 'description' is a field in your Firestore documents
                    val workOffer = document.getString("description")
                    workOffer?.let { workOffers.add(it) }
                }
                workListAdapter.notifyDataSetChanged() // Update the ListView
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error getting work offers: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun acceptWorkOffer(offer: String) {
        // Logic to accept the selected work offer
        // You can save this acceptance in Firestore
        val db = FirebaseFirestore.getInstance()
        val workAcceptanceData = hashMapOf(
            "acceptedBy" to "Worker's ID or Name", // Replace this with the actual worker's ID or name
            "offer" to offer,
            "isAccepted" to true
        )

        db.collection("acceptedWorkOffers").add(workAcceptanceData)
            .addOnSuccessListener {
                Toast.makeText(this, "Accepted offer: $offer", Toast.LENGTH_SHORT).show()
                // Optional: Remove the offer from the list or update it in Firestore
                workOffers.remove(offer)
                workListAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error accepting offer: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

}




//class WorkerDashboardActivity : AppCompatActivity() {
//
//    private lateinit var workListView: ListView // ListView to display available work offers
//    private lateinit var workListAdapter: ArrayAdapter<String> // Adapter for the ListView
//    private val workOffers = mutableListOf<String>() // List to hold work offers
//
//    @SuppressLint("MissingInflatedId")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_worker_dashboard)
//
//        workListView = findViewById(R.id.workListView1)
//
//        // Initialize the adapter
//        workListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, workOffers)
//        workListView.adapter = workListAdapter
//
//        // Load work offers from Firestore
//        loadWorkOffers()
//
//        // Set click listener for accepting a work offer
//        workListView.setOnItemClickListener { _, _, position, _ ->
//            val selectedOffer = workOffers[position]
//            acceptWorkOffer(selectedOffer)
//        }
//    }
//
//    private fun loadWorkOffers() {
//        // Fetch work offers from Firestore
//        val db = FirebaseFirestore.getInstance()
//        db.collection("workOffers").get()
//            .addOnSuccessListener { documents ->
//                for (document in documents) {
//                    val workOffer = document.getString("description") // Adjust based on your Firestore structure
//                    workOffer?.let { workOffers.add(it) }
//                }
//                workListAdapter.notifyDataSetChanged() // Update the ListView
//            }
//            .addOnFailureListener { exception ->
//                Toast.makeText(this, "Error getting work offers: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    private fun acceptWorkOffer(offer: String) {
//        // Logic to accept the selected work offer
//        // You can save this acceptance in Firestore
//        val db = FirebaseFirestore.getInstance()
//        val workAcceptanceData = hashMapOf(
//            "acceptedBy" to "Worker's ID or Name", // You can replace this with the actual worker's ID or name
//            "offer" to offer
//        )
//
//        db.collection("acceptedWorkOffers").add(workAcceptanceData)
//            .addOnSuccessListener {
//                Toast.makeText(this, "Accepted offer: $offer", Toast.LENGTH_SHORT).show()
//                // Optional: Remove the offer from the list or update it in Firestore
//                workOffers.remove(offer)
//                workListAdapter.notifyDataSetChanged()
//            }
//            .addOnFailureListener { exception ->
//                Toast.makeText(this, "Error accepting offer: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//}




//class WorkerDashboardActivity : AppCompatActivity() {
//
//    private lateinit var workListView: ListView // ListView to display available work offers
//    private lateinit var workListAdapter: ArrayAdapter<String> // Adapter for the ListView
//    private val workOffers = mutableListOf<String>() // List to hold work offers
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_worker_dashboard)
//
//        workListView = findViewById(R.id.workListView)
//
//        // Initialize the adapter
//        workListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, workOffers)
//        workListView.adapter = workListAdapter
//
//        // Load work offers from Firestore
//        loadWorkOffers()
//
//        // Set click listener for accepting a work offer
//        workListView.setOnItemClickListener { _, _, position, _ ->
//            val selectedOffer = workOffers[position]
//            acceptWorkOffer(selectedOffer)
//        }
//    }
//
//    private fun loadWorkOffers() {
//        // Fetch work offers from Firestore
//        val db = FirebaseFirestore.getInstance()
//        db.collection("workOffers").get()
//            .addOnSuccessListener { documents ->
//                for (document in documents) {
//                    val workOffer = document.getString("description") // Adjust based on your Firestore structure
//                    workOffer?.let { workOffers.add(it) }
//                }
//                workListAdapter.notifyDataSetChanged() // Update the ListView
//            }
//            .addOnFailureListener { exception ->
//                Toast.makeText(this, "Error getting work offers: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    private fun acceptWorkOffer(offer: String) {
//        // Logic to accept the selected work offer
//        // You can also save this acceptance in Firestore or show a confirmation dialog
//        Toast.makeText(this, "Accepted offer: $offer", Toast.LENGTH_SHORT).show()
//
//        // Optional: Remove the offer from the list or update it in Firestore
//        // workOffers.remove(offer)
//        // workListAdapter.notifyDataSetChanged()
//    }
//}
