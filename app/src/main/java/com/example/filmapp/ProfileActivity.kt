package com.example.filmapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firestore.v1.StructuredAggregationQuery.Aggregation.Count

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var profileImage: ImageView
    private lateinit var changePhotoText: TextView
    private lateinit var followingCountText: TextView
    private lateinit var followersCountText: TextView
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var logoutButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var badgesRecyclerView: RecyclerView

    // Define your badges
    private val availableBadges = listOf(
        Badge(
            id = "follower_5",
            title = "Popular 5",
            imageResId = R.drawable.badge_popular1,
            condition = { userData ->
                (userData["followerCount"] as? Long ?: 0) >= 5
            }
        ),
        Badge(
            id = "follower_10",
            title = "Popular 10",
            imageResId = R.drawable.badge_popular2,
            condition = { userData ->
                (userData["followerCount"] as? Long ?: 0) >= 10
            }
        ),
        Badge(
            id = "follower_25",
            title = "Popular 25",
            imageResId = R.drawable.badge_popular3,
            condition = { userData ->
                (userData["followerCount"] as? Long ?: 0) >= 25
            }
        ),
        Badge(
            id = "following_5",
            title = "Follower 5",
            imageResId = R.drawable.badge_follower1,
            condition = { userData ->
                (userData["followingCount"] as? Long ?: 0) >= 5
            }
        ),
        Badge(
            id = "following_10",
            title = "Follower 10",
            imageResId = R.drawable.badge_follower2,
            condition = { userData ->
                (userData["followingCount"] as? Long ?: 0) >= 10
            }
        ),
        Badge(
            id = "following_25",
            title = "Follower 25",
            imageResId = R.drawable.badge_follower3,
            condition = { userData ->
                (userData["followingCount"] as? Long ?: 0) >= 25
            }
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        profileImage = findViewById(R.id.profile_image)
        changePhotoText = findViewById(R.id.change_photo_text)
        followingCountText = findViewById(R.id.following_count)
        followersCountText = findViewById(R.id.followers_count)
        usernameText = findViewById(R.id.profile_username)
        emailText = findViewById(R.id.profile_email)
        logoutButton = findViewById(R.id.logout_button)
        badgesRecyclerView = findViewById(R.id.badges_recycler_view)

        // Setup badges recycler view
        badgesRecyclerView.layoutManager = GridLayoutManager(this, 3)

        bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_profile
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_reviews -> {
                    startActivity(Intent(this, ReviewsListActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_profile -> {
                    true
                }
                else -> false
            }
        }

        loadUserData()

        changePhotoText.setOnClickListener {
            openAvatarPicker()
        }
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LogoutActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val avatarResId = result.data?.getIntExtra("avatarResId", -1)
            if (avatarResId != -1) {
                Glide.with(this)
                    .load(avatarResId)
                    .circleCrop()
                    .into(profileImage)
                if (avatarResId != null) {
                    uploadAvatarToFirebase(avatarResId)
                }
            }
        }
    }

    private fun openAvatarPicker() {
        val intent = Intent(this, AvatarPickerActivity::class.java)
        pickAvatarLauncher.launch(intent)
    }

    private fun uploadAvatarToFirebase(avatarResId: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .update("avatarID", avatarResId)
                .addOnSuccessListener {
                    Toast.makeText(this, "Picture updated", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser

        currentUser?.let { user ->
            emailText.text = user.email

            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val username = document.getString("username") ?: "No username"
                        val followerCount = document.getLong("followerCount") ?: 0
                        val followingCount = document.getLong("followingCount") ?: 0
                        val avatarID = document.getLong("avatarID")?.toInt() ?: R.drawable.ic_profile_placeholder
                        val reviewCount = document.getLong("reviewCount") ?: 0

                        usernameText.text = username
                        followingCountText.text = followingCount.toString()
                        followersCountText.text = followerCount.toString()

                        Glide.with(this)
                            .load(avatarID)
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .into(profileImage)

                        // Load user badges
                        loadUserBadges(document.data ?: emptyMap())
                    } else {
                        usernameText.text = user.displayName ?: "User"
                        loadUserBadges(emptyMap())
                    }
                }
                .addOnFailureListener {
                    usernameText.text = user.displayName ?: "User"
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    loadUserBadges(emptyMap())
                }
        }
    }

    private fun loadUserBadges(userData: Map<String, Any>) {
        val earnedBadges = availableBadges.filter { badge ->
            badge.condition(userData)
        }

        badgesRecyclerView.adapter = BadgeAdapter(earnedBadges)
    }

    override fun onResume() {
        super.onResume()
        bottomNavigationView.selectedItemId = R.id.navigation_profile
        loadUserData()
    }
}