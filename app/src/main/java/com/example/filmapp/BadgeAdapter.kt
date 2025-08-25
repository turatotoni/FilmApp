package com.example.filmapp

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import com.bumptech.glide.Glide

class BadgeAdapter(private val badges: List<Badge>) :
    RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    class BadgeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.badge_image)
        val titleView: TextView = view.findViewById(R.id.badge_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]

        Glide.with(holder.itemView.context)
            .load(badge.imageResId)
            .into(holder.imageView)

        holder.titleView.text = badge.title
    }

    override fun getItemCount() = badges.size
}