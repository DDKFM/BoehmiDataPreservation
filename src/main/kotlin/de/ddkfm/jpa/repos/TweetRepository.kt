package de.ddkfm.jpa.repos

import de.ddkfm.jpa.models.Gif
import de.ddkfm.jpa.models.Tweet
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TweetRepository : JpaRepository<Tweet, String> {
    fun findByTweetId(id : Long) : Optional<Tweet>

    fun findByTweetIdIn(tweetId: List<Long>, pageable: Pageable) : List<Tweet>


    @Query("SELECT h, count(h) FROM Tweet t JOIN t.hashtags h GROUP BY h")
    fun getTopHashtags(pageable: Pageable) : List<Array<Any>>
}
