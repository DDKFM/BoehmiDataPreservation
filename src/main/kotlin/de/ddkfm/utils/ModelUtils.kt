package de.ddkfm.utils

import de.ddkfm.Twitter
import de.ddkfm.models.Gif
import de.ddkfm.repositories.LuceneRepository
import org.apache.lucene.document.Document

fun Document.toGifMetaData() : Gif {
    val tweetId = this.get("twitterId").toLong()
    val otherTwitterUrls = this.get("sameTweetIds")
        .split(" ")
        .filter { it != "" }
        .map { tweetId ->
            val tweetId = tweetId.toLong()
            LuceneRepository.urlCache.get(tweetId) { tweetId.getTweetUrl()}
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
