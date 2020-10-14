package de.ddkfm

import de.ddkfm.twitter.StreamListener
import twitter4j.*
import twitter4j.Twitter
import java.util.concurrent.locks.ReentrantLock

object Twitter {
    private val twitter = TwitterFactory.getSingleton()
    val rateLimitLock = ReentrantLock()
    private val streamConnection by lazy {
        TwitterStreamFactory.getSingleton()
    }

     fun <T> doWithTwitter(doThings : Twitter.() -> T) : T  {
        while(rateLimitLock.isLocked)
            Thread.sleep(500)
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
