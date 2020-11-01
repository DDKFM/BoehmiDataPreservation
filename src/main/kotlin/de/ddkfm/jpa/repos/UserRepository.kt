package de.ddkfm.jpa.repos

import de.ddkfm.jpa.models.Tweeter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<Tweeter, String> {
    fun findByUserId(userId : Long) : Optional<Tweeter>

    @Query("select distinct t.user from Tweet t where t.gif.deleted = false")
    fun findByNotDeleted() : List<Tweeter>
}
