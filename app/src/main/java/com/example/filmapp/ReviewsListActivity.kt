package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReviewsListActivity : AppCompatActivity() {
    private lateinit var adapter: ReviewsAdapter
    private lateinit var bottomNavigationView: BottomNavigationView
    private val reviewRepository = ReviewRepository()
    private var loadingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reviews_list)

        val recyclerView = findViewById<RecyclerView>(R.id.reviewsRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this@ReviewsListActivity)
        adapter = ReviewsAdapter(emptyList())
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)

        loadReviews()

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_reviews
        bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_reviews -> {
                    true
                }
                else -> false
            }
        }
    }

    private fun loadReviews() {
        loadingJob = lifecycleScope.launch {
            try {
                val reviews = reviewRepository.getUserReviews()
                adapter.updateReviews(reviews)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun handleError(e: Exception) {
        Log.e("ReviewsListActivity", "Error loading reviews", e)
        runOnUiThread {
            Toast.makeText(
                this@ReviewsListActivity,
                "Error loading reviews: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    override fun onResume() {
        super.onResume()
        // Osigurajte da je Home označen kad se vratite na ovaj Activity
        bottomNavigationView.selectedItemId = R.id.navigation_reviews
    }
}