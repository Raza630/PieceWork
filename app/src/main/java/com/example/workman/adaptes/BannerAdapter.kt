package com.example.workman.adaptes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.workman.R
import com.example.workman.dataClass.Banner
import com.example.workman.databinding.ItemWorkerOfferBinding

class BannerAdapter(private val banners: List<Banner>) :
    RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    class BannerViewHolder(private val binding: ItemWorkerOfferBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(banner: Banner) {
            binding.bannerTitle.text = banner.title
            binding.bannerdescription.text = banner.description
            binding.banneraveragePay.text = banner.averagePay
            // Load image with Glide
            Glide.with(binding.bannerImage.context)
                .load(banner.imageUrl)
                .placeholder(R.drawable.ic_illustration) // Your placeholder image
                .into(binding.bannerImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemWorkerOfferBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position])
    }

    override fun getItemCount(): Int = banners.size
}