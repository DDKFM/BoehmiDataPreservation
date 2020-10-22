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
        val tweet = statusUtils.toTweet(status, keywords)
            ?: return
        val gifId = tweet.gif.id ?: return
    }
}
