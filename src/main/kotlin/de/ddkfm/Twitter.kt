package de.ddkfm

import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

object Twitter {
    private val twitter = TwitterFactory.getSingleton()

    fun <T> doWithTwitter(doThings : Twitter.() -> T) : T  {
        return twitter.doThings()
    }
}
