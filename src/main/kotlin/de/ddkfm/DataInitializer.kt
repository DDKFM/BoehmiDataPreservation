package de.ddkfm

import de.ddkfm.configuration.DataConfiguration
import de.ddkfm.jpa.models.Gif
import de.ddkfm.jpa.models.Tweet
import de.ddkfm.jpa.models.Tweeter
import de.ddkfm.jpa.repos.GifRepository
import de.ddkfm.jpa.repos.TweetRepository
import de.ddkfm.jpa.repos.UserRepository
import de.ddkfm.repositories.FileRepository
import de.ddkfm.repositories.LuceneRepository
import de.ddkfm.utils.*
import org.apache.commons.codec.digest.DigestUtils
import org.apache.lucene.document.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import twitter4j.MediaEntity
import twitter4j.Status
import twitter4j.TwitterException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

@Component
class DataInitializer : CommandLineRunner {

    @Autowired
    lateinit var gifRepo : GifRepository

    @Autowired
    lateinit var tweetRepo : TweetRepository

    @Autowired
    lateinit var userRepo : UserRepository

    override fun run(vararg args: String) {
        val users = DataConfiguration.config.following.users
        Twitter.stream(users) { downloadContent(this)}
        if(DataConfiguration.config.locations.lucene != null)
            migrateLuceneToHibernate()
    }
    fun downloadContent(status : Status) {
        if(status.mediaEntities.any { it.type == "animated_gif" }) {
            status.toTweet()
        }
    }

    private fun migrateLuceneToHibernate() {
        val documents = LuceneRepository.query("*:*", Integer.MAX_VALUE, 0)
        val pool = Executors.newFixedThreadPool(10)
        Twitter.doWithTwitter {
            for (document in documents.content) {
                pool.submit {
                    val tweetId = document.get("twitterId").toLong()
                    val otherIds = document.get("sameTweetIds").split(" ")
                        .mapNotNull { it.toLongOrNull() }
                    otherIds.plus(tweetId).forEach { id ->
                        this.migrateStatus(id, document)
                    }
                }
            }
        }
        println(documents.hits)
    }

    private fun twitter4j.Twitter.migrateStatus(tweetId : Long, document : Document) {
        if (tweetRepo.findByTweetId(tweetId).isPresent) {
            println("skip $tweetId")
            return
        }
        val status = try {
            this.showStatus(tweetId)
        } catch (e: TwitterException) {
            println("$tweetId (${document.get("user")})" + e.message)
            null
        } ?: return

        val keywords = document.get("keywords").split(" ")
            .filter { it.isNotEmpty() }
            .filter { it != "neomagazin" }
            .map { it.toLowerCase() }
            .distinct()
        val tweet = status.toTweet(keywords)
            ?: return
        val gifId = tweet.gif.id ?: return
    }

    fun Status.toUser() : Tweeter {
        return userRepo.findByUserId(this.user.id)
            .orElseGet {
                val user = Tweeter(
                    userId = this.user.id,
                    name = this.user.name,
                    screenName = this.user.screenName,
                    profileImage = this.user.get400x400ProfileImageURLHttps()
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
    fun ByteArray.sha256() : String {
        return DigestUtils.sha256Hex(this)
    }
    fun Status.toGif( keywords : List<String> = emptyList()) : Gif? {
        val byteArray = this.download()
            ?: return null
        val hash = byteArray.sha256()
        val gif = Optional.ofNullable(gifRepo.findByHash(hash).firstOrNull())
            .orElseGet {
                val gif = Gif(
                    posterUrl = mediaEntities.map { it.mediaURLHttps}.first(),
                    mediaUrl = mediaEntities.map { media ->
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
    fun Status.toTweet(keywords : List<String> = emptyList()) : Tweet? {
        return tweetRepo.findByTweetId(this.id)
            .orElseGet {
                val gif = this.toGif(keywords = keywords)
                    ?: return@orElseGet null
                val tweet = Tweet(
                    tweetId = this.id,
                    user = this.toUser(),
                    createdAt = this.createdAt.toLocalDateTime(),
                    deletedOnTwitter = false,
                    gif = gif,
                    text = this.text,
                    hashtags = this.hashtagEntities.map { it.text.toLowerCase() }.distinct().toMutableList()
                )
                tweetRepo.save(tweet)
                tweet
            }
    }
}
