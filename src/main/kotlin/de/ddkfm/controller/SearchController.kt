package de.ddkfm.controller

import de.ddkfm.repositories.LuceneRepository
import de.ddkfm.models.GifSearchResponse
import de.ddkfm.utils.toGifMetaData
import org.apache.lucene.document.Document
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/v1")
class SearchController {

    @GetMapping("/search")
    fun search(
        @RequestParam("query", defaultValue = "") query: String = "",
        @RequestParam("limit", defaultValue = "50") limit : Int = 50,
        @RequestParam("offset", defaultValue = "0") offset : Int = 0
    ): ResponseEntity<GifSearchResponse> {
        val documents = LuceneRepository.query("tweet:$query* OR keywords:$query* - deleted:true", limit, offset)
        val gifs = documents.content.map(Document::toGifMetaData)
        return ok(
            GifSearchResponse(
                count = documents.hits,
                limit = limit,
                offset = offset,
                gifs = gifs
            )
        )
    }

    @PostMapping("/searchByIds")
    fun searchByIds(
        @RequestBody(required = false) ids : List<String>?,
        @RequestParam("limit", defaultValue = "50") limit : Int = 50,
        @RequestParam("offset", defaultValue = "0") offset : Int = 0
    ) : ResponseEntity<GifSearchResponse> {
        val tweetSearch = ids?.joinToString(separator = " or ") { "twitterId: $it" } ?: ""
        val luceneQuery = "$tweetSearch - deleted:true"
        println(luceneQuery)
        val documents = LuceneRepository.query(luceneQuery, limit, offset)
        return ok(
            GifSearchResponse(
                count = documents.hits,
                limit = limit,
                offset = offset,
                gifs = documents.content.map(Document::toGifMetaData)
            )
        )
    }

}
