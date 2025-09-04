package com.example.filmapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MovieDetailsActivity : AppCompatActivity() {

    private lateinit var moviePoster: ImageView
    private lateinit var movieTitle: TextView
    private lateinit var movieReleaseDate: TextView
    private lateinit var tmdbRating: TextView
    private lateinit var appAverageRating: TextView
    private lateinit var movieOverview: TextView
    private lateinit var createReviewButton: Button
    private lateinit var watchTrailerButton: Button
    private lateinit var top3Button: ImageButton
    private lateinit var reviewsAdapter: ReviewsAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var currentMovie: Movie
    private lateinit var top3Manager: Top3Manager

    private lateinit var reviewsContainer: LinearLayout
    private var isInTop3 = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_details)

        // Initialize Top3Manager
        top3Manager = Top3Manager(this)

        // Initialize views
        moviePoster = findViewById(R.id.moviePoster)
        movieTitle = findViewById(R.id.movieTitle)
        movieReleaseDate = findViewById(R.id.movieReleaseDate)
        tmdbRating = findViewById(R.id.tmdbRating)
        appAverageRating = findViewById(R.id.appAverageRating)
        movieOverview = findViewById(R.id.movieOverview)
        createReviewButton = findViewById(R.id.createReviewButton)
        watchTrailerButton = findViewById(R.id.watchTrailerButton)
        top3Button = findViewById(R.id.top3Button)
        reviewsContainer = findViewById(R.id.reviewsContainer)

        // Initialize Firestore
        db = Firebase.firestore

        // Get movie data from intent
        currentMovie = intent.getParcelableExtra<Movie>("MOVIE") ?: run {
            Toast.makeText(this, "Movie data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        isInTop3 = top3Manager.isInTop3(currentMovie.id)
        updateTop3ButtonAppearance()


        setupMovieDetails(currentMovie)


//        setupReviewsRecyclerView()


        loadReviews()


        createReviewButton.setOnClickListener {
            val intent = Intent(this, ReviewActivity::class.java).apply {
                putExtra("MOVIE", currentMovie)
            }
            startActivity(intent)
        }


        watchTrailerButton.setOnClickListener {
            openYouTubeTrailer(currentMovie.title)
        }


        top3Button.setOnClickListener {
            toggleTop3()
        }
    }

    private fun toggleTop3() {
        CoroutineScope(Dispatchers.IO).launch {
            if (isInTop3) {
                // Remove from Top 3
                top3Manager.removeFromTop3(currentMovie.id)
                withContext(Dispatchers.Main) {
                    isInTop3 = false
                    updateTop3ButtonAppearance()
                    Toast.makeText(this@MovieDetailsActivity, "Removed from Top 3", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Try to add to Top 3
                val success = top3Manager.addToTop3(currentMovie)
                withContext(Dispatchers.Main) {
                    if (success) {
                        isInTop3 = true
                        updateTop3ButtonAppearance()
                        Toast.makeText(this@MovieDetailsActivity, "Added to Top 3!", Toast.LENGTH_SHORT).show()
                    } else {
                        val currentCount = top3Manager.getTop3Count()
                        Toast.makeText(
                            this@MovieDetailsActivity,
                            "You can only have 3 movies in your Top 3. Remove one to add this movie.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateTop3ButtonAppearance() {
        val drawableRes = if (isInTop3) {
            R.drawable.ic_star_filled
        } else {
            R.drawable.ic_star_outline
        }
        top3Button.setImageResource(drawableRes)
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

//    private fun setupReviewsRecyclerView() {
//        reviewsAdapter = ReviewsAdapter(emptyList()) { review ->
//            val intent = Intent(this, DisplayReviewActivity::class.java).apply {
//                putExtra("review", review)
//            }
//            startActivity(intent)
//        }
//
//        reviewsRecyclerView.apply {
//            layoutManager = LinearLayoutManager(this@MovieDetailsActivity)
//            adapter = reviewsAdapter
//            setHasFixedSize(true)
//        }
//    }

    private fun openYouTubeTrailer(movieTitle: String) {
        try {
            val searchQuery = "${movieTitle} official trailer"
            val encodedQuery = Uri.encode(searchQuery)

            val youtubeIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
                setPackage("com.google.android.youtube")
            }

            if (youtubeIntent.resolveActivity(packageManager) != null) {
                startActivity(youtubeIntent)
            } else {
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
            .get()
            .addOnSuccessListener { querySnapshot ->
                try {
                    reviewsContainer.removeAllViews() // Clear previous reviews

                    val reviews = querySnapshot.toObjects(Review::class.java)
                    Log.d("MovieDetails", "Loaded ${reviews.size} reviews for movie ${currentMovie.id}")

                    if (reviews.isEmpty()) {
                        val noReviewsText = TextView(this).apply {
                            text = "No reviews yet"
                            setTextColor(ContextCompat.getColor(context, R.color.light_gray))
                            textSize = 16f
                            gravity = Gravity.CENTER
                            setPadding(0, 32.dpToPx(), 0, 32.dpToPx())
                        }
                        reviewsContainer.addView(noReviewsText)
                    } else {
                        // First get all unique user IDs
                        val userIds = reviews.map { it.userId }.distinct()

                        // Fetch all usernames first
                        fetchAllUsernames(userIds) { usernameMap ->
                            // Now add all reviews with proper usernames
                            reviews.forEach { review ->
                                addReviewToContainer(review, usernameMap)
                            }
                        }
                    }

                    // Calculate average
                    if (reviews.isNotEmpty()) {
                        val average = reviews.map { it.rating }.average().toFloat()
                        appAverageRating.text = "⭐ %.1f/10 (FilmApp Rating)".format(average)
                    } else {
                        appAverageRating.text = "⭐ No reviews yet"
                    }
                } catch (e: Exception) {
                    Log.e("MovieDetails", "Error parsing reviews", e)
                }
            }
            .addOnFailureListener { error ->
                Log.e("MovieDetails", "Error loading reviews", error)
            }
    }

    private fun fetchAllUsernames(userIds: List<String>, callback: (Map<String, String>) -> Unit) {
        val usernameMap = mutableMapOf<String, String>()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // If no users to fetch, return empty map
        if (userIds.isEmpty()) {
            callback(usernameMap)
            return
        }

        var completedFetches = 0

        userIds.forEach { userId ->
            if (userId == currentUserId) {
                usernameMap[userId] = "Your Review"
                completedFetches++
                if (completedFetches == userIds.size) {
                    callback(usernameMap)
                }
            } else {
                FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            usernameMap[userId] = document.getString("username") ?: "Unknown User"
                        } else {
                            usernameMap[userId] = "Unknown User"
                        }
                        completedFetches++
                        if (completedFetches == userIds.size) {
                            callback(usernameMap)
                        }
                    }
                    .addOnFailureListener {
                        usernameMap[userId] = "Unknown User"
                        completedFetches++
                        if (completedFetches == userIds.size) {
                            callback(usernameMap)
                        }
                    }
            }
        }
    }

    private fun addReviewToContainer(review: Review, usernameMap: Map<String, String>) {
        val reviewView = LayoutInflater.from(this).inflate(R.layout.item_review, reviewsContainer, false)

        val movieTitle: TextView = reviewView.findViewById(R.id.reviewMovieTitle)
        val moviePoster: ImageView = reviewView.findViewById(R.id.reviewMoviePoster)
        val ratingText: TextView = reviewView.findViewById(R.id.reviewRatingText)
        val reviewText: TextView = reviewView.findViewById(R.id.reviewText)
        val username: TextView = reviewView.findViewById(R.id.reviewUsername)

        movieTitle.text = review.movieTitle
        reviewText.text = review.reviewText
        ratingText.text = "⭐ ${review.rating.toInt()}/10"

        // Set username from the map
        username.text = usernameMap[review.userId] ?: "Unknown User"

        review.moviePosterPath?.let { path ->
            Glide.with(this)
                .load("https://image.tmdb.org/t/p/w500$path")
                .into(moviePoster)
        }

        reviewView.setOnClickListener {
            val intent = Intent(this, DisplayReviewActivity::class.java).apply {
                putExtra("review", review)
            }
            startActivity(intent)
        }

        reviewsContainer.addView(reviewView)
    }
    fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    override fun onDestroy() {
        super.onDestroy()
        if (!isDestroyed) {
            Glide.with(this).clear(moviePoster)
        }
    }
}