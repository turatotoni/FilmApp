package com.example.filmapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface TMDBService {
    @GET("movie/popular") // GET zahtjev na popularne filmove
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String, //api kljuc
        @Query("language") language: String = "en-US", //jezik
        @Query("page") page: Int = 1 //broj stranice
    ): MovieResponse //sve u ovaj objekt

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"

        fun create(): TMDBService {
            val retrofit = Retrofit.Builder() //stvara retrofit instancu
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) //pretvorba JSON u kotlin
                .build()
            return retrofit.create(TMDBService::class.java) //konkretna implementacija
        }
    }
}