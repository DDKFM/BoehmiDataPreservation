package de.ddkfm.utils

import de.ddkfm.models.Gif
import de.ddkfm.models.TwitterUser
import de.ddkfm.repositories.LuceneRepository
import org.apache.lucene.document.Document
import twitter4j.User

fun Document.toGifMetaData() : Gif {
    val tweetId = this.get("twitterId").toLong()
    val otherTwitterUrls = this.get("sameTweetIds")
        .split(" ")
        .filter { it != "" }
        .map { id ->
            val tweetIdInt = id.toLong()
            LuceneRepository.urlCache.get(tweetIdInt) { tweetIdInt.getTweetUrl()}
        }
    return Gif(
        url = "/v1/gifs/$tweetId",
        keywords = this.get("keywords").split(" "),
        user = this.get("user"),
        tweetUrl = this.get("twitterUrl"),
        otherTweetUrls = otherTwitterUrls,
        posterUrl = LuceneRepository.posterCache.get(tweetId) { tweetId.getPosterUrl()}
    )
}


fun User.toTwitterUser() : TwitterUser {
    return TwitterUser(
        username = this.screenName,
        name = this.name,
        description = this.description,
        accountLink = "https://twitter.com/${this.screenName}",
        follower = this.followersCount,
        profileImage = this.get400x400ProfileImageURLHttps()

    )
}
