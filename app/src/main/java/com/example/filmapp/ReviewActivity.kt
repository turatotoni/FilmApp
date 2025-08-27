package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class ReviewActivity : AppCompatActivity() {
    private lateinit var moviePoster: ImageView
    private lateinit var movieTitle: TextView
    private lateinit var reviewText: EditText
    private lateinit var submitButton: Button
    private lateinit var ratingSpinner: Spinner
    private lateinit var cancelButton: Button

    private val reviewRepository = ReviewRepository()
    private lateinit var currentMovie: Movie
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        moviePoster = findViewById(R.id.moviePoster)
        movieTitle = findViewById(R.id.movieTitle)
        reviewText = findViewById(R.id.reviewText)
        submitButton = findViewById(R.id.submitButton)
        ratingSpinner = findViewById(R.id.ratingSpinner)
        cancelButton = findViewById(R.id.cancelButton)

        checkExistingReview()

        currentMovie = intent.getParcelableExtra<Movie>("MOVIE") ?: run {
            Toast.makeText(this, "Movie data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cancelButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Display movie info
        movieTitle.text = currentMovie.title //displayanje postera
        Glide.with(this)
            .load("https://image.tmdb.org/t/p/w500${currentMovie.poster_path}")
            .into(moviePoster)

        submitButton.setOnClickListener { //glavni dio
            submitReview()

            val intent = Intent(this@ReviewActivity, HomeActivity::class.java) //kada zavrsi review prebaci me na home
            startActivity(intent)
            //Ako bismo stavili finish() odmah nakon submitReview(), aktivnost bi se zatvorila prije nego što korutina

        }

    }

    private fun submitReview() {
        val selectedRating = ratingSpinner.selectedItem.toString().toInt()
        val text = reviewText.text.toString().trim()

        if (selectedRating == 0) {
            Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show()
            return
        }
        if (text.isEmpty()) {
            Toast.makeText(this, "Please write your review", Toast.LENGTH_SHORT).show()
            return
        }

        val review = Review( //napravi review objekt
            userId = auth.currentUser?.uid ?: "",
            movieId = currentMovie.id,
            movieTitle = currentMovie.title,
            moviePosterPath = currentMovie.poster_path,
            rating = selectedRating.toFloat(),
            reviewText = text
        )

        Log.d("ReviewActivity", "Submitting reviews $review")

        val progressDialog = MaterialAlertDialogBuilder(this) //za izgled malo bolje
            .setView(R.layout.dialog_progress)
            .setCancelable(false)
            .create()

        progressDialog.show()
        submitButton.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) { //korutina se pokrece u pozadinskom threadu
            try {
                reviewRepository.addReview(review)


                val userId = auth.currentUser?.uid
                if (userId != null) {
                    updateUserReviewCount(userId)
                }

                withContext(Dispatchers.Main) { //ako sve uspije na vrca se na main thread
                    if (!isFinishing) {
                        if (progressDialog.isShowing) {
                            progressDialog.dismiss()
                        }
                        Toast.makeText(this@ReviewActivity, "Review submitted!", Toast.LENGTH_SHORT).show()
                        finish() //dodajem finish tu a ne gore iza intent-a zato sta ova funkcija radi asinhrono znaci ako bi bila gore izvrsava se odmah cim krene ova korutina
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isFinishing) {
                        if (progressDialog.isShowing) {
                            progressDialog.dismiss()
                        }
                        submitButton.isEnabled = true //ako ne uspije poslat korisnik moze probat ponovo
                        Toast.makeText(
                            this@ReviewActivity,
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }


    private suspend fun updateUserReviewCount(userId: String) {
        try {
            val userRef = firestore.collection("users").document(userId)

            // Try to increment the review count
            userRef.update("reviewCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            // If the field doesn't exist yet, set it to 1
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                if (!userDoc.exists() || !userDoc.contains("reviewCount")) {
                    firestore.collection("users").document(userId)
                        .set(mapOf("reviewCount" to 1), com.google.firebase.firestore.SetOptions.merge())
                        .await()
                }
            } catch (e2: Exception) {
                Log.e("ReviewActivity", "Failed to initialize reviewCount: ${e2.message}")
            }
        }
    }


    private fun checkExistingReview() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userReviews = reviewRepository.getUserReviews()
                val existingReview = userReviews.firstOrNull { it.movieId == currentMovie.id }

                withContext(Dispatchers.Main) {
                    if (existingReview != null) {
                        showAlreadyReviewedDialog(existingReview)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReviewActivity,
                        "Error checking reviews: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showAlreadyReviewedDialog(existingReview: Review) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Already Reviewed")
            .setMessage("You've already reviewed this movie on ${Date(existingReview.timestamp)}")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isDestroyed) {  //ovo sad dodo jer baca illegalArgument (zatvara nes sta se vec zatvorilo)
            Glide.with(this).clear(moviePoster)
        }
    }

}