package com.example.filmapp

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReviewRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun addReview(review: Review) { //dodavanje reviewa
        val currentUser = auth.currentUser ?: throw Exception("User not logged in") //provjera usera
        val reviewWithUserId = review.copy(userId = currentUser.uid) //trenutni user pise ovaj review

        try {
            firestore.collection("reviews") //dodavanje na firestore
                .add(reviewWithUserId)
                .await()
        } catch (e: Exception) {
            Log.e("ReviewRepository", "Error adding review", e)
            throw e
        }
    }

    suspend fun getUserReviews(): List<Review> {
        val currentUser = auth.currentUser ?: return emptyList() //daj ovog usera

        return try {
            firestore.collection("reviews") //njegova kolekcija review-a
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                .toObjects(Review::class.java)
        } catch (e: Exception) {
            Log.e("ReviewRepository", "Error getting reviews", e)
            emptyList()
        }
    }
}