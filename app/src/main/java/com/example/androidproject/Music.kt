package com.example.androidproject
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Music(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val data: String,
    val albumId: Long
) : Parcelable
