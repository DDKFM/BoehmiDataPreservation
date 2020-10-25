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

    @Autowired
    lateinit var statusUtils : StatusUtils

    override fun run(vararg args: String) {
        val users = DataConfiguration.config.following.users
        Twitter.stream(users) { downloadContent(this)}
        if(DataConfiguration.config.locations.lucene != null)
            migrateLuceneToHibernate()
    }
    fun downloadContent(status : Status) {
        if(status.mediaEntities.any { it.type == "animated_gif" }) {
            statusUtils.toTweet(status)
        }
    }

    private fun migrateLuceneToHibernate() {
        val documents = LuceneRepository.query("deleted:false", Integer.MAX_VALUE, 0)
        val pool = Executors.newFixedThreadPool(1)
        println("EXECUTE WITH 1 THREAD")
        for (document in documents.content) {
            pool.submit {
                val tweetId = document.get("twitterId").toLong()
                val otherIds = document.get("sameTweetIds").split(" ")
                    .mapNotNull { it.toLongOrNull() }
                otherIds.plus(tweetId).forEach { id ->
                    migrateStatus(id, document)
                }
            }
        }
        println(documents.hits)
    }

    private fun migrateStatus(tweetId : Long, document : Document) {
        Twitter.doWithTwitter {
            if (tweetRepo.findByTweetId(tweetId).isPresent) {
                println("skip $tweetId")
                return@doWithTwitter
            }
            val status = retryOnRateLimit { this.showStatus(tweetId) }
                ?: return@doWithTwitter
            val keywords = document.get("keywords").split(" ")
                .filter { it.isNotEmpty() }
                .filter { it != "neomagazin" }
                .map { it.toLowerCase() }
                .distinct()
            val tweet = statusUtils.toTweet(status, keywords)
                ?: return@doWithTwitter
            val gifId = tweet.gif.id ?: return@doWithTwitter
        }
    }

    fun <T> twitter4j.Twitter.retryOnRateLimit(block : twitter4j.Twitter.() -> T) : T? {
        return try {
            this.block()
        } catch (e: TwitterException) {
            if(e.errorCode == 88) {
                var remaining = this.rateLimitStatus["/statuses/show/:id"]!!.remaining
                println(remaining)
                while(remaining <= 10) {
                    Thread.sleep(10000)
                    remaining = this.rateLimitStatus["/statuses/show/:id"]!!.remaining
                }
                retryOnRateLimit(block)
            }
            null
        }
    }
}
