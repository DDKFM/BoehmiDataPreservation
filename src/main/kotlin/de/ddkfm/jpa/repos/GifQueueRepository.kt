package de.ddkfm.jpa.repos

import de.ddkfm.jpa.models.GifQueue
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface GifQueueRepository : PagingAndSortingRepository<GifQueue, String> {

    @Query("FROM GifQueue g WHERE g.accepted = true")
    fun findByAcceptedTrue(pageRequest: PageRequest) : List<GifQueue>
}
