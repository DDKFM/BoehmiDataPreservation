package de.ddkfm

import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.InputStream
import javax.servlet.http.HttpServletResponse


@RestController
@RequestMapping("/")
@Tag(name = "search")
class Static {


    @GetMapping("/")
    fun index(response : HttpServletResponse){
        val file = File("./index.html")
        response.contentType = "text/html"
        IOUtils.copy(file.inputStream(), response.outputStream)
    }

    @GetMapping("/index.html")
    fun indexFile(response : HttpServletResponse){
        val file = File("./index.html")
        response.contentType = "text/html"
        IOUtils.copy(file.inputStream(), response.outputStream)
    }

    @GetMapping("/index.js")
    fun js(response : HttpServletResponse){
        val file = File("./index.js")
        response.contentType = "application/javascript"
        IOUtils.copy(file.inputStream(), response.outputStream)
    }

}
