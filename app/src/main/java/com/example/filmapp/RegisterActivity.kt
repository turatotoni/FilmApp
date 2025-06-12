package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener //ovo je kao break za petlje znaci izlazi totalno iz setOnClickLIstenera bez nastavljanja registracije
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Provjeri jedinstvenost username-a
            firestore.collection("users") //dohvaca popis usera
                .whereEqualTo("username", username) //trazi ako imamo dupli username
                .get() //dohvaca rezultate pretrage
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) { //ako nemamo dupli username
                        // Registracija
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    user?.let { firebaseUser -> //null provjera
                                        val userId = firebaseUser.uid
                                        val userData = hashMapOf( //mapiramo sve vrijednosti
                                            "username" to username,
                                            "email" to email,
                                            "avatarID" to R.drawable.ic_profile_placeholder,
                                            "followerCount" to 0,
                                            "followingCount" to 0
                                        )

                                        firestore.collection("users") //napravimo usera u firestore-u
                                            .document(userId)
                                            .set(userData)
                                            .addOnSuccessListener {

                                                val batch = firestore.batch() //radimo batch da vise operacija izvedemo odjednom
                                                //ako jedna zapne sve su zapele

                                                // Create followers subcollection
                                                val followersRef = firestore
                                                    .collection("followers")
                                                    .document(userId)
                                                    .collection("userFollowers")
                                                    .document("init")
                                                batch.set(followersRef, mapOf<String, Any>(
                                                    "createdAt" to FieldValue.serverTimestamp()
                                                ))

                                                // Create following subcollection
                                                val followingRef = firestore
                                                    .collection("following")
                                                    .document(userId)
                                                    .collection("userFollowing")
                                                    .document("init")
                                                batch.set(followingRef, mapOf<String, Any>(
                                                    "createdAt" to FieldValue.serverTimestamp()
                                                ))

                                                batch.commit()
                                                    .addOnSuccessListener {
                                                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                                        startActivity(Intent(this, HomeActivity::class.java))
                                                        finish()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(this, "Failed to initialize follow collections: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    } ?: run {
                                        Toast.makeText(this, "User creation failed", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error checking username: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}