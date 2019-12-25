package de.ddkfm.models

data class Gif(
    val url : String,
    val tweetUrl : String,
    val otherTweetUrls : List<String>,
    val user : String,
    val keywords : List<String>
)
