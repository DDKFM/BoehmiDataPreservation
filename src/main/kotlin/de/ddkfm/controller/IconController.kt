package de.ddkfm.controller

import com.fasterxml.jackson.databind.ObjectMapper
import de.ddkfm.configuration.DataConfiguration
import de.ddkfm.jpa.models.GifQueue
import de.ddkfm.jpa.repos.GifQueueRepository
import de.ddkfm.jpa.repos.GifRepository
import de.ddkfm.jpa.repos.TweetRepository
import de.ddkfm.models.FederatedSystem
import de.ddkfm.models.GifRequest
import de.ddkfm.models.GifResponse
import de.ddkfm.repositories.FileRepository
import de.ddkfm.utils.sha256
import de.ddkfm.utils.toGifResponse
import kong.unirest.JsonNode
import kong.unirest.Unirest
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
import java.io.File
import java.time.LocalDateTime


@RestController
@RequestMapping("/v1/icons")
class IconController {
    companion object {
        val AVAILABLE_FILENAMES = listOf(
            "16.png",
            "32.png",
            "70.png",
            "150.png",
            "196.png",
            "apple-touch-icon-152.png",
            "apple-touch-icon-167.png",
            "apple-touch-icon-180.png",
            "ultras.png"
        )
    }
    @GetMapping("/{filename}")
    fun getIcons(@PathVariable("filename") filename : String) : ResponseEntity<StreamingResponseBody> {
        if(filename !in AVAILABLE_FILENAMES)
            return badRequest().build()
        val iconsLocations = DataConfiguration.config.locations.icons
        val content = try {
            if(iconsLocations.startsWith("classpath:")) {
                val actualLocation = iconsLocations.replaceFirst("classpath:", "")
                this.javaClass.classLoader.getResourceAsStream("$actualLocation/$filename")
            } else {
                File(iconsLocations, filename).inputStream()

            }
        } catch (e : Exception) {
            e.printStackTrace()
            null
        }
            ?: return notFound().build()
        val responseBody = StreamingResponseBody {
            IOUtils.copy(content, it)
        }
        return ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(responseBody)
    }

    @GetMapping("/federations/{federationId}/{filename}")
    fun getIcons(
        @PathVariable("federationId") federationId : String,
        @PathVariable("filename") filename : String) : ResponseEntity<StreamingResponseBody> {
        if(filename !in AVAILABLE_FILENAMES)
            return badRequest().build()
        val system = DataConfiguration.config.federation.systems.firstOrNull { it.id == federationId }
            ?: return notFound().build()
        val request = Request.Builder()
            .url("${system.url}/v1/icons/$filename")
            .get()
            .build()
        val response = OkHttpClient().newCall(request).execute()
        val responseBody = StreamingResponseBody { out ->
            response.use {
                if(response.code == 200) {
                    IOUtils.copy(response.body?.byteStream(), out)
                }
            }
        }
        return ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(responseBody)
    }
}
