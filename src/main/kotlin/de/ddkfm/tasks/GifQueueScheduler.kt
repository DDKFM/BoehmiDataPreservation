package de.ddkfm.tasks

import de.ddkfm.Twitter
import de.ddkfm.jpa.repos.GifQueueRepository
import de.ddkfm.jpa.repos.GifRepository
import de.ddkfm.jpa.repos.TweetRepository
import de.ddkfm.jpa.repos.UserRepository
import de.ddkfm.utils.StatusUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.transaction.Transactional

@Component
class GifQueueScheduler {
    @Autowired
    lateinit var queueRepo : GifQueueRepository

    @Autowired
    lateinit var statusUtils : StatusUtils

    @Scheduled(fixedRate = 30000)
    @Transactional
    fun run() {
        val gifs = queueRepo.findByAcceptedTrue(PageRequest.of(0, 20, Sort.by("created").ascending()))
        if(gifs.isEmpty())
            return
        for(gif in gifs) {
            Twitter.doWithTwitter {
                for(tweetId in gif.tweetIds) {
                    val status = showStatus(tweetId)
                    val tweet = statusUtils.toTweet(status, keywords = gif.keywords)
                    if(tweet != null)
                        queueRepo.delete(gif)
                }
            }
        }
    }
}
