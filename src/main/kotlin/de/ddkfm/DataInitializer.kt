package de.ddkfm

import de.ddkfm.repositories.GifRepository
import de.ddkfm.repositories.LuceneRepository
import de.ddkfm.utils.*
import kong.unirest.Unirest
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import twitter4j.Paging
import java.util.concurrent.*
import kotlin.concurrent.thread

@Component
class DataInitializer : CommandLineRunner {
    override fun run(vararg args: String) {
        val scheduler = Executors.newScheduledThreadPool(1)
        val fetchingInterval = System.getenv("FETCHING_INTERVAL")?.toLongOrNull() ?: 15L
        scheduler.scheduleAtFixedRate({ scanTwitterUsers() }, 0, fetchingInterval, TimeUnit.MINUTES)
    }

    fun scanTwitterUsers() {
        println("convertAllGifs")
        GifRepository.convertAllFiles()
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
                        GifRepository.storeGif(tweet.id, bytes)
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
}
