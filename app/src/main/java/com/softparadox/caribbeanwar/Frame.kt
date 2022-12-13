package com.softparadox.caribbeanwar

data class Frame(
    val id: String,
    val status: String,
    val rotation: String,
    val hit: Boolean = false
)