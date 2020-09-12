package de.ddkfm.controller

import de.ddkfm.models.Gif
import de.ddkfm.repositories.GifRepository
import de.ddkfm.repositories.LuceneRepository
import de.ddkfm.utils.create
import de.ddkfm.utils.toGifMetaData
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse


@RestController
@RequestMapping("/v1")
class GifController {

    @GetMapping("/gifs/{tweetId}")
    fun getGifMetadata(@PathVariable("tweetId") tweetId: Long) : ResponseEntity<Gif> {
        val document = LuceneRepository.searchForId(tweetId) ?: return notFound().build()
        return ok(document.toGifMetaData())
    }

    @GetMapping("/gifs/random")
    fun getRandomGif() : ResponseEntity<Gif> {
        val documents = LuceneRepository.query("tweet:* OR keywords:* - deleted:true", 1000, 0)
        val document = documents.content.random()
        return ok(document.toGifMetaData())
    }

    @GetMapping("/gifs/{tweetId}/data")
    fun getGif(@PathVariable("tweetId") tweetId: Long, response : HttpServletResponse) {
        val gif = GifRepository.findById(tweetId)
        response.contentType = "video/mp4"
        if(gif == null) return
        response.outputStream.write(gif.readBytes())
    }

    @GetMapping("/gifs/{tweetId}/gifdata")
    fun getGifAsRealGif(@PathVariable("tweetId") tweetId: Long, response : HttpServletResponse) {
        val document = LuceneRepository.searchForId(tweetId)
        val gif = GifRepository.findGifById( tweetId)
        response.contentType = "image/gif"
        if(gif == null) return
        response.outputStream.write(gif.readBytes())
    }

    @PostMapping("/gifs/{tweetId}/keywords")
    fun addKeywords(@PathVariable("tweetId") tweetId: Long,
                    @RequestBody keywords : List<String>
    ) : ResponseEntity<String> {
        LuceneRepository.searchForId(tweetId)
            ?: return badRequest().build()
        LuceneRepository.update(tweetId) {
            create("keywords", keywords.joinToString(separator = " "))
        }
        return ok("")
    }

    @DeleteMapping("/gifs/{tweetId}")
    fun deleteGif(@PathVariable("tweetId") tweetId : Long) : ResponseEntity<Boolean> {
        LuceneRepository.searchForId(tweetId)
            ?: return badRequest().build()
        return ok(
            LuceneRepository.delete(tweetId)?.get("deleted")?.contentEquals("true") != null
        )
    }
}
