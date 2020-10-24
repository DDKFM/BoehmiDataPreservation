package de.ddkfm.configuration

data class LocationConfiguration(
    var lucene : String? = null,
    var gifs : String = "/data/gifs",
    var videos : String = "/data/videos",
    var icons: String = "classpath:static/icons"
)
