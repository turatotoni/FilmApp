package com.example.filmapp

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch //treba importat nes u gradle i vljd je toto

//obraduje podatke i salje ih na ekran
//MovieRepository dohvaća podatke s TMDB-a i vraća ih ViewModel-u.
//ViewModel ažurira LiveData

class MoviesViewModel : ViewModel() {
    private val repository = MovieRepository()
    val popularMovies = MutableLiveData<List<Movie>>()//promjenjivi kontejner za podatke koji obaviještava Activity o promjenama
    val upcomingMovies = MutableLiveData<List<Movie>>()
    val topRatedMovies = MutableLiveData<List<Movie>>()
    val isLoading = MutableLiveData<Boolean>()


    val movies = MutableLiveData<List<Movie>>()
    fun fetchPopularMovies() {
        val randomPage = (1..30).random() //da nisu uvijek isti popularni filmovi
        viewModelScope.launch {
            try {
                val result = repository.getPopularMovies(randomPage)
                popularMovies.postValue(result) //azuriranje LiveData s novim filmovimay
                movies.postValue(result)
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "Error fetching popular movies", e)
            }
        }
    }

    fun fetchTopRatedMovies() {
        val randomPage = (1..10).random() // Top rated has fewer pages typically
        viewModelScope.launch {
            try {
                val result = repository.getTopRatedMovies(randomPage)
                topRatedMovies.postValue(result)
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "Error fetching top rated movies", e)
            }
        }
    }

    // Add function for upcoming movies
    fun fetchUpcomingMovies() {
        val randomPage = (1..5).random() // Upcoming usually has fewer pages
        viewModelScope.launch {
            try {
                val result = repository.getUpcomingMovies(randomPage)
                upcomingMovies.postValue(result)
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "Error fetching upcoming movies", e)
            }
        }
    }

    // Function to fetch all categories at once
    fun fetchAllMovies() {
        isLoading.postValue(true)

        viewModelScope.launch {
            try {
                // Fetch all categories in parallel
                val popularDeferred = async { repository.getPopularMovies((1..30).random()) }
                val topRatedDeferred = async { repository.getTopRatedMovies((1..10).random()) }
                val upcomingDeferred = async { repository.getUpcomingMovies((1..5).random()) }

                // Wait for all requests to complete
                popularMovies.postValue(popularDeferred.await())
                topRatedMovies.postValue(topRatedDeferred.await())
                upcomingMovies.postValue(upcomingDeferred.await())

            } catch (e: Exception) {
                Log.e("MoviesViewModel", "Error fetching movies", e)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
//        val testList = listOf(
//            Movie(1, "Test Film 1", "Opis 1", null, 7.5, "2021-01-01"),
//            Movie(2, "Test Film 2", "Opis 2", null, 8.0, "2022-01-01")
//        )


}