package de.ddkfm.utils

import de.ddkfm.jpa.models.Gif
import de.ddkfm.jpa.models.Tweet
import de.ddkfm.jpa.models.Tweeter
import de.ddkfm.models.GifResponse
import de.ddkfm.models.TweeterResponse
import de.ddkfm.models.UserResponse

fun Gif.toGifResponse() : GifResponse {
    val firstTweet = tweets.first()
    return GifResponse(
        url = "/v1/gifs/$id",
        keywords = this.keywords,
        user = UserResponse(
            name = firstTweet.user.name,
            screenName = firstTweet.user.screenName
        ),
        tweetUrls = tweets.map { it.getTweetURL() },
        posterUrl = this.posterUrl
    )
}
fun Tweet.getTweetURL() : String {
    return "https://twitter.com/${user.screenName}/status/${tweetId}"
}

fun Tweeter.toTwitterUser() : TweeterResponse {
    return TweeterResponse(
        username = this.screenName,
        name = this.name,
        accountLink = "https://twitter.com/${this.screenName}",
        profileImage = this.profileImage
    )
}
