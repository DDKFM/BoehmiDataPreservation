package de.ddkfm.models

data class GifResponse(
    val url : String,
    val posterUrl : String?,
    val tweetUrls : List<String>,
    val keywords : List<String>
)
