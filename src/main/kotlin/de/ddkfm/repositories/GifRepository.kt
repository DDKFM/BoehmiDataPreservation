package de.ddkfm.repositories

import java.io.File
import java.io.InputStream

object GifRepository {
    private val gifsLocation = File(System.getenv("GIF_LOCATION") ?: "./gifs")
    init {
        if(!gifsLocation.exists())
            gifsLocation.mkdirs()
    }

    fun findById(tweetId : Long) : InputStream? {
        val gif = File(gifsLocation,"$tweetId.mp4")
        return if(gif.exists())
            gif.inputStream()
        else
            null
    }

    fun storeGif(tweetId: Long, data : ByteArray) {
        val gif = File(gifsLocation,"$tweetId.mp4")
        gif.writeBytes(data)
    }
}
