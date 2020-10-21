package de.ddkfm.configuration

data class LocationConfiguration(
    var lucene : String? = null,
    var gifs : String? = "./gifs",
    var videos : String? = "./videos"
)
