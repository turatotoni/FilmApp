package com.example.filmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HorizontalMovieAdapter(
    private var movies: List<Movie>,
    private val onItemClick: (Movie) -> Unit
) : RecyclerView.Adapter<HorizontalMovieAdapter.HorizontalMovieViewHolder>() {

    class HorizontalMovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //val title: TextView = view.findViewById(R.id.movieTitle)
        val poster: ImageView = view.findViewById(R.id.moviePoster)
        //val rating: TextView = view.findViewById(R.id.movieRating)
        // No need for releaseDate and overview in horizontal layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorizontalMovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie_horizontal, parent, false) // Different layout!
        return HorizontalMovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: HorizontalMovieViewHolder, position: Int) {
        val movie = movies[position]
        //holder.title.text = movie.title
        //holder.rating.text = "⭐ ${"%.1f".format(movie.vote_average)}"
        // No release date or overview in horizontal layout

        // Load image using Glide
        Glide.with(holder.itemView.context)
            .load("https://image.tmdb.org/t/p/w500${movie.poster_path}")
            .into(holder.poster)

        holder.itemView.setOnClickListener {
            onItemClick(movie)
        }
    }

    fun updateMovies(newMovies: List<Movie>) {
        this.movies = newMovies
        notifyDataSetChanged()
    }

    override fun getItemCount() = movies.size
}