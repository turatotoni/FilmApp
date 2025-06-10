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
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var profileImage: ImageView
    private lateinit var changePhotoText: TextView
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var logoutButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        profileImage = findViewById(R.id.profile_image)
        changePhotoText = findViewById(R.id.change_photo_text)
        usernameText = findViewById(R.id.profile_username)
        emailText = findViewById(R.id.profile_email)
        logoutButton = findViewById(R.id.logout_button)


        bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_profile
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
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
            //Toast.makeText(this, "Change photo clicked", Toast.LENGTH_SHORT).show()
        }
        logoutButton.setOnClickListener {
            auth.signOut()
            //trebalo bi samo napraviti da kad se log autas odma ides na log aut a ne da prelazi mainactivity i slicno
            val intent = Intent(this, LogoutActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            overridePendingTransition(0, 0) // Bez animacija
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
            // Spremi samo referencu na avatar (npr. "avatar_3")
            val avatarName = resources.getResourceEntryName(avatarResId)
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .update("avatar", avatarName)
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
                        usernameText.text = username

                        // Dohvati ime avatara iz Firestore-a (npr. "avatar3")
                        val avatarName = document.getString("avatar")
                        if (!avatarName.isNullOrEmpty()) {
                            // Pretvori ime avatara u resource ID
                            val avatarResId = resources.getIdentifier(
                                avatarName,
                                "drawable",
                                packageName
                            )

                            if (avatarResId != 0) {
                                Glide.with(this)
                                    .load(avatarResId)
                                    .circleCrop()
                                    .into(profileImage)
                            }
                        }
                    } else {
                        usernameText.text = user.displayName ?: "User"
                    }
                }
                .addOnFailureListener {
                    usernameText.text = user.displayName ?: "User"
                    Toast.makeText(this, "CRASHHH", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onResume() { //svaki put kad se vratim na ovaj activity opet osvjezi podatke od korisnika
        super.onResume()
        bottomNavigationView.selectedItemId = R.id.navigation_profile
        loadUserData()
    }
}