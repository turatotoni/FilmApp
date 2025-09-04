package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class FollowListActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var emptyTextView: TextView

    private var isFollowersList = false
    private val TAG = "FollowListActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow_list)

        Log.d(TAG, "onCreate: Activity started")

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.follow_recycler_view)
        titleTextView = findViewById(R.id.title_text_view)
        emptyTextView = findViewById(R.id.empty_text_view)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Get the type from intent (followers or following)
        isFollowersList = intent.getBooleanExtra("IS_FOLLOWERS", false)
        Log.d(TAG, "isFollowersList: $isFollowersList")

        if (isFollowersList) {
            titleTextView.text = "Followers"
            loadFollowers()
        } else {
            titleTextView.text = "Following"
            loadFollowing()
        }
    }

    private fun loadFollowers() {
        val currentUserId = auth.currentUser?.uid
        Log.d(TAG, "loadFollowers: currentUserId = $currentUserId")

        if (currentUserId == null) {
            Log.e(TAG, "loadFollowers: No current user ID")
            showEmptyState("You are not followed by anyone")
            return
        }

        Log.d(TAG, "loadFollowers: Querying followers/$currentUserId/userFollowers")


        firestore.collection("followers")
            .document(currentUserId)
            .collection("userFollowers")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "loadFollowers: Success! Found ${documents.size()} documents")
                Log.d(TAG, "loadFollowers: Document IDs: ${documents.documents.map { it.id }}")
                Log.d(TAG, "loadFollowers: Document data: ${documents.documents.map { it.data }}")

                if (documents.isEmpty) {
                    Log.d(TAG, "loadFollowers: No followers found")
                    showEmptyState("You are not followed by anyone")
                } else {

                    val followerIds = documents.documents.map { it.id }
                    Log.d(TAG, "loadFollowers: Follower IDs extracted: $followerIds")

                    if (followerIds.isEmpty()) {
                        Log.d(TAG, "loadFollowers: No follower IDs extracted")
                        showEmptyState("You are not followed by anyone")
                    } else {
                        loadUsersData(followerIds, true)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "loadFollowers: Error getting followers", exception)
                showEmptyState("Failed to load followers")
            }
    }

    private fun loadFollowing() {
        val currentUserId = auth.currentUser?.uid
        Log.d(TAG, "loadFollowing: currentUserId = $currentUserId")

        if (currentUserId == null) {
            Log.e(TAG, "loadFollowing: No current user ID")
            showEmptyState("You are not following anyone")
            return
        }

        Log.d(TAG, "loadFollowing: Finding all users that current user follows")


        firestore.collection("users")
            .get()
            .addOnSuccessListener { allUsers ->
                Log.d(TAG, "loadFollowing: Found ${allUsers.size()} total users")

                if (allUsers.isEmpty()) {
                    Log.d(TAG, "loadFollowing: No users found")
                    showEmptyState("You are not following anyone")
                    return@addOnSuccessListener
                }

                val followingIds = mutableListOf<String>()
                var processedCount = 0

                Log.d(TAG, "loadFollowing: Checking ${allUsers.size()} users for follow status")

                for (userDoc in allUsers.documents) {
                    val targetUserId = userDoc.id


                    if (targetUserId != currentUserId) {
                        Log.d(TAG, "loadFollowing: Checking if follows $targetUserId")


                        firestore.collection("followers")
                            .document(targetUserId)
                            .collection("userFollowers")
                            .document(currentUserId)
                            .get()
                            .addOnSuccessListener { followDoc ->
                                processedCount++

                                Log.d(TAG, "loadFollowing: Checked $targetUserId, exists: ${followDoc.exists()}, processed: $processedCount/${allUsers.size()}")

                                if (followDoc.exists()) {
                                    followingIds.add(targetUserId)
                                    Log.d(TAG, "loadFollowing: Added $targetUserId to following list")
                                }

                                // If we've processed all users, load the data
                                if (processedCount == allUsers.size()) {
                                    Log.d(TAG, "loadFollowing: Finished processing all users. Found ${followingIds.size} following")

                                    if (followingIds.isEmpty()) {
                                        showEmptyState("You are not following anyone")
                                    } else {
                                        loadUsersData(followingIds, false)
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                processedCount++
                                Log.e(TAG, "loadFollowing: Error checking follow status for $targetUserId", exception)

                                if (processedCount == allUsers.size()) {
                                    Log.d(TAG, "loadFollowing: Finished processing with errors. Found ${followingIds.size} following")

                                    if (followingIds.isEmpty()) {
                                        showEmptyState("You are not following anyone")
                                    } else {
                                        loadUsersData(followingIds, false)
                                    }
                                }
                            }
                    } else {
                        processedCount++
                        Log.d(TAG, "loadFollowing: Skipped self ($targetUserId), processed: $processedCount/${allUsers.size()}")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "loadFollowing: Error getting all users", exception)
                showEmptyState("Failed to load following")
            }
    }


    private fun loadUsersData(userIds: List<String>, isFollowers: Boolean) {
        Log.d(TAG, "loadUsersData: Loading data for ${userIds.size} user IDs: $userIds")
        Log.d(TAG, "loadUsersData: Is followers list: $isFollowers")

        if (userIds.isEmpty()) {
            Log.d(TAG, "loadUsersData: No user IDs to load")
            if (isFollowers) {
                showEmptyState("You are not followed by anyone")
            } else {
                showEmptyState("You are not following anyone")
            }
            return
        }


        val batches = userIds.chunked(10)
        Log.d(TAG, "loadUsersData: Split into ${batches.size} batches")

        val allUsers = mutableListOf<User>()
        var completedBatches = 0

        for ((index, batch) in batches.withIndex()) {
            Log.d(TAG, "loadUsersData: Processing batch $index with ${batch.size} IDs: $batch")


            firestore.collection("users")
                .whereIn(FieldPath.documentId(), batch)
                .get()
                .addOnSuccessListener { documents ->
                    completedBatches++
                    Log.d(TAG, "loadUsersData: Batch $index success! Found ${documents.size()} users")
                    Log.d(TAG, "loadUsersData: Batch document IDs: ${documents.documents.map { it.id }}")

                    val users = documents.documents.map { document ->
                        User(
                            id = document.id, // Use document ID as user ID
                            username = document.getString("username") ?: "",
                            email = document.getString("email") ?: "",
                            avatarID = document.getLong("avatarID")?.toInt() ?: R.drawable.ic_profile_placeholder,
                            followerCount = document.getLong("followerCount") ?: 0,
                            followingCount = document.getLong("followingCount") ?: 0,
                            reviewCount = document.getLong("reviewCount") ?: 0,
                            has5Likes = document.getBoolean("has5Likes") ?: false,
                            has10Likes = document.getBoolean("has10Likes") ?: false
                        )
                    }
                    allUsers.addAll(users)

                    Log.d(TAG, "loadUsersData: Batch $index completed. Total users so far: ${allUsers.size}")
                    Log.d(TAG, "loadUsersData: Completed batches: $completedBatches/${batches.size}")


                    if (completedBatches == batches.size) {
                        Log.d(TAG, "loadUsersData: All batches completed. Total users found: ${allUsers.size}")

                        if (allUsers.isEmpty()) {
                            Log.d(TAG, "loadUsersData: No users found in final result")
                            if (isFollowers) {
                                showEmptyState("You are not followed by anyone")
                            } else {
                                showEmptyState("You are not following anyone")
                            }
                        } else {
                            Log.d(TAG, "loadUsersData: Setting up adapter with ${allUsers.size} users")
                            recyclerView.adapter = UserAdapter(allUsers) { user ->
                                Log.d(TAG, "loadUsersData: User clicked: ${user.username} (${user.id})")
                                val intent = Intent(this, UserProfileActivity::class.java).apply {
                                    putExtra("USER_ID", user.id)
                                }
                                startActivity(intent)
                            }
                            recyclerView.visibility = View.VISIBLE
                            emptyTextView.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    completedBatches++
                    Log.e(TAG, "loadUsersData: Batch $index failed", exception)

                    Log.d(TAG, "loadUsersData: Batch $index failed. Completed batches: $completedBatches/${batches.size}")

                    if (completedBatches == batches.size) {
                        Log.d(TAG, "loadUsersData: All batches completed (some failed). Total users found: ${allUsers.size}")

                        if (allUsers.isEmpty()) {
                            if (isFollowers) {
                                showEmptyState("Failed to load followers")
                            } else {
                                showEmptyState("Failed to load following")
                            }
                        } else {
                            Log.d(TAG, "loadUsersData: Setting up adapter with ${allUsers.size} users (partial success)")
                            recyclerView.adapter = UserAdapter(allUsers) { user ->
                                val intent = Intent(this, UserProfileActivity::class.java).apply {
                                    putExtra("USER_ID", user.id)
                                }
                                startActivity(intent)
                            }
                            recyclerView.visibility = View.VISIBLE
                            emptyTextView.visibility = View.GONE
                        }
                    }
                }
        }
    }

    private fun showEmptyState(message: String) {
        Log.d(TAG, "showEmptyState: $message")
        recyclerView.visibility = View.GONE
        emptyTextView.visibility = View.VISIBLE
        emptyTextView.text = message
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Activity destroyed")
    }
}