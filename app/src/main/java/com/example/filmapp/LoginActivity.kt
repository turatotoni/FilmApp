package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {
    //Firebase variable which we will use to save emails in Firebase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //getInstance() is like getting the "master" key to all Firebase services
        auth = FirebaseAuth.getInstance()

        val loginBtn = findViewById<Button>(R.id.loginButton)
        val registerTextView = findViewById<TextView>(R.id.registerTextView)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)


        loginBtn.setOnClickListener {
            //converts the views we got by findViewById to strings
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)

                    .addOnCompleteListener(this) { task ->
                        //task: This is like a "report card" from Firebase
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                            // Update FCM token
                            updateFcmToken(userId)

                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity (intent)
                            //we use finish() when we dont expect to go back to this activity, like here we dont
                            //expect to go back to login after we already successfully logged in
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Authentication failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        //this is when you click on don't have an account.
        registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateFcmToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_DEBUG", "New token for $userId: $token")

                FirebaseFirestore.getInstance().collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM_DEBUG", "Token saved to Firestore for $userId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM_DEBUG", "Failed to save token", e)
                    }
            } else {
                Log.e("FCM_DEBUG", "FCM token fetch failed", task.exception)
            }
        }
    }
}