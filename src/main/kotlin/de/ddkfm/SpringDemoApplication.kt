package de.ddkfm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration

@SpringBootApplication
@Configuration
class SpringDemoApplication

fun main(args: Array<String>) {
    runApplication<SpringDemoApplication>(*args)
}
