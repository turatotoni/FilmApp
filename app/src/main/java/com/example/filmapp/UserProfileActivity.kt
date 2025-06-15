package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var reviewsAdapter: ReviewsAdapter
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var reviewsSectionTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        reviewsRecyclerView = findViewById(R.id.reviews_recycler_view)
        reviewsSectionTitle = findViewById(R.id.reviews_section_title)

        // Setup RecyclerView
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)
        reviewsAdapter = ReviewsAdapter(emptyList()) { review ->
            val intent = Intent(this, DisplayReviewActivity::class.java).apply {
                putExtra("review", review)
            }
            startActivity(intent)
        }
        reviewsRecyclerView.adapter = reviewsAdapter

        val userId = intent.getStringExtra("USER_ID")
        val currentUserId = auth.currentUser?.uid

        if (userId != null) {
            loadUserProfile(userId)

            val followButton = findViewById<Button>(R.id.follow_button)

            if (userId == currentUserId) {
                followButton.visibility = View.GONE
            } else {
                checkFollowingStatus(currentUserId, userId, followButton)

                followButton.setOnClickListener {
                    toggleFollowStatus(currentUserId, userId, followButton)
                }
            }
        }
    }

    private fun loadUserProfile(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Load user data
                    val username = document.getString("username") ?: ""
                    val avatarID = document.getLong("avatarID")?.toInt() ?: R.drawable.ic_profile_placeholder
                    val followerCount = document.getLong("followerCount") ?: 0
                    val followingCount = document.getLong("followingCount") ?: 0

                    // Update UI
                    findViewById<TextView>(R.id.user_name).text = username
                    findViewById<TextView>(R.id.followers_count).text = followerCount.toString()
                    findViewById<TextView>(R.id.following_count).text = followingCount.toString()

                    // Load avatar (using Glide or similar library)
                    Glide.with(this)
                        .load(avatarID)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(findViewById(R.id.user_avatar_profile))
                }
            }
    }


    private fun checkFollowingStatus(currentUserId: String?, targetUserId: String, button: Button) { //provjeravamo da li vec pratimo ovog user-a
        if (currentUserId == null) return

        firestore.collection("followers")
            .document(targetUserId)
            .collection("userFollowers")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                val isFollowing = document.exists()
                button.text = if (document.exists()) "Unfollow" else "Follow"

                if (isFollowing) {
                    loadUserReviews(targetUserId)
                } else {
                    hideReviews()
                }
            }
    }

    private fun toggleFollowStatus(currentUserId: String?, targetUserId: String, button: Button) {
        if (currentUserId == null) return

        val followersRef = firestore.collection("followers")
            .document(targetUserId)
            .collection("userFollowers")
            .document(currentUserId)

        followersRef.get().addOnSuccessListener { document ->
            if (document.exists()) { //ako ga vec imamo u dokumentu znaci da ga pratimo
                // Unfollow
                followersRef.delete()
                button.text = "Follow"
                updateFollowerCount(targetUserId, -1)
                updateFollowingCount(currentUserId, -1)
                hideReviews()
            } else {
                // Follow
                followersRef.set(mapOf("timestamp" to FieldValue.serverTimestamp()))
                button.text = "Unfollow"
                updateFollowerCount(targetUserId, 1)
                updateFollowingCount(currentUserId, 1)
                loadUserReviews(targetUserId)
                sendFollowNotification(currentUserId, targetUserId)
            }
        }

    }

    private fun loadUserReviews(userId: String) { //prikaz reviewa na profilu
        CoroutineScope(Dispatchers.IO).launch { //rad u pozadini
            try {
                val reviews = firestore.collection("reviews") //trazenje u firestore svih reviewa s ovim userIDem
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                    .toObjects(Review::class.java)//pretvorba u objekt

                withContext(Dispatchers.Main) { //kada su reviewi spremni za prikazat vraca se u main thread
                    if (reviews.isNotEmpty()) {
                        reviewsAdapter.updateReviews(reviews)
                        reviewsSectionTitle.visibility = View.VISIBLE
                        reviewsRecyclerView.visibility = View.VISIBLE
                    } else {
                        reviewsSectionTitle.visibility = View.VISIBLE
                        reviewsSectionTitle.text = R.string.no_reviews_yet.toString()
                        reviewsRecyclerView.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    reviewsSectionTitle.visibility = View.VISIBLE
                    reviewsSectionTitle.text = R.string.error_loading_reviews.toString()
                    reviewsRecyclerView.visibility = View.GONE
                }
            }
        }
    }

    private fun sendFollowNotification(followerId: String, targetUserId: String) {
        // Get follower's username first
        firestore.collection("users").document(followerId).get()
            .addOnSuccessListener { document ->
                val followerName = document.getString("username") ?: "Someone"

                // Create notification document in Firestore
                val notificationData = hashMapOf(
                    "type" to "follow",
                    "senderId" to followerId,
                    "receiverId" to targetUserId,
                    "username" to followerName, // Store username to avoid extra lookups
                    "timestamp" to FieldValue.serverTimestamp(),
                    "read" to false
                )

                firestore.collection("notifications")
                    .add(notificationData)
                    .addOnFailureListener { e ->
                        Log.e("Notifications", "Error creating follow notification", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Notifications", "Error getting follower info", e)
            }
    }

    private fun hideReviews() {
        reviewsSectionTitle.visibility = View.GONE
        reviewsRecyclerView.visibility = View.GONE
    }

    private fun updateFollowerCount(userId: String, change: Int) {
        firestore.collection("users").document(userId)
            .update("followerCount", FieldValue.increment(change.toLong()))
    }

    private fun updateFollowingCount(userId: String, change: Int) {
        firestore.collection("users").document(userId)
            .update("followingCount", FieldValue.increment(change.toLong()))
    }
}