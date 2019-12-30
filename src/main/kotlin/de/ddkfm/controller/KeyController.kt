package de.ddkfm.controller

import de.ddkfm.repositories.LuceneRepository
import de.ddkfm.models.GifSearchResponse
import de.ddkfm.utils.toGifMetaData
import org.apache.lucene.document.Document
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/v1/keywords")
class KeyController {

    @GetMapping("/top")
    fun getTopKeywords(@RequestParam("top") top: Int = 10): ResponseEntity<Map<String, Int>> {
        val documents = LuceneRepository.query("keywords:* - deleted:true", Integer.MAX_VALUE, 0)
        val keywords = documents.content.map { it["keywords"].split(" ") }.flatten()
        val topN = keywords
            .groupBy { it }
            .map { it.key to it.value.size }
            .take(top)
            .toMap()
        return ok(topN)
    }

}
