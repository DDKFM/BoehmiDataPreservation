package de.ddkfm

import kong.unirest.Unirest
import twitter4j.Paging
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.io.File

fun main(args : Array<String>) {
    val gifDir = File("gifs")
    if(!gifDir.exists())
        gifDir.mkdirs();
    //twitter.getAllGifURLsFromUser("vera_meleena")
    //twitter.getAllGifURLsFromUser("LeyLaDona")

    //val result = twitter.tweetsWithText("¯\\_(ツ)_/¯")
    //println("$result (${result.second / result.first.toDouble() * 100}%)")

}

fun Twitter.tweetsWithText(text : String) : Pair<Int, Int>{
    var page = 1
    var replyCount = 0
    var replyCountWithText = 0
    while(true) {
        try {
            val timeline = this.timelines().getUserTimeline("neomagazin", Paging(page, 500))
            val replies = timeline.filter { it.inReplyToStatusId > -1 }.map { it.id to it.text }
            val repliesWithText = replies.filter { it.second.contains(text) }
            println(repliesWithText)
            replyCount += replies.size
            replyCountWithText += repliesWithText.size
            println("page: $page, total: ${replies.size}, with $text: ${repliesWithText.size}")
            if(page == 20)
                break
            page++
        } catch (e: Exception) {
            e.printStackTrace()
            break;
        }
    }
    return replyCount to replyCountWithText
}

val stopWords = File("german-stopwords.txt").readLines().map { it.substringBefore("|").trim() }.distinct()
fun Twitter.wordsInReplys() {
    println(stopWords)
    var page = 1
    val words = mutableMapOf<String, Int>()
    while(true) {
        try {
            val timeline = this.timelines().getUserTimeline("janboehm", Paging(page, 500))
            val replies = timeline
                .map { it.text.toLowerCase().split(" ") }
                .flatten()
                .filter { it !in stopWords }
            val singleWords = replies.groupBy { it }.map { it.key to it.value.size }.toMap()
            for((k, v) in singleWords) {
                if(words.containsKey(k))
                    words[k] = words[k]?.plus(v) ?: 0
                else
                    words[k] = 0
            }
            if(page == 20)
                break
            page++
        } catch (e: Exception) {
            e.printStackTrace()
            break;
        }
    }
    val sortedWithLimit = words.toList().sortedByDescending { it.second }.take(20)
    sortedWithLimit.forEach { entry ->
        println("${entry.first}: ${entry.second}")
    }
}


fun Twitter.getAllGifURLsFromUser(user : String) {
    var page = 1
    var nullCounter = 0;
    while(true) {
        try {
            val timeline = this.timelines().getUserTimeline(user, Paging(page, 500))
            val tweets = timeline
                .asSequence()
                .dropWhile { it.isRetweet }//retweets raus
                .map { tweet -> tweet.id to tweet.mediaEntities }//twitter id to mediaEntities
                .filter { it.second.isNotEmpty() }//leere Entities raus
                .filter { it.second.any { it.type == "animated_gif" } }//nur gifs bitte
                .map { it.first to it.second.map { it.videoVariants.map { it.url } } }//alle Variantes dazu
                .toList()
            println("Tweets fetched: ${tweets.size}")

            for(tweet in tweets) {
                val bytes = tweet.second.first().first().let { Unirest.get(it).asBytes().body }
                val file = File("gifs/${tweet.first}.mp4")
                file.parentFile.mkdirs()
                file.writeBytes(bytes)
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
