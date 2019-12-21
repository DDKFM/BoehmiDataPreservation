package de.ddkfm.utils

import de.ddkfm.models.Gif
import org.apache.lucene.document.Document

fun Document.toGifMetaData() : Gif {
    val tweetId = this.get("twitterId")
    return Gif(
        url = "/v1/gifs/$tweetId",
        keywords = this.get("keywords").split(" "),
        user = this.get("user"),
        tweetUrl = this.get("twitterUrl")
    )
}
