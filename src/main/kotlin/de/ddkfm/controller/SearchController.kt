package de.ddkfm.controller

import de.ddkfm.repositories.LuceneIndex
import de.ddkfm.models.Gif
import de.ddkfm.models.GifSearchResponse
import de.ddkfm.utils.toGifMetaData
import org.apache.lucene.document.Document
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/v1")
class SearchController {

    @GetMapping("/search")
    fun search(
        @RequestParam("query", defaultValue = "") query: String = "",
        @RequestParam("limit", defaultValue = "50") limit : Int = 50,
        @RequestParam("offset", defaultValue = "0") offset : Int = 0
    ): ResponseEntity<GifSearchResponse> {
        val documents = LuceneIndex.query("tweet:*$query OR keywords:*$query", limit, offset)
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

}
