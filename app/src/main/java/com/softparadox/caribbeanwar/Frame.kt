package com.softparadox.caribbeanwar

data class Frame(
    val id: String,
    val status: String,
    val rotation: Int,
    val hit: Boolean = false
)