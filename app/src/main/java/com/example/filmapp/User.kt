package com.example.filmapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val avatarID: Int = 0, //prebacio sam string u int jer sam tako prvo napravio s avatarpicker i avataradapter; bio je veliki problem jer nisam mogo ucitat sliku jer sam loado kao string a ne kao id
    val followerCount: Long = 0,
    val followingCount: Long = 0
) : Parcelable