package com.example.filmapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Review(
    val userId: String = "",
    val movieId: Int = 0,
    val movieTitle: String = "",
    val moviePosterPath: String? = null,
    val rating: Float = 0f,  // 1-10 scale
    val reviewText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: List<String> = emptyList(),
    val dislikes: List<String> = emptyList()
) : Parcelable