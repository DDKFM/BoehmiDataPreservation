package de.ddkfm.models

data class Gif(
    val url : String,
    val tweetUrl : String,
    val user : String,
    val keywords : List<String>
)
