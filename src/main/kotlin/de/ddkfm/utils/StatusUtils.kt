package de.ddkfm.utils

import de.ddkfm.jpa.models.Gif
import de.ddkfm.jpa.models.Tweet
import de.ddkfm.jpa.models.Tweeter
import de.ddkfm.jpa.repos.GifRepository
import de.ddkfm.jpa.repos.TweetRepository
import de.ddkfm.jpa.repos.UserRepository
import de.ddkfm.repositories.FileRepository
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import twitter4j.Status
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Component
class StatusUtils {
    @Autowired
    lateinit var gifRepo : GifRepository

    @Autowired
    lateinit var tweetRepo : TweetRepository

    @Autowired
    lateinit var userRepo : UserRepository

    @Autowired
    lateinit var statusUtils : StatusUtils

    fun toUser(status : Status) : Tweeter {
        return userRepo.findByUserId(status.user.id)
            .orElseGet {
                val user = Tweeter(
                    userId = status.user.id,
                    name = status.user.name,
                    screenName = status.user.screenName,
                    profileImage = status.user.get400x400ProfileImageURLHttps()
                )
                userRepo.save(user)
                user
            }
    }
    fun Date.toLocalDateTime() : LocalDateTime {
        return this.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
    }
    fun toGif(status : Status, keywords : List<String> = emptyList()) : Gif? {
        val byteArray = status.download()
            ?: return null
        val hash = byteArray.sha256()
        val gif = Optional.ofNullable(gifRepo.findByHash(hash).firstOrNull())
            .orElseGet {
                val gif = Gif(
                    posterUrl = status.mediaEntities.map { it.mediaURLHttps}.first(),
                    mediaUrl = status.mediaEntities.map { media ->
                        media.videoVariants.map { it.url }
                    }.flatten().first(),
                    deleted = false,
                    hash = hash,
                    keywords = keywords.toMutableList()
                )
                gifRepo.save(gif)
                gif
            }
        val gifId = gif.id ?: return null
        if(!FileRepository.existsByGifId(gifId)) {
            FileRepository.storeGif(gifId, byteArray)
        }
        return gif
    }
    fun toTweet(status : Status, keywords : List<String> = emptyList()) : Tweet? {
        return tweetRepo.findByTweetId(status.id)
            .orElseGet {
                val gif = toGif(status, keywords = keywords)
                    ?: return@orElseGet null
                val tweet = Tweet(
                    tweetId = status.id,
                    user = toUser(status),
                    createdAt = status.createdAt.toLocalDateTime(),
                    deletedOnTwitter = false,
                    gif = gif,
                    text = status.text,
                    hashtags = status.hashtagEntities.map { it.text.toLowerCase() }.distinct().toMutableList()
                )
                tweetRepo.save(tweet)
                tweet
            }
    }
}

fun ByteArray.sha256() : String {
    return DigestUtils.sha256Hex(this)
}
fun String.sha256() : String {
    return DigestUtils.sha256Hex(this.toByteArray())
}
