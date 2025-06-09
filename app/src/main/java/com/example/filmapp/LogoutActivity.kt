package com.example.filmapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class LogoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logout)

        val btnBackToMain = findViewById<Button>(R.id.btnBackToMain)

        btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK //brisemo sve aktivnosti
            //prethodno otvorene, tako da korisnik ne moze samo s back ici nazad na logout activity
            startActivity(intent)

            finish()
        }
    }
}