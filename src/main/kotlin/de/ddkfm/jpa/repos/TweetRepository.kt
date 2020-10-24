package de.ddkfm.jpa.repos

import de.ddkfm.jpa.models.Gif
import de.ddkfm.jpa.models.Tweet
import org.springframework.data.domain.Page
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

    @Query("SELECT t.gif FROM Tweet t LEFT JOIN t.gif.keywords k LEFT JOIN t.hashtags h LEFT JOIN t.user u WHERE t.gif.deleted = false AND (lower(k) like %:keywords% OR lower(h) like %:keywords%) ORDER BY t.createdAt DESC ")
    fun findByKeywordsContains(
        keywords: String,
        pageable: Pageable
    ) : Page<Gif>
}
