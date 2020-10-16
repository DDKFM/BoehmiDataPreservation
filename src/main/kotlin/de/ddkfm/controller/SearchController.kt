package de.ddkfm.controller

import de.ddkfm.jpa.repos.GifRepository
import de.ddkfm.jpa.repos.TweetRepository
import de.ddkfm.models.GifResponse
import de.ddkfm.repositories.LuceneRepository
import de.ddkfm.models.GifSearchResponse
import de.ddkfm.utils.toGifResponse
import org.apache.lucene.document.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping("/v1")
class SearchController {

    @Autowired
    lateinit var gifRepo : GifRepository

    @Autowired
    lateinit var tweetRepo : TweetRepository

    @GetMapping("/search")
    fun search(
        @RequestParam("query", defaultValue = "") query: String = "",
        @RequestParam("limit", defaultValue = "50") limit : Int = 50,
        @RequestParam("offset", defaultValue = "0") offset : Int = 0
    ): ResponseEntity<GifSearchResponse> {
        val pageRequest = PageRequest.of(offset / limit, limit)
        val gifs = if(query.isEmpty())
            gifRepo.findByDeletedFalse(pageRequest)
        else
            gifRepo.findByKeywordsContains(mutableListOf(query), pageRequest)
        return ok(
            GifSearchResponse(
                count = gifs.totalElements,
                limit = limit,
                offset = offset,
                gifs = gifs.content.map { it.toGifResponse() }
            )
        )
    }

    @GetMapping("/searchByUser")
    fun searchByUser(
        @RequestParam("username", defaultValue = "") username: String = "",
        @RequestParam("limit", defaultValue = "50") limit : Int = 50,
        @RequestParam("offset", defaultValue = "0") offset : Int = 0
    ): ResponseEntity<GifSearchResponse> {
        val gifs = gifRepo.findByUserName(username, PageRequest.of(offset, limit))
        return ok(
            GifSearchResponse(
                count = gifs.size.toLong(),
                limit = limit,
                offset = offset,
                gifs = gifs.map { it.toGifResponse() }
            )
        )
    }

    @PostMapping("/searchByIds")
    fun searchByIds(
        @RequestBody(required = false) ids : List<String>?,
        @RequestParam("limit", defaultValue = "50") limit : Int = 50,
        @RequestParam("offset", defaultValue = "0") offset : Int = 0
    ) : ResponseEntity<GifSearchResponse> {
        val tweetIds = ids?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        val gifs = if(tweetIds.isNotEmpty()) {
            tweetRepo.findByTweetIdIn(tweetIds, PageRequest.of(offset, limit))
                .map { it.gif }
                .distinctBy { it.id }
        } else {
            val ids = ids ?: emptyList()
            gifRepo.findByIdInAndDeletedFalse(ids, PageRequest.of(offset, limit))
        }
        return ok(
            GifSearchResponse(
                count = gifs.size.toLong(),
                limit = limit,
                offset = offset,
                gifs = gifs.map { it.toGifResponse() }
            )
        )
    }

}
