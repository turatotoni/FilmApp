package com.example.filmapp

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repository = MovieRepository()
    val searchResults = MutableLiveData<List<Movie>>()

    fun searchMovies(query: String) {
        viewModelScope.launch {
            try {
                val results = repository.searchMovies(query)
                searchResults.postValue(results)
            } catch (e: Exception) {
                // Obrada grešaka
                Log.e("SearchViewModel", "Error searching movies", e)
            }
        }
    }
}