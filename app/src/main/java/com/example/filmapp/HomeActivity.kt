package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

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
            recyclerView.adapter = MovieAdapter(movies) { movie -> //dodana lambda kada se stisne na film, otvori se novi review activity
                val intent = Intent(this, ReviewActivity::class.java).apply {
                    putExtra("MOVIE", movie)
                }
                startActivity(intent)
            }
        }

        viewModel.fetchPopularMovies()

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_search-> {
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
                R.id.navigation_home -> {
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Osigurajte da je Home označen kad se vratite na ovaj Activity
        bottomNavigationView.selectedItemId = R.id.navigation_home
    }
}
