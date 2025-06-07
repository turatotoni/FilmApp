package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //provjerava je li netko vec ulogiran ako je vraca nas na homeactivity
        //komentiram ovo zasad jer mi stalno otvara HomeActivity
//        if (FirebaseAuth.getInstance().currentUser != null) {
//            startActivity(Intent(this, HomeActivity::class.java))
//            finish()
//        }
        setContentView(R.layout.activity_main)
        //FirebaseApp.initializeApp(this) ovo sam iso probat ako ne bude radilo dodavanje u manifest al je radilo

        //connected variables to buttons on xml
        val loginBtn = findViewById<Button>(R.id.loginButton)
        val registerBtn = findViewById<Button>(R.id.registerButton)

        loginBtn.setOnClickListener {
            //make a new intent and from MainActivity to other activities given in the intent
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        registerBtn.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}