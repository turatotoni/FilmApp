package com.example.filmapp

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

class Top3Manager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "Top3Preferences"
        private const val KEY_TOP3_MOVIES = "top3_movies"
        private const val MAX_TOP3 = 3
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val gson = Gson()

    suspend fun addToTop3(movie: Movie): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false
        val currentTop3 = getTop3().toMutableList()

        // Check if movie is already in Top 3
        if (currentTop3.any { it.id == movie.id }) {
            return true // Already in Top 3
        }

        // Check if we have reached the maximum
        if (currentTop3.size >= MAX_TOP3) {
            return false // Cannot add more than 3
        }

        // Add to Top 3
        currentTop3.add(movie)
        saveTop3ToFirestore(currentTop3, currentUserId)
        saveTop3ToLocal(currentTop3) // Keep local cache for offline use
        return true
    }

    suspend fun removeFromTop3(movieId: Int) {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentTop3 = getTop3().toMutableList()
        currentTop3.removeAll { it.id == movieId }
        saveTop3ToFirestore(currentTop3, currentUserId)
        saveTop3ToLocal(currentTop3) // Keep local cache for offline use
    }

    suspend fun getTop3(): List<Movie> {
        // Try to get from Firestore first, fall back to local storage
        return try {
            val currentUserId = auth.currentUser?.uid ?: return getTop3FromLocal()
            val top3 = getTop3FromFirestore(currentUserId)
            saveTop3ToLocal(top3) // Update local cache
            top3
        } catch (e: Exception) {
            // If Firestore fails, use local storage
            getTop3FromLocal()
        }
    }

    suspend fun getTop3ForUser(userId: String): List<Movie> {
        return try {
            getTop3FromFirestore(userId)
        } catch (e: Exception) {
            emptyList() // Return empty list if unable to fetch
        }
    }

    fun isInTop3(movieId: Int): Boolean {
        // Use local cache for quick checks
        return getTop3FromLocal().any { it.id == movieId }
    }

    fun getTop3Count(): Int {
        return getTop3FromLocal().size
    }

    suspend fun clearTop3() {
        val currentUserId = auth.currentUser?.uid ?: return
        saveTop3ToFirestore(emptyList(), currentUserId)
        saveTop3ToLocal(emptyList())
    }

    private suspend fun getTop3FromFirestore(userId: String): List<Movie> {
        val document = firestore.collection("userTop3").document(userId).get().await()
        val json = document.getString("movies") ?: return emptyList()
        val type = object : TypeToken<List<Movie>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private suspend fun saveTop3ToFirestore(movies: List<Movie>, userId: String) {
        val top3Data = hashMapOf(
            "movies" to gson.toJson(movies),
            "lastUpdated" to FieldValue.serverTimestamp(),
            "userId" to userId
        )

        firestore.collection("userTop3").document(userId)
            .set(top3Data)
            .await()
    }

    private fun getTop3FromLocal(): List<Movie> {
        val json = sharedPreferences.getString(KEY_TOP3_MOVIES, null)
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<Movie>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }
    }

    private fun saveTop3ToLocal(movies: List<Movie>) {
        val json = gson.toJson(movies)
        sharedPreferences.edit().putString(KEY_TOP3_MOVIES, json).apply()
    }
}