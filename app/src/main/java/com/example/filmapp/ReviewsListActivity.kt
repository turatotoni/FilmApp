package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.PopupMenu
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
    private lateinit var filterButton: ImageButton
    private val reviewRepository = ReviewRepository()
    private var loadingJob: Job? = null
    private var allReviews: List<Review> = emptyList()
    private var currentFilter: FilterOption = FilterOption.NONE

    enum class FilterOption {
        NONE, RATING_HIGH, RATING_LOW, MOST_LIKES, MOST_DISLIKES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reviews_list)

        val recyclerView = findViewById<RecyclerView>(R.id.reviewsRecyclerView)
        filterButton = findViewById(R.id.filterButton)

        recyclerView.layoutManager = LinearLayoutManager(this@ReviewsListActivity)

        adapter = ReviewsAdapter(emptyList()) { review ->
            val intent = Intent(this, DisplayReviewActivity::class.java).apply {
                putExtra("review", review)
            }
            startActivity(intent)
        }

        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)

        setupFilterButton()
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

    private fun setupFilterButton() {
        filterButton.setOnClickListener {
            showFilterMenu()
        }
    }

    private fun showFilterMenu() {
        val popup = PopupMenu(this, filterButton)
        popup.menuInflater.inflate(R.menu.reviews_filter_menu, popup.menu)

        // Mark current filter option
        when (currentFilter) {
            FilterOption.RATING_HIGH -> popup.menu.findItem(R.id.filter_rating_high).isChecked = true
            FilterOption.RATING_LOW -> popup.menu.findItem(R.id.filter_rating_low).isChecked = true
            FilterOption.MOST_LIKES -> popup.menu.findItem(R.id.filter_most_likes).isChecked = true
            FilterOption.MOST_DISLIKES -> popup.menu.findItem(R.id.filter_most_dislikes).isChecked = true
            FilterOption.NONE -> popup.menu.findItem(R.id.filter_none).isChecked = true
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.filter_none -> {
                    currentFilter = FilterOption.NONE
                    applyFilter()
                    true
                }
                R.id.filter_rating_high -> {
                    currentFilter = FilterOption.RATING_HIGH
                    applyFilter()
                    true
                }
                R.id.filter_rating_low -> {
                    currentFilter = FilterOption.RATING_LOW
                    applyFilter()
                    true
                }
                R.id.filter_most_likes -> {
                    currentFilter = FilterOption.MOST_LIKES
                    applyFilter()
                    true
                }
                R.id.filter_most_dislikes -> {
                    currentFilter = FilterOption.MOST_DISLIKES
                    applyFilter()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun applyFilter() {
        val filteredReviews = when (currentFilter) {
            FilterOption.NONE -> allReviews
            FilterOption.RATING_HIGH -> allReviews.sortedByDescending { it.rating }
            FilterOption.RATING_LOW -> allReviews.sortedBy { it.rating }
            FilterOption.MOST_LIKES -> allReviews.sortedByDescending { it.likes.size }
            FilterOption.MOST_DISLIKES -> allReviews.sortedByDescending { it.dislikes.size }
        }
        adapter.updateReviews(filteredReviews)
    }

    private fun loadReviews() {
        loadingJob = lifecycleScope.launch {
            try {
                allReviews = reviewRepository.getUserReviews()
                applyFilter()
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
        // Refresh reviews when returning to this activity
        loadReviews()
        bottomNavigationView.selectedItemId = R.id.navigation_reviews
    }
}