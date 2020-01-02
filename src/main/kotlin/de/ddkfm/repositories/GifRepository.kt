package de.ddkfm.repositories

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import java.io.File
import java.io.InputStream

object GifRepository {
    private val gifsLocation = File(System.getenv("GIF_LOCATION") ?: "./gifs")
    private val videoLocation = File(System.getenv("VIDEO_LOCATION") ?: "./videos")
    init {
        if(!gifsLocation.exists())
            gifsLocation.mkdirs()
        if(!videoLocation.exists())
            videoLocation.mkdirs()
    }

    fun findById(tweetId : Long) : InputStream? {
        val gif = File(videoLocation,"$tweetId.mp4")
        return if(gif.exists())
            gif.inputStream()
        else
            null
    }

    fun findGifById(tweetId : Long) : InputStream? {
        val gif = File(gifsLocation,"$tweetId.gif")
        return if(gif.exists())
            gif.inputStream()
        else
            null
    }
    fun convertAllFiles() {
        val files = videoLocation.listFiles()
        for(file in files) {
            val id = file.name.replace(".mp4", "").toLongOrNull()
                ?: continue
            val gif = File(gifsLocation,"$id.gif")
            if(gif.exists())
                continue
            convertToGif(file, gif)
        }
    }
    fun convertToGif(videoFile : File, gifFile : File) {
        println("convertFile $videoFile")
        val ffmpeg = FFmpeg("/usr/bin/ffmpeg")
        val ffprobe = FFprobe("/usr/bin/ffprobe")

        val builder = FFmpegBuilder()
            .setInput(videoFile.absolutePath)
            .overrideOutputFiles(true)
            .addOutput(gifFile.absolutePath)
            .setFormat("gif")
            .done()
        val executor = FFmpegExecutor(ffmpeg, ffprobe)
        executor.createJob(builder).run()
    }

    fun storeGif(tweetId: Long, data : ByteArray) {
        val videoFile = File(videoLocation,"$tweetId.mp4")
        videoFile.writeBytes(data)
        convertToGif(videoFile, File(gifsLocation,"$tweetId.gif"))
    }
}
