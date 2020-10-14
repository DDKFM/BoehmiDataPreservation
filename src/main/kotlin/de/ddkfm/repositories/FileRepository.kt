package de.ddkfm.repositories

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import java.io.File
import java.io.InputStream

object FileRepository {
    private val gifsLocation = File(System.getenv("GIF_LOCATION") ?: "./gifs")
    private val videoLocation = File(System.getenv("VIDEO_LOCATION") ?: "./videos")
    init {
        if(!gifsLocation.exists())
            gifsLocation.mkdirs()
        if(!videoLocation.exists())
            videoLocation.mkdirs()
    }

    fun findById(tweetId : String) : InputStream? {
        val gif = File(videoLocation,"$tweetId.mp4")
        return if(gif.exists())
            gif.inputStream()
        else
            null
    }

    fun findGifById(tweetId : String) : InputStream? {
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
    fun existsByGifId(gifId : String) : Boolean {
        val videoFile = File(videoLocation, "$gifId.mp4")
        val gifFile = File(gifsLocation, "$gifId.gif")
        return videoFile.exists() && gifFile.exists()
    }
    fun convertToGif(videoFile : File, gifFile : File) {
        println("convertFile $videoFile")
        val ffmpeg = FFmpeg("/usr/bin/ffmpeg")
        val ffprobe = FFprobe("/usr/bin/ffprobe")

        val builder = FFmpegBuilder()
            .setInput(videoFile.absolutePath)
            .addExtraArgs("-r", "15")
            .overrideOutputFiles(true)
            .addOutput(gifFile.absolutePath)
            .setFormat("gif")
            .done()
        val executor = FFmpegExecutor(ffmpeg, ffprobe)
        executor.createJob(builder).run()
    }

    fun storeGif(gifId: String?, data : ByteArray) {
        if(gifId == null)
            return
        val videoFile = File(videoLocation, "$gifId.mp4")
        if(!videoFile.exists()) {
            videoFile.writeBytes(data)
        }
        val gifFile = File(gifsLocation, "$gifId.gif")
        if(!gifFile.exists()) {
            convertToGif(videoFile, gifFile)
        }
    }
}
