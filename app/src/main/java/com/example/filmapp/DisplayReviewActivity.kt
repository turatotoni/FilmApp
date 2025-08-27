// DisplayReviewActivity.kt
package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DisplayReviewActivity : AppCompatActivity() {
    private lateinit var moviePoster: ImageView
    private lateinit var movieTitle: TextView
    private lateinit var ratingText: TextView
    private lateinit var reviewText: TextView
    private lateinit var likeButton: ImageButton
    private lateinit var dislikeButton: ImageButton
    private lateinit var likeCount: TextView
    private lateinit var dislikeCount: TextView
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var usernameText: TextView

    private lateinit var review: Review
    private lateinit var user: User
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_review)

        // Initialize views
        moviePoster = findViewById(R.id.moviePoster)
        movieTitle = findViewById(R.id.movieTitle)
        ratingText = findViewById(R.id.ratingText)
        reviewText = findViewById(R.id.reviewText)
        likeButton = findViewById(R.id.likeButton)
        dislikeButton = findViewById(R.id.dislikeButton)
        likeCount = findViewById(R.id.likeCount)
        dislikeCount = findViewById(R.id.dislikeCount)
        usernameText = findViewById(R.id.usernameText)

        reviewRepository = ReviewRepository()

        review = intent.getParcelableExtra("review") ?: run {
            Toast.makeText(this, "Review not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        user = intent.getParcelableExtra("user") ?: User()

        setupViews()
        setupLikeDislikeButtons()


        lifecycleScope.launch {
            try {
                refreshReviewData()
            } catch (e: Exception) {
                Log.e("DisplayReviewActivity", "Error loading review data", e)
            }
        }
    }

    private fun setupViews() {
        movieTitle.text = review.movieTitle
        ratingText.text = "⭐ ${review.rating.toInt()}/10"
        reviewText.text = review.reviewText

        if (user.username.isNotEmpty()) {
            usernameText.text = user.username
        } else {
            fetchUser()
        }

        review.moviePosterPath?.let { path ->
            Glide.with(this)
                .load("https://image.tmdb.org/t/p/w500$path")
                .apply(RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                )
                .into(moviePoster)
        }

        updateLikeDislikeCounts()
    }

    private fun fetchUser() {
        firestore.collection("users").document(review.userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    user = document.toObject(User::class.java) ?: User()
                    usernameText.text = user.username
                } else {
                    usernameText.text = "Unknown User"
                }
            }
            .addOnFailureListener { e ->
                Log.e("DisplayReviewActivity", "Error fetching user", e)
                usernameText.text = "Unknown User"
            }
    }



    private fun setupLikeDislikeButtons() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Disable buttons if it's the user's own review
        if (review.userId == currentUserId) {
            likeButton.isEnabled = false
            dislikeButton.isEnabled = false

            return
        }

        likeButton.setOnClickListener {
            toggleLike(currentUserId)
        }

        dislikeButton.setOnClickListener {
            toggleDislike(currentUserId)
        }
    }

    private fun toggleLike(userId: String) {
        lifecycleScope.launch {
            try {
                val currentLikes = review.likes.toMutableList()
                val currentDislikes = review.dislikes.toMutableList()

                val wasLiked = currentLikes.contains(userId)

                if (wasLiked) {
                    currentLikes.remove(userId)
                } else {
                    currentLikes.add(userId)
                    currentDislikes.remove(userId)
                }

                val documentId = getReviewDocumentId()
                reviewRepository.updateReviewLikesDislikes(
                    reviewId = documentId,
                    likes = currentLikes,
                    dislikes = currentDislikes
                )

                review = review.copy(likes = currentLikes, dislikes = currentDislikes)
                updateLikeDislikeCounts()

                if (!wasLiked) {
                    sendLikeNotification(userId, review.userId, review.movieTitle)
                    checkLikeBadges(review.userId, currentLikes.size)
                }
            } catch (e: Exception) {
                Log.e("DisplayReviewActivity", "Error toggling like", e)
                Toast.makeText(this@DisplayReviewActivity,
                    "Failed to update like: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleDislike(userId: String) {
        lifecycleScope.launch {
            try {
                val currentLikes = review.likes.toMutableList()
                val currentDislikes = review.dislikes.toMutableList()

                val wasDisliked = currentDislikes.contains(userId)

                if (wasDisliked) {
                    currentDislikes.remove(userId)
                } else {
                    currentDislikes.add(userId)
                    currentLikes.remove(userId)
                }

                val documentId = getReviewDocumentId()
                reviewRepository.updateReviewLikesDislikes(
                    reviewId = documentId,
                    likes = currentLikes,
                    dislikes = currentDislikes
                )

                review = review.copy(likes = currentLikes, dislikes = currentDislikes)
                updateLikeDislikeCounts()

                if (!wasDisliked) {
                    sendDislikeNotification(userId, review.userId, review.movieTitle)
                }
            } catch (e: Exception) {
                Log.e("DisplayReviewActivity", "Error toggling dislike", e)
                Toast.makeText(this@DisplayReviewActivity,
                    "Failed to update dislike: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendLikeNotification(likerId: String, reviewOwnerId: String, movieTitle: String) {
        if (likerId == reviewOwnerId) return

        // Get liker's username first
        firestore.collection("users").document(likerId).get()
            .addOnSuccessListener { document ->
                val likerName = document.getString("username") ?: "Someone"

                val notificationData = hashMapOf(
                    "type" to "like", // or "dislike"
                    "senderId" to likerId,
                    "receiverId" to reviewOwnerId,
                    "movieTitle" to movieTitle,
                    "username" to likerName,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "read" to false,
                    "sent" to false,
                    //"sendOnLogin" to true // Mark for sending on login
                )

                firestore.collection("notifications")
                    .add(notificationData)
                    .addOnFailureListener { e ->
                        Log.e("Notifications", "Error creating like notification", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Notifications", "Error getting liker info", e)
            }
    }

    private fun sendDislikeNotification(dislikerId: String, reviewOwnerId: String, movieTitle: String) {
        if (dislikerId == reviewOwnerId) return

        // Get disliker's username first
        firestore.collection("users").document(dislikerId).get()
            .addOnSuccessListener { document ->
                val dislikerName = document.getString("username") ?: "Someone"

                val notificationData = hashMapOf(
                    "type" to "dislike", // or "dislike"
                    "senderId" to dislikerId,
                    "receiverId" to reviewOwnerId,
                    "movieTitle" to movieTitle,
                    "username" to dislikerName,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "read" to false,
                    "sent" to false,
                    //"sendOnLogin" to true // Mark for sending on login
                )

                firestore.collection("notifications")
                    .add(notificationData)
                    .addOnFailureListener { e ->
                        Log.e("Notifications", "Error creating dislike notification", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Notifications", "Error getting disliker info", e)
            }
    }

    private suspend fun checkLikeBadges(userId: String, likeCount: Int) {
        try {
            val userRef = firestore.collection("users").document(userId)
            val updates = mutableMapOf<String, Any>()

            // Check if the review has enough likes for each badge
            if (likeCount >= 5) updates["has5Likes"] = true
            if (likeCount >= 10) updates["has10Likes"] = true

            // Update the user document if any badges are earned
            if (updates.isNotEmpty()) {
                userRef.update(updates).await()
                Log.d("LikeBadges", "Updated like badges for user $userId with $likeCount likes")
            }
        } catch (e: Exception) {
            Log.e("LikeBadges", "Failed to update like badges: ${e.message}")
        }
    }

    private suspend fun getReviewDocumentId(): String {
        return try {
            val query = firestore.collection("reviews")
                .whereEqualTo("userId", review.userId)
                .whereEqualTo("movieId", review.movieId)
                .whereEqualTo("timestamp", review.timestamp)
                .limit(1)
                .get()
                .await()

            query.documents.firstOrNull()?.id
                ?: throw Exception("Review document not found")
        } catch (e: Exception) {
            Log.e("DisplayReviewActivity", "Error getting review document ID", e)
            throw e
        }
    }

    private fun updateLikeDislikeCounts() {
        likeCount.text = review.likes.size.toString()
        dislikeCount.text = review.dislikes.size.toString()

        val currentUserId = auth.currentUser?.uid ?: return
        likeButton.isSelected = review.likes.contains(currentUserId)
        dislikeButton.isSelected = review.dislikes.contains(currentUserId)
    }


    private suspend fun refreshReviewData() {
        val documentId = getReviewDocumentId()
        val updatedReview = firestore.collection("reviews")
            .document(documentId)
            .get()
            .await()
            .toObject(Review::class.java)

        updatedReview?.let {
            review = it
            runOnUiThread {
                setupViews()
                setupLikeDislikeButtons()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                refreshReviewData()
            } catch (e: Exception) {
                Log.e("DisplayReviewActivity", "Error refreshing review data", e)
            }
        }
    }


}