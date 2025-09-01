package com.example.filmapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Top3Manager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "Top3Preferences"
        private const val KEY_TOP3_MOVIES = "top3_movies"
        private const val MAX_TOP3 = 3
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun addToTop3(movie: Movie): Boolean {
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
        saveTop3(currentTop3)
        return true
    }

    fun removeFromTop3(movieId: Int) {
        val currentTop3 = getTop3().toMutableList()
        currentTop3.removeAll { it.id == movieId }
        saveTop3(currentTop3)
    }

    fun getTop3(): List<Movie> {
        val json = sharedPreferences.getString(KEY_TOP3_MOVIES, null)
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<Movie>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }
    }

    fun isInTop3(movieId: Int): Boolean {
        return getTop3().any { it.id == movieId }
    }

    fun getTop3Count(): Int {
        return getTop3().size
    }

    private fun saveTop3(movies: List<Movie>) {
        val json = gson.toJson(movies)
        sharedPreferences.edit().putString(KEY_TOP3_MOVIES, json).apply()
    }

    fun clearTop3() {
        sharedPreferences.edit().remove(KEY_TOP3_MOVIES).apply()
    }
}