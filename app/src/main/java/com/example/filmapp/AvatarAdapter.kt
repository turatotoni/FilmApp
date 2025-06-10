package com.example.filmapp

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater

class AvatarAdapter(private val avatars: List<Int>, private val onAvatarSelected: (Int) -> Unit) :
    RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    class AvatarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.avatarImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_avatar, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        holder.imageView.setImageResource(avatars[position])
        holder.itemView.setOnClickListener {
            onAvatarSelected(avatars[position])
        }
    }

    override fun getItemCount() = avatars.size
}