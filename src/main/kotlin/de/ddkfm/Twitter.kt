package de.ddkfm

import twitter4j.RateLimitStatusEvent
import twitter4j.RateLimitStatusListener
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.util.concurrent.locks.ReentrantLock

object Twitter {
    private val twitter = TwitterFactory.getSingleton()
    val rateLimitLock = ReentrantLock()
    val listener = twitter.addRateLimitStatusListener(object : RateLimitStatusListener {
        override fun onRateLimitReached(p0: RateLimitStatusEvent?) {
            println(p0)
        }

        override fun onRateLimitStatus(p0: RateLimitStatusEvent?) {
            val remaining = p0?.rateLimitStatus?.remaining ?: return
            println("rateLimit: $remaining")
            if(remaining < 10) {
                rateLimitLock.lock()
            } else if(rateLimitLock.isLocked) {
                rateLimitLock.unlock()
            }
        }

    })

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
}
