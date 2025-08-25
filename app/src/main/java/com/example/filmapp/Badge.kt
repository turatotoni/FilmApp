package com.example.filmapp

data class Badge(
    val id: String,
    val title: String,
    val imageResId: Int,
    val condition: (userData: Map<String, Any>) -> Boolean
)
