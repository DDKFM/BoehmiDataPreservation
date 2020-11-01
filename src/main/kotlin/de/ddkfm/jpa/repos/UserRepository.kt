package de.ddkfm.jpa.repos

import de.ddkfm.jpa.models.Tweeter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<Tweeter, String> {
    fun findByUserId(userId : Long) : Optional<Tweeter>

    @Query("select u from Tweet t inner join Tweeter u inner join Gif g where g.deleted = false")
    fun findByNotDeleted() : List<Tweeter>
}
