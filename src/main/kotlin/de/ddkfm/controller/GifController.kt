package de.ddkfm.controller

import de.ddkfm.repositories.GifRepository
import de.ddkfm.repositories.LuceneIndex
import de.ddkfm.models.Gif
import de.ddkfm.utils.toGifMetaData
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse


@RestController
@RequestMapping("/v1")
class GifController {

    @GetMapping("/gifs/{tweetId}")
    fun getGifMetadata(@PathVariable("tweetId") tweetId: Long) : ResponseEntity<Gif> {
        val document = LuceneIndex.searchForId(tweetId) ?: return notFound().build()
        return ok(document.toGifMetaData())
    }

    @GetMapping("/gifs/{tweetId}/data")
    fun getGif(@PathVariable("tweetId") tweetId: Long, response : HttpServletResponse) {
        val document = LuceneIndex.searchForId(tweetId)
        val gif = GifRepository.findById(tweetId)
        response.contentType = "video/mp4"
        IOUtils.copy(gif, response.outputStream)
    }

    @PostMapping("/gifs/{tweetId}")
    fun addKeywords(@PathVariable("tweetId") tweetId: Long,
                    @RequestBody keywords : List<String>
    ) : ResponseEntity<Gif> {
        var document = LuceneIndex.searchForId(tweetId) ?: return badRequest().build()
        val existingKeywords = document.get("keywords").split(" ")
        val newKeywords = existingKeywords.union(keywords)
        document = LuceneIndex.addOrUpdate(
            tweetId,
            mapOf("keywords" to newKeywords.joinToString(separator = " "))
        )
            ?: return badRequest().build()
        return ok(document.toGifMetaData())
    }

}
