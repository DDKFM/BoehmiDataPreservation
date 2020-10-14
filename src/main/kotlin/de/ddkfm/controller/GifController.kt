package de.ddkfm.controller

import de.ddkfm.jpa.repos.GifRepository
import de.ddkfm.jpa.repos.TweetRepository
import de.ddkfm.models.GifResponse
import de.ddkfm.repositories.FileRepository
import de.ddkfm.repositories.LuceneRepository
import de.ddkfm.utils.create
import de.ddkfm.utils.toGifResponse
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import javax.servlet.http.HttpServletResponse


@RestController
@RequestMapping("/v1")
class GifController {

    @Autowired
    lateinit var gifRepo : GifRepository

    @Autowired
    lateinit var tweetRepo : TweetRepository

    @GetMapping("/gifs/{id}")
    fun getGifMetadata(@PathVariable("id") id: String) : ResponseEntity<GifResponse> {
        val gif = gifRepo.findById(id).orElse(null)
            ?: return notFound().build()
        return ok(gif.toGifResponse())
    }

    @GetMapping("/gifs/{id}/data")
    fun getGif(
        @PathVariable("id") id: String,
        @RequestParam("type", defaultValue = "video/mp4", required = false) type : String = "video/mp4") : ResponseEntity<StreamingResponseBody> {
        val gif = when(type) {
            "image/gif" -> FileRepository.findGifById(id)
            else -> FileRepository.findById(id)
        } ?: return notFound().build()
        val stream = StreamingResponseBody { out -> gif.use { IOUtils.copy(gif, out) } }
        return ok()
            .contentType(MediaType.parseMediaType("video/mp4"))
            .body(stream)
    }

    @PostMapping("/gifs/{id}/keywords")
    fun addKeywords(@PathVariable("id") id: String,
                    @RequestBody keywords : List<String>
    ) : ResponseEntity<GifResponse> {
        val gif = gifRepo.findById(id).orElse(null)
            ?: return notFound().build()
        gif.keywords.clear()
        gif.keywords.addAll(keywords)
        gifRepo.save(gif)
        return ok(gif.toGifResponse())
    }

    @DeleteMapping("/gifs/{id}")
    fun deleteGif(@PathVariable("id") id : String) : ResponseEntity<Boolean> {
        val gif = gifRepo.findById(id).orElse(null)
            ?: return notFound().build()
        gif.deleted = true
        gifRepo.save(gif)
        return ok(true)
    }
}
