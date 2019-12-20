package de.ddkfm

import org.apache.commons.codec.digest.DigestUtils
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.io.File

@Component
class DataInitializer : CommandLineRunner {
    override fun run(vararg args: String) {

        val gifs = File("./gifs").listFiles()
        for(gif in gifs) {
            val tweetId = gif.name.replace(".mp4", "").toLong()
            val existing = LuceneIndex.searchForId(tweetId)
            if(existing == null) {
                println(tweetId)
                val tweet = Twitter.doWithTwitter { this.tweets().showStatus(tweetId) }
                val hash = DigestUtils.sha256Hex(gif.inputStream())
                LuceneIndex.addOrUpdate(tweetId, mapOf(
                    "keywords" to "",
                    "tweet" to tweet.text,
                    "hash" to hash
                ))
            }
        }

    }
}
