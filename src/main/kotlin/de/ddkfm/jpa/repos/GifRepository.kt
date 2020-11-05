package de.ddkfm.jpa.repos

import de.ddkfm.jpa.models.Gif
import de.ddkfm.jpa.models.Tweet
import de.ddkfm.models.GifResponse
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GifRepository : PagingAndSortingRepository<Gif, String> {

    fun findByHash(hash : String) : List<Gif>

    @Query("SELECT t.gif FROM Tweet t WHERE t.user.screenName = :userName and t.gif.deleted = false")
    fun findByUserName(userName : String, pageable: Pageable) : Page<Gif>
    fun findByDeletedFalse(pageable: Pageable) : Page<Gif>

    @Query("SELECT k, count(k) FROM Gif g JOIN g.keywords k GROUP BY k")
    fun getTopKeywords(pageable: Pageable) : List<Array<Any>>

    /*
    @Query("""
        SELECT *
        FROM gif g
            left join tweet t on g.id = t.gif_id
            left join tweeter u on t.user_id = u.id
            left join tweet_hashtags h on h.tweet_id = t.id
            left join gif_keywords k on g.id = k.gif_id
        WHERE
            g.deleted = false
        and (
                k.keywords like :keyword
            or
                h.hashtags like :keyword
        )
        LIMIT :limit
        OFFSET :offset
    """, nativeQuery = true)

     */
    @Query("SELECT g FROM Gif g LEFT JOIN g.keywords k LEFT JOIN g.tweets t LEFT JOIN t.user u WHERE g.deleted = false AND k in :keywords ORDER BY t.createdAt DESC ")
    fun findByKeywordsContains(
        keywords: MutableList<String>,
        pageable: Pageable
    ) : Page<Gif>

    @Query("""
        select 
            '/v1/gifs/' || g.id,
            g.poster_url, 
            STRING_AGG(distinct 'https://twitter.com/' || u.screen_name || '/statuses/' || t.tweet_id, ',') as tweet_urls,
            STRING_AGG(distinct k.keywords, ',') as blub,
            STRING_AGG(distinct h.hashtags, ',') as bla
        from tweet t 
        inner join gif g on t.gif_id = g.id 
        inner join tweeter u on t.user_id = u.id 
        left join gif_keywords k on k.gif_id = g.id 
        left join tweet_hashtags h on h.tweet_id = t.id
        where
            g.deleted = false
        and
            (
                g.id in (select gif_id from gif_keywords where keywords ilike :keyword)
                OR t.id in (select tweet_id from tweet_hashtags where hashtags ilike :keyword)
            )
        group by 
            g.id, g.poster_url
    """,
        countQuery = """
            select
                count(1) 
            from 
                tweet t
                inner join gif g on t.gif_id = g.id
                inner join tweeter u on t.user_id = u.id
                left join gif_keywords k on k.gif_id = g.id
                left join tweet_hashtags h on h.tweet_id = t.id
            where
                g.deleted = false
            and (
                k.keywords ilike :keyword
                OR h.hashtags ilike :keyword
            )
            group by
                g.id, g.poster_url
    """,
        nativeQuery = true)
    fun findByKeywordAndHashtag(keyword : String, pageable: Pageable) : Page<Array<Any?>>

    fun findByIdInAndDeletedFalse(id: List<String>, pageable: Pageable) : Page<Gif>

    @Query("select k from Gif g left join g.keywords k where g.deleted = false and lower(k) like %:filter group by k order by count(*) desc")
    fun getKeywords(filter : String, pageable: Pageable) : List<String>

    @Query("select h from Tweet t inner join t.hashtags h where t.gif.deleted = false and lower(h) like %:filter group by h order by count(*) desc")
    fun getHashtags(filter : String, pageable: Pageable) : List<String>

}
