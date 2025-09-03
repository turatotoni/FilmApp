package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ImageView
import android.widget.Button
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.bumptech.glide.Glide

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var viewModel: MoviesViewModel
    private lateinit var loadingProgress: ProgressBar

    // RecyclerViews for different sections
    private lateinit var popularRecyclerView: RecyclerView
    private lateinit var topRatedRecyclerView: RecyclerView
    private lateinit var upcomingRecyclerView: RecyclerView

    // Adapters for different sections
    private lateinit var popularAdapter: HorizontalMovieAdapter
    private lateinit var topRatedAdapter: HorizontalMovieAdapter
    private lateinit var upcomingAdapter: HorizontalMovieAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is logged in
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_home)

        // Initialize views
        initViews()

        // Setup ViewModel and observers
        setupViewModel()

        // Setup refresh listener
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchAllMovies()
        }

        // Setup navigation
        setupNavigation()

        // Fetch initial data
        viewModel.fetchAllMovies()
    }

    private fun initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        loadingProgress = findViewById(R.id.loadingProgress)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Initialize RecyclerViews
        popularRecyclerView = findViewById(R.id.popularRecyclerView)
        topRatedRecyclerView = findViewById(R.id.topRatedRecyclerView)
        upcomingRecyclerView = findViewById(R.id.upcomingRecyclerView)

        // Setup horizontal layout managers
        popularRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        topRatedRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        upcomingRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Initialize adapters with empty lists
        val clickListener = { movie: Movie ->
            val intent = Intent(this, MovieDetailsActivity::class.java).apply {
                putExtra("MOVIE", movie)
            }
            startActivity(intent)
        }

        popularAdapter = HorizontalMovieAdapter(emptyList(), clickListener)
        topRatedAdapter = HorizontalMovieAdapter(emptyList(), clickListener)
        upcomingAdapter = HorizontalMovieAdapter(emptyList(), clickListener)

        popularRecyclerView.adapter = popularAdapter
        topRatedRecyclerView.adapter = topRatedAdapter
        upcomingRecyclerView.adapter = upcomingAdapter
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this).get(MoviesViewModel::class.java)

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            swipeRefreshLayout.isRefreshing = isLoading
        }

        // Observe popular movies
        viewModel.popularMovies.observe(this) { movies ->
            popularAdapter.updateMovies(movies)
            // Optionally set the first popular movie as featured
            if (movies.isNotEmpty()) {
                setFeaturedMovie(movies.first())
            }
        }

        // Observe top rated movies
        viewModel.topRatedMovies.observe(this) { movies ->
            topRatedAdapter.updateMovies(movies)
        }

        // Observe upcoming movies
        viewModel.upcomingMovies.observe(this) { movies ->
            upcomingAdapter.updateMovies(movies)
        }
    }

    private fun setFeaturedMovie(movie: Movie) {
        val featuredBanner = findViewById<View>(R.id.featuredBanner)
        val featuredImage = findViewById<ImageView>(R.id.featuredImage)
        val featuredTitle = findViewById<TextView>(R.id.featuredTitle)
        val featuredRating = findViewById<TextView>(R.id.featuredRating)
        val featuredButton = findViewById<Button>(R.id.featuredButton)

        // Load movie poster using Glide
        movie.poster_path?.let { posterPath ->
            val posterUrl = "https://image.tmdb.org/t/p/w500$posterPath"
            Glide.with(this)
                .load(posterUrl)
                .into(featuredImage)
        }

        featuredTitle.text = movie.title
        featuredRating.text = "⭐ ${movie.vote_average}"

        featuredButton.setOnClickListener {
            val intent = Intent(this, MovieDetailsActivity::class.java).apply {
                putExtra("MOVIE", movie)
            }
            startActivity(intent)
        }

        featuredBanner.visibility = View.VISIBLE
    }

    private fun setupNavigation() {
        bottomNavigationView.selectedItemId = R.id.navigation_home
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
                R.id.navigation_reviews -> {
                    startActivity(Intent(this, ReviewsListActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_home -> true
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNavigationView.selectedItemId = R.id.navigation_home
    }
}