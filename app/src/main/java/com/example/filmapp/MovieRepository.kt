package com.example.filmapp

import com.example.filmapp.BuildConfig

//Moive Repository koji dohvaca podatke i vraca ih kao listu List<Movie>
class MovieRepository {
    private val tmdbService = TMDBService.create()

    suspend fun getPopularMovies(): List<Movie> { //suspend ->funkcija koja moze bit blokirana bez prekidanja glavnog threada
        return try {
            val response = tmdbService.getPopularMovies(BuildConfig.TMDB_API_KEY)
            response.results
        } catch (e: Exception) {
            emptyList() // Vrati praznu listu ako nešto pođe po zlu
        }
    }
}