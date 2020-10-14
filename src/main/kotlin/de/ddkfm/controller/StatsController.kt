package de.ddkfm.controller

import de.ddkfm.jpa.repos.GifRepository
import de.ddkfm.jpa.repos.TweetRepository
import de.ddkfm.repositories.LuceneRepository
import de.ddkfm.models.GifSearchResponse
import org.apache.lucene.document.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/v1/stats")
class StatsController {

    @Autowired
    lateinit var gifRepo : GifRepository

    @Autowired
    lateinit var tweetRepo : TweetRepository

    @GetMapping("/keywords")
    fun getTopKeywords(@RequestParam("top", defaultValue = "10") top: Int = 10): ResponseEntity<Map<String, Long>> {
        val topKeywords = gifRepo.getTopKeywords(PageRequest.of(0, top))
        val stats = topKeywords
            .map { it[0] as String to it[1] as Long }
            .toMap()
        return ok(stats)    
    }

    @GetMapping("/hashtags")
    fun getHashtags(@RequestParam("top", defaultValue = "10") top: Int = 10): ResponseEntity<Map<String, Long>> {
        val topKeywords = tweetRepo.getTopHashtags(PageRequest.of(0, top))
        val stats = topKeywords
            .map { it[0] as String to it[1] as Long }
            .toMap()
        return ok(stats)
    }

}
