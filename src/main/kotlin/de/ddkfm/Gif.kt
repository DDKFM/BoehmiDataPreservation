package de.ddkfm

data class Gif(
    val url : String,
    val keywords : List<String>
)

data class GifListResponse(
    val count : Long,
    val limit : Int,
    val offset : Int,
    val gifs : List<Gif>
)
