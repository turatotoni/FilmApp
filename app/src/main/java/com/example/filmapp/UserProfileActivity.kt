package com.example.filmapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

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
                button.text = if (document.exists()) "Unfollow" else "Follow"
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
            } else {
                // Follow
                followersRef.set(mapOf("timestamp" to FieldValue.serverTimestamp()))
                button.text = "Unfollow"
                updateFollowerCount(targetUserId, 1)
                updateFollowingCount(currentUserId, 1)
            }
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