package com.example.filmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class ReviewsAdapter(private var reviews: List<Review>) : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    private var ratingSpinner : Spinner? = null

    class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val movieTitle: TextView = view.findViewById(R.id.reviewMovieTitle)
        val moviePoster: ImageView = view.findViewById(R.id.reviewMoviePoster)
        val ratingText: TextView = view.findViewById(R.id.reviewRatingText)
        val reviewText: TextView = view.findViewById(R.id.reviewText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]

        Glide.with(holder.itemView.context).clear(holder.moviePoster)

        holder.movieTitle.text = review.movieTitle
        holder.reviewText.text = review.reviewText
        //pokusavam napravit da nemoram slat spinner na prikaz, nego textview koji mogu ljepse prikazat
        //zelim da spinner spremi ocjenu i onda poslat int u funkciji getSelectedRating

        //na kraju mi getselectedrating uopce nije trebao
        holder.ratingText.text = "⭐ ${review.rating.toInt()}/10"

        // Postavi Spinner s ocjenama 1-10
//        val ratings = (1..10).map { it.toString() }
//        val adapter = ArrayAdapter(
//            holder.itemView.context,
//            android.R.layout.simple_spinner_item,
//            ratings
//        ).apply {
//            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        }
//
//        holder.ratingSpinner.adapter = adapter
//        holder.ratingSpinner.setSelection(review.rating.toInt() - 1) // Postavi odabranu ocjenu
//        holder.ratingSpinner.isEnabled = false // Onemogući mijenjanje ocjene u prikazu

        if (ratingSpinner == null) {
            ratingSpinner = Spinner(holder.itemView.context).apply {
                adapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_item,
                    (1..10).map { it.toString() }
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            }
        }

        ratingSpinner?.setSelection(review.rating.toInt() - 1)

        review.moviePosterPath?.let { path ->
            Glide.with(holder.itemView.context)
                .load("https://image.tmdb.org/t/p/w500$path")
                .apply(RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                )
                .into(holder.moviePoster)
        }
    }


    override fun onViewRecycled(holder: ReviewViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.itemView.context).clear(holder.moviePoster)
    }

    fun updateReviews(newReviews: List<Review>) {
        this.reviews = newReviews
        notifyDataSetChanged()
    }

    override fun getItemCount() = reviews.size
}