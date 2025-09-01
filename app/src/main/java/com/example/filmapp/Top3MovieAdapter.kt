package com.example.filmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class Top3MovieAdapter(
    private var movies: List<Movie>,
    private val onItemClick: (Movie) -> Unit
) : RecyclerView.Adapter<Top3MovieAdapter.Top3MovieViewHolder>() {

    class Top3MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.movie_poster)
        val title: TextView = view.findViewById(R.id.movie_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Top3MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top3_movie, parent, false)
        return Top3MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: Top3MovieViewHolder, position: Int) {
        val movie = movies[position]

        // Load movie poster
        Glide.with(holder.itemView.context)
            .load("https://image.tmdb.org/t/p/w500${movie.poster_path}")
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .into(holder.poster)

        holder.title.text = movie.title

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