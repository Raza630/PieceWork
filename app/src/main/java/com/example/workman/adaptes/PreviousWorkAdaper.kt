package com.example.workman.adaptes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.workman.CustomStarRatingBar
import com.example.workman.R
import com.example.workman.dataClass.Workers


class PreviousWorkAdaper(private var workers: List<Workers>, private val onItemClickListener: (Workers) -> Unit) :
    RecyclerView.Adapter<PreviousWorkAdaper.WorkerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_worker, parent, false)
        return WorkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        val worker = workers[position]
        holder.workerName.text = worker.name
        holder.workerImage.setImageResource(worker.imageResId)
        holder.starRatingBar.setRating(worker.starRating) // Use the starRating property
        holder.ratingValueTextView.text = worker.starRating.toString() // Set the rating value

        holder.itemView.setOnClickListener{
            onItemClickListener(worker)
        }
    }

    override fun getItemCount(): Int = workers.size

    fun updateList(newWorkers: List<Workers>) {
        workers = newWorkers
        notifyDataSetChanged()
    }

    class WorkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val workerImage: ImageView = itemView.findViewById(R.id.workerImage)
        val workerName: TextView = itemView.findViewById(R.id.workerName)
        val starRatingBar: CustomStarRatingBar = itemView.findViewById(R.id.starRatingBar)
        val ratingValueTextView: TextView = itemView.findViewById(R.id.ratingCountTextView)

    }
}