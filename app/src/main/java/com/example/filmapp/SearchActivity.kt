package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.widget.SearchView
import androidx.lifecycle.ViewModel
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class SearchActivity : AppCompatActivity() {

    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var searchAdapter: MovieAdapter
    private lateinit var searchView: SearchView
    private lateinit var viewModel: SearchViewModel
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Inicijalizacija RecyclerView-a
        searchRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        searchRecyclerView.layoutManager = LinearLayoutManager(this)

        searchAdapter = MovieAdapter(emptyList()) { movie -> //dodana lambda kada se stisne na film, otvori se novi review activity
            val intent = Intent(this, ReviewActivity::class.java).apply {
                putExtra("MOVIE", movie)
            }
            startActivity(intent)
        }// Koristimo isti adapter kao za Home

        searchRecyclerView.adapter = searchAdapter

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_reviews -> {
                    startActivity(Intent(this, ReviewsListActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_search -> {
                    true
                }
                else -> false
            }
        }

        // Inicijalizacija ViewModel-a
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)

        // Pretplata na promjene rezultata pretrage
        viewModel.searchResults.observe(this) { movies ->
            searchAdapter.updateMovies(movies)
        }

        // Postavljanje SearchView-a
        searchView = findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.searchMovies(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.length > 4) {
                    viewModel.searchMovies(newText)
                }
                return true
            }
        })
    }
    override fun onResume() {
        super.onResume()
        // Osigurajte da je Home označen kad se vratite na ovaj Activity
        bottomNavigationView.selectedItemId = R.id.navigation_search
    }
}