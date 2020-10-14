package de.ddkfm.models

data class GifSearchResponse(
    val count : Long,
    val limit : Int,
    val offset : Int,
    val gifs : List<GifResponse>
)
