package de.ddkfm

import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.InputStream
import javax.servlet.http.HttpServletResponse


@RestController
@RequestMapping("/v1")
@Tag(name = "search")
class Search {

    @GetMapping("/search")
    fun search(
        @RequestParam("query", defaultValue = "") query: String = "",
        @RequestParam("limit", defaultValue = "50") limit : Int = 50,
        @RequestParam("offset", defaultValue = "0") offset : Int = 0
    ): ResponseEntity<GifListResponse> {
        val documents = LuceneIndex.query("tweet:*$query", limit, offset)
        val gifs = documents.content.map { document ->
            return@map Gif(
                url = "/v1/gifs/${document.get("twitterId")}",
                keywords = document.get("keywords").split(" ")
            )
        }
        return ok(
            GifListResponse(
                count = documents.hits,
                limit = limit,
                offset = offset,
                gifs = gifs
        ))
    }

    @GetMapping("/gifs/{tweetId}")
    fun getGifMetadata(@PathVariable("tweetId") tweetId: Long) : ResponseEntity<Gif> {
        val document = LuceneIndex.searchForId(tweetId) ?: return notFound().build()
        return ok(Gif(
            url = "/v1/gifs/${document.get("twitterId")}",
            keywords = document.get("keywords").split(" ")
        ))
    }

    @GetMapping("/gifs/{tweetId}/data")
    fun getGif(@PathVariable("tweetId") tweetId: Long, response : HttpServletResponse) {
        val document = LuceneIndex.searchForId(tweetId)
        val gif = File("./gifs/$tweetId.mp4")
        response.contentType = "video/mp4"
        IOUtils.copy(gif.inputStream(), response.outputStream)
    }

    @PostMapping("/gifs/{tweetId}")
    fun addKeywords(@PathVariable("tweetId") tweetId: Long,
                    @RequestBody keywords : List<String>
    ) : ResponseEntity<Gif> {
        var document = LuceneIndex.searchForId(tweetId) ?: return badRequest().build()
        val existingKeywords = document.get("keywords").split(" ")
        val newKeywords = existingKeywords.union(keywords)
        document = LuceneIndex.addOrUpdate(tweetId, mapOf("keywords" to newKeywords.joinToString(separator = " ")))
            ?: return badRequest().build()
        return ok(Gif(
            url = "/v1/gifs/${document.get("twitterId")}",
            keywords = document.get("keywords").split(" ")
        ))
    }

}
