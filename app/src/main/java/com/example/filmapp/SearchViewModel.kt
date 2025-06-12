package com.example.filmapp

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repository = MovieRepository()
    private val userRepository = UserRepository()

    val searchResults = MutableLiveData<List<Movie>>()
    val userResults = MutableLiveData<List<User>>()

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

    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                val results = userRepository.searchUsers(query)
                userResults.postValue(results)
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching users", e)
            }
        }
    }
}