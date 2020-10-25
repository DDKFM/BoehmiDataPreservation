package de.ddkfm

import de.ddkfm.configuration.DataConfiguration
import de.ddkfm.twitter.StreamListener
import twitter4j.*
import twitter4j.Twitter
import twitter4j.conf.Configuration
import twitter4j.conf.ConfigurationBuilder
import java.util.concurrent.locks.ReentrantLock

object Twitter {
    private val configuration = ConfigurationBuilder()
        .setGZIPEnabled(true)
        .setOAuthAccessToken(DataConfiguration.config.twitter.accessToken)
        .setOAuthAccessTokenSecret(DataConfiguration.config.twitter.accessTokenSecret)
        .setOAuthConsumerKey(DataConfiguration.config.twitter.consumerKey)
        .setOAuthConsumerSecret(DataConfiguration.config.twitter.consumerKeySecret)
        .build()
    private val twitter by lazy {TwitterFactory(configuration).instance }
    private val streamConnection by lazy { TwitterStreamFactory(configuration).instance }

     fun <T> doWithTwitter(doThings : Twitter.() -> T) : T  {
         return twitter.doThings()
    }
    fun <T> doTryWithTwitter(doThings : Twitter.() -> T) : T? {
        return try {
            this.doWithTwitter(doThings)
        } catch (e : Exception) {
            null
        }
    }
    fun stream(users : List<String> = emptyList(), streamFunction : Status.() -> Unit) {
        val userIds = doWithTwitter { users.map { showUser(it).id } }
        with(streamConnection) {
            this.addListener(StreamListener(streamFunction))
            val query = FilterQuery().apply {
                if(userIds.isNotEmpty())
                    follow(*userIds.toLongArray())
            }
            this.filter(query)
        }
    }
}
