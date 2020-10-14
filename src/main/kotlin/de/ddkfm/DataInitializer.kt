package de.ddkfm

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
        //val scheduler = Executors.newScheduledThreadPool(1)
        //val fetchingInterval = System.getenv("FETCHING_INTERVAL")?.toLongOrNull() ?: 15L
        //scheduler.scheduleAtFixedRate({ scanTwitterUsers() }, 0, fetchingInterval, TimeUnit.MINUTES)

        //migrateLuceneToHibernate()
        /*
        Twitter.doWithTwitter {
            val status = this.showStatus(1306657293518467074)
            val tweet = status.toTweet()
            tweetRepo.save(tweet)
            println(tweet)
            FileRepository.storeGif(tweet.gif.id!!, status.download())
        }

         */
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


    /*
    fun scanTwitterUsers() {
        println("convertAllGifs")
        LuceneGifRepository.convertAllFiles()
        println("cache Twitter Users")
        val users = System.getenv("TWITTER_USERS")?.split(",") ?: emptyList()
        for(user in users) {
            thread {
                println("Cache user $user")
                Twitter.doWithTwitter { indexTweetsFromUser(user) }
            }
        }
    }

    fun twitter4j.Twitter.indexTweetsFromUser(user : String) {
        val userObj = this.users().showUser(user)
        LuceneRepository.userCache.put(user, userObj)
        var page = 1
        var nullCounter = 0
        while(true) {
            try {
                val timeline = this.timelines().getUserTimeline(user, Paging(page, 500))
                val tweets = timeline
                    .asSequence()
                    .dropWhile { it.isRetweet }//retweets raus
                    .filter { tweet -> tweet.mediaEntities.isNotEmpty() }//leere Entities raus
                    .filter { tweet -> tweet.mediaEntities.any { it.type == "animated_gif" } }//nur gifs bitte
                    .toList()
                thread {
                    println("Tweets fetched: ${tweets.size}")

                    for(tweet in tweets) {
                        val existing = LuceneRepository.searchForAnyId(tweet.id)
                        LuceneRepository.urlCache.get(tweet.id) { tweet.mediaEntities.map { it.expandedURL }.first() }
                        LuceneRepository.posterCache.get(tweet.id) {tweet.mediaEntities.map { it.mediaURLHttps }.first()}
                        if(existing != null)
                            continue//if the tweetId is already in the index, continue
                        val tweetUrl = tweet.mediaEntities
                            .map { it.videoVariants.map { it.url } }
                            .flatten()
                            .first()//fetch the mediaUrl
                        val bytes = tweetUrl?.download()//Download the GIF
                            ?: continue
                        val hash = DigestUtils.sha256Hex(bytes)
                        val mainDocument = LuceneRepository.searchForHash(hash)
                        if(mainDocument != null) {//any tweet with the same gif hash
                            LuceneRepository.update(mainDocument["twitterId"].toLong()) {  ->
                                appendOrCreate("text", tweet.text)
                                appendOrCreate("sameTweetIds", tweet.id)
                            }
                            println("duplicate GIF for tweetId ${mainDocument["twitterId"].toLong()} found")
                            continue
                        }
                        LuceneGifRepository.storeGif(tweet.id, bytes)
                        LuceneRepository.create(tweet.id) {
                            create("twitterId", tweet.id)
                            create("tweet", tweet.text)
                            create("hash", hash)
                            create("user", tweet.user.screenName)
                            create("twitterUrl", tweet.mediaEntities.map { it.expandedURL }.first())
                            create("keywords", "neomagazin")
                            create("sameTweetIds", "")
                            create("deleted", false)
                        }
                        println("tweet with id ${tweet.id} created (hash: $hash)")
                    }
                }
                if(tweets.isEmpty())
                    nullCounter++
                if(nullCounter > 5)
                    break;
                page++
            } catch (e: Exception) {
                e.printStackTrace()
                break
            }
        }
    }

     */
}
