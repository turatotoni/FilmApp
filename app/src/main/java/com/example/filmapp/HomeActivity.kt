package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Provjera prijave
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return  // Prekini izvršavanje
        }

        setContentView(R.layout.activity_home)

        // Inicijalizacija RecyclerView-a
        val recyclerView = findViewById<RecyclerView>(R.id.moviesRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            setHasFixedSize(true)  // Optimizacija ako su svi redovi iste veličine
        }

        // ViewModel i promatranje podataka
        val viewModel = ViewModelProvider(this).get(MoviesViewModel::class.java)
        viewModel.movies.observe(this) { movies ->
            recyclerView.adapter = MovieAdapter(movies)
        }

        viewModel.fetchPopularMovies()
        Log.d("API_KEY", BuildConfig.TMDB_API_KEY)

    }

}
