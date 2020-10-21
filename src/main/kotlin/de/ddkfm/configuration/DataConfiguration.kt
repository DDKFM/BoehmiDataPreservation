package de.ddkfm.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import kotlin.system.exitProcess

object DataConfiguration {
    val yamlObjectMapper by lazy { ObjectMapper(YAMLFactory()).registerModule(KotlinModule()) }
    val config by lazy {
        val configFile = File("./config.yaml")
        return@lazy try {
            yamlObjectMapper.readValue(configFile.inputStream(), Configuration::class.java)
        } catch (e : Exception) {
            println("error while reading the configuration")
            e.printStackTrace()
            exitProcess(-1)
        }
    }
}
