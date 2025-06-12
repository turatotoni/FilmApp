package com.example.filmapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize //omogucava da saljemo nase kreirane objekte kroz Intente
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String?, // Relativna putanja (npr. "/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg")
    val vote_average: Double,
    val release_date: String
) : Parcelable

// Omot za API odgovor (TMDB vraća listu filmova unutar "results")
data class MovieResponse(
    val results: List<Movie>
)