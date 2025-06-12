package com.example.filmapp

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch //treba importat nes u gradle i vljd je toto

//obraduje podatke i salje ih na ekran
//MovieRepository dohvaća podatke s TMDB-a i vraća ih ViewModel-u.
//ViewModel ažurira LiveData

class MoviesViewModel : ViewModel() {
    private val repository = MovieRepository()
    val movies = MutableLiveData<List<Movie>>() //promjenjivi kontejner za podatke koji obaviještava Activity o promjenama

    fun fetchPopularMovies() {
        val randomPage = (1..30).random() //da nisu uvijek isti popularni filmovi
        viewModelScope.launch {
            try {
                val result = repository.getPopularMovies(randomPage)
                movies.postValue(result) //azuriranje LiveData s novim filmovimay
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "Error fetching movies", e)
            }

       }
//        val testList = listOf(
//            Movie(1, "Test Film 1", "Opis 1", null, 7.5, "2021-01-01"),
//            Movie(2, "Test Film 2", "Opis 2", null, 8.0, "2022-01-01")
//        )

    }
}