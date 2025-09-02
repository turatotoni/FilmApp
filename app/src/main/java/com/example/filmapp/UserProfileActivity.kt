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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private lateinit var top3Adapter: Top3MovieAdapter
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var top3RecyclerView: RecyclerView
    private lateinit var reviewsSectionTitle: TextView
    private lateinit var top3SectionTitle: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var top3Manager: Top3Manager
    private var userId: String? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        top3Manager = Top3Manager(this)

        // Initialize views
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        reviewsRecyclerView = findViewById(R.id.reviews_recycler_view)
        top3RecyclerView = findViewById(R.id.top3_recycler_view)
        reviewsSectionTitle = findViewById(R.id.reviews_section_title)
        top3SectionTitle = findViewById(R.id.top3_section_title)

        // Setup RecyclerViews
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)
        top3RecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Setup Adapters
        reviewsAdapter = ReviewsAdapter(emptyList()) { review ->
            val intent = Intent(this, DisplayReviewActivity::class.java).apply {
                putExtra("review", review)
            }
            startActivity(intent)
        }

        top3Adapter = Top3MovieAdapter(emptyList()) { movie ->
            val intent = Intent(this, MovieDetailsActivity::class.java).apply {
                putExtra("MOVIE", movie)
            }
            startActivity(intent)
        }

        reviewsRecyclerView.adapter = reviewsAdapter
        top3RecyclerView.adapter = top3Adapter

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        userId = intent.getStringExtra("USER_ID")
        currentUserId = auth.currentUser?.uid

        if (userId != null) {
            loadUserProfile(userId!!)

            val followButton = findViewById<Button>(R.id.follow_button)

            if (userId == currentUserId) {
                followButton.visibility = View.GONE
                // If viewing own profile, show everything
                loadUserReviews(userId!!)
                loadTop3Movies(userId!!)
            } else {
                checkFollowingStatus(currentUserId, userId!!, followButton)

                followButton.setOnClickListener {
                    toggleFollowStatus(currentUserId, userId!!, followButton)
                }
            }
        }
    }

    private fun refreshData() {
        userId?.let {
            loadUserProfile(it)
            currentUserId?.let { currentId ->
                findViewById<Button>(R.id.follow_button)?.let { button ->
                    checkFollowingStatus(currentId, it, button)
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

                    // Load avatar
                    Glide.with(this)
                        .load(avatarID)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(findViewById(R.id.user_avatar_profile))
                }
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener {
                swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun loadTop3Movies(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val top3Movies = top3Manager.getTop3ForUser(userId)

                withContext(Dispatchers.Main) {
                    if (top3Movies.isNotEmpty()) {
                        top3Adapter.updateMovies(top3Movies)
                        top3SectionTitle.visibility = View.VISIBLE
                        top3RecyclerView.visibility = View.VISIBLE
                    } else {
                        top3SectionTitle.visibility = View.GONE
                        top3RecyclerView.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    top3SectionTitle.visibility = View.GONE
                    top3RecyclerView.visibility = View.GONE
                }
            }
        }
    }

    private fun checkFollowingStatus(currentUserId: String?, targetUserId: String, button: Button) {
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
                    loadTop3Movies(targetUserId) // Load Top 3 when following
                } else {
                    hideReviews()
                    hideTop3() // Hide Top 3 when not following
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
            if (document.exists()) {
                // Unfollow
                followersRef.delete()
                button.text = "Follow"
                updateFollowerCount(targetUserId, -1)
                updateFollowingCount(currentUserId, -1)
                hideReviews()
                hideTop3() // Hide Top 3 when unfollowing
            } else {
                // Follow
                followersRef.set(mapOf("timestamp" to FieldValue.serverTimestamp()))
                button.text = "Unfollow"
                updateFollowerCount(targetUserId, 1)
                updateFollowingCount(currentUserId, 1)
                loadUserReviews(targetUserId)
                loadTop3Movies(targetUserId) // Load Top 3 when following
                sendFollowNotification(currentUserId, targetUserId)
            }
        }
    }

    private fun loadUserReviews(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reviews = firestore.collection("reviews")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                    .toObjects(Review::class.java)

                withContext(Dispatchers.Main) {
                    if (reviews.isNotEmpty()) {
                        reviewsAdapter.updateReviews(reviews)
                        reviewsSectionTitle.visibility = View.VISIBLE
                        reviewsSectionTitle.text = getString(R.string.reviews)
                        reviewsRecyclerView.visibility = View.VISIBLE
                    } else {
                        reviewsSectionTitle.visibility = View.VISIBLE
                        reviewsSectionTitle.text = getString(R.string.no_reviews_yet)
                        reviewsRecyclerView.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    reviewsSectionTitle.visibility = View.VISIBLE
                    reviewsSectionTitle.text = getString(R.string.error_loading_reviews)
                    reviewsRecyclerView.visibility = View.GONE
                }
            }
        }
    }

    private fun hideReviews() {
        reviewsSectionTitle.visibility = View.GONE
        reviewsRecyclerView.visibility = View.GONE
    }

    private fun hideTop3() {
        top3SectionTitle.visibility = View.GONE
        top3RecyclerView.visibility = View.GONE
    }

    private fun sendFollowNotification(followerId: String, targetUserId: String) {
        firestore.collection("users").document(followerId).get()
            .addOnSuccessListener { document ->
                val followerName = document.getString("username") ?: "Someone"

                val notificationData = hashMapOf(
                    "type" to "follow",
                    "senderId" to followerId,
                    "receiverId" to targetUserId,
                    "username" to followerName,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "read" to false,
                    "sent" to false
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

    private fun updateFollowerCount(userId: String, change: Int) {
        firestore.collection("users").document(userId)
            .update("followerCount", FieldValue.increment(change.toLong()))
    }

    private fun updateFollowingCount(userId: String, change: Int) {
        firestore.collection("users").document(userId)
            .update("followingCount", FieldValue.increment(change.toLong()))
    }
}