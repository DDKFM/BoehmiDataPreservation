package de.ddkfm.jpa.repos

import de.ddkfm.jpa.models.Gif
import de.ddkfm.jpa.models.Tweet
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

    @Query("SELECT g FROM Gif g JOIN Tweet t WHERE t.user.name = :userName")
    fun findByUserName(userName : String, pageable: Pageable) : List<Gif>

    fun findByDeletedFalse(pageable: Pageable) : List<Gif>

    @Query("SELECT k, count(k) FROM Gif g JOIN g.keywords k GROUP BY k")
    fun getTopKeywords(pageable: Pageable) : List<Array<Any>>

    @Query("SELECT COUNT(g) from Gif g where deleted = false")
    fun getCountWhereDeletedIsFalse() : Int

    @Query("""
        SELECT * 
        FROM gif g 
            left join tweet t on g.id = t.gif_id
            left join tweeter u on t.user_id = u.id
            left join tweet_hashtags h on h.tweet_id = t.id
            left join gif_keywords k on g.id = k.gif_id
        WHERE
            g.deleted = 0
        and (
                k.keywords like :keyword
            or
                h.hashtags like :keyword
        )
        LIMIT :limit
        OFFSET :offset
    """, nativeQuery = true)
    fun findByKeyword(
        @Param("keyword") keywords: String,
        @Param("limit") limit : Int,
        @Param("offset") offset : Int) : List<Gif>

    fun findByIdInAndDeletedFalse(id: List<String>, pageable: Pageable) : List<Gif>
}
