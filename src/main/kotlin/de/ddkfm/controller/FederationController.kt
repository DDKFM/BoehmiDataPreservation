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
class FederationController {
    @GetMapping("/federations")
    fun getFederatedSystem() : ResponseEntity<List<FederatedSystem>> {
        return ok(DataConfiguration.config.federation.systems.map { FederatedSystem(it.id, it.url, it.name) })
    }
}
