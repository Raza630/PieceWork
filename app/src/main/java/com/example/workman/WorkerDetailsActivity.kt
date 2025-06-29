package com.example.workman

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workman.adaptes.PreviousWorkAdaper
import com.example.workman.adaptes.WorkerAdapter
import com.example.workman.dataClass.Workers

class WorkerDetailsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var workers: List<Workers>
    private lateinit var previousWorkAdaper: PreviousWorkAdaper

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_details)
        val periviosworkrecyclerView: RecyclerView = findViewById(R.id.periviosworkrecyclerView)


        workers = listOf(
            Workers("John Doe", R.drawable.ic_icon__profile, "Professionals",2.0f),
            Workers("Jane Smith", R.drawable.ic_icon__profile, "Associate Professionals",4.5f),
            Workers("Mike Johnson", R.drawable.ic_icon__profile, "Clerks",5.0f),
            Workers("John Doe", R.drawable.ic_icon__profile, "Professionals",3.5f),
            Workers("Jane Smith", R.drawable.ic_icon__profile, "Associate Professionals",4.5f),
            Workers("Mike Johnson", R.drawable.ic_icon__profile, "Clerks",3.5f),
            Workers("John Doe", R.drawable.ic_icon__profile, "Professionals",2.5f),
            Workers("Jane Smith", R.drawable.ic_icon__profile, "Associate Professionals",3.5f),
            Workers("Mike Johnson", R.drawable.ic_icon__profile, "Clerks",4.1f),
            Workers("John Doe", R.drawable.ic_icon__profile, "Professionals",1.5f),
            Workers("Jane Smith", R.drawable.ic_icon__profile, "Associate Professionals",4.5f),
            Workers("Mike Johnson", R.drawable.ic_icon__profile, "Clerks",3.5f),
            Workers("John Doe", R.drawable.ic_icon__profile, "Professionals",3.3f),
            Workers("Jane Smith", R.drawable.ic_icon__profile, "Associate Professionals",4.5f),
            Workers("Mike Johnson", R.drawable.ic_icon__profile, "Clerks",2.1f),
            Workers("John Doe", R.drawable.ic_icon__profile, "Professionals",2.3f),
            Workers("Jane Smith", R.drawable.ic_icon__profile, "Associate Professionals",2.4f),
            Workers("Mike Johnson", R.drawable.ic_icon__profile, "Clerks",5.0f)
            // Add more workers as needed
        )


        previousWorkAdaper = PreviousWorkAdaper(workers) {
            val intent = Intent(this, WorkerDetailsActivity::class.java)

            startActivity(intent)
        }

        periviosworkrecyclerView.layoutManager = LinearLayoutManager(this)
        periviosworkrecyclerView.adapter = previousWorkAdaper

    }
}