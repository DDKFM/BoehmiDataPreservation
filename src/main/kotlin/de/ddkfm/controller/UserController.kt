package de.ddkfm.controller

import de.ddkfm.Twitter
import de.ddkfm.jpa.repos.UserRepository
import de.ddkfm.repositories.LuceneRepository
import de.ddkfm.models.TweeterResponse
import de.ddkfm.utils.toTwitterUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/v1")
class UserController {

    @Autowired
    lateinit var userRepo : UserRepository
    @GetMapping("/users")
    fun getUsers() : ResponseEntity<List<TweeterResponse>> {
        val users = userRepo.findByNotDeleted()
            .map { it.toTwitterUser() }
        return ok(users)
    }
}
