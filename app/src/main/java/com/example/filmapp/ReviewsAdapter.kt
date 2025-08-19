package com.example.filmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReviewsAdapter(
    private var reviews: List<Review>,
    private val onItemClick: (Review) -> Unit
) : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usernameCache = mutableMapOf<String, String>() // Cache for usernames

    class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val movieTitle: TextView = view.findViewById(R.id.reviewMovieTitle)
        val moviePoster: ImageView = view.findViewById(R.id.reviewMoviePoster)
        val ratingText: TextView = view.findViewById(R.id.reviewRatingText)
        val reviewText: TextView = view.findViewById(R.id.reviewText)
        val username: TextView = view.findViewById(R.id.reviewUsername)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        val currentUserId = auth.currentUser?.uid ?: ""

        Glide.with(holder.itemView.context).clear(holder.moviePoster)

        holder.movieTitle.text = review.movieTitle
        holder.reviewText.text = review.reviewText
        holder.ratingText.text = "⭐ ${review.rating.toInt()}/10"

        // Handle username display
        if (review.userId == currentUserId) {
            // Hide username for current user's reviews
            holder.username.visibility = View.GONE
        } else {
            // Show username for other users' reviews
            holder.username.visibility = View.VISIBLE
            if (usernameCache.containsKey(review.userId)) {
                holder.username.text = usernameCache[review.userId]
            } else {
                holder.username.text = "Loading..." // Placeholder while loading
                fetchUsername(review.userId, holder)
            }
        }

        review.moviePosterPath?.let { path ->
            Glide.with(holder.itemView.context)
                .load("https://image.tmdb.org/t/p/w500$path")
                .apply(RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                )
                .into(holder.moviePoster)
        }

        holder.itemView.setOnClickListener {
            onItemClick(review)
        }
    }

    private fun fetchUsername(userId: String, holder: ReviewViewHolder) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val username = document.getString("username") ?: "Unknown User"

                // Cache the username for future use
                usernameCache[userId] = username

                // Update the TextView if this holder is still showing the same user
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION &&
                    currentPosition < reviews.size &&
                    reviews[currentPosition].userId == userId) {
                    holder.username.text = username
                }
            }
            .addOnFailureListener {
                // Only set "Unknown User" if this is still the correct item
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION &&
                    currentPosition < reviews.size &&
                    reviews[currentPosition].userId == userId) {
                    holder.username.text = "Unknown User"
                }
                usernameCache[userId] = "Unknown User"
            }
    }

    override fun onViewRecycled(holder: ReviewViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.itemView.context).clear(holder.moviePoster)
    }

    fun updateReviews(newReviews: List<Review>) {
        this.reviews = newReviews
        notifyDataSetChanged()
    }

    override fun getItemCount() = reviews.size
}