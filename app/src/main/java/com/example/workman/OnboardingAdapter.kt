package com.example.workman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class OnboardingPage(
    val imageRes: Int,
    val title: String,
    val description: String,
    val permissionType: PermissionType = PermissionType.NONE,
    val permissionButtonText: String = ""
)

enum class PermissionType {
    NONE, LOCATION, NOTIFICATION
}

class OnboardingAdapter(
    private val pages: List<OnboardingPage>,
    private val onPermissionClick: (PermissionType, Int) -> Unit
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    // Track granted permissions to update UI
    private val grantedPermissions = mutableSetOf<PermissionType>()

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgOnboarding: ImageView = view.findViewById(R.id.imgOnboarding)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val btnPermission: TextView = view.findViewById(R.id.btnPermission)
        val tvPermissionStatus: TextView = view.findViewById(R.id.tvPermissionStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val page = pages[position]
        holder.imgOnboarding.setImageResource(page.imageRes)
        holder.tvTitle.text = page.title
        holder.tvDescription.text = page.description

        if (page.permissionType != PermissionType.NONE) {
            val isGranted = grantedPermissions.contains(page.permissionType)

            if (isGranted) {
                holder.btnPermission.visibility = View.GONE
                holder.tvPermissionStatus.visibility = View.VISIBLE
                holder.tvPermissionStatus.text = "✓ Permission Granted"
            } else {
                holder.btnPermission.visibility = View.VISIBLE
                holder.btnPermission.text = page.permissionButtonText
                holder.tvPermissionStatus.visibility = View.GONE

                holder.btnPermission.setOnClickListener {
                    onPermissionClick(page.permissionType, position)
                }
            }
        } else {
            holder.btnPermission.visibility = View.GONE
            holder.tvPermissionStatus.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = pages.size

    fun markPermissionGranted(type: PermissionType) {
        grantedPermissions.add(type)
        val position = pages.indexOfFirst { it.permissionType == type }
        if (position != -1) notifyItemChanged(position)
    }
}

