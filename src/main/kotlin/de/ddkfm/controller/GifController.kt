package de.ddkfm.controller

import com.fasterxml.jackson.databind.ObjectMapper
import de.ddkfm.configuration.DataConfiguration
import de.ddkfm.jpa.models.GifQueue
import de.ddkfm.jpa.repos.GifQueueRepository
import de.ddkfm.jpa.repos.GifRepository
import de.ddkfm.jpa.repos.TweetRepository
import de.ddkfm.models.GifRequest
import de.ddkfm.models.GifResponse
import de.ddkfm.repositories.FileRepository
import de.ddkfm.utils.sha256
import de.ddkfm.utils.toGifResponse
import kong.unirest.JsonNode
import kong.unirest.Unirest
import kong.unirest.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.time.LocalDateTime


@RestController
@RequestMapping("/v1")
class GifController {

    companion object {
        private const val FEDERATED_ID_HEADER = "X-FEDERATED-ID"
        private const val FEDERATED_SECRET_HEADER = "X-FEDERATED-SECRET"
    }
    @Autowired
    lateinit var gifRepo : GifRepository

    @Autowired
    lateinit var tweetRepo : TweetRepository

    @Autowired
    lateinit var queueRepo : GifQueueRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

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
    @PostMapping("/gifs")
    fun addGif(@RequestBody gif : GifRequest,
               @RequestHeader(FEDERATED_SECRET_HEADER, required = false) federatedSecret : String?,
               @RequestHeader(FEDERATED_ID_HEADER, required = false) federatedId : String?) : ResponseEntity<String> {

        var accepted = true
        if(federatedId != null && federatedSecret != null) {
            val federationSystem = DataConfiguration.config.federation.systems.firstOrNull { it.id == federatedId }
            if(federationSystem == null || federationSystem.secret != federatedSecret.sha256())
                return badRequest().build()
            accepted = true
        }

        val gifQueueEntry = GifQueue(tweetIds = gif.tweetIds.mapNotNull { it.toLongOrNull() }.toMutableList(),
            keywords = gif.keywords.toMutableList(),
            accepted = accepted,
            created = LocalDateTime.now()
        )
        queueRepo.save(gifQueueEntry)
        return ok("")
    }

    @PutMapping("/gifs/{id}")
    fun moveGif(@PathVariable("id") id: String,
                    @RequestBody federationSystemId : String
    ) : ResponseEntity<Boolean> {
        val gif = gifRepo.findById(id).orElse(null)
            ?: return notFound().build()
        try {
            val federationSystem = DataConfiguration.config.federation.systems.firstOrNull { it.id == federationSystemId }
                ?: return notFound().build()
            val tweetIds = gif.tweets.map { it.tweetId.toString() }
            val keywords = gif.keywords
            val gifRequest = objectMapper.writeValueAsString(GifRequest(tweetIds, keywords))
            val request = Request.Builder()
                .url("${federationSystem.url}/v1/gifs")
                .addHeader(FEDERATED_ID_HEADER, DataConfiguration.config.federation.id)
                .addHeader(FEDERATED_SECRET_HEADER, DataConfiguration.config.federation.secret)
                .post(gifRequest.toRequestBody("application/json".toMediaType()))
                .build()
            val response = OkHttpClient().newCall(request).execute()
            response.use {
                if(response.code != 200)
                    return badRequest().body(false)
            }
            val delete = deleteGif(id).body
                ?: return badRequest().build()
            return ok(delete)
        } catch (e : Exception) {
            throw e
        }
    }
}
