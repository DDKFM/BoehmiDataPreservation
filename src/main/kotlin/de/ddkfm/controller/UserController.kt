package de.ddkfm.controller

import de.ddkfm.Twitter
import de.ddkfm.repositories.GifRepository
import de.ddkfm.repositories.LuceneRepository
import de.ddkfm.models.Gif
import de.ddkfm.models.TwitterUser
import de.ddkfm.utils.appendOrCreate
import de.ddkfm.utils.create
import de.ddkfm.utils.toGifMetaData
import de.ddkfm.utils.toTwitterUser
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.web.bind.annotation.*
import twitter4j.User
import javax.servlet.http.HttpServletResponse


@RestController
@RequestMapping("/v1")
class UserController {

    @GetMapping("/users")
    fun getUsers() : ResponseEntity<List<TwitterUser>> {
        val users = System.getenv("TWITTER_USERS")?.split(",") ?: emptyList()
        val userObjects = users
            .map { LuceneRepository.userCache.get(it) { Twitter.getUser(it) } }
            .map { it.toTwitterUser() }
        return ok(userObjects)
    }
}
