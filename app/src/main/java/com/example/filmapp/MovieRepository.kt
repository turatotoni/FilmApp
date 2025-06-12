package com.example.filmapp

import com.example.filmapp.BuildConfig
import com.google.api.Page

//Moive Repository koji dohvaca podatke i vraca ih kao listu List<Movie>
class MovieRepository {
    private val tmdbService = TMDBService.create()

    suspend fun getPopularMovies(page: Int): List<Movie> { //suspend ->funkcija koja moze bit blokirana bez prekidanja glavnog threada
        return try {
            val response = tmdbService.getPopularMovies(BuildConfig.TMDB_API_KEY, page = page)
            response.results
        } catch (e: Exception) {
            emptyList() // Vrati praznu listu ako nešto pođe po zlu
        }
    }

    suspend fun searchMovies(query: String): List<Movie> {
        return try {
            val response = tmdbService.searchMovies(
                apiKey = BuildConfig.TMDB_API_KEY,
                query = query,
                language = "en-US",
                page = 1
            )
            response.results.map { movie ->
                Movie(
                    id = movie.id,
                    title = movie.title,
                    overview = movie.overview,
                    poster_path = movie.poster_path,
                    vote_average = movie.vote_average,
                    release_date = movie.release_date
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}