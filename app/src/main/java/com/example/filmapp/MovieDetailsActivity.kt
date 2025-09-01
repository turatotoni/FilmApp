package com.example.filmapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MovieDetailsActivity : AppCompatActivity() {

    private lateinit var moviePoster: ImageView
    private lateinit var movieTitle: TextView
    private lateinit var movieReleaseDate: TextView
    private lateinit var tmdbRating: TextView
    private lateinit var appAverageRating: TextView
    private lateinit var movieOverview: TextView
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var createReviewButton: Button
    private lateinit var watchTrailerButton: Button

    private lateinit var reviewsAdapter: ReviewsAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var currentMovie: Movie

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_details)

        // Initialize views
        moviePoster = findViewById(R.id.moviePoster)
        movieTitle = findViewById(R.id.movieTitle)
        movieReleaseDate = findViewById(R.id.movieReleaseDate)
        tmdbRating = findViewById(R.id.tmdbRating)
        appAverageRating = findViewById(R.id.appAverageRating)
        movieOverview = findViewById(R.id.movieOverview)
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView)
        createReviewButton = findViewById(R.id.createReviewButton)
        watchTrailerButton = findViewById(R.id.watchTrailerButton)

        // Initialize Firestore
        db = Firebase.firestore

        // Get movie data from intent
        currentMovie = intent.getParcelableExtra<Movie>("MOVIE") ?: run {
            Toast.makeText(this, "Movie data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up UI with movie data
        setupMovieDetails(currentMovie)

        // Set up reviews RecyclerView
        setupReviewsRecyclerView()

        // Load reviews for this movie
        loadReviews()

        // Set up create review button
        createReviewButton.setOnClickListener {
            val intent = Intent(this, ReviewActivity::class.java).apply {
                putExtra("MOVIE", currentMovie)
            }
            startActivity(intent)
        }


        watchTrailerButton.setOnClickListener {
            openYouTubeTrailer(currentMovie.title)
        }
    }

    private fun setupMovieDetails(movie: Movie) {
        // Load movie poster
        Glide.with(this)
            .load("https://image.tmdb.org/t/p/w500${movie.poster_path}")
            .into(moviePoster)

        movieTitle.text = movie.title
        val year = movie.release_date?.take(4) ?: ""
        movieReleaseDate.text = if (year.isNotEmpty()) "($year)" else ""
        movieOverview.text = movie.overview
        tmdbRating.text = "⭐ ${"%.1f".format(movie.vote_average)}/10 (IMDb Rating)"
        appAverageRating.text = "⭐ Loading..." // Will be updated when reviews are loaded
    }

    private fun setupReviewsRecyclerView() {
        reviewsAdapter = ReviewsAdapter(emptyList()) { review ->
            // Handle review item click if needed
            val intent = Intent(this, DisplayReviewActivity::class.java).apply {
                putExtra("review", review)
            }
            startActivity(intent)
        }

        reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MovieDetailsActivity)
            adapter = reviewsAdapter
            setHasFixedSize(true)
        }
    }


    private fun openYouTubeTrailer(movieTitle: String) {
        try {
            // Create search query for the movie trailer
            val searchQuery = "${movieTitle} official trailer"
            val encodedQuery = Uri.encode(searchQuery)

            // First try to open YouTube app
            val youtubeIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
                setPackage("com.google.android.youtube")
            }

            // If YouTube app is installed, open it
            if (youtubeIntent.resolveActivity(packageManager) != null) {
                startActivity(youtubeIntent)
            } else {
                // Fallback to browser
                val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
                }
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening trailer", Toast.LENGTH_SHORT).show()
            Log.e("MovieDetails", "Error opening trailer", e)
        }
    }

    private fun loadReviews() {
        db.collection("reviews")
            .whereEqualTo("movieId", currentMovie.id)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MovieDetails", "Error loading reviews", error)
                    Toast.makeText(
                        this,
                        "Error loading reviews: ${error.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    try {
                        val reviews = querySnapshot.toObjects(Review::class.java)
                        Log.d("MovieDetails", "Loaded ${reviews.size} reviews")
                        reviewsAdapter.updateReviews(reviews)

                        // Calculate average
                        if (reviews.isNotEmpty()) {
                            val average = reviews.map { it.rating }.average().toFloat()
                            appAverageRating.text = "⭐ %.1f/10 (FilmApp Rating)".format(average)
                        } else {
                            appAverageRating.text = "⭐ No reviews yet"
                        }
                    } catch (e: Exception) {
                        Log.e("MovieDetails", "Error parsing reviews", e)
                        Toast.makeText(
                            this,
                            "Error parsing review data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } ?: run {
                    Log.d("MovieDetails", "No reviews found")
                    appAverageRating.text = "⭐ No reviews yet"
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isDestroyed) {
            Glide.with(this).clear(moviePoster)
        }
    }
}