package de.ddkfm

import de.ddkfm.repositories.GifRepository
import de.ddkfm.repositories.LuceneIndex
import kong.unirest.Unirest
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import twitter4j.Paging

@Component
class DataInitializer : CommandLineRunner {
    override fun run(vararg args: String) {
        Twitter.doWithTwitter { indexTweetsFromUser("vera_meleena") }
    }

    fun twitter4j.Twitter.indexTweetsFromUser(user : String) {
        var page = 1
        var nullCounter = 0;
        while(true) {
            try {
                val timeline = this.timelines().getUserTimeline(user, Paging(page, 500))
                val tweets = timeline
                    .asSequence()
                    .dropWhile { it.isRetweet }//retweets raus
                    .filter { tweet -> tweet.mediaEntities.isNotEmpty() }//leere Entities raus
                    .filter { tweet -> tweet.mediaEntities.any { it.type == "animated_gif" } }//nur gifs bitte
                    .toList()
                println("Tweets fetched: ${tweets.size}")

                for(tweet in tweets) {
                    if(GifRepository.findById(tweet.id) != null)
                        continue
                    val tweetUrl = tweet.mediaEntities
                        .map { it.videoVariants.map { it.url } }
                        .flatten()
                        .first()
                    val bytes = tweetUrl.let { Unirest.get(it).asBytes().body }
                    GifRepository.storeGif(tweet.id, bytes)
                    val existing = LuceneIndex.searchForId(tweet.id)
                    if(existing == null) {
                        println(tweet.id)
                        val hash = DigestUtils.sha256Hex(bytes)
                        LuceneIndex.addOrUpdate(tweet.id, mapOf(
                            "keywords" to "neomagazin",
                            "tweet" to tweet.text,
                            "hash" to hash,
                            "user" to tweet.user.screenName,
                            "twitterUrl" to tweet.mediaEntities.map { it.expandedURL }.first()

                        ))
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
