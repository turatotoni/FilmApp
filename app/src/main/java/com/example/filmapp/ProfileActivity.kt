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
            openGalleryForImage()
            //Toast.makeText(this, "Change photo clicked", Toast.LENGTH_SHORT).show()
        }
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LogoutActivity::class.java)) //napravi ipak da te baci na logoutactivity a ne odma na login i registraciju
            finishAffinity()
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImageUri = result.data?.data
            selectedImageUri?.let {
                // Obrada odabrane slike
                Glide.with(this)
                    .load(it)
                    .circleCrop()
                    .into(profileImage)
                //Moras stavit sliku na Firestore
            }
            Toast.makeText(this, "Uspješno ste promijenili sliku profila!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "SLika se ije prom", Toast.LENGTH_SHORT).show()
        }
    }


    private fun loadUserData() {
        val currentUser = auth.currentUser //dohvacamo trenutnog usera
        currentUser?.let { user -> //?.let provjerava ako je user null; ako nije nastavlja se kod

            emailText.text = user.email

            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val username = document.getString("username") ?: "No username" //?: ako nije null uzmi lijevu vrijednost, inace desnu
                        usernameText.text = username
                    } else {
                        usernameText.text = user.displayName ?: "User" //imamo i ovaj else dio u slucaju da Firestore dokument uopce ne postoji
                    }
                }
                .addOnFailureListener {
                    usernameText.text = user.displayName ?: "User"
                }
        }
    }

    override fun onResume() { //svaki put kad se vratim na ovaj activity opet osvjezi podatke od korisnika
        super.onResume()
        bottomNavigationView.selectedItemId = R.id.navigation_profile
        loadUserData()
    }
}