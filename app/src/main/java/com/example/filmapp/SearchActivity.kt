package com.example.filmapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView


class SearchActivity : AppCompatActivity() {

    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var searchAdapter: MovieAdapter
    private lateinit var userAdapter: UserAdapter
    private lateinit var searchView: SearchView
    private lateinit var viewModel: SearchViewModel
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var searchToggle: Button

    private var currentSearchType: SearchType = SearchType.MOVIES//tip podataka koji trazimo, napocetku stavljen na filmove

    enum class SearchType {
        MOVIES, USERS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initializeViews()
        setupAdapters()
        setupViewModel()
        setupSearchView()
        setupNavigation()
    }

    private fun initializeViews() {
        searchRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        searchRecyclerView.layoutManager = LinearLayoutManager(this)

        searchView = findViewById(R.id.searchView)
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        searchEditText?.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary))

        searchToggle = findViewById(R.id.search_toggle)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        updateToggleButtonText()
    }

    private fun setupAdapters() { //2 adaptera jedan koji stavlja filove, drugi usere
        searchAdapter = MovieAdapter(emptyList()) { movie ->
            val intent = Intent(this, MovieDetailsActivity::class.java).apply { //ako se klikne ide se na recenziranje filma
                putExtra("MOVIE", movie)
            }
            startActivity(intent)
        }

        userAdapter = UserAdapter(emptyList()) { user ->
            val intent = Intent(this, UserProfileActivity::class.java).apply {//ako se klikne ide se na profil od usera
                putExtra("USER_ID", user.id)
            }
            startActivity(intent)
        }

        updateRecyclerViewAdapter() //zavisi sta trazimo filmove ii usere taj adapter se ucitava
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java) //jedan viewmodel preko kojeg trazim i usere i filmove

        viewModel.searchResults.observe(this) { movies ->
            if (currentSearchType == SearchType.MOVIES) {
                searchAdapter.updateMovies(movies)
            }
        }

        viewModel.userResults.observe(this) { users ->
            if (currentSearchType == SearchType.USERS) {
                userAdapter.updateUsers(users)
            }
        }
    }

    private fun setupSearchView() { //pretraga
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.length > 2) {
                    performSearch(newText)
                } else {
                    clearResults()
                }
                return true
            }
        })

        searchToggle.setOnClickListener {
            toggleSearchType()
        }
    }

    private fun performSearch(query: String) {
        when (currentSearchType) {
            SearchType.MOVIES -> viewModel.searchMovies(query)
            SearchType.USERS -> viewModel.searchUsers(query)
        }
    }

    private fun clearResults() {
        when (currentSearchType) {
            SearchType.MOVIES -> searchAdapter.updateMovies(emptyList())
            SearchType.USERS -> userAdapter.updateUsers(emptyList())
        }
    }

    private fun toggleSearchType() {
        currentSearchType = when (currentSearchType) { //AKO JE TRENUTNI TIP MOVIES POSTAJE USERS KAD SE STISNE I OBRNUTO
            SearchType.MOVIES -> SearchType.USERS
            SearchType.USERS -> SearchType.MOVIES
        }
        updateToggleButtonText() //Promjena teksta gumba
        searchView.setQuery("", false) //brise query
        clearResults()
        updateRecyclerViewAdapter() //updatea adapter na onaj koji smo stavili toggle
    }

    private fun updateToggleButtonText() { //Promjena teksta u gumbu
        searchToggle.text = when (currentSearchType) {
            SearchType.MOVIES -> "Search Movies"
            SearchType.USERS -> "Search Users"
        }
    }

    private fun updateRecyclerViewAdapter() {
        searchRecyclerView.adapter = when (currentSearchType) {
            SearchType.MOVIES -> searchAdapter
            SearchType.USERS -> userAdapter
        }
    }

    private fun setupNavigation() {
        bottomNavigationView.selectedItemId = R.id.navigation_search
        bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_reviews -> {
                    startActivity(Intent(this, ReviewsListActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_search -> true
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNavigationView.selectedItemId = R.id.navigation_search
    }
}